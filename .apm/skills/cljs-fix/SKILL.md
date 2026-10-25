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
argument-hint: "[lint|format|test|advanced|dry] [--report] [all|<path>|<glob>]"
user-invocable: true
---

# ClojureScript Fix

Run lint, format, test, advanced-compilation, and duplicate-form checks on ClojureScript source files. The `format` step writes by default; `lint`, `test`, `advanced`, and `dry` are pure-read of source. See the package's [CONVENTIONS.md](https://github.com/brackendev/clojurescript-skills/blob/master/CONVENTIONS.md) for the argument grammar this skill follows.

## Arguments

| Input             | Target                                                                       |
|-------------------|------------------------------------------------------------------------------|
| (no argument)     | Run all five steps (`lint`, `format`, `test`, `advanced`, `dry`) on the project's source and test paths |
| `all`             | Same as (no argument); accepted for family consistency                       |
| `lint`            | Run lint only                                                                |
| `format`          | Run format only                                                              |
| `test`            | Run tests only                                                               |
| `advanced`        | Run advanced-compilation sanity check only                                   |
| `dry`             | Run dry only                                                                 |
| `<path>` `<glob>` | Restrict the lint, format, and dry steps to those files or directories. The `test` and `advanced` steps ignore paths and always run the project's test suite and build |
| `--report`        | Replace `cljfmt fix` with non-writing `cljfmt check` in the format step      |

Step keywords are combinable (for example, `/cljs-fix lint test dry`), and paths combine with them (`/cljs-fix format src/app`). The `--report` flag may appear in any position. When `--report` is present without an explicit step keyword, every step still runs, and only the format step's behavior changes.

The step keyword `dry` is the dry4clj duplicate-form scan, not a dry-run mode. Only the literal `--report` token disables writes.

## Mutation

Only the `format` step writes source. It runs `cljfmt fix` through the runner the Format step below selects, rewriting `.cljs`, `.cljc`, and other Clojure source files in place. With `--report`, it runs `cljfmt check` through the same runner, which exits non-zero when files would change but does not write.

