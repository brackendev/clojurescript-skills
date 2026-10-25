# JavaScript Interop and Externs

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

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

### Referred globals and method values

ClojureScript 1.12.116 added `:refer-global` and Clojure 1.12 method value syntax. `:refer-global` lists the globals a namespace uses without the `js/` prefix and accepts `:rename`. Only one `:refer-global` clause is allowed per `ns` form. Once a global is referred, `Class/member` reaches its static members, `Class/new` is the constructor, and `Class/.method` is a function that calls the instance method on its first argument:

```clojure
(ns my-app.core
  (:refer-global :only [Date Promise String] :rename {Date JsDate}))

(JsDate/now)                                  ;; js/Date.now
(JsDate/new)                                  ;; (js/Date.)
(Promise/resolve 10)                          ;; (.resolve js/Promise 10)
(mapv String/.toUpperCase ["foo" "bar"])      ;; ["FOO" "BAR"]
```

`:require-global` gives a namespace alias to a library that the page already loaded as a global, usually through a `<script>` tag, with no build configuration: `(:require-global [Idiomorph :as idio])`, then `(idio/morph ...)`. At the REPL, `(refer-global :only '[Date])` and `(require-global '[Idiomorph :as idio])` do the same. Keep the `js/` forms when the project must compile on ClojureScript versions before 1.12.116.

### Native object creation

```clojure
;; good
(js-obj "name" "Bruce" "age" 30)

;; good: convert a Clojure map at the boundary
(clj->js {:name "Bruce" :age 30})
```

### Native object access

Prefer `goog.object/get` and `goog.object/set` for object property access when the property name is dynamic, externs hygiene matters, or advanced optimization is on. Reserve `aget` and `aset` for arrays. `aget` is a JavaScript array index, not an object lookup. On an object it works without optimization but breaks under `:advanced`, because the index becomes a property name that the renamer may have changed:

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

ClojureScript 1.12.116 also added `cljs.proxy`, an experimental namespace that exposes ClojureScript maps as JavaScript objects and vectors as array-like values without copying them through `clj->js`. Treat it as experimental and keep `clj->js` as the default at the boundary.

## Type Hints and Externs

Two type-hint forms matter on ClojureScript. `^boolean` is the only hint that changes the generated code, and `^js` drives externs inference:

| Hint | Purpose |
|------|---------|
| `^boolean` | Avoid the checked-`if` runtime that handles JS's wider falsy set. See "Truthiness and Boolean Handling" in `references/host-semantics.md`. |
| `^js`, `^js/Foo.Bar` | Mark a value as foreign JavaScript so externs inference does not rename its property names under advanced compilation. |

```clojure
;; good: hint a foreign JS instance so .baz is not renamed
(defn wrap-baz [^js/Foo.Bar x]
  (.baz x))
```

### Externs inference

Under `:advanced` optimizations the Google Closure Compiler renames every property name it does not see in an externs file. Code like `(.-userName user)` on a value the compiler cannot infer becomes `(.x user)` in the output, and the property no longer matches the JavaScript object the rest of the system produces, so code that works in development breaks in production. For projects that target advanced compilation, enable externs inference and add `^js/Foo.Bar` hints until the warnings clear. The compiler will warn on every interop call where it cannot determine the target type and emit an inferred externs file when types are known:

```edn
;; build config (compiler options)
{:infer-externs true}
```

```clojure
;; at the top of any namespace that does interop
(set! *warn-on-infer* true)
```

Without `^js` hints, calls like `(.baz x)` on a parameter `x` of unknown type will produce a `Cannot infer target type` warning at compile time. With `^js/Foo.Bar x` the compiler accepts the call and writes the property into the inferred externs file. Add manual externs (`.js` files with JSDoc annotations) only for what inference cannot reach.

Externs inference requires ClojureScript 1.10.238 or later.

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
