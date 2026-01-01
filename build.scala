//> using scala 3.3.4
//> using dep org.typelevel::laika-io:1.3.2
//> using dep org.typelevel::cats-effect:3.6.3
//> using dep org.http4s::http4s-ember-server:0.23.33

import cats.effect.*
import laika.api.*
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
import java.nio.file.{Files, Path as JPath, StandardCopyOption}
import java.time.LocalDate
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

object Build extends IOApp:

  val outputDir = "_site"
  val srcDir = JPath.of("src")
  val blogDir = srcDir.resolve("blog")

  case class BlogPost(title: String, date: LocalDate, category: String, slug: String):
    def url: String = s"/blog/$category/$slug"  // Absolute path, clean URL

  // Parse a blog post file to extract metadata
  def parsePost(file: JPath): Option[BlogPost] =
    val content = Files.readString(file)
    val titlePattern = """laika\.title\s*=\s*"([^"]+)"""".r
    val datePattern = """laika\.metadata\.date\s*=\s*"(\d{4}-\d{2}-\d{2})"""".r
    
    for
      titleMatch <- titlePattern.findFirstMatchIn(content)
      dateMatch <- datePattern.findFirstMatchIn(content)
    yield
      val title = titleMatch.group(1)
      val date = LocalDate.parse(dateMatch.group(1))
      val category = file.getParent.getFileName.toString
      val slug = file.getFileName.toString.stripSuffix(".md")
      BlogPost(title, date, category, slug)

  // Scan blog directory for all posts
  def scanPosts: IO[List[BlogPost]] = IO {
    Files.walk(blogDir).iterator().asScala.toList
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".md"))
      .filterNot(p => p.getFileName.toString == "index.md")
      .flatMap(parsePost)
      .sortBy(_.date)(Ordering[LocalDate].reverse)
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
    scanPosts.flatMap { posts =>
      IO {
        val indexContent = generateBlogIndex(posts)
        Files.writeString(blogDir.resolve("index.md"), indexContent)
      }
    }

  // Convert slug.html to slug/index.html for clean URLs
  def convertToCleanUrls(dir: JPath): IO[Unit] = IO {
    Files.walk(dir).iterator().asScala.toList
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".html"))
      .filterNot(p => p.getFileName.toString == "index.html")
      .foreach { htmlFile =>
        val fileName = htmlFile.getFileName.toString
        val baseName = fileName.stripSuffix(".html")
        val newDir = htmlFile.getParent.resolve(baseName)
        val newFile = newDir.resolve("index.html")
        Files.createDirectories(newDir)
        Files.move(htmlFile, newFile, StandardCopyOption.REPLACE_EXISTING)
      }
  }

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
    }.void *> convertToCleanUrls(JPath.of(outputDir))

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
