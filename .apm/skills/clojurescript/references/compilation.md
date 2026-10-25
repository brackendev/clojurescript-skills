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
- `Foo/bar` resolves `Foo` as a namespace, a namespace alias, a ClojureScript type (`PersistentVector/EMPTY`), or, since 1.12.116, a global referred with `:refer-global`. For a global that is not referred, use `(.member js/Class)` or `js/Class.member`.
- `:refer-global` and `:require-global` are accepted `ns` clauses since 1.12.116. See "Referred globals and method values" in `references/interop.md`.

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
