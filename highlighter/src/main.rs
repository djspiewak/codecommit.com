use serde::{Deserialize, Serialize};
use std::io::{self, BufRead, Write};
use syntect::highlighting::ThemeSet;
use syntect::html::highlighted_html_for_string;
use syntect::parsing::SyntaxSet;

#[derive(Deserialize)]
struct Request {
    language: String,
    code: String,
}

#[derive(Serialize)]
struct Response {
    html: String,
}

fn main() {
    // Load syntax and theme sets once at startup
    let ss = SyntaxSet::load_defaults_newlines();
    let ts = ThemeSet::load_defaults();
    let theme = &ts.themes["InspiredGitHub"];

    let stdin = io::stdin();
    let mut stdout = io::stdout();

    // Process JSON lines from stdin
    for line in stdin.lock().lines() {
        let line = match line {
            Ok(l) => l,
            Err(_) => break,
        };

        if line.is_empty() {
            continue;
        }

        let request: Request = match serde_json::from_str(&line) {
            Ok(r) => r,
            Err(e) => {
                let resp = Response {
                    html: format!("<pre><code>Error parsing request: {}</code></pre>", e),
                };
                let _ = writeln!(stdout, "{}", serde_json::to_string(&resp).unwrap());
                let _ = stdout.flush();
                continue;
            }
        };

        let syntax = ss
            .find_syntax_by_token(&request.language)
            .or_else(|| ss.find_syntax_by_extension(&request.language))
            .unwrap_or_else(|| ss.find_syntax_plain_text());

        let html = match highlighted_html_for_string(&request.code, &ss, syntax, theme) {
            Ok(h) => h,
            Err(_) => format!(
                "<pre><code>{}</code></pre>",
                html_escape(&request.code)
            ),
        };

        let resp = Response { html };
        let _ = writeln!(stdout, "{}", serde_json::to_string(&resp).unwrap());
        let _ = stdout.flush();
    }
}

fn html_escape(s: &str) -> String {
    s.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
}
