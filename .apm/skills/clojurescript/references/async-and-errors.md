# Async Functions and Exceptions

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

## Async Functions

`^:async` makes the compiler emit a JavaScript async function, and `await` suspends it until a Promise settles. The function always returns a Promise. `try` / `catch` around `await` handles rejections the same way it handles synchronous throws:

```clojure
(ns my-app.api
  (:refer-global :only [Promise fetch]))

;; good: metadata on the name, await directly in the async body
(defn ^:async fetch-json [url]
  (try
    (let [resp (await (fetch url))]
      (js->clj (await (.json resp)) :keywordize-keys true))
    (catch :default e
      {:error (ex-message e)})))

;; good: await several Promises in parallel
(defn ^:async fetch-all [urls]
  (await (Promise/all (mapv fetch-json urls))))

;; bad: metadata on the argument vector does not make the function async, so await fails to compile
(defn fetch-json ^:async [url]
  (await (fetch url)))
```

- Put `^:async` on the function name (`(defn ^:async f ...)`) or on the `fn` symbol (`(^:async fn [x] ...)`). Metadata on the argument vector or on the whole `(fn ...)` form does not make the function async. The [Async Functions reference](https://clojurescript.org/reference/async-functions) calls those placements invalid, and an `await` inside fails to compile with "await can only be used in async contexts".
- Every arity of a multi-arity function is async. Sync and async arities cannot be mixed.
- `await` belongs to the innermost function, not to the enclosing `^:async` function. A function created inside an async function is not async unless it carries its own `^:async`. `(map (fn [url] (await (fetch url))) urls)` inside an async function fails to compile with "await can only be used in async contexts". Marking the inner `fn` `^:async` compiles but returns a seq of Promises. Collect the Promises with `mapv` and `await` a single `Promise/all`.
- There is no top-level `await`.
- `await` here is a `cljs.core` macro and is unrelated to `clojure.core/await` for JVM agents.

Prefer `^:async` over `.then` chains and over `core.async` for single asynchronous actions. `core.async` remains the tool for channels, backpressure, and pipelines.

Other callbacks run while a function waits on `await`, so state read before an `await` can be stale after it. Do not deref an atom, `await`, and then `reset!` a value computed from the earlier read. Make the update after the `await` in one `swap!` that reads the current value:

```clojure
;; bad: another callback can change cart between the deref and the reset!
(let [items (:items @cart)
      price (await (fetch-price id))]
  (reset! cart (assoc @cart :items (conj items {:id id :price price}))))

;; good: the update reads the current value
(let [price (await (fetch-price id))]
  (swap! cart update :items conj {:id id :price price}))
```

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

`(catch :default e ...)` is the ClojureScript catch-all: it catches every thrown value. JavaScript code can throw values that are not `Error` instances (strings, numbers, plain objects), and `(catch js/Error e ...)` lets those pass through: `(throw "boom")` escapes it. `ex-info` values are `js/Error` instances. `ex-message` returns `nil` for a thrown value that is not an `Error`, so a `:default` handler that reports the failure falls back to `(str e)`.

### Reader-conditional exception types

When the same `.cljc` file is consumed on the JVM and on ClojureScript, use a reader conditional in the catch clause so each host sees the right type. The general rules for reader conditionals are in `clojure/references/reader-conditionals.md` in the `clojure` skill (clojure-skills).

```clojure
;; the handler expects an Error and reads its message
(try
  (parse-int s)
  (catch #?(:clj Exception :cljs js/Error) e
    (handle-error (ex-message e))))

;; a boundary that must turn any failure into data
(try
  (call-plugin f)
  (catch #?(:clj Exception :cljs :default) e
    {:error (or (ex-message e) (str e))}))
```

`#?(:clj Exception :cljs js/Error)` pairs the ordinary exception types of both hosts. It is not a ClojureScript catch-all, because `(catch js/Error e ...)` does not catch a thrown string or plain object. When the handler must catch everything that ClojureScript code can throw, use `:default` in the `:cljs` branch.

## Resource Cleanup

ClojureScript has no `with-open`. `cljs.core` excludes `with-open` and `with-precision` from the Clojure macros it refers (`cljs/core.cljc` line 17 at tag `r1.12.145`), and nothing defines them, so a `with-open` form compiles with only a "Use of undeclared Var" warning and fails when it runs. Release a resource in `finally`:

```clojure
(ns my-app.files
  (:require ["fs" :as fs]
            [clojure.string :as str]))

;; synchronous: close the file descriptor in finally
(defn read-text [path]
  (let [fd (.openSync fs path "r")]
    (try
      (.readFileSync fs fd "utf8")
      (finally
        (.closeSync fs fd)))))

;; async: await the work inside try, and await the close
(defn ^:async first-line [path]
  (let [^js fh (await (.open (.-promises fs) path "r"))]
    (try
      (first (str/split-lines (await (.readFile fh "utf8"))))
      (finally
        (await (.close fh))))))
```

- In an async function, `await` the work inside the `try`. A Promise that the body returns without `await` settles after `finally` has already released the resource, and its rejection bypasses the `catch`.
- `cljs.main` with `--target node` resolves only the plain Node module names (`"fs"`, `"path"`, `"crypto"`, and the rest of its list), not `"fs/promises"` or `"node:fs"`, so reach the promise API through `(.-promises fs)`.
- Call Node modules with method syntax, as above, in builds that use `:advanced` ("Type Hints and Externs" in `references/interop.md`).