The `advanced` step writes generated JavaScript under `out/` (or the project's compiler `:output-dir`), which is a build artifact, not source. The `lint`, `test`, and `dry` steps are pure-read regardless of `--report`.

This skill excludes vendored, generated, and dependency-locked paths from the file set it walks. The filter combines `.gitignore` matches and a hardcoded floor (`node_modules/`, `vendor/`, `third_party/`, `.bundle/`, `target/`, `build/`, `dist/`, `out/`, `.shadow-cljs/`, `cljd-out/`, `*.lock`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `Gemfile.lock`, `Cargo.lock`, `poetry.lock`, `composer.lock`). The `advanced` step's writes to the compiler `:output-dir` are build artifacts produced by the compiler itself and are outside the source-mutation scope of the rule. Naming a vendored source path directly through `<path>` or `<glob>` bypasses the filter for that target. When the filter excludes any path, add a single "skipped N vendored or generated paths" line to the report. Under `--report`, list every skipped path. The full policy is Rule 4 in the package's [CONVENTIONS.md](https://github.com/brackendev/clojurescript-skills/blob/master/CONVENTIONS.md).

## Steps

Parse `$ARGUMENTS` to determine which steps to run, which paths or globs it names, and whether `--report` is present. If no step keyword is supplied (or only `all` is supplied), run every step in order.

### Source and test paths

The lint and format steps run on the project's source and test paths. When the arguments name paths or globs, use those instead, and expand each glob to the files it matches. Otherwise resolve the paths from the build configuration instead of assuming directory names:

- `deps.edn`: the top-level `:paths`, plus the `:extra-paths` of the `:test` alias.
- `shadow-cljs.edn`: `:source-paths`, unless `shadow-cljs.edn` sets `:deps`. With `:deps`, shadow-cljs takes its source paths from `deps.edn`, so use the `deps.edn` paths instead, plus the `:extra-paths` of the aliases that `:deps {:aliases [...]}` names.
- Leiningen `project.clj`: `:source-paths` and `:test-paths`.

Skip resource directories and paths that do not exist. If no configuration declares paths, use the `src` and `test` directories that exist.

### 1. Lint

Run clj-kondo on the resolved paths. clj-kondo lints `.cljs` and `.cljc` natively:

```bash
clj-kondo --lint <paths...>
```

Report pass if exit code is 0 and there are no errors. Show the clj-kondo output regardless.

### 2. Format

Run cljfmt with the first runner the project supports, in the order of this table, and check each condition before running its command. Without a `:cljfmt` alias, `clojure -M:cljfmt fix` treats `fix` as a script path and fails with "fix (No such file or directory)", after the warning "Specified aliases are undeclared and are not being used: [:cljfmt]" when the run rebuilds the classpath.

| Runner | When | Fix | Check |
|--------|------|-----|-------|
| Project alias | `deps.edn` defines a `:cljfmt` alias under `:aliases` | `clojure -M:cljfmt fix` | `clojure -M:cljfmt check` |
| Leiningen plugin | `project.clj` lists `dev.weavejester/lein-cljfmt` in `:plugins` | `lein cljfmt fix` | `lein cljfmt check` |
| Standalone binary | `command -v cljfmt` succeeds | `cljfmt fix` | `cljfmt check` |
| Clojure CLI tool | `clojure -Ttools list` shows a tool named `cljfmt` | `clojure -Tcljfmt fix` | `clojure -Tcljfmt check` |

Without `--report`, run the Fix command. With `--report`, run the Check command, which writes nothing.

When no runner is available, report `SKIPPED`, show the alias to add under `:aliases` in `deps.edn`, and name the cljfmt README (https://github.com/weavejester/cljfmt) for installing the binary, the Clojure CLI tool, or the Leiningen plugin instead:

```clojure
:cljfmt {:replace-deps {dev.weavejester/cljfmt {:mvn/version "0.16.6"}}
         :main-opts    ["-m" "cljfmt.main"]}
```

`:replace-deps` runs cljfmt without the project's dependencies, which a formatter does not need, so a conflicting or unresolvable project dependency cannot stop the format step.

**Configuration.** cljfmt loads `.cljfmt.edn` or `cljfmt.edn` from the project directory or the nearest parent directory that has one. It ignores `.cljfmt.clj` and `cljfmt.clj` with the warning "Warning: .clj config file '.cljfmt.clj' detected but --read-clj-config-files is not set. Ignoring config file.", because it would read those files as Clojure code. Never fail the step because of the configuration file's name. When the project has only a `.clj` configuration file, ask the user before adding `--read-clj-config-files` after `fix` or `check`. The project alias and the binary accept that flag, and the Clojure CLI tool cannot read a `.clj` configuration file at all. If the user declines, or the runner cannot read the file, report `SKIPPED` with the reason, because cljfmt would otherwise format against its defaults instead of the project's style. The Leiningen plugin reads its options from the `:cljfmt` map in `project.clj`.

**Paths.** Outside Leiningen, cljfmt formats `src`, `test`, and `project.clj` unless the configuration sets `:paths`. Pass the paths when the arguments name paths, or when the resolved source and test paths differ from `src` and `test` and the configuration sets no `:paths`. The project alias, the binary, and the Leiningen plugin take them as arguments after the command (`clojure -M:cljfmt fix src/main src/test`). The Clojure CLI tool takes them as an option (`clojure -Tcljfmt fix :paths '["src/main" "src/test"]'`). Pass only paths that exist, because cljfmt stops with "No such file:" on a missing one. The default file pattern, `#"\.(?:clj[csx]?|bb)$"`, covers `.cljs`, `.cljc`, and `.clj` files.

Report pass if the exit code is 0, fail otherwise. Show any formatting changes (under `fix`) or the diff (under `check`).

### 3. Test

Run the ClojureScript test suite. The standard idiom is a Node test runner namespace that calls `cljs.test/run-tests`. `cljs.main --compile` only compiles, so run the output with Node afterward:

```bash
clojure -M:test && node out/test.js
```

The `:test` alias compiles the test runner for the Node target into `out/test.js`. Example alias shape:

```clojure
:test {:extra-paths ["test"]
       :main-opts   ["-m" "cljs.main"
                     "--target" "node"
                     "--output-to" "out/test.js"
                     "--compile" "<namespace>.test-runner"]}
```

If the project uses shadow-cljs, compile its `:node-test` build and run the file named by that build's `:output-to` (for a build named `:test` with `:output-to "out/node-tests.js"`, `npx shadow-cljs compile test && node out/node-tests.js`), or use the project's documented test command. If the project uses figwheel-main, follow the figwheel-main test runner instructions. Detect the toolchain by inspecting `deps.edn`, `shadow-cljs.edn`, or `figwheel-main.edn` before running.

When the project has none of these test configurations, report `SKIPPED` and say which configuration is missing. Report pass if the runner exits 0 and the output contains no failure or error summary. Show the runner output.

### 4. Advanced

Compile the project under `:advanced` optimizations as a production-build sanity check:

```bash
clojure -M -m cljs.main --optimizations advanced --compile <main-ns>
```

If the project defines a `:build` alias for the production build, prefer it:

```bash
clojure -M:build
```

Report fail if exit code is non-zero, or if the output contains `Cannot infer target type in expression` warnings. Those inference failures predict property renaming under advanced compilation and must be resolved before release. The compiler prints them only for namespaces that set `(set! *warn-on-infer* true)`, or for the whole build when its compiler options set `:warnings {:infer-warning true}`. The `^js` hints that clear them protect property names only when `:infer-externs true` is set (`:target :bundle` sets it). Without inference, advanced compilation renames hinted properties without any warning.

When the project has no advanced build alias and the namespace cannot be determined, skip this step and record `advanced: SKIPPED`.

### 5. Dry

Scan for duplicate top-level forms with [dry4clj](https://github.com/unclebob/dry4clj). This step is advisory. It reports duplication candidates for review but never fails the run and never halts the pipeline.

Determine the project's production source directories from its build configuration instead of assuming a directory name:

- `deps.edn`: the top-level `:paths` (the `:dry4clj` alias is defined here). Tests usually live under a separate alias's `:extra-paths`, so scanning `:paths` excludes them.
- `shadow-cljs.edn`: `:source-paths`, unless `shadow-cljs.edn` sets `:deps`. With `:deps`, shadow-cljs takes its source paths from `deps.edn`, so use the `deps.edn` paths instead.

Scan the production source paths only and exclude test paths. If no configuration declares source paths, scan the source directories that exist in the repository, excluding any test directory. Do not assume `src`. When the arguments name paths or globs, scan exactly those, including a test path the user named.

Check that `deps.edn` defines a `:dry4clj` alias before running the scan. When it does not, report `SKIPPED` and show the alias to add under `:aliases`:

```clojure
:dry4clj {:extra-deps {io.github.unclebob/dry4clj {:git/url "https://github.com/unclebob/dry4clj.git"
                                                   :git/sha "5994ef2bcaaae8d12bcd0afb4f091ae09083228c"}}
          :main-opts  ["-m" "dry4clj.core"]}
```

Run dry4clj with EDN output over the resolved paths:

```bash
clojure -M:dry4clj --edn <source-paths...>
```

Do not pass `--threshold`, `--min-lines`, or `--min-nodes`. Scoping to production source removes the dominant noise, which is repeated test scaffolding; tightening the score or size knobs either hides real matches or has no effect on exact-structure duplicates.

dry4clj exits 0 after a completed scan, and the EDN output never prints a clean-state message, so do not use the exit code or any text match to detect duplicates. A non-zero exit means the scan itself failed (for example, a file the reader cannot parse) and is reported as `ERROR`. Parse the EDN map `{:candidates [...]}` and classify the result:

| Result   | Condition                                                                                                                  |
|----------|----------------------------------------------------------------------------------------------------------------------------|
| `PASS`   | `:candidates` is empty.                                                                                                     |
| `REVIEW` | `:candidates` has one or more entries. List them and continue.                                                             |
| `ERROR`  | The command fails or its output cannot be parsed. Report the cause, and do not report `PASS`. |
| `SKIPPED` | No production source path can be identified, or `deps.edn` defines no `:dry4clj` alias. |

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

- The lint step needs a clj-kondo release that recognizes `cljs.core/await`, which the latest release does. Upgrade clj-kondo instead of suppressing an `await` finding in `.clj-kondo/config.edn`.
- `clj-kondo` lints `.cljs` and `.cljc` files but does not run the ClojureScript compiler. Type-related failures (`Cannot infer target type`, missing externs) only surface in the `advanced` step.
- The `advanced` step is slow on every run, because the Google Closure Compiler optimizes the whole program each time, even when no source changed.
- dry4clj reads source without resolving namespace aliases, so an alias-qualified auto-resolved keyword such as `::str/ok` stops the dry scan with "Invalid token" and a non-zero exit. Plain auto-resolved keywords such as `::ok` read correctly. Report the file and the error as `ERROR`, keep the step advisory, and do not rewrite source to work around the reader.
