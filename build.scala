//> using scala 3.3.4
//> using dep org.typelevel::laika-io:1.3.2
//> using dep org.typelevel::cats-effect:3.6.3
//> using dep org.http4s::http4s-ember-server:0.23.33
//> using javaOpt --enable-native-access=ALL-UNNAMED

import cats.effect.*
import laika.api.*
import laika.api.bundle.*
import laika.format.*
import laika.io.syntax.*
import laika.config.*
import laika.ast.Path.Root
import laika.ast.*
import laika.theme.Theme
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.*
import com.comcast.ip4s.*
import java.time.LocalDate
import java.lang.foreign.*
import java.lang.invoke.MethodHandle

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

  // Extension bundle providing the @:blogIndex directive
  object BlogIndexDirective extends DirectiveRegistry:
    import BlockDirectives.dsl.*
    import laika.api.config.Key

    case class BlogPost(title: String, date: LocalDate, path: laika.ast.Path)

    val blockDirectives = Seq(
      BlockDirectives.create("blogIndex") {
        cursor.map { docCursor =>
          // Find the blog subtree from root
          val blogPath = Root / "blog"
          val allDocs = docCursor.root.target.tree.allDocuments

          // Extract posts from documents under /blog (excluding index pages)
          val posts = allDocs.flatMap { doc =>
            if doc.path.isSubPath(blogPath) && doc.path.basename != "index" then
              for
                title <- doc.config.get[String](Key("laika", "title")).toOption
                dateStr <- doc.config.get[String](Key("laika", "metadata", "date")).toOption
                date <- scala.util.Try(LocalDate.parse(dateStr)).toOption
              yield BlogPost(title, date, doc.path)
            else
              None
          }

          // Group by year, sorted descending
          val postsByYear = posts
            .sortBy(_.date)(Ordering[LocalDate].reverse)
            .groupBy(_.date.getYear)
            .toList
            .sortBy(-_._1)

          // Generate AST blocks
          val blocks = postsByYear.flatMap { (year, yearPosts) =>
            val yearHeader = Header(2, Seq(Text(year.toString)))
            val postBlocks = yearPosts.map { post =>
              Paragraph(Seq(
                SpanLink.internal(post.path)(post.title),
                Text(s" — ${post.date}")
              ))
            }
            yearHeader +: postBlocks
          }

          BlockSequence(blocks)
        }
      }
    )

    val spanDirectives = Nil
    val templateDirectives = Nil
    val linkDirectives = Nil

  val outputDir = "_site"

  // Use a custom theme - templates and CSS are provided in src/templates and src/theme
  val codeCommitTheme = Theme.empty

  // Use lenient message handling - don't fail on warnings/errors, don't render them
  val lenientFilters = MessageFilters.custom(MessageFilter.None, MessageFilter.None)

  def transformer(highlighter: Highlighter) = Transformer
    .from(Markdown)
    .to(HTML)
    .using(Markdown.GitHubFlavor)
    .using(SyntectHighlighting(highlighter))
    .using(BlogIndexDirective)
    .using(PrettyURLs)
    .withRawContent
    .withMessageFilters(lenientFilters)
    .parallel[IO]
    .withTheme(codeCommitTheme)
    .build

  def build: IO[Unit] =
    Highlighter.resource.flatMap(transformer).use { t =>
      t.fromDirectory("src")
        .toDirectory(outputDir)
        .transform
    }.void

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
