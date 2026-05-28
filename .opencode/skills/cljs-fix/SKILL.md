---
name: cljs-fix
description: "Fix a ClojureScript project (lint, format, test, advanced, dry); format writes by default"
argument-hint: "[lint|format|test|advanced|dry] [--report] [all]"
user-invocable: true
disable-model-invocation: true
---

# ClojureScript Fix

Run lint, format, test, advanced-compilation, and duplicate-form checks on ClojureScript source files. The `format` step writes by default; `lint`, `test`, `advanced`, and `dry` are pure-read of source. See `CONVENTIONS.md` in the repo root for the argument grammar this skill follows.

## Arguments

| Input             | Target                                                                       |
|-------------------|------------------------------------------------------------------------------|
| (no argument)     | Run all five steps (`lint`, `format`, `test`, `advanced`, `dry`) on the project (src, test) |
| `all`             | Same as (no argument); accepted for family consistency                       |
| `lint`            | Run lint only                                                                |
| `format`          | Run format only                                                              |
| `test`            | Run tests only                                                               |
| `advanced`        | Run advanced-compilation sanity check only                                   |
| `dry`             | Run dry only                                                                 |
| `--report`        | Replace `cljfmt fix` with non-writing `cljfmt check` in the format step      |

Step keywords are combinable (for example, `/cljs-fix lint test dry`). The `--report` flag may appear in any position. When `--report` is present without an explicit step keyword, every step still runs; only the format step's behavior changes.

The step keyword `dry` is the dry4clj duplicate-form scan, not a dry-run mode. Only the literal `--report` token disables writes.

## Mutation

Only the `format` step writes source. It runs `clj -M:cljfmt fix` by default, rewriting `.cljs` and `.cljc` files in place. With `--report`, the step runs `clj -M:cljfmt check`, which exits non-zero when files would change but does not write.

The `advanced` step writes generated JavaScript under `out/` (or the project's compiler `:output-dir`), which is a build artifact, not source. The `lint`, `test`, and `dry` steps are pure-read regardless of `--report`.

This skill excludes vendored, generated, and dependency-locked paths from the file set it walks. The filter combines `.gitignore` matches and a hardcoded floor (`node_modules/`, `vendor/`, `third_party/`, `.bundle/`, `target/`, `build/`, `dist/`, `out/`, `.shadow-cljs/`, `cljd-out/`, `*.lock`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `Gemfile.lock`, `Cargo.lock`, `poetry.lock`, `composer.lock`). The `advanced` step's writes to the compiler `:output-dir` are build artifacts produced by the compiler itself and are outside the source-mutation scope of the rule. Naming a vendored source path directly through `<path>` or `<glob>` bypasses the filter for that target. The full policy is Rule 4 in CONVENTIONS.md.

## Steps

Parse `$ARGUMENTS` to determine which steps to run and whether `--report` is present. If no step keyword is supplied (or only `all` is supplied), run every step in order.

### 1. Lint

Run clj-kondo on the ClojureScript source. clj-kondo lints `.cljs` and `.cljc` natively:

```bash
clj-kondo --lint src test
```

If the project has no `test/` directory, lint `src` only.

Report pass if exit code is 0 and there are no errors. Show the clj-kondo output regardless.

### 2. Format

Without `--report`, run cljfmt to fix formatting. cljfmt covers `.cljs` and `.cljc` along with `.clj`:

```bash
clj -M:cljfmt fix
```

With `--report`, run cljfmt in check mode (no writes):

```bash
clj -M:cljfmt check
```

Report pass if exit code is 0, fail otherwise. Show any formatting changes (under `fix`) or the diff (under `check`).

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
ClojureScript Fix Results:
  Lint:     PASS/FAIL/SKIPPED
  Format:   PASS/FAIL/SKIPPED
  Test:     PASS/FAIL/SKIPPED
  Advanced: PASS/FAIL/SKIPPED
  Dry:      PASS/FAIL/SKIPPED
```

If `--report` was passed, append `(report mode: format checked, not written)` after the summary. If any step fails, stop and report the failure. Do not continue to subsequent steps.

## Gotchas

- `clj-kondo` lints `.cljs` and `.cljc` files but does not run the ClojureScript compiler. Type-related failures (`Cannot infer target type`, missing externs) only surface in the `advanced` step.
- The `test` step assumes the project compiles its test suite for Node and runs through `cljs.main`. shadow-cljs and figwheel-main projects need their own command; detect the toolchain before invoking.
- The `advanced` step is slow on first run (the Google Closure Compiler does whole-program analysis). Caching is automatic across runs.
- Set `(set! *warn-on-infer* true)` per namespace and `:infer-externs true` in the project's compiler options so the `advanced` step catches inference failures. Without these, advanced compilation can silently rename property names.
