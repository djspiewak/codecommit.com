use std::ffi::{c_char, CStr, CString};
use std::ptr;
use syntect::highlighting::ThemeSet;
use syntect::html::highlighted_html_for_string;
use syntect::parsing::SyntaxSet;

pub struct Highlighter {
    syntax_set: SyntaxSet,
    theme_set: ThemeSet,
}

fn html_escape(s: &str) -> String {
    s.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
}

/// Map language tokens to display names for the badge.
/// Falls back to the syntect-detected name if no override exists.
fn language_display_name<'a>(token: &'a str, syntect_name: &'a str) -> &'a str {
    match token.to_lowercase().as_str() {
        "js" | "javascript" => "JavaScript",
        "ts" | "typescript" => "TypeScript",
        "rb" | "ruby" => "Ruby",
        "py" | "python" => "Python",
        "rs" | "rust" => "Rust",
        "sh" | "bash" | "shell" => "Shell",
        "cs" | "csharp" => "C#",
        "cpp" | "c++" => "C++",
        "md" | "markdown" => "Markdown",
        "yml" | "yaml" => "YAML",
        "hs" | "haskell" => "Haskell",
        "ml" | "ocaml" => "OCaml",
        "ex" | "elixir" => "Elixir",
        "erl" | "erlang" => "Erlang",
        "kt" | "kotlin" => "Kotlin",
        "sc" | "scala" => "Scala",
        "clj" | "clojure" => "Clojure",
        "fs" | "fsharp" => "F#",
        "go" | "golang" => "Go",
        "java" => "Java",
        "c" => "C",
        "sql" => "SQL",
        "html" => "HTML",
        "css" => "CSS",
        "xml" => "XML",
        "json" => "JSON",
        "toml" => "TOML",
        "text" | "plaintext" | "plain text" => "Text",
        _ => syntect_name,
    }
}

/// Create a new highlighter instance. Returns null on failure.
#[no_mangle]
pub extern "C" fn highlighter_new() -> *mut Highlighter {
    let h = Box::new(Highlighter {
        syntax_set: SyntaxSet::load_defaults_newlines(),
        theme_set: ThemeSet::load_defaults(),
    });
    Box::into_raw(h)
}

/// Free a highlighter instance.
#[no_mangle]
pub extern "C" fn highlighter_free(h: *mut Highlighter) {
    if !h.is_null() {
        unsafe {
            drop(Box::from_raw(h));
        }
    }
}

/// Highlight code and return HTML. Caller must free the result with highlighter_free_string.
/// Returns null on failure.
#[no_mangle]
pub extern "C" fn highlighter_highlight(
    h: *mut Highlighter,
    language: *const c_char,
    code: *const c_char,
) -> *mut c_char {
    if h.is_null() || language.is_null() || code.is_null() {
        return ptr::null_mut();
    }

    let highlighter = unsafe { &*h };

    let language = match unsafe { CStr::from_ptr(language) }.to_str() {
        Ok(s) => s,
        Err(_) => return ptr::null_mut(),
    };

    let code = match unsafe { CStr::from_ptr(code) }.to_str() {
        Ok(s) => s,
        Err(_) => return ptr::null_mut(),
    };

    let syntax = highlighter
        .syntax_set
        .find_syntax_by_token(language)
        .or_else(|| highlighter.syntax_set.find_syntax_by_extension(language))
        .unwrap_or_else(|| highlighter.syntax_set.find_syntax_plain_text());

    let theme = &highlighter.theme_set.themes["InspiredGitHub"];

    // Get a display name for the language badge
    let display_name = language_display_name(language, syntax.name.as_str());

    let inner_html = match highlighted_html_for_string(code, &highlighter.syntax_set, syntax, theme) {
        Ok(h) => h,
        Err(_) => format!("<pre><code>{}</code></pre>", html_escape(code)),
    };

    // Wrap in container with language badge
    let html = format!(
        r#"<div class="code-block"><span class="code-lang-badge">{}</span>{}</div>"#,
        html_escape(display_name),
        inner_html
    );

    match CString::new(html) {
        Ok(cs) => cs.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

/// Free a string returned by highlighter_highlight.
#[no_mangle]
pub extern "C" fn highlighter_free_string(s: *mut c_char) {
    if !s.is_null() {
        unsafe {
            drop(CString::from_raw(s));
        }
    }
}
