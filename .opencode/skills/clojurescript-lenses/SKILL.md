---
name: clojurescript-lenses
description: >-
  Translate code-lenses design philosophies into ClojureScript-specific
  patterns. Auto-triggers when working in ClojureScript alongside code-lenses
  skills. Layers on top of clojure-lenses (in clojure-skills) and covers only
  the deltas that come from JavaScript interop, externs and advanced
  compilation, macro stage separation, the absence of refs / agents / STM, the
  JS event loop, and the absence of var reification. Covers the four default
  code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't Validate)
  and the two opt-in philosophies (APOSD, Legacy Code) that activate when their
  lens is added with `+aposd` or `+legacy-code` or invoked directly.
user-invocable: false
---

# Code Lenses for ClojureScript

Layered on top of [clojure-lenses](https://github.com/brackendev/clojure-skills) (in `clojure-skills`), which covers the host-neutral Clojure translations. This skill records only the ClojureScript-specific deltas that the baseline does not address: JavaScript interop boundaries, externs and advanced compilation, macro stage separation, the JS event loop, the absence of refs / agents / STM, and the limits of var reification.

The default code-lenses review set is `grug`, `honest-code`, `tidy-first`, and `parse-dont-validate`. `aposd` and `legacy-code` are opt-in (added with `+aposd` or `+legacy-code`, or invoked directly). The sections below apply when the corresponding lens is active; APOSD and Legacy Code translations are present so the lens can use them when explicitly invoked, but they are not part of the default trigger set.

When the [code-lenses](https://github.com/brackendev/code-lenses) plugin is active in a ClojureScript project, apply the baseline `clojure-lenses` translations first; reach for this skill for the JavaScript-specific additions below.

## Grug Brain

- Default building blocks remain maps, vectors, and sets with namespaced keys. JavaScript objects belong at the interop boundary only; convert with `clj->js` and `js->clj` once and operate on Clojure data inside the namespace.
- Reach for `deftype` only when implementing a JavaScript protocol or interop that requires a named prototype. Use plain functions and data for everything else.
- The macro / function divide is its own complexity: macros must live in `.clj` or `.cljc` files and be required via `:require-macros`, `:refer-macros`, or `:include-macros`. Treat that as a reason to prefer functions; reach for macros only when the macro is doing real syntactic work that a function cannot.
- REPL-driven development on ClojureScript means the browser REPL or Node REPL. Spike in the REPL before introducing abstractions. The cost of starting a CLJS REPL is higher than a JVM REPL, so once a REPL is up, keep using it.
- `:advanced` compilation is a complexity demon waiting to happen. Enable externs inference (`:infer-externs true`, `*warn-on-infer*`) early and add `^js` / `^js/Foo.Bar` hints incrementally so production builds do not surprise you.
- Reagent / re-frame / Fulcro are powerful, but they are abstraction. A vector of hiccup and an atom is simpler when the problem fits. Reach for a framework when the open polymorphism it provides matches a real need.

## A Philosophy of Software Design

- Namespaces are still modules. The `clojure-lenses` rule applies: small public API, private helpers via `defn-`. ClojureScript adds the externs-inference angle: a deep module hides interop ugliness so the public API does not leak `js/` prefixes or property-renaming hazards.
- The interop boundary is a module boundary. Convert at the edges with `js->clj` and `clj->js`; inside the module, operate on Clojure data. Callers should not see `goog.object/get` or `aget` in the public API of a namespace whose job is application logic.
- Hide framework-specific lifecycle behind namespace boundaries. Reagent components, re-frame events, and React hooks each have their own conventions; the namespace that owns a feature should expose Clojure data in and Clojure data out, not Reagent atoms or React refs.
- Information hiding includes externs. A namespace that wraps a JS library should hide the externs and `^js` hints from callers. Downstream code receives Clojure values typed appropriately.
- `cljs.spec.alpha` and Malli work at boundaries the same way they do on the JVM. Use them at HTTP, WebSocket, and `localStorage` boundaries to parse raw input into domain maps.

## Tidy First

- Guard clauses still mean flattening nested `if` / `when` / `let` with `cond`, `if-let`, `when-let`, `some->`, and `some->>`. Reader conditionals (`#?(:cljs ...)`) are a tidying tool when the same `.cljc` file must read on the JVM and on the JS host.
- Explaining variables: extract complex JS-interop expressions into named `let` bindings. `(.-userName user)` is opaque under advanced compilation; `(let [user-name (.-userName user)] ...)` is barely better but makes the externs hazard explicit, and a typed `(let [^js/UserRecord user user, user-name (.-userName user)] ...)` is honest.
- Extract helper: pull blocks from a `let` body or threading pipeline into a named `defn-`. ClojureScript-specific helpers often live near the interop boundary so the calling code stays free of `js/` prefixes.
- Reading order: ClojureScript files compile top-down the same as JVM Clojure. Helpers-first ordering is natural. The lazy seq versus eager `mapv` choice still matters; eager wins when side effects matter, which on ClojureScript means DOM writes, `localStorage`, and `fetch`.
- Threading macros (`->`, `->>`, `as->`) remain the primary flattening tool. `some->` and `some->>` are especially useful around JS values that may be `nil` (which equals `undefined` and `null`).
- REPL-guided refactoring: evaluate forms in the browser or Node REPL. The REPL evaluator runs in the live JS host, so refactorings are visible immediately.

## Parse, Don't Validate

- The parse layer at JS boundaries: input from JSON, `fetch` responses, `localStorage`, `URLSearchParams`, and DOM form values. Use `js->clj :keywordize-keys true` once, then conform with `cljs.spec.alpha` or Malli into a domain map.
- Smart constructors apply: a `parse-user` function that takes raw JS data and returns either a conformed map or throws `ex-info` with `:reason` data. Downstream functions receive parsed data and do not re-validate.
- Namespaced keys remain the marker for parsed data: `:user/email`, not `:email`.
- Closed maps via Malli's `:closed` option still catch typos. Add closed schemas at the boundary to reject unexpected keys from JS payloads.
- Tagged maps for sum types: `{:type :loading}`, `{:type :loaded :data ...}`, `{:type :error :message ...}`. Dispatch with `case`, `cond`, or multimethods.
- The `^js/Foo.Bar` hint is the externs-aware parse marker at the interop boundary. Without it, the compiler may rename properties under advanced compilation and silently break parse code that worked in development.
- Do not simulate branded types with `deftype` wrappers; the baseline rule against simulated branded types still applies, and ClojureScript adds the cost of breaking interop.

## Honest Code

- Data is data. Clojure maps, vectors, and sets are honest; `js-obj` and `clj->js` results belong at the interop boundary. Keep the inside of a namespace Clojure-shaped.
- Pure functions over immutable data are idiomatic. Reagent components are pure functions of state; re-frame subscriptions are pure transformations of the app DB; React function components in Fulcro / Helix are pure. Hidden state is dishonest in any of those.
- One source of truth: ClojureScript apps often centralize state in a single `re-frame` app-db or a single `defonce` atom. That is the honest variant. Distributing state across many top-level `def` atoms makes reasoning fragile.
- Compose flat, never deep: hiccup is flat composition. Threading macros, `comp`, transducers, and middleware stacks (Reagent / re-frame interceptors) are flat. Avoid deep React component hierarchies that hide their own state.
- Let It Crash, ClojureScript edition: catch JS-typed exceptions where they have a named type (`js/TypeError`, `js/Error`) and `:default` for the catch-all. For background work (web workers, service workers), surface failure to the main thread and let the supervisor decide. There is no Erlang-style supervisor on the JS host.
- Boring tests: `cljs.test` with `(is (= (f input) expected))` is the boring test. Reach for `with-redefs`-style stubbing rarely; ClojureScript's var semantics do not match JVM's, and dependency injection through function arguments or protocols is more honest.
- Declare what, not how: spec / Malli schemas, re-frame interceptors, and Reagent reactions are declarations. Prefer them to imperative effect chains.
- Atoms remain honest state. The JS host is single-threaded per realm, so `swap!` semantics simplify: no race conditions inside a synchronous call. `core.async` channels are honest when the use case requires backpressure or pipelines, but a `js/Promise` is simpler for a single asynchronous action.

## Legacy Code

- Seams in ClojureScript: vars (rebindable in tests with `with-redefs`, but vars are not reified at runtime, so the seam is shallower than on the JVM), higher-order functions (pass a function instead of hardcoding a call), protocols at boundaries, and system maps (Component / Integrant / Mount work on ClojureScript as well as on the JVM).
- Extract Interface translates to extracting a protocol or, more often, a function parameter. ClojureScript protocols are useful when the same shape must work on plain JS objects and on Clojure values; otherwise function parameters suffice.
- Parameterize Constructor translates to passing a deps map or system key. Functions that touch JS globals (`js/window`, `js/document`, `js/fetch`) should receive those through arguments so tests can substitute.
- Sprout method/class translates to sprouting a new function or namespace. New behavior goes in a new `defn` called from the legacy code, not into the legacy `defn` itself.
- Wrap method/class translates to a wrapper function or namespace. Useful for wrapping a third-party JS library so callers see Clojure data and the externs / `^js` hints stay inside the wrapper.
- Scratch refactoring maps to REPL-driven exploration in the browser or Node REPL. The compiler emits source maps, so stack traces from REPL evaluations point back to ClojureScript source.
- Effect sketching: trace which namespaces and vars a change touches, paying special attention to anything that calls into `js/` globals or uses `^js`. `clj-kondo` covers `.cljs` and `.cljc` files; use it to map the dependency graph before changing tightly coupled namespaces.
- Hard dependencies in ClojureScript include direct interop calls on `js/window`, `js/document`, `js/localStorage`, `js/fetch`, framework singletons (a global `re-frame` app-db when a parameterized one would do), and namespace-level `defonce` state that is reached from many places.
- Characterization tests use `cljs.test`. For UI behavior, snapshot the rendered hiccup; for `re-frame` flows, dispatch known events and inspect the resulting app-db. The REPL is the fastest way to discover what an existing namespace returns.

## Gotchas

- `with-redefs` looks like a seam, but vars on ClojureScript are not reified at runtime in the same way; the rebind only affects code that resolves the var dynamically. Code that closes over the original value at definition time will keep using the original. Prefer dependency injection.
- Reagent reactions and re-frame subscriptions look like pure functions but participate in a reactive graph. Refactoring that changes when a subscription evaluates can break rendering even if the final output is unchanged.
- React hooks (used through Helix, UIx, or interop) have ordering requirements. "Extract helper" that pulls a hook call into a conditional branch breaks the hook contract. Mention this whenever a refactor touches a hook-bearing function.
- Source maps cover stack traces but not `console.log` of compiled code. When debugging interop, log Clojure values before they hit `clj->js`, not after.
