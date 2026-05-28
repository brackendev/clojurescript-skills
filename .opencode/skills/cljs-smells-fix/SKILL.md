---
name: cljs-smells-fix
description: "Fix ClojureScript code against ClojureScript-specific smells; placeholder, see TODO. When implemented, auto-applies mechanical and DEFECT-tier findings and reports the rest. Pass --report to disable writes."
argument-hint: "[path|all] [--report]"
allowed-tools: Bash, Read, Edit, Grep, Glob
user-invocable: true
disable-model-invocation: true
---

# ClojureScript Smells Fix (Placeholder)

This skill is a placeholder. ClojureScript-specific code review against a curated smells catalog is planned but not yet implemented. Running `/cljs-smells-fix` today shows this notice and exits. The argument grammar and mutation contract land now so the eventual implementation has a contract to honor. See `CONVENTIONS.md` in the repo root for the standard.

## Arguments

| Input              | Target                                                                       |
|--------------------|------------------------------------------------------------------------------|
| (no argument)      | Fix changed files only (staged + unstaged)                                   |
| `all`              | Fix the full codebase, sampling high-risk and high-traffic namespaces        |
| `path/to/dir`      | Fix files under directory                                                    |
| `path/to/file.cljs`| Fix specific file                                                            |
| `--report`         | Disable all writes; produce the report only                                  |

Examples:

```
/cljs-smells-fix
/cljs-smells-fix src/my_app
/cljs-smells-fix src/my_app/core.cljs
/cljs-smells-fix all
/cljs-smells-fix --report
```

When implemented, the skill mirrors the mutation contract of `/clj-smells-fix`: Stage 1 mechanical findings and Stage 2 `DEFECT`-tier findings within a defined safety band are auto-applied; `SMELL` and `HINT` findings remain report-only; `--report` disables all writes.

When (no argument) is invoked outside a git worktree, the eventual implementation will ask the operator what to fix rather than widening silently to `all`.

The eventual implementation will honor Rule 4 in CONVENTIONS.md: vendored, generated, and dependency-locked paths are excluded from broad scopes (`.gitignore` matches plus a hardcoded floor of `node_modules/`, `vendor/`, `third_party/`, `.bundle/`, `target/`, `build/`, `dist/`, `out/`, `.shadow-cljs/`, `cljd-out/`, and the standard lock files). Naming a vendored path directly through `<path>` or `<glob>` bypasses the filter for that target.

## Status

**Not implemented.** A ClojureScript-specific smells catalog is pending. Reusing the JVM-Clojure [clj-smells catalog](https://github.com/nufuturo-ufcg/clj-smells-catalog) catches the cross-dialect smells the `/clj-smells-fix` skill in [clojure-skills](https://github.com/brackendev/clojure-skills) already covers, but it does not flag the ClojureScript-specific failure modes that come from JavaScript interop, externs handling, advanced compilation, and the JS event loop.

For host-neutral smells in `.cljs` and `.cljc` files, run `/clj-smells-fix` from `clojure-skills`. It already filters `.cljs` files into scope and uses clj-kondo plus an LLM pass against the curated catalog.

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
cljs-smells-fix is not yet implemented.

A ClojureScript-specific smells catalog is in development. For now:
  - Run /clj-smells-fix from clojure-skills for host-neutral smells in .cljs files.
  - Use /cljs-fix for lint, format, test, advanced-compilation, and duplicate-form checks.
  - The clojurescript skill (auto-invoked) covers idiomatic ClojureScript style and JS interop.

To track progress, see TODO.md in the clojurescript-skills repo.
```

Do not run any analysis. Do not invoke clj-kondo. Do not consult the JVM clj-smells catalog. Do not write to any source file.
