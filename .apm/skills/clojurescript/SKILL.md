---
name: clojurescript
description: >-
  Use when writing, editing, reviewing, or discussing ClojureScript code that
  compiles to JavaScript and runs on a JS host (browser, Node.js, or other JS
  runtime). Triggers: .cljs files, .cljc files compiled to JS, shadow-cljs.edn,
  figwheel-main.edn, ClojureScript deps.edn projects, cljs.test, JavaScript
  interop (`js/`, `.-prop`, `(.method obj ...)`, `set!` on properties,
  `js-obj`, `clj->js`, `js->clj`, `goog.object/get`, `goog.object/set`,
  `aget`, `aset`), global references (`:refer-global`, `:require-global`),
  method values (`String/.toUpperCase`, `Date/new`), async functions
  (`^:async`, `await`, `deftest ^:async`, `js/Promise`), externs inference
  (`^js`, `^js/Foo`, `*warn-on-infer*`,
  `:infer-externs`), macro stage separation (`:require-macros`,
  `:refer-macros`, `:include-macros`), reader conditionals (`#?`, `#?@`,
  `:cljs`, `:default`), JS-flavored numerics (no `Ratio`, `BigInt`,
  `BigDecimal`; `(= 0.0 0)` is true), `^boolean` type hint, browser REPL,
  Node REPL, source maps, npm interop, the `cljs.main` CLI, shadow-cljs, or
  figwheel-main. Covers JavaScript interop, async functions, host-typed
  exception handling (`catch :default`, `js/Error`), externs and advanced
  compilation, macro stage separation, JS-flavored numerics and truthiness, host-specific
  aliases (`goog.object`, `goog.string`, `goog.dom`), and the ClojureScript
  project workflow.
user-invocable: false
---

# ClojureScript

