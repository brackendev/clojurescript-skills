# Numbers, Truthiness, and Other Host Semantics

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

## Numbers

There is no `Ratio`, `BigDecimal`, or `BigInt` literal in ClojureScript. A ratio literal such as `1/2` fails to compile ("clojure.lang.Ratio is not a valid ClojureScript constant"), and the compiler emits `N` and `M` literals as doubles, so `9007199254740993N` silently becomes `9007199254740992` and `0.1M` is `0.1`. Numbers are JavaScript numbers, which means a single IEEE 754 double covers both integers and floats. Equality reflects JavaScript semantics:

```clojure
;; ClojureScript
(= 0.0 0)    ;; ⇒ true   (JVM Clojure: false)
(= 1.0 1)    ;; ⇒ true   (JVM Clojure: false)
```

Code that relies on JVM Clojure's distinction between `0` and `0.0` behaves differently here. The same applies to `1.0`, `42.0`, and every other float with an integer value.

Integer operations follow from the same representation:

- `/` never produces a ratio. `(/ 1000 3)` is `333.3333333333333`. Use `quot`, `rem`, and `mod` for integer division.
- `int` truncates to 32 bits, because it compiles to `(x | 0)`: `(int 3000000000)` is `-1294967296`. `long` truncates toward zero without that limit.
- Integers are exact only up to `Number.MAX_SAFE_INTEGER` (9007199254740991). `(+ js/Number.MAX_SAFE_INTEGER 2)` is `9007199254740992`. `parse-long` returns `nil` for a result outside the safe range, and `(js/Number.isSafeInteger n)` checks a computed value.

For integers beyond the safe range, use `js/BigInt`. It has no literal, so build it from a string, `(js/BigInt "9007199254740993")`. `(= (js/BigInt 1) 1)` is `false`, and arithmetic that mixes a BigInt with a number throws `TypeError` ("Cannot mix BigInt and other types, use explicit conversions"), so convert at the boundary.

## Money

ClojureScript has no decimal type: `0.1M` is the double `0.1`, and `(+ 0.1M 0.2M)` is `0.30000000000000004`. Keep money as an integer count of the currency's minor unit, such as cents, below `Number.MAX_SAFE_INTEGER`, and convert only at the edges.

Parse a decimal string into minor units without floating point. `(* 100 (parse-double "19.99"))` is `1998.9999999999998`, so truncating it with `long` loses a cent:

```clojure
;; good: build the integer from the digits
(defn parse-cents
  "Returns the integer cents in a string such as \"19.99\", or nil."
  [s]
  (when-let [[_ sign whole frac] (re-matches #"([+-]?)(\d+)(?:\.(\d{1,2}))?" s)]
    (parse-long (str sign whole (subs (str frac "00") 0 2)))))

(parse-cents "19.99")             ;; => 1999
(parse-cents "0.5")               ;; => 50
(parse-cents "1.999")             ;; => nil
(parse-cents "90071992547409.92") ;; => nil, beyond the safe range

;; bad: (long (* 100 (parse-double "19.99"))) is 1998
```

- Split an amount with `quot` and `rem`, and assign the remainder explicitly: `(quot 1000 3)` is `333`, and `(rem 1000 3)` is the `1` cent left over.
- `js/Math.round` rounds halves toward positive infinity (`(js/Math.round -2.5)` is `-2`), so write the rounding rule out when negative amounts can occur.
- Format for display only: `(.format (js/Intl.NumberFormat. "en-US" #js {:style "currency" :currency "USD"}) (/ cents 100))` returns `"$19.99"` for `1999`.

## Truthiness and Boolean Handling

Clojure's `if` treats only `nil` and `false` as falsy on every host, including ClojureScript. The compiler inserts a runtime check on every `if` to preserve this semantic against JavaScript's wider falsy set (`0`, `""`, `NaN`, `null`, `undefined`). The `^boolean` type hint tells the compiler the expression is already a JS boolean and the check can be skipped. Reach for it on hot paths where the predicate always produces `true` or `false` (Iron Law CLJS-5). If a hinted expression returns `0` or `""`, the unchecked `if` takes the false branch, while Clojure semantics would take the true branch.

```clojure
;; good: the hint goes on the function name, as in cljs.core
(defn ^boolean flagged? [user]
  (true? (:flagged? user)))

;; bad: a hint on the argument vector does not reach callers, so every
;; (if (flagged? u) ...) keeps the runtime check
(defn flagged? ^boolean [user]
  (true? (:flagged? user)))
```

For runtime predicates on JS values, use the canonical Clojure predicates (`nil?`, `string?`, `number?`, `boolean?`, `fn?`) rather than `goog.isString` and friends. ClojureScript depends on a Clojure-maintained fork of Google Closure Library that keeps `goog.isString` and the other simple `goog.isXxx` helpers for libraries that call them. They work, but they are not idiomatic ClojureScript.

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
- `monitor-enter` and `monitor-exit` do not exist. `locking` compiles, but it only evaluates its body and takes no lock.

## Vars

Vars are not reified at runtime on ClojureScript. The compiler emits compile-time metadata only. Consequences:

- `(var foo)` and `#'foo` return compile-time `Var` instances; runtime introspection that JVM tooling relies on is limited.
- `def` produces an ordinary JavaScript variable and evaluates to its value, not to the var. REPLs set the `:def-emits-var` compiler option, so `def` prints the var there.
- `:private` metadata is not enforced. Calling a private var from another namespace compiles and runs, and the compiler prints a `var: ... is not public` warning.
- `with-redefs` works by `set!` on each var for the duration of the body. Its docstring warns that advanced compilation resolves vars statically and asks for `^:dynamic` on any var that must be redefined in a production build. `intern` does not exist. Prefer passing dependencies as function arguments, or use protocols at boundaries with test doubles.
