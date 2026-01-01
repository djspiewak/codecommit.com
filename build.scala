//> using scala 3.3.4
//> using dep org.typelevel::laika-io:1.3.2
//> using dep org.typelevel::cats-effect:3.6.3
//> using dep org.http4s::http4s-ember-server:0.23.33
//> using dep co.fs2::fs2-io:3.12.2

import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path, CopyFlag, CopyFlags}
import laika.api.*
import laika.format.*
import laika.io.syntax.*
import laika.config.*
import laika.ast.Path.Root
import laika.ast.*
import laika.helium.Helium
import laika.helium.config.*
import laika.config.SyntaxHighlighting
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.*
import com.comcast.ip4s.*
import java.time.LocalDate

object Build extends IOApp:

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

  val transformer = Transformer
    .from(Markdown)
    .to(HTML)
    .using(Markdown.GitHubFlavor)
    .using(SyntaxHighlighting)
    .withRawContent
    .withMessageFilters(lenientFilters)
    .parallel[IO]
    .withTheme(helium)
    .build

  def build: IO[Unit] =
    writeBlogIndex *>
    transformer.use { t =>
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