Layered on top of the [clojure](https://github.com/brackendev/clojure-skills) skill, which defines host-neutral Clojure family guidance. This skill applies only to ClojureScript (`.cljs` files, `.cljc` files compiled to JS, ClojureScript `deps.edn` projects, `shadow-cljs.edn`, `figwheel-main.edn`). It overrides the baseline for JavaScript interop, externs inference, macro stage separation (`:require-macros`, `:refer-macros`, `:include-macros`), host-typed exception handling (`catch :default`, `js/Error`), JS-flavored numerics and truthiness, host-specific aliases (`goog.object`, `goog.string`, `goog.dom`), and the `cljs.main` project workflow.

JVM and ClojureDart deltas live in their own packages ([clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills), [clojuredart-skills](https://github.com/brackendev/clojuredart-skills)). Do not apply this skill's JS interop, externs, or `catch :default` rules to those dialects.

## Key Rules

1. **Property access uses a leading hyphen.** `(.-prop obj)` reads a property; `(set! (.-prop obj) v)` assigns it. `(.method obj args)` calls a method. The hyphen is what distinguishes property access from method invocation in ClojureScript.
2. **Globals live under the `js/` namespace unless the namespace refers them.** `js/document`, `js/window`, `js/Promise`, `js/Error`. Since ClojureScript 1.12.116, `(:refer-global :only [Promise])` in the `ns` form makes `Promise` usable without the prefix. On older compilers, `js/` is the only path to native globals.
3. **Catch with `:default` when you want to catch everything; otherwise name a JS type.** ClojureScript supports `(catch :default e ...)`. Prefer `js/Error` and its subclasses (`js/TypeError`, `js/RangeError`, `js/SyntaxError`) when the failure mode has a named JS type.
4. **Macros must be defined in `.clj` or `.cljc` files.** Macros and functions run in different compilation stages. A macro and a function may share the same name. Consumers load macros with `:require-macros`, `:refer-macros`, or `:include-macros`, or with a plain `:require` and `:refer` when the library namespace loads its own macros (implicit macro loading, which covers `cljs.test`).
5. **`:import` is only for Google Closure classes.** `(:import [goog Uri])` works; `(:import [java.util Date])` does not exist on this host. Use `(:require ...)` and `js/` for everything else.
6. **No `gen-class`, no refs, no agents, no STM.** Atoms are the only built-in state primitive. Concurrency primitives that depend on multiple OS threads do not exist; JS has a single execution thread per realm.
7. **Numbers are JavaScript numbers.** There is no `Ratio`, `BigDecimal`, or `BigInt` literal. `(= 0.0 0)` evaluates to `true`. Use `js/BigInt` directly when arbitrary-precision integers are required.
8. **Use `^boolean` to avoid checked-`if`.** It is the only type hint with runtime significance for code generation, and it suppresses the runtime check that copes with JavaScript's wider falsy set (`0`, `""`, `NaN`, `null`, `undefined`).
9. **Use `^js` and `^js/Foo.Bar` to drive externs inference for advanced compilation.** Without hints on foreign JavaScript values, the Google Closure Compiler may rename property names and break interop at advanced optimization levels.
10. **Convert at the boundary, not at every use.** Use `js->clj` and `clj->js` once at the interop boundary to translate between persistent collections and native JS objects, then operate on Clojure data inside the namespace.
11. **Use `^:async` and `await` for Promise-based code on ClojureScript 1.12.145 or later.** Put `^:async` on the function name or on the `fn` symbol, never on the argument vector. `await` is valid only directly inside an `^:async` function.

## References

The Key Rules are the short form. Each reference file below holds the detailed guidance and the examples for one topic. Before writing, reviewing, or refactoring ClojureScript code, read every reference whose topic the task touches, and read the next one when the work moves to a new topic.

| When the task involves | Read |
|------------------------|------|
| JavaScript interop: properties, methods, globals, constructors, native objects, `clj->js` and `js->clj`, type hints, externs inference, or the `goog` aliases | `references/interop.md` |
| Async functions, promises, `await`, or catching and throwing JavaScript errors | `references/async-and-errors.md` |
| Macros and compilation stages, `ns` forms and requires, or reader conditionals in `.cljc` files | `references/compilation.md` |
| Numbers, truthiness and `^boolean`, characters, atoms, or vars on a JavaScript host | `references/host-semantics.md` |
| Writing or running `cljs.test` tests, including async tests | `references/testing.md` |
| The `cljs.main` CLI, shadow-cljs, figwheel-main, builds, or the REPL | `references/project-workflows.md` |

## Project Workflow

CLI commands (`cljs.main`), project layout, `deps.edn` configuration for ClojureScript, the browser REPL, the Node REPL, source maps, advanced compilation, externs handling, npm interop patterns, and notes on the shadow-cljs and figwheel-main community alternatives live in `references/project-workflows.md`. Load it on demand.

## Gotchas

### `*out*` and `*err*` are not implemented

Use `(.log js/console ...)` or `(println ...)` directly. Bindings around `*out*` that work on the JVM do nothing on ClojureScript.

### `cljs.reader` for `read` and `read-string`

The reader functions are not in `cljs.core`; they are in `cljs.reader`. Use `(require '[cljs.reader :as reader])` and `(reader/read-string s)`.

### `:elide-asserts` over `*assert*`

Setting `*assert*` to false at runtime does not work on ClojureScript. Use the `:elide-asserts true` compiler option for production builds to remove `assert` calls.

### New interop syntax requires a recent compiler

`:refer-global`, `:require-global`, and method values (`String/.toUpperCase`, `Date/new`) need ClojureScript 1.12.116 or later. `^:async` and `await` need 1.12.145 or later. Check the project's `org.clojure/clojurescript` version, or the version bundled with its shadow-cljs release, before writing them. On older compilers the `ns` form fails to parse or `await` resolves to nothing.

### Older clj-kondo flags `await`

clj-kondo recognizes `cljs.core/await` starting with v2026.05.25, which also added the `:await-without-async-fn` and `:misplaced-async-metadata` linters. Older versions report `error: Unresolved symbol: await`. Upgrade clj-kondo instead of suppressing the error.
