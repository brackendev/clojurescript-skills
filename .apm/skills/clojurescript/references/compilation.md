# Macros, Namespaces, and Reader Conditionals

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

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

A macro and a function can have the same name in ClojureScript, and each is reachable from its own compilation stage. In JVM Clojure the macro name shadows the function in its namespace, so check for name collisions when porting a JVM namespace whose macro shadowed an underlying function.

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
- `Foo/bar` resolves `Foo` as a namespace, a namespace alias, a ClojureScript type (`PersistentVector/EMPTY`), or a global referred with `:refer-global`. The static members of `Math` and `String` (`Math/floor`, `String/fromCharCode`) also resolve without `:refer-global`. For any other global that is not referred, use `(.member js/Class)` or `js/Class.member`.
- `:refer-global` and `:require-global` are `ns` clauses. See "Referred globals and method values" in `references/interop.md`.

## Reader Conditionals

The general rules (the platform tags, what a conditional with no matching branch reads, `#?@` splicing, and the portable `:require` and catch patterns) are in `clojure/references/reader-conditionals.md` in the `clojure` skill (clojure-skills). The ClojureScript compiler reads `.cljc` files with the feature set `#{:cljs}`, so it takes the first branch tagged `:cljs` or `:default`. These parts are specific to ClojureScript:

- `.cljs` files do not read reader conditionals. A `#?` form in a `.cljs` file fails to compile with "Conditional read not allowed". Move shared code into `.cljc` when both hosts must read it.
- A top-level `#?@` fails in the ClojureScript compiler with "cond-splice not in list", while the JVM reader reports "Reader conditional splicing not allowed at the top level." for the same form.
- A `.cljc` namespace that loads macros on both hosts uses `:refer-macros` in the `:cljs` branch of a conditional `:require`, as shown under "Macros and Compilation Stages" above.
- A catch type in a `.cljc` file needs `:default` in the `:cljs` branch when the handler must catch every thrown value ("Reader-conditional exception types" in `references/async-and-errors.md`).
