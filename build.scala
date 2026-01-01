//> using scala 3.3.4
//> using dep org.typelevel::laika-io:1.3.2
//> using dep org.typelevel::cats-effect:3.6.3
//> using dep org.http4s::http4s-ember-server:0.23.33
//> using dep co.fs2::fs2-io:3.12.2
//> using javaOpt --enable-native-access=ALL-UNNAMED

import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path, CopyFlag, CopyFlags}
import laika.api.*
import laika.api.bundle.*
import laika.format.*
import laika.io.syntax.*
import laika.config.*
import laika.ast.Path.Root
import laika.ast.*
import laika.helium.Helium
import laika.helium.config.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.*
import com.comcast.ip4s.*
import java.time.LocalDate
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import cats.data.NonEmptySet

object Build extends IOApp:

  // Path to the syntect-based highlighter library
  val highlighterLib =
    val os = System.getProperty("os.name").toLowerCase
    val ext = if os.contains("linux") then "so" else if os.contains("mac") then "dylib" else "dll"
    java.nio.file.Path.of(s"highlighter/target/release/libhighlighter.$ext")

  def escapeHtml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  // FFI highlighter using Java FFM
  class Highlighter private[Build] (
    handle: MemorySegment,
    freeFn: MethodHandle,
    highlightFn: MethodHandle,
    freeStringFn: MethodHandle
  ):
    private def free(): Unit = freeFn.invoke(handle)

    def highlight(language: String, code: String): String =
      // Allocate C strings in a confined arena for this call
      val callArena = Arena.ofConfined()
      try
        val langPtr = callArena.allocateFrom(language)
        val codePtr = callArena.allocateFrom(code)
        val resultPtr = highlightFn.invoke(handle, langPtr, codePtr).asInstanceOf[MemorySegment]
        if resultPtr == MemorySegment.NULL then
          s"""<pre><code class="$language">${escapeHtml(code)}</code></pre>"""
        else
          val len = Highlighter.strlen.invoke(resultPtr).asInstanceOf[Long]
          val html = resultPtr.reinterpret(len + 1).getString(0)
          freeStringFn.invoke(resultPtr)
          html
      finally
        callArena.close()

  object Highlighter:
    private val linker = Linker.nativeLinker()
    private val lookup = SymbolLookup.libraryLookup(highlighterLib, Arena.global())

    private val newFn = linker.downcallHandle(
      lookup.find("highlighter_new").orElseThrow(),
      FunctionDescriptor.of(ValueLayout.ADDRESS)
    )

    private val freeFn = linker.downcallHandle(
      lookup.find("highlighter_free").orElseThrow(),
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    )

    private val highlightFn = linker.downcallHandle(
      lookup.find("highlighter_highlight").orElseThrow(),
      FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    )

    private val freeStringFn = linker.downcallHandle(
      lookup.find("highlighter_free_string").orElseThrow(),
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    )

    private val strlen = linker.downcallHandle(
      linker.defaultLookup().find("strlen").orElseThrow(),
      FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    )

    val resource: Resource[IO, Highlighter] =
      Resource.make(
        IO {
          val handle = newFn.invoke().asInstanceOf[MemorySegment]
          if handle == MemorySegment.NULL then
            throw new RuntimeException("Failed to create highlighter")
          new Highlighter(handle, freeFn, highlightFn, freeStringFn)
        }
      )(h => IO(h.free()))

  // Extension bundle for syntect highlighting using render overrides
  class SyntectHighlighting(highlighter: Highlighter) extends ExtensionBundle:
    val description = "Syntect-based syntax highlighting"

    override def renderOverrides: Seq[RenderOverrides] = Seq(
      HTML.Overrides {
        case (_, cb: CodeBlock) =>
          val language = cb.language
          val code = cb.extractText
          highlighter.highlight(language, code)
      }
    )

  val outputDir = "_site"
  val srcDir = Path("src")
  val blogDir = srcDir / "blog"

  case class BlogPost(title: String, date: LocalDate, category: String, slug: String):
    def url: String = s"/blog/$category/$slug"  // Absolute path, clean URL

  // Parse a blog post file to extract metadata
  def parsePost(file: Path, content: String): Option[BlogPost] =
    val titlePattern = """laika\.title\s*=\s*"([^"]+)"""".r
    val datePattern = """laika\.metadata\.date\s*=\s*"(\d{4}-\d{2}-\d{2})"""".r

    for
      titleMatch <- titlePattern.findFirstMatchIn(content)
      dateMatch <- datePattern.findFirstMatchIn(content)
    yield
      val title = titleMatch.group(1)
      val date = LocalDate.parse(dateMatch.group(1))
      val category = file.parent.map(_.fileName.toString).getOrElse("")
      val slug = file.fileName.toString.stripSuffix(".md")
      BlogPost(title, date, category, slug)

  // Scan blog directory for all posts
  def scanPosts: Stream[IO, BlogPost] =
    Files[IO].walk(blogDir)
      .filter(_.extName == ".md")
      .filterNot(_.fileName.toString == "index.md")
      .evalFilter(Files[IO].isRegularFile)
      .flatMap { path =>
        Files[IO].readUtf8(path)
          .foldMonoid
          .map(content => parsePost(path, content))
          .unNone
      }

  // Generate blog index markdown with raw HTML links
  def generateBlogIndex(posts: List[BlogPost]): String =
    val header = """{%
laika.title = "Blog"
%}

# Blog

"""
    val postsByYear = posts.groupBy(_.date.getYear).toList.sortBy(-_._1)
    val postList = postsByYear.map { (year, yearPosts) =>
      val yearHeader = s"## $year\n\n"
      val links = yearPosts.map { post =>
        val escapedTitle = post.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        s"""<p><a href="${post.url}">$escapedTitle</a> — ${post.date}</p>"""
      }.mkString("\n")
      yearHeader + links
    }.mkString("\n\n")

    header + postList + "\n"

  // Write blog index before build
  def writeBlogIndex: IO[Unit] =
    scanPosts
      .compile
      .toList
      .map(_.sortBy(_.date)(Ordering[LocalDate].reverse))
      .map(generateBlogIndex)
      .flatMap { indexContent =>
        Stream.emit(indexContent)
          .through(fs2.text.utf8.encode)
          .through(Files[IO].writeAll(blogDir / "index.md"))
          .compile
          .drain
      }

  // Convert slug.html to slug/index.html for clean URLs
  def convertToCleanUrls(dir: Path): IO[Unit] =
    Files[IO].walk(dir)
      .filter(_.extName == ".html")
      .evalFilter(Files[IO].isRegularFile)
      .filterNot(_.fileName.toString == "index.html")
      .evalMap { htmlFile =>
        val baseName = htmlFile.fileName.toString.stripSuffix(".html")
        val newDir = htmlFile.parent.map(_ / baseName).getOrElse(Path(baseName))
        val newFile = newDir / "index.html"
        Files[IO].createDirectories(newDir) *>
          Files[IO].move(htmlFile, newFile, CopyFlags(CopyFlag.ReplaceExisting))
      }
      .compile
      .drain

  val helium = Helium.defaults
    .site.metadata(
      title = Some("Code Commit"),
      authors = Seq("Daniel Spiewak"),
      language = Some("en")
    )
    .site.topNavigationBar(
      homeLink = IconLink.internal(Root / "index.md", HeliumIcon.home),
      navLinks = Seq(
        TextLink.internal(Root / "about.md", "About"),
        TextLink.internal(Root / "blog" / "index.md", "Blog")
      )
    )
    .site.footer(
      "© Daniel Spiewak"
    )
    .build

  // Use lenient message handling - don't fail on warnings/errors, don't render them
  val lenientFilters = MessageFilters.custom(MessageFilter.None, MessageFilter.None)

  def transformer(highlighter: Highlighter) = Transformer
    .from(Markdown)
    .to(HTML)
    .using(Markdown.GitHubFlavor)
    .using(SyntectHighlighting(highlighter))
    .withRawContent
    .withMessageFilters(lenientFilters)
    .parallel[IO]
    .withTheme(helium)
    .build

  def build: IO[Unit] =
    writeBlogIndex *>
    Highlighter.resource.flatMap(transformer).use { t =>
      t.fromDirectory("src")
        .toDirectory(outputDir)
        .transform
    }.void *> convertToCleanUrls(Path(outputDir))

  def serve(port: Port): IO[Unit] =
    val routes = Router("/" -> fileService[IO](FileService.Config(outputDir))).orNotFound
    EmberServerBuilder.default[IO]
      .withHost(host"localhost")
      .withPort(port)
      .withHttpApp(routes)
      .build
      .useForever

  def run(args: List[String]): IO[ExitCode] =
    args match
      case "serve" :: rest =>
        val port = rest.headOption.flatMap(Port.fromString).getOrElse(port"4242")
        build *>
          IO.println(s"Serving site at http://localhost:$port") *>
          IO.println("Press Ctrl+C to stop") *>
          serve(port).as(ExitCode.Success)
      case _ =>
        build.as(ExitCode.Success)
