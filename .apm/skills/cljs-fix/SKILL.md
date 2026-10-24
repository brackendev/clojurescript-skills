---
name: cljs-fix
description: >-
  Fix a ClojureScript project with lint, format, test, advanced, and dry
  (duplicate-form) steps. The format step writes by default. Use when asked to
  lint, format, test, or check a ClojureScript project (shadow-cljs.edn,
  figwheel-main.edn, or cljs.main), to fix clj-kondo warnings or cljfmt
  formatting in .cljs files, or to verify an :advanced build. For Electric or
  Fulcro projects, prefer /electric-fix or /fulcro-fix when installed. Use
  --report to replace the formatting write with cljfmt check.
argument-hint: "[lint|format|test|advanced|dry] [--report] [all]"
user-invocable: true
---

# ClojureScript Fix

Run lint, format, test, advanced-compilation, and duplicate-form checks on ClojureScript source files. The `format` step writes by default; `lint`, `test`, `advanced`, and `dry` are pure-read of source. See the package's [CONVENTIONS.md](https://github.com/brackendev/clojurescript-skills/blob/master/CONVENTIONS.md) for the argument grammar this skill follows.

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

This skill excludes vendored, generated, and dependency-locked paths from the file set it walks. The filter combines `.gitignore` matches and a hardcoded floor (`node_modules/`, `vendor/`, `third_party/`, `.bundle/`, `target/`, `build/`, `dist/`, `out/`, `.shadow-cljs/`, `cljd-out/`, `*.lock`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `Gemfile.lock`, `Cargo.lock`, `poetry.lock`, `composer.lock`). The `advanced` step's writes to the compiler `:output-dir` are build artifacts produced by the compiler itself and are outside the source-mutation scope of the rule. Naming a vendored source path directly through `<path>` or `<glob>` bypasses the filter for that target. When the filter excludes any path, add a single "skipped N vendored or generated paths" line to the report. Under `--report`, list every skipped path. The full policy is Rule 4 in the package's [CONVENTIONS.md](https://github.com/brackendev/clojurescript-skills/blob/master/CONVENTIONS.md).

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

Run the ClojureScript test suite. The standard idiom is a Node test runner namespace that calls `cljs.test/run-tests`. `cljs.main --compile` only compiles, so run the output with Node afterward:

```bash
clj -M:test && node out/test.js
```

The `:test` alias compiles the test runner for the Node target into `out/test.js`. Example alias shape:

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

Report fail if exit code is non-zero, or if the output contains `Cannot infer target type in expression` warnings when `*warn-on-infer*` is set in the project. Those inference failures predict property renaming under advanced compilation and must be resolved before release. The step catches them only when the project sets `(set! *warn-on-infer* true)` in each namespace and `:infer-externs true` in its compiler options. Without these, advanced compilation can rename property names silently.

When the project has no advanced build alias and the namespace cannot be determined, skip this step and record `advanced: SKIPPED`.

### 5. Dry

Scan for duplicate top-level forms with [dry4clj](https://github.com/unclebob/dry4clj). This step is advisory. It reports duplication candidates for review but never fails the run and never halts the pipeline.

Determine the project's production source directories from its build configuration instead of assuming a directory name:

- `deps.edn`: the top-level `:paths` (the `:dry4clj` alias is defined here). Tests usually live under a separate alias's `:extra-paths`, so scanning `:paths` excludes them.
- `shadow-cljs.edn`: `:source-paths`, unless `shadow-cljs.edn` sets `:deps`. With `:deps`, shadow-cljs takes its source paths from `deps.edn`, so use the `deps.edn` paths instead.

Scan the production source paths only and exclude test paths. If no configuration declares source paths, scan the source directories that exist in the repository, excluding any test directory. Do not assume `src`.

Check that `deps.edn` defines a `:dry4clj` alias before running the scan. When it does not, report `SKIP` and show the alias to add under `:aliases`:

```clojure
:dry4clj {:extra-deps {io.github.unclebob/dry4clj {:git/url "https://github.com/unclebob/dry4clj.git"
                                                    :git/sha "5994ef2bcaaae8d12bcd0afb4f091ae09083228c"}}
           :main-opts  ["-m" "dry4clj.core"]}
```

Run dry4clj with EDN output over the resolved paths:

```bash
clj -M:dry4clj --edn <source-paths...>
```

Do not pass `--threshold`, `--min-lines`, or `--min-nodes`. Scoping to production source removes the dominant noise, which is repeated test scaffolding; tightening the score or size knobs either hides real matches or has no effect on exact-structure duplicates.

dry4clj exits 0 after a completed scan, and the EDN output never prints a clean-state message, so do not use the exit code or any text match to detect duplicates. A non-zero exit means the scan itself failed (for example, a file the reader cannot parse) and is reported as `ERROR`. Parse the EDN map `{:candidates [...]}` and classify the result:

| Result   | Condition                                                                                                                  |
|----------|----------------------------------------------------------------------------------------------------------------------------|
| `PASS`   | `:candidates` is empty.                                                                                                     |
| `REVIEW` | `:candidates` has one or more entries. List them and continue.                                                             |
| `ERROR`  | The command fails or its output cannot be parsed. Report the cause, and do not report `PASS`. |
| `SKIP`   | No production source path can be identified, or `deps.edn` defines no `:dry4clj` alias. |

For `REVIEW`, sort candidates by exact matches first (`:score` equal to `1.0`), then by descending `min(:left-nodes, :right-nodes)`, then by descending `:score`. Report the scanned source paths, the candidate count, and the highest-priority candidates with their score, node counts, and both file ranges. Note that each candidate needs source inspection before extraction, and that test directories were excluded because repeated test scaffolding is often intentional.

Upstream dry4clj scans `.clj`, `.cljc`, and `.cljs` files by default, so the ClojureScript source is covered without additional configuration.

## Report

After running all requested steps, print a summary:

```
ClojureScript Fix Results:
  Lint:     PASS/FAIL/SKIPPED
  Format:   PASS/FAIL/SKIPPED
  Test:     PASS/FAIL/SKIPPED
  Advanced: PASS/FAIL/SKIPPED
  Dry:      PASS/REVIEW/ERROR/SKIPPED
```

If `--report` was passed, append `(report mode: format checked, not written)` after the summary. If a lint, format, test, or advanced step fails, stop and report the failure; do not continue to subsequent steps.

## Gotchas

- clj-kondo versions before 2026.05.25 do not recognize `cljs.core/await` and report `Unresolved symbol: await` in `^:async` functions. Treat that lint failure as an outdated clj-kondo, upgrade it, and re-run the lint step. Do not suppress the symbol in `.clj-kondo/config.edn`.
- `clj-kondo` lints `.cljs` and `.cljc` files but does not run the ClojureScript compiler. Type-related failures (`Cannot infer target type`, missing externs) only surface in the `advanced` step.
- The `advanced` step is slow on first run (the Google Closure Compiler does whole-program analysis). Caching is automatic across runs.
- dry4clj reads source without resolving namespace aliases, so an alias-qualified auto-resolved keyword such as `::str/ok` stops the dry scan with "Invalid token" and a non-zero exit. Plain auto-resolved keywords such as `::ok` read correctly. Report the file and the error as `ERROR`, keep the step advisory, and do not rewrite source to work around the reader.
