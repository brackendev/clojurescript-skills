---
name: clojurescript
description: >-
  Use when writing, editing, reviewing, or discussing ClojureScript code that
  compiles to JavaScript and runs on a JS host (browser, Node.js, or other JS
  runtime). Triggers: .cljs files, .cljc files compiled to JS, shadow-cljs.edn,
  figwheel-main.edn, ClojureScript deps.edn projects, cljs.test, JavaScript
  interop (`js/`, `.-prop`, `(.method obj ...)`, `set!` on properties,
  `js-obj`, `clj->js`, `js->clj`, `goog.object/get`, `goog.object/set`,
  `aget`, `aset`), externs inference (`^js`, `^js/Foo`, `*warn-on-infer*`,
  `:infer-externs`), macro stage separation (`:require-macros`,
  `:refer-macros`, `:include-macros`), reader conditionals (`#?`, `#?@`,
  `:cljs`, `:default`), JS-flavored numerics (no `Ratio`, `BigInt`,
  `BigDecimal`; `(= 0.0 0)` is true), `^boolean` type hint, browser REPL,
  Node REPL, source maps, npm interop, the `cljs.main` CLI, shadow-cljs, or
  figwheel-main. Covers JavaScript interop, host-typed exception handling
  (`catch :default`, `js/Error`), externs and advanced compilation, macro
  stage separation, JS-flavored numerics and truthiness, host-specific
  aliases (`goog.object`, `goog.string`, `goog.dom`), and the ClojureScript
  project workflow.
user-invocable: false
---

# ClojureScript

