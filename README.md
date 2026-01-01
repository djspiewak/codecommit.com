# codecommit.com

Static blog built with [Laika](https://typelevel.org/Laika/) and [syntect](https://github.com/trishume/syntect).

## Dependencies

- Rust toolchain (stable)
- [Scala CLI](https://scala-cli.virtuslab.org/) with JVM 22+ (required for Java FFM)

## Build

```bash
# Build the syntax highlighter (Rust FFI library)
cargo build --release --manifest-path highlighter/Cargo.toml

# Build the site (outputs to _site/)
scala-cli run build.scala
```

## Serve locally

```bash
scala-cli run build.scala -- serve        # http://localhost:4242
scala-cli run build.scala -- serve 8080   # custom port
```

Note that this does not monitor for source changes, it only serves `_site/`. You'll need to run `scala-cli run build.scala` in order to update for any changes, including to the markdown. You might be able to approximate `~` behavior here with the watch flag: `scala-cli run build.scala -w` (but this might not pick up the markdown)

## Adding a blog post

1. Create `src/blog/<category>/<slug>.md` (categories: `scala`, `java`, `ruby`, `eclipse`, etc.)

2. Add frontmatter and content:

```markdown
{%
laika.title = "Your Post Title"
laika.metadata.date = "2026-01-15"
%}

# Your Post Title

Post content in markdown...
```

3. Commit and push
