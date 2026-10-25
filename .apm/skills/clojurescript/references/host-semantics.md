# Numbers, Truthiness, and Other Host Semantics

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

## Numbers

There is no `Ratio`, `BigDecimal`, or `BigInt` literal in ClojureScript. Numbers are JavaScript numbers, which means a single IEEE 754 double covers both integers and floats. Equality reflects JavaScript semantics:

```clojure
;; ClojureScript
(= 0.0 0)    ;; ⇒ true   (JVM Clojure: false)
(= 1.0 1)    ;; ⇒ true   (JVM Clojure: false)
```

Code that relies on JVM Clojure's distinction between `0` and `0.0` behaves differently here. The same applies to `1.0`, `42.0`, and every other float with an integer value.

For arbitrary-precision integer work, use `js/BigInt` directly (and remember it does not interoperate with regular JS numbers via `=` or arithmetic without conversion).

## Truthiness and Boolean Handling

Clojure's `if` treats only `nil` and `false` as falsy on every host, including ClojureScript. The compiler inserts a runtime check on every `if` to preserve this semantic against JavaScript's wider falsy set (`0`, `""`, `NaN`, `null`, `undefined`). The `^boolean` type hint tells the compiler the expression is already a JS boolean and the check can be skipped. Reach for it on hot paths where the predicate always produces `true` or `false` (Iron Law CLJS-5). If a hinted expression returns `0` or `""`, the unchecked `if` takes the false branch, while Clojure semantics would take the true branch.

```clojure
;; good
(defn flagged? ^boolean [user]
  (true? (:flagged? user)))
```

For runtime predicates on JS values, use the canonical Clojure predicates (`nil?`, `string?`, `number?`, `boolean?`, `fn?`) rather than `goog.isString` and friends. Upstream Google Closure Library removed several `goog.isXxx` helpers, and the Clojure-maintained fork that ClojureScript depends on since 1.12.42 restored them for older libraries. They work, but they are not idiomatic ClojureScript.

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
- `with-redefs` and `intern` are not available in the same shape as JVM Clojure. Prefer passing dependencies as function arguments, or use protocols at boundaries with test doubles.
