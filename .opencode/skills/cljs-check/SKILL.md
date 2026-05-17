---
name: cljs-check
description: Run the ClojureScript quality pipeline (lint, format, test, advanced, dry)
argument-hint: "[lint|format|test|advanced|dry]"
user-invocable: true
disable-model-invocation: true
---

# ClojureScript Quality Check

Run lint, format, test, advanced-compilation, and duplicate-form checks on ClojureScript source files.

## Steps

Parse `$ARGUMENTS` to determine which steps to run. If empty, run all steps in order.

| Argument | Steps |
|----------|-------|
| (empty) | lint, format, test, advanced, dry |
| `lint` | lint only |
| `format` | format only |
| `test` | test only |
| `advanced` | advanced only |
| `dry` | dry only |

Multiple arguments can be combined (e.g., `lint test dry`).

### 1. Lint

Run clj-kondo on the ClojureScript source. clj-kondo lints `.cljs` and `.cljc` natively:

```bash
clj-kondo --lint src test
```

If the project has no `test/` directory, lint `src` only.

Report pass if exit code is 0 and there are no errors. Show the clj-kondo output regardless.

### 2. Format

Run cljfmt to fix formatting. cljfmt covers `.cljs` and `.cljc` along with `.clj`:

```bash
clj -M:cljfmt fix
```

Report pass if exit code is 0, fail otherwise. Show any formatting changes.

### 3. Test

Run the ClojureScript test suite. The standard idiom is a Node test runner namespace that calls `cljs.test/run-tests`:

```bash
clj -M:test
```

The `:test` alias should compile and run the test runner under the Node target. Example alias shape (created by `/cljs-new`):

```clojure
:test {:extra-paths ["test"]
       :main-opts   ["-m" "cljs.main"
                     "--target" "node"
                     "--output-to" "out/test.js"
                     "--compile" "<namespace>.test-runner"]}
```

If the project uses shadow-cljs, invoke its test runner instead (`npx shadow-cljs compile test && node out/test.js`, or the project's documented test command). If the project uses figwheel-main, follow the figwheel-main test runner instructions. Detect the toolchain by inspecting `deps.edn`, `shadow-cljs.edn`, or `figwheel-main.edn` before running.

Report pass if the runner exits 0 and the output contains no failure or error summary. Show the runner output.

### 4. Advanced

Compile the project under `:advanced` optimizations as a production-build sanity check:

```bash
clj -M -m cljs.main --optimizations advanced --compile <main-ns>
```

If the project defines a `:build` alias for the production build, prefer it:

```bash
clj -M:build
```

Report fail if exit code is non-zero, or if the output contains `Cannot infer target type in expression` warnings when `*warn-on-infer*` is set in the project. Those inference failures predict property renaming under advanced compilation and must be resolved before release.

When the project has no advanced build alias and the namespace cannot be determined, skip this step and record `advanced: SKIPPED`.

### 5. Dry

Scan for duplicate top-level forms with [dry4clj](https://github.com/unclebob/dry4clj):

```bash
clj -M:dry4clj src test
```

If the project has no `test/` directory, scan `src` only.

dry4clj exits 0 whether or not it finds candidates, so the step must inspect output. Report pass only when exit code is 0 and stdout contains the literal `No duplicate candidates found.`. Otherwise report fail and show the reported candidates. The project's `deps.edn` must define a `:dry4clj` alias; if the alias is missing, the Clojure CLI exits non-zero and the step fails.

Upstream dry4clj scans `.clj`, `.cljc`, and `.cljs` files by default, so the ClojureScript source is covered without additional configuration.

## Report

After running all requested steps, print a summary:

```
ClojureScript Check Results:
  Lint:     PASS/FAIL/SKIPPED
  Format:   PASS/FAIL/SKIPPED
  Test:     PASS/FAIL/SKIPPED
  Advanced: PASS/FAIL/SKIPPED
  Dry:      PASS/FAIL/SKIPPED
```

If any step fails, stop and report the failure. Do not continue to subsequent steps.

## Gotchas

- `clj-kondo` lints `.cljs` and `.cljc` files but does not run the ClojureScript compiler. Type-related failures (`Cannot infer target type`, missing externs) only surface in the `advanced` step.
- The `test` step assumes the project compiles its test suite for Node and runs through `cljs.main`. shadow-cljs and figwheel-main projects need their own command; detect the toolchain before invoking.
- The `advanced` step is slow on first run (the Google Closure Compiler does whole-program analysis). Caching is automatic across runs.
- Set `(set! *warn-on-infer* true)` per namespace and `:infer-externs true` in the project's compiler options so the `advanced` step catches inference failures. Without these, advanced compilation can silently rename property names.
