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

    let html = match highlighted_html_for_string(code, &highlighter.syntax_set, syntax, theme) {
        Ok(h) => h,
        Err(_) => format!("<pre><code>{}</code></pre>", html_escape(code)),
    };

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