Layered on top of the [clojure](https://github.com/brackendev/clojure-skills) skill, which defines host-neutral Clojure family guidance. This skill applies only to ClojureScript (`.cljs` files, `.cljc` files compiled to JS, ClojureScript `deps.edn` projects, `shadow-cljs.edn`, `figwheel-main.edn`). It overrides the baseline for JavaScript interop, externs inference, macro stage separation (`:require-macros`, `:refer-macros`, `:include-macros`), host-typed exception handling (`catch :default`, `js/Error`), JS-flavored numerics and truthiness, host-specific aliases (`goog.object`, `goog.string`, `goog.dom`), and the `cljs.main` project workflow.

JVM and ClojureDart deltas live in their own packages ([clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills), [clojuredart-skills](https://github.com/brackendev/clojuredart-skills)). Do not apply this skill's JS interop, externs, or `catch :default` rules to those dialects.

## Key Rules

1. **Property access uses a leading hyphen.** `(.-prop obj)` reads a property; `(set! (.-prop obj) v)` assigns it. `(.method obj args)` calls a method. The hyphen is what distinguishes property access from method invocation in ClojureScript.
2. **Globals live under the `js/` namespace.** `js/document`, `js/window`, `js/Promise`, `js/Error`. There is no other path to native globals.
3. **Catch with `:default` when you want to catch everything; otherwise name a JS type.** ClojureScript supports `(catch :default e ...)`. Prefer `js/Error` and its subclasses (`js/TypeError`, `js/RangeError`, `js/SyntaxError`) when the failure mode has a named JS type.
4. **Macros must be defined in `.clj` or `.cljc` files and required with `:require-macros`, `:refer-macros`, or `:include-macros`.** Macros and functions run in different compilation stages. A macro and a function may share the same name.
5. **`:import` is only for Google Closure classes.** `(:import [goog Uri])` works; `(:import [java.util Date])` does not exist on this host. Use `(:require ...)` and `js/` for everything else.
6. **No `gen-class`, no refs, no agents, no STM.** Atoms are the only built-in state primitive. Concurrency primitives that depend on multiple OS threads do not exist; JS has a single execution thread per realm.
7. **Numbers are JavaScript numbers.** There is no `Ratio`, `BigDecimal`, or `BigInt` literal. `(= 0.0 0)` evaluates to `true`. Use `js/BigInt` directly when arbitrary-precision integers are required.
8. **Use `^boolean` to avoid checked-`if`.** It is the only type hint with runtime significance for code generation, and it suppresses the runtime check that copes with JavaScript's wider falsy set (`0`, `""`, `NaN`, `null`, `undefined`).
9. **Use `^js` and `^js/Foo.Bar` to drive externs inference for advanced compilation.** Without hints on foreign JavaScript values, the Google Closure Compiler may rename property names and break interop at advanced optimization levels.
10. **Convert at the boundary, not at every use.** Use `js->clj` and `clj->js` once at the interop boundary to translate between persistent collections and native JS objects, then operate on Clojure data inside the namespace.

## JavaScript Interop

### Property access

```clojure
;; good: read a property
(.-length s)
(.-innerHTML el)

;; good: write a property
(set! (.-innerHTML el) "<p>hi</p>")

;; bad: method-call form when no method exists
(.length s)
```

### Method calls

```clojure
;; good
(.toUpperCase s)
(.appendChild parent child)
(.then promise on-fulfilled on-rejected)

;; bad: hyphen form when calling a method
(.-toUpperCase s)
```

### Globals

```clojure
;; good
js/document
js/window
js/Promise
js/Error

(.getElementById js/document "app")
(js/parseInt s 10)
```

### Constructors

Use the same dot form Clojure uses for JVM constructors, but the class is reached through `js/`:

```clojure
;; good
(js/Date.)
(js/Error. "boom")
(js/Promise. (fn [resolve reject] ...))
```

### Native object creation

```clojure
;; good
(js-obj "name" "Bruce" "age" 30)

;; good: convert a Clojure map at the boundary
(clj->js {:name "Bruce" :age 30})
```

### Native object access

Prefer `goog.object/get` and `goog.object/set` for object property access when the property name is dynamic, externs hygiene matters, or advanced optimization is on. Reserve `aget` and `aset` for arrays:

```clojure
;; good: dynamic property name, advanced-compilation safe
(goog.object/get user "name")
(goog.object/set user "name" "Bruce")

;; good: array access
(aget xs 0)
(aset xs 0 :first)

;; bad: aget on an object renames the property under advanced compilation
(aget user "name")
```

### Converting between Clojure and JavaScript

```clojure
;; good: convert at the boundary
(defn save-user! [user]
  (js/fetch "/api/users"
            (clj->js {:method "POST"
                      :body (.stringify js/JSON (clj->js user))})))

(defn parse-response [resp]
  (-> resp
      (.json)
      (.then #(js->clj % :keywordize-keys true))))
```

`(js->clj x :keywordize-keys true)` converts string keys to Clojure keywords. The default leaves them as strings.

## Exception Handling

`ex-info` for data-carrying exceptions remains the baseline. On ClojureScript, catch JS-typed exceptions when the failure mode already has a named JS type, and use `:default` for the catch-all:

```clojure
;; good
(try
  (do-work x)
  (catch js/TypeError e ...)
  (catch js/Error e ...)
  (catch :default e ...))

;; bad: js/Object catches almost everything but loses intent
(try
  (do-work x)
  (catch js/Object e ...))
```

`(catch :default e ...)` is the ClojureScript-specific catch-all and is the closest equivalent to "catch any thrown value." JavaScript code can throw non-`Error` values (strings, numbers, plain objects); `:default` covers those too.

### Reader-conditional exception types

When the same `.cljc` file is consumed on the JVM and on ClojureScript, use a reader conditional in the catch clause so each host sees the right type:

```clojure
(try
  (parse-int s)
  (catch #?(:clj Exception :cljs js/Error) e
    (handle-error e)))
```

## Macros and Compilation Stages

Macros run at compile time. In ClojureScript that compile time is JVM Clojure (the ClojureScript compiler itself runs on the JVM, or on a self-hosted ClojureScript). Macros therefore must live in `.clj` or `.cljc` files, not in `.cljs` files.

```clojure
;; src/my_app/macros.clj  (or my_app/macros.cljc)
(ns my-app.macros)

(defmacro with-timing [label & body]
  `(let [start# (.getTime (js/Date.))
         result# (do ~@body)]
     (js/console.log ~label "took" (- (.getTime (js/Date.)) start#) "ms")
     result#))

;; src/my_app/core.cljs
(ns my-app.core
  (:require [my-app.util :as util])
  (:require-macros [my-app.macros :refer [with-timing]]))

(with-timing "fetch" (util/fetch "/api"))
```

When the macro file is `.cljc` and the macro and a function share a name, prefer `:include-macros true` from a single `:require` clause:

```clojure
(ns my-app.core
  (:require [my-app.lib :refer [some-fn some-macro] :include-macros true]))
```

When only macros need importing across hosts, use `:refer-macros` from within a reader-conditional `:require`:

```clojure
(ns my-app.core
  (:require #?(:clj  [my-app.lib :refer [some-fn some-macro]]
               :cljs [my-app.lib :refer [some-fn] :refer-macros [some-macro]])))
```

A macro and a function can have the same name in ClojureScript; this is unlike JVM Clojure where the macro name shadows the function in its namespace.

## Namespaces

```clojure
(ns my-app.core
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [goog.string :as gstr]
   [my-app.db :as db])
  (:require-macros
   [my-app.macros :refer [with-timing]])
  (:import
   [goog Uri]))
```

- `:import` is only for Google Closure classes (e.g., `goog.Uri`, `goog.date.Date`). JavaScript classes that are not Closure classes are reached through `js/` or via `:require` of an npm/bundled module.
- `:refer :all` is not supported on ClojureScript.
- `gen-class` and `gen-interface` are not implemented.
- `Foo/bar` always means `Foo` is a namespace. There is no `Class/staticMember` form for JS classes; use `(.member js/Class)` or import the value.

## Type Hints and Externs

The compiler uses two type-hint forms with runtime significance on ClojureScript:

| Hint | Purpose |
|------|---------|
| `^boolean` | Avoid the checked-`if` runtime that handles JS's wider falsy set. |
| `^js`, `^js/Foo.Bar` | Mark a value as foreign JavaScript so externs inference does not rename its property names under advanced compilation. |

```clojure
;; good: avoid checked-if for a hot predicate
(defn active? ^boolean [user]
  (true? (:active? user)))

;; good: hint a foreign JS instance so .baz is not renamed
(defn wrap-baz [^js/Foo.Bar x]
  (.baz x))
```

### Externs inference

For projects that target advanced compilation, enable externs inference. The compiler will warn on every interop call where it cannot determine the target type and emit an inferred externs file when types are known:

```edn
;; build config (compiler options)
{:infer-externs true}
```

```clojure
;; at the top of any namespace that does interop
(set! *warn-on-infer* true)
```

Without `^js` hints, calls like `(.baz x)` on a parameter `x` of unknown type will produce a `Cannot infer target type` warning at compile time. With `^js/Foo.Bar x` the compiler accepts the call and writes the property into the inferred externs file. Manual externs (`.js` files with JSDoc annotations) remain available for cases that inference cannot handle.

Externs inference requires ClojureScript 1.10.238 or later.

## Numbers

There is no `Ratio`, `BigDecimal`, or `BigInt` literal in ClojureScript. Numbers are JavaScript numbers, which means a single IEEE 754 double covers both integers and floats. Equality reflects JavaScript semantics:

```clojure
;; ClojureScript
(= 0.0 0)    ;; ⇒ true   (JVM Clojure: false)
(= 1.0 1)    ;; ⇒ true   (JVM Clojure: false)
```

For arbitrary-precision integer work, use `js/BigInt` directly (and remember it does not interoperate with regular JS numbers via `=` or arithmetic without conversion).

## Truthiness and Boolean Handling

Clojure's `if` treats only `nil` and `false` as falsy on every host, including ClojureScript. The compiler inserts a runtime check on every `if` to preserve this semantic against JavaScript's wider falsy set (`0`, `""`, `NaN`, `null`, `undefined`). The `^boolean` type hint tells the compiler the expression is already a JS boolean and the check can be skipped. Reach for it on hot paths where the predicate is known to produce a boolean.

```clojure
;; good
(defn flagged? ^boolean [user]
  (true? (:flagged? user)))
```

For runtime predicates on JS values, use the canonical Clojure predicates (`nil?`, `string?`, `number?`, `boolean?`, `fn?`) rather than `goog.isString` and friends. Closure's `goog.isXxx` helpers are deprecated in modern Closure and are not idiomatic ClojureScript.

## Characters

ClojureScript has no character type. `\a` reads as the single-character string `"a"`. Treat character literals as one-character strings everywhere; do not pattern-match on a separate character predicate.

```clojure
;; good
(re-find #"\d" s)
(filter #{\a \e \i \o \u} s)
```

`(filter #{\a \e \i \o \u} s)` works because the set members are one-character strings, and iterating a string yields one-character strings.

## State Management

This section covers what is host-specific. The baseline atom rules still apply.

- Refs, agents, and STM (`dosync`, `alter`, `ref`, `agent`, `send`, `send-off`, `io!`) are not available on ClojureScript. Do not reach for them.
- `binding` and dynamic vars work, with the caveat that JavaScript is single-threaded per realm; "thread-local" semantics collapse to "synchronous-call-local."
- `monitor-enter`, `monitor-exit`, and `locking` are not implemented.

## Vars

Vars are not reified at runtime on ClojureScript. The compiler emits compile-time metadata only. Consequences:

- `(var foo)` and `#'foo` return compile-time `Var` instances; runtime introspection that JVM tooling relies on is limited.
- `def` produces an ordinary JavaScript variable and evaluates to its value, not to the var.
- `:private` metadata is not enforced by the compiler.
- `with-redefs` and `intern` are not available in the same shape as JVM Clojure. Prefer passing dependencies as function arguments.

## Reader Conditionals

`.cljc` files support reader conditionals. The platform tags are `:clj`, `:cljs`, `:cljr`, and `:default`:

```clojure
;; standard reader conditional
(defn parse-int [s]
  #?(:clj  (java.lang.Integer/parseInt s)
     :cljs (js/parseInt s 10)))

;; splicing reader conditional inside a vector
(defn supported-platforms []
  [#?@(:clj  [:jvm]
       :cljs [:browser :node])])

;; namespace form
(ns my-app.core
  (:require
   #?(:clj  [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer-macros [deftest is testing]])))
```

If no tag matches and no `:default` is provided, the reader returns nothing (not `nil`). A splicing reader conditional cannot splice multiple top-level forms; use one standard conditional per top-level form instead.

`.cljs` files do not support reader conditionals. Move shared code into `.cljc` when both hosts must read it.

## ClojureScript Aliases

These namespaces are ClojureScript-specific. Use the conventional alias:

| Namespace | Alias |
|-----------|-------|
| `goog.object` | `gobj` |
| `goog.string` | `gstr` |
| `goog.dom` | `gdom` |
| `cljs.core.async` | `async` |
| `cljs.spec.alpha` | `s` |
| `cljs.test` | `t` (when shadowing `clojure.test` would be confusing) |

`clojure.string`, `clojure.set`, `clojure.edn`, `clojure.walk`, and `clojure.pprint` are baseline aliases and live in the [clojure](https://github.com/brackendev/clojure-skills) skill. On ClojureScript they ship under those same names and the baseline aliases (`str`, `set`, `edn`, `walk`, `pp`) still apply.

## Testing

`cljs.test` provides the testing macros. Catch JS-typed exceptions in `thrown?` assertions:

```clojure
(ns my-app.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [my-app.core :as core]))

(deftest parse-int-test
  (testing "parses decimal strings"
    (is (= 42 (core/parse-int "42"))))
  (testing "throws on garbage"
    (is (thrown? js/Error (core/parse-int! "not-a-number")))))
```

For `.cljc` tests that run on both hosts, use a reader conditional in the require and in the thrown-type:

```clojure
(ns my-app.core-test
  (:require
   #?(:clj  [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer-macros [deftest is testing]])
   [my-app.core :as core]))

(deftest parse-int-test
  (is (thrown? #?(:clj Exception :cljs js/Error) (core/parse-int! "not-a-number"))))
```

`with-redefs` is not the same as on the JVM (vars are not reified). Prefer passing dependencies as arguments, or use protocols at boundaries with test doubles.

## Project Workflow

CLI commands (`cljs.main`), project layout, `deps.edn` configuration for ClojureScript, the browser REPL, the Node REPL, source maps, advanced compilation, externs handling, npm interop patterns, and notes on the shadow-cljs and figwheel-main community alternatives live in `references/project-workflows.md`. Load it on demand.

## Gotchas

### `:advanced` compilation renames property names

Under `:advanced` optimizations the Google Closure Compiler renames every property name it does not see in an externs file. Code like `(.-userName user)` on a value the compiler cannot infer becomes `(.x user)` in the output, and the property no longer matches the JavaScript object the rest of the system produces. Symptoms: code works in development, breaks in production. Fix: enable `:infer-externs true`, set `*warn-on-infer*` per namespace, and add `^js/Foo.Bar` hints until the warnings clear. Add manual externs only for what inference cannot reach.

### `aget` on objects breaks under advanced compilation

`aget` is a JavaScript array index, not an object lookup. Using `aget` on an object happens to work without optimization but breaks under `:advanced` because the index becomes a property name that the renamer may have changed. Use `goog.object/get` for objects and reserve `aget` for arrays.

### `(= 0.0 0)` returns `true`

JS numerics use one IEEE 754 double for both integer and float. Equality on ClojureScript reflects that. Code that relies on JVM Clojure's distinction between `0` and `0.0` will behave differently here. The same caveat applies to `1.0`, `42.0`, etc.

### `*out*` and `*err*` are not implemented

Use `(.log js/console ...)` or `(println ...)` directly. Bindings around `*out*` that work on the JVM do nothing on ClojureScript.

### Macro and function name sharing

Unlike on the JVM, a macro and a function may share a name in ClojureScript. This is occasionally a footgun when porting a JVM namespace whose macro shadowed an underlying function; on ClojureScript both names are accessible from their respective stages.

### `cljs.reader` for `read` and `read-string`

The reader functions are not in `cljs.core`; they are in `cljs.reader`. Use `(require '[cljs.reader :as reader])` and `(reader/read-string s)`.

### `:elide-asserts` over `*assert*`

Setting `*assert*` to false at runtime does not work on ClojureScript. Use the `:elide-asserts true` compiler option for production builds to remove `assert` calls.
