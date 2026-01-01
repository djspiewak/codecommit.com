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
import scala.jdk.CollectionConverters.*

object Build extends IOApp:

  val outputDir = "_site"

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
