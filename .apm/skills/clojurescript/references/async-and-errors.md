# Async Functions and Exceptions

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

## Async Functions

Since ClojureScript 1.12.145, `^:async` makes the compiler emit a JavaScript async function, and `await` suspends it until a Promise settles. The function always returns a Promise. `try` / `catch` around `await` handles rejections the same way it handles synchronous throws:

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

;; bad: metadata on the argument vector is ignored, and await fails to compile
(defn fetch-json ^:async [url]
  (await (fetch url)))
```

- Put `^:async` on the function name (`(defn ^:async f ...)`) or on the `fn` symbol (`(^:async fn [x] ...)`). Metadata on the argument vector or on the whole `(fn ...)` form is ignored.
- Every arity of a multi-arity function is async. Sync and async arities cannot be mixed.
- `await` belongs to the innermost function, not to the enclosing `^:async` function. A function created inside an async function is not async unless it carries its own `^:async`. `(map (fn [url] (await (fetch url))) urls)` inside an async function fails to compile with "await can only be used in async contexts". Marking the inner `fn` `^:async` compiles but returns a seq of Promises. Collect the Promises with `mapv` and `await` a single `Promise/all`.
- There is no top-level `await`.
- `await` here is a `cljs.core` macro and is unrelated to `clojure.core/await` for JVM agents.

Prefer `^:async` over `.then` chains and over `core.async` for single asynchronous actions on projects that use 1.12.145 or later. `core.async` remains the tool for channels, backpressure, and pipelines.

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
