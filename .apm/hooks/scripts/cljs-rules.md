# ClojureScript rules from clojurescript-skills

This is a ClojureScript project. The Iron Laws below come from the `clojurescript` skill and add to the `clojure` baseline laws. Never write code that breaks one. Before writing, reviewing, or refactoring code in this project, load the `clojurescript` skill along with the `clojure` skill. The skills hold the full laws, their exceptions, and the references.

## Iron Laws

- CLJS-1: Keep JavaScript property names safe from `:advanced` renaming. Turn on externs inference, and clear every "Cannot infer target type" warning with `^js`, or `^clj` for ClojureScript values.
- CLJS-2: Never put untrusted strings in HTML or URL sinks such as `innerHTML`, `dangerouslySetInnerHTML`, or a user-controlled `href`. Set `textContent`, sanitize HTML with a maintained sanitizer, and allow only expected URL schemes.
- CLJS-3: Test the release build, not the dev build, because renaming and missing externs appear only under `:advanced` optimization.
- CLJS-4: `js->clj` keeps string keys unless you pass `:keywordize-keys true`.
- CLJS-5: Hint `^boolean` only on values that are always `true` or `false`, because the hint makes `0` and `""` falsy.
