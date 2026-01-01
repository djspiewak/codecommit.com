//> using scala 3.3.4
//> using dep org.typelevel::laika-io:1.3.2
//> using dep org.typelevel::cats-effect:3.6.3
//> using dep org.http4s::http4s-ember-server:0.23.33
//> using javaOpt --enable-native-access=ALL-UNNAMED

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import laika.api.*
import laika.api.bundle.*
import laika.format.*
import laika.io.syntax.*
import laika.io.model.*
import laika.config.*
import laika.api.config.Key
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

  // Shared calendar widget HTML generation
  object CalendarWidget:
    private val months = Vector(
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    def html(date: LocalDate): String =
      val day = date.getDayOfMonth
      val month = months(date.getMonthValue - 1)
      val year = date.getYear
      s"""<div class="calendar-widget">
         |  <span class="calendar-day">$day</span>
         |  <span class="calendar-month">$month</span>
         |  <span class="calendar-year">$year</span>
         |</div>""".stripMargin

    def block(date: LocalDate): RawContent =
      RawContent(cats.data.NonEmptySet.of("html"), html(date))

  // Extension bundle that adds a calendar date widget to blog posts
  object CalendarWidgetExtension extends ExtensionBundle:
    val description = "Adds a calendar date widget to blog posts"

    override def rewriteRules: RewriteRules.RewritePhaseBuilder = {
      case RewritePhase.Resolve =>
        Seq { cursor =>
          val docPath = cursor.path
          val isBlogPost = docPath.isSubPath(Root / "blog") && docPath.basename != "index"

          if !isBlogPost then
            Right(RewriteRules.empty)
          else
            // Try to get the date from document config
            val dateOpt = for
              dateStr <- cursor.config.get[String](Key("laika", "metadata", "date")).toOption
              date <- scala.util.Try(LocalDate.parse(dateStr)).toOption
            yield date

            dateOpt match
              case Some(date) =>
                // Insert the calendar widget after the first H1 (title)
                var inserted = false
                Right(RewriteRules.forBlocks {
                  case h: Header if h.level == 1 && !inserted =>
                    inserted = true
                    RewriteAction.Replace(BlockSequence(h, CalendarWidget.block(date)))
                })
              case None =>
                Right(RewriteRules.empty)
        }
    }

  // Extension bundle providing blog directives
  class BlogDirectives(categories: Seq[Category]) extends DirectiveRegistry:
    import laika.api.config.Key

    private val postsPerPage = 10

    case class BlogPost(title: String, date: LocalDate, path: laika.ast.Path, firstParagraphs: Seq[Block])

    // Extract enough text content from paragraphs (target ~150 chars minimum)
    private def extractParagraphs(content: RootElement): Seq[Block] =
      val paragraphs = content.collect { case p: Paragraph => p }
      var charCount = 0
      val minChars = 150
      paragraphs.takeWhile { p =>
        val take = charCount < minChars
        charCount += p.extractText.length
        take
      }

    // Helper to generate post entry HTML blocks
    private def generatePostBlocks(posts: Vector[BlogPost]): Vector[Block] =
      posts.flatMap { post =>
        val calendarBlock = CalendarWidget.block(post.date)
        val linkPath = "/" + post.path.withSuffix("html").relative.toString.replace(".html", "/")
        val titleHtml = s"""<h2 class="blog-index-title"><a href="$linkPath">${escapeHtml(post.title)}</a></h2>"""
        val titleBlock = RawContent(cats.data.NonEmptySet.of("html"), titleHtml)
        val wrapperStart = RawContent(cats.data.NonEmptySet.of("html"), """<article class="blog-index-entry">""")
        val bodyStart = RawContent(cats.data.NonEmptySet.of("html"), """<div class="blog-index-body">""")
        val textStart = RawContent(cats.data.NonEmptySet.of("html"), """<div class="blog-index-body-text">""")
        val textEnd = RawContent(cats.data.NonEmptySet.of("html"), """</div>""")
        val bodyEnd = RawContent(cats.data.NonEmptySet.of("html"), """</div></article>""")

        val ellipsisSpan = SpanLink.internal(post.path)(Text(" …")).withStyle("read-more")
        val paragraphsWithEllipsis = if post.firstParagraphs.nonEmpty then
          val lastPara = post.firstParagraphs.last match
            case p: Paragraph => Paragraph(p.content :+ ellipsisSpan)
            case other => other
          post.firstParagraphs.init :+ lastPara
        else
          post.firstParagraphs

        Vector(wrapperStart, titleBlock, bodyStart, calendarBlock, textStart) ++ paragraphsWithEllipsis ++ Vector(textEnd, bodyEnd)
      }

    val blockDirectives = Seq(
      // @:blogIndex - lists all blog posts with pagination
      BlockDirectives.create("blogIndex") {
        import BlockDirectives.dsl.*
        (attribute(0).as[Int].optional, cursor).mapN { (pageOpt, docCursor) =>
          val currentPage = pageOpt.getOrElse(1)
          val blogPath = Root / "blog"
          val allDocs = docCursor.root.target.tree.allDocuments

          val posts = allDocs.flatMap { doc =>
            if doc.path.isSubPath(blogPath) && doc.path.basename != "index" then
              for
                title <- doc.config.get[String](Key("laika", "title")).toOption
                dateStr <- doc.config.get[String](Key("laika", "metadata", "date")).toOption
                date <- scala.util.Try(LocalDate.parse(dateStr)).toOption
              yield BlogPost(title, date, doc.path, extractParagraphs(doc.content))
            else
              None
          }.toVector.sortBy(_.date)(Ordering[LocalDate].reverse)

          val totalPosts = posts.size
          val totalPages = (totalPosts + postsPerPage - 1) / postsPerPage
          val startIdx = (currentPage - 1) * postsPerPage
          val pagePosts = posts.slice(startIdx, startIdx + postsPerPage)
          val postBlocks = generatePostBlocks(pagePosts)

          val paginationHtml = if totalPages > 1 then
            val pageLinks = (1 to totalPages).map { p =>
              if p == currentPage then
                s"""<span class="pagination-current">$p</span>"""
              else
                val href = if p == 1 then "/blog/" else s"/blog/page/$p/"
                s"""<a href="$href" class="pagination-link">$p</a>"""
            }.mkString(" ")

            val prevLink = if currentPage > 1 then
              val href = if currentPage == 2 then "/blog/" else s"/blog/page/${currentPage - 1}/"
              s"""<a href="$href" class="pagination-prev">« Previous</a>"""
            else ""

            val nextLink = if currentPage < totalPages then
              s"""<a href="/blog/page/${currentPage + 1}/" class="pagination-next">Next »</a>"""
            else ""

            s"""<nav class="pagination">$prevLink $pageLinks $nextLink</nav>"""
          else ""

          val paginationBlock = RawContent(cats.data.NonEmptySet.of("html"), paginationHtml)
          BlockSequence(postBlocks :+ paginationBlock)
        }
      },

      // @:categoryIndex(slug) - lists posts in a specific category
      BlockDirectives.create("categoryIndex") {
        import BlockDirectives.dsl.*
        (attribute(0).as[String], cursor).mapN { (categorySlug, docCursor) =>
          val categoryPath = Root / "blog" / categorySlug
          val allDocs = docCursor.root.target.tree.allDocuments

          val posts = allDocs.flatMap { doc =>
            if doc.path.isSubPath(categoryPath) && doc.path.basename != "index" then
              for
                title <- doc.config.get[String](Key("laika", "title")).toOption
                dateStr <- doc.config.get[String](Key("laika", "metadata", "date")).toOption
                date <- scala.util.Try(LocalDate.parse(dateStr)).toOption
              yield BlogPost(title, date, doc.path, extractParagraphs(doc.content))
            else
              None
          }.toVector.sortBy(_.date)(Ordering[LocalDate].reverse)

          BlockSequence(generatePostBlocks(posts))
        }
      }
    )

    val spanDirectives = Nil

    // @:categoryList - generates category links for the sidebar
    val templateDirectives = Seq(
      TemplateDirectives.create("categoryList") {
        import TemplateDirectives.dsl.*
        TemplateDirectives.dsl.cursor.map { _ =>
          val listItems = categories.map { cat =>
            s"""<li><a href="/blog/${cat.slug}/">${cat.name}</a> <span class="category-count">(${cat.postCount})</span></li>"""
          }.mkString("\n        ")
          TemplateString(s"""<ul>
        $listItems
      </ul>""")
        }
      }
    )

    val linkDirectives = Nil

  // Count blog posts to determine pagination needs
  def countBlogPosts(srcDir: Path): IO[Int] =
    val blogDir = srcDir / "blog"
    Files[IO].exists(blogDir).flatMap:
      case false => IO.pure(0)
      case true =>
        Files[IO].walk(blogDir)
          .evalFilter(p => Files[IO].isRegularFile(p))
          .filter(p => p.extName == ".md" && p.fileName.toString != "index.md")
          .compile
          .count
          .map(_.toInt)

  // Detect categories from blog subdirectories (directories containing .md files)
  case class Category(name: String, slug: String, postCount: Int)

  // Special display names for categories where slug.capitalize isn't correct
  val categoryDisplayNames = Map(
    "net" -> ".Net"
  )

  def detectCategories(srcDir: Path): IO[Seq[Category]] =
    val blogDir = srcDir / "blog"
    Files[IO].exists(blogDir).flatMap:
      case false => IO.pure(Seq.empty)
      case true =>
        Files[IO].list(blogDir)
          .evalFilter(p => Files[IO].isDirectory(p))
          .evalMapFilter { catDir =>
            val slug = catDir.fileName.toString
            // Skip special directories
            if slug == "page" || slug == "misc" then IO.pure(None)
            else
              Files[IO].list(catDir)
                .evalFilter(p => Files[IO].isRegularFile(p))
                .filter(p => p.extName == ".md" && p.fileName.toString != "index.md")
                .compile
                .count
                .map { postCount =>
                  if postCount > 0 then
                    val name = categoryDisplayNames.getOrElse(slug, slug.capitalize)
                    Some(Category(name, slug, postCount.toInt))
                  else None
                }
          }
          .compile
          .toList
          .map(_.sortBy(-_.postCount)) // Sort by post count descending

  // Generate virtual pagination pages content
  def paginationPageContent(page: Int): String =
    s"""{%
       |laika.title = "Blog - Page $page"
       |%}
       |
       |@:blogIndex($page)
       |""".stripMargin

  // Generate virtual category index page content
  def categoryIndexContent(category: Category): String =
    s"""{%
       |laika.title = "${category.name}"
       |%}
       |
       |# ${category.name}
       |
       |@:categoryIndex(${category.slug})
       |""".stripMargin

  val outputDir = "_site"

  // Use a custom theme - templates and CSS are provided in src/templates and src/theme
  val codeCommitTheme = Theme.empty

  def transformer(highlighter: Highlighter, categories: Seq[Category]) = Transformer
    .from(Markdown)
    .to(HTML)
    .using(Markdown.GitHubFlavor)
    .using(SyntectHighlighting(highlighter))
    .using(BlogDirectives(categories))
    .using(CalendarWidgetExtension)
    .using(PrettyURLs)
    .withRawContent
    .parallel[IO]
    .withTheme(codeCommitTheme)
    .build

  def build: IO[Unit] =
    val srcDir = Path("src")
    val postsPerPage = 10

    for
      postCount <- countBlogPosts(srcDir)
      totalPages = (postCount + postsPerPage - 1) / postsPerPage
      categories <- detectCategories(srcDir)

      // Generate virtual pagination pages (page 2 and onwards)
      baseInput = InputTree[IO].addDirectory("src")

      // Add pagination pages
      withPagination = (2 to totalPages).foldLeft(baseInput) { (tree, page) =>
        val content = paginationPageContent(page)
        val path = Root / "blog" / "page" / page.toString / "index.md"
        tree.addString(content, path)
      }

      // Add category index pages
      combinedInputs = categories.foldLeft(withPagination) { (tree, category) =>
        val content = categoryIndexContent(category)
        val path = Root / "blog" / category.slug / "index.md"
        tree.addString(content, path)
      }

      _ <- Highlighter.resource.flatMap(h => transformer(h, categories)).use { t =>
        t.fromInput(combinedInputs)
          .toDirectory(outputDir)
          .transform
      }
    yield ()

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
