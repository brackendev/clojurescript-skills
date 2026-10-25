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

`:refer-global` lists the globals a namespace uses without the `js/` prefix and accepts `:rename`, and ClojureScript supports the Clojure method value syntax. Only one `:refer-global` clause is allowed per `ns` form. A second one fails with "Only one :refer-global form is allowed per namespace definition". Once a global is referred, `Class/member` reaches its static members, `Class/new` is the constructor, and `Class/.method` is a function that calls the instance method on its first argument:

```clojure
(ns my-app.core
  (:refer-global :only [Date Promise String] :rename {Date JsDate}))

(JsDate/now)                                  ;; js/Date.now
(JsDate/new)                                  ;; (js/Date.)
(Promise/resolve 10)                          ;; (.resolve js/Promise 10)
(mapv String/.toUpperCase ["foo" "bar"])      ;; ["FOO" "BAR"]
```

`:require-global` gives a namespace alias to a library that the page already loaded as a global, usually through a `<script>` tag, with no build configuration: `(:require-global [Idiomorph :as idio])`, then `(idio/morph ...)`. At the REPL, `(refer-global :only '[Date])` and `(require-global '[Idiomorph :as idio])` do the same.

### Native object creation

```clojure
;; good
(js-obj "name" "Bruce" "age" 30)

;; good: convert a Clojure map at the boundary
(clj->js {:name "Bruce" :age 30})
```

### Native object access

Use `goog.object/get` and `goog.object/set` for string-keyed object access, such as a dynamic property name or a key that must survive advanced optimization. Reserve `aget` and `aset` for arrays: their docstrings say they work on JavaScript arrays, and with the `:checked-arrays :warn` compiler option the compiler reports `aget` on an object ("arguments must be an array followed by numeric indices ... consider goog.object/get for object access"). A string key is never renamed, whichever function reads it:

```clojure
;; good: string-keyed object access, advanced-compilation safe
(goog.object/get user "name")
(goog.object/set user "name" "Bruce")

;; good: array access
(aget xs 0)
(aset xs 0 :first)

;; bad: aget is for arrays; :checked-arrays reports this call
(aget user "name")
```

Do not mix the two access styles on one property of an object that ClojureScript code builds. Under `:advanced`, `(set! (.-score o) 5)` writes a renamed property unless an extern names `score`, so `(goog.object/get o "score")` can return `nil` in the release build.

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

`(js->clj x :keywordize-keys true)` converts string keys to Clojure keywords. The default leaves them as strings, so keyword lookups on the result silently return `nil` (Iron Law CLJS-4):

```clojure
(:name (js->clj #js {:name "Ada"}))                       ; => nil, the key is "name"
(:name (js->clj #js {:name "Ada"} :keywordize-keys true)) ; => "Ada"
```

`cljs.proxy` is an experimental namespace that exposes ClojureScript maps as JavaScript objects and vectors as array-like values without copying them through `clj->js`. Treat it as experimental and keep `clj->js` as the default at the boundary.

## Type Hints and Externs

Two type-hint forms matter in application code. `^boolean` removes the truthiness check from the generated `if`, and `^js` drives externs inference:

| Hint | Purpose |
|------|---------|
| `^boolean` | Avoid the checked-`if` runtime that handles JS's wider falsy set. See "Truthiness and Boolean Handling" in `references/host-semantics.md`. |
| `^js`, `^js/Foo.Bar` | Mark a value as foreign JavaScript so externs inference does not rename its property names under advanced compilation. |

```clojure
;; good: hint a foreign JS instance so .baz is not renamed
(defn wrap-baz [^js/Foo.Bar x]
  (.baz x))
```

### Externs inference (Iron Law CLJS-1)

Under `:advanced` optimizations the Google Closure Compiler renames every property name it does not see in an externs file. Code like `(.-userName user)` on a value the compiler cannot infer becomes `(.x user)` in the output, and the property no longer matches the JavaScript object the rest of the system produces, so code that works in development breaks in production. For projects that target advanced compilation, enable externs inference and add `^js/Foo.Bar` hints until the warnings clear. The compiler will warn on every interop call where it cannot determine the target type and emit an inferred externs file when types are known:

```edn
;; build config (compiler options). The stock compiler defaults to false,
;; except that :target :bundle turns inference on.
{:infer-externs true}
```

A `^js` hint has no effect while externs inference is off: in a test with the stock compiler and `:advanced`, a hinted property access still returned `nil`. The `*warn-on-infer*` warnings appear either way, so a build that prints no warning can still rename hinted properties when `:infer-externs` is off. shadow-cljs defaults `:infer-externs` to `:auto` and needs no hint on `js/` globals or on values bound by `:require`. Its User's Guide also advises against annotating everything with `^js` and recommends `^clj` for ClojureScript values that the compiler cannot infer.

```clojure
;; at the top of any namespace that does interop
(set! *warn-on-infer* true)
```

The `:warnings {:infer-warning true}` compiler option turns on the same warnings for the whole build. Without `^js` hints, calls like `(.baz x)` on a parameter `x` of unknown type produce a `Cannot infer target type` warning at compile time. With `^js/Foo.Bar x` the compiler accepts the call and writes the property into `inferred_externs.js` in the output directory. A bare `^js` also protects the property, but prints "Adding extern to Object for property ... due to ambiguous expression" while `*warn-on-infer*` is set. Add manual externs (`.js` files with JSDoc annotations) only for what inference cannot reach.

With `cljs.main --target node`, inference does not cover a function called through the alias of a Node built-in module. After `(:require ["fs" :as fs])`, an `:advanced` build with `:infer-externs true` renamed `existsSync` in `(fs/existsSync "deps.edn")`, which then failed with "is not a function", while `(.existsSync fs "deps.edn")` ran. Call Node built-in modules with method syntax in builds that use `:advanced`.

## Inserting HTML and URLs (Iron Law CLJS-2)

MDN calls `innerHTML` "probably the most common vector for cross-site scripting (XSS) attacks, where potentially unsafe strings provided by a user are injected into the DOM without first being sanitized." The same applies to every API that parses a string as HTML, such as `insertAdjacentHTML` and React's `dangerouslySetInnerHTML`. The OWASP XSS Prevention Cheat Sheet also lists unvalidated URL values: a user-controlled `href` or `src` can hold a `javascript:` URL, so allow only expected schemes such as `https:` and `mailto:`.

```clojure
;; bad: comment can contain <img src=x onerror=...>
(set! (.-innerHTML el) comment)

;; good: plain text is never parsed as HTML
(set! (.-textContent el) comment)
```

When content must stay HTML, sanitize it first with a maintained sanitizer such as DOMPurify, which OWASP recommends, and treat the sanitized result as the only value that reaches the HTML API.

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
