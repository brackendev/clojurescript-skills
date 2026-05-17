---
name: cljs-smells-review
description: Review ClojureScript code against ClojureScript-specific smells (placeholder, see TODO)
argument-hint: "[scope or options...]"
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
disable-model-invocation: true
---

# ClojureScript Smells Review (Placeholder)

This skill is a placeholder. ClojureScript-specific code review against a curated smells catalog is planned but not yet implemented. Running `/cljs-smells-review` today shows this notice and exits.

## Status

**Not implemented.** A ClojureScript-specific smells catalog is pending. Reusing the JVM-Clojure [clj-smells catalog](https://github.com/nufuturo-ufcg/clj-smells-catalog) catches the cross-dialect smells the `/clj-smells-review` skill in [clojure-skills](https://github.com/brackendev/clojure-skills) already covers, but it does not flag the ClojureScript-specific failure modes that come from JavaScript interop, externs handling, advanced compilation, and the JS event loop.

For host-neutral smells in `.cljs` and `.cljc` files, run `/clj-smells-review` from `clojure-skills`. It already filters `.cljs` files into scope and uses clj-kondo plus an LLM pass against the curated catalog.

## Planned Categories

When implemented, this review will cover ClojureScript-specific failure modes:

- **Advanced-compilation hazards**: `aget` on objects, property access without `^js` hints on values that flow into advanced builds, `goog.object/get` with non-string keys, missing externs for libraries that escape inference.
- **Macro stage confusion**: macros defined in `.cljs` files (must be `.clj` or `.cljc`), `:require-macros` used where `:include-macros true` would suffice, `:refer` used for symbols that are actually macros.
- **Truthiness and number traps**: `if` on values that are `0`, `""`, or `NaN` without `^boolean` hints, equality checks across `0` and `0.0` (which evaluates to `true` on ClojureScript), reliance on `Ratio` or `BigDecimal` literals that do not exist on the host.
- **Async / event-loop patterns**: blocking patterns ported from JVM code, `core.async` `<!` outside a `go` block in code that runs in the browser, unhandled rejected promises, leaks from unwrapped `setTimeout` / `setInterval`.
- **Reagent / re-frame patterns**: derefing reagent atoms outside reactive contexts, side-effecting subscriptions, mutating component-local state across render boundaries, missing keys on lists, hooks called conditionally.
- **JS interop hygiene**: `js->clj` without `:keywordize-keys true` followed by `:foo` lookups that miss, `clj->js` without symmetric `:keyword-fn`, direct `js/window` access in code that should run server-side under Node.
- **Build-tool drift**: shadow-cljs and figwheel-main configuration disagreeing with `deps.edn`, `:foreign-libs` paths that point at moved files, mixed npm and CLJSJS dependencies for the same library.

## Tracking

See `TODO.md` in the repo root.

## Output

When invoked, print this notice and exit:

```
cljs-smells-review is not yet implemented.

A ClojureScript-specific smells catalog is in development. For now:
  - Run /clj-smells-review from clojure-skills for host-neutral smells in .cljs files.
  - Use /cljs-check for lint, format, test, advanced-compilation, and duplicate-form checks.
  - The clojurescript skill (auto-invoked) covers idiomatic ClojureScript style and JS interop.

To track progress, see TODO.md in the clojurescript-skills repo.
```

Do not run any analysis. Do not invoke clj-kondo. Do not consult the JVM clj-smells catalog.
