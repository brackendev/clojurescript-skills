# Changelog

## [Unreleased]

## [0.1.4] - 2026-05-22

### Changed

- The `cljs-smells-review` placeholder is renamed to `cljs-smells-fix` to match the mutating contract adopted by the new `clj-smells-fix` skill in [clojure-skills](https://github.com/brackendev/clojure-skills). The placeholder still prints a "not yet implemented" notice; the rename, argument grammar (`[path|all] [--report]`), and frontmatter all reflect the eventual fix-by-default behavior. The "not yet implemented" notice now points operators at `/clj-smells-fix` for host-neutral smells in `.cljs` files. Operators with a saved `/cljs-smells-review` invocation should replace it with `/cljs-smells-fix`.

## [0.1.3] - 2026-05-20

### Changed

- `CONTRIBUTING.md` is aligned to the family-wide structural template. The Layout row for `CONVENTIONS.md` uses the canonical phrasing; step 1 of "Adding or modifying a skill" no longer carries an inline `CONVENTIONS.md` reminder (the dedicated Skill conventions section now opens with that pointer); the Skill conventions table cites package-specific example skills (`cljs-fix`, `cljs-new`).

## [0.1.2] - 2026-05-20

### Changed

- The `cljs-tidy` skill is renamed to `cljs-fix` to adopt the noun-first canonical naming pattern (`<target>-<verb>`) shared across the agent-skills family. The verb suffix `-fix` consistently signals a mutating quality pipeline (lint, format, test, advanced, dry). Operators with a saved `/cljs-tidy` invocation should replace it with `/cljs-fix`. The skill's behavior is unchanged; only the name moves.

### 0.1.1

#### Added

- A repo-root `CONVENTIONS.md` that defines the argument grammar, scope vocabulary, and mutation defaults every user-invocable skill in this package follows. Three rules cover argument grammar (one sanctioned flag, `--report`), scope vocabulary (`(no argument)`, `all`, `<path>`), and mutation-as-default. The document lists `/cljs-new` as the standard's positional-required exemption and includes an author checklist that runs against every migrated skill.

#### Changed

- The `cljs-check` skill is renamed to `cljs-tidy`. The verb now matches the default behavior: `cljfmt fix` runs by default and rewrites files in the `format` step. Operators with a saved `/cljs-check` invocation should replace it with `/cljs-tidy`. The new `--report` flag swaps the format step for `cljfmt check`, which previews diffs without writing; `lint`, `test`, `advanced`, and `dry` are pure-read of source regardless.
- The `cljs-upgrade` skill adds a `--report` flag that prints the current `:mvn/version` for `org.clojure/clojurescript` and the latest released version from Maven Central without writing `deps.edn` or running the advanced compile. The frontmatter description and `argument-hint` document the new grammar.
- The `cljs-smells-review` skill renames its prior section structure to the canonical `## Arguments` table and clarifies in the frontmatter that the skill is pure-report (never writes). The opt-in `fix` modifier never existed for this skill, so no operator-facing invocations change.
- The `cljs-new` skill adds a `## Arguments` section that documents its positional `<project-name>` exemption and a `## Mutation` section that lists the files it writes.

## 0.1.0

### Added

- Initial release. ClojureScript skills layered on top of [clojure-skills](https://github.com/brackendev/clojure-skills) so the baseline can stay host-neutral across `.clj`, `.cljs`, `.cljc`, and `.cljd`.
- **clojurescript** (model-invoked): JavaScript interop (`js/`, `.-prop`, method calls, `set!` on properties, `js-obj`, `clj->js`, `js->clj`, `goog.object/get` and `goog.object/set`), host-typed exception handling (`catch :default`, `js/Error`), externs inference (`:infer-externs`, `*warn-on-infer*`, `^js`, `^js/Foo`), macro stage separation (`:require-macros`, `:refer-macros`, `:include-macros`), namespace forms (`:import` for Google Closure classes only, no `gen-class`), JS-flavored numbers (no `Ratio`, `BigInt`, `BigDecimal`; `(= 0.0 0) ⇒ true`), `^boolean` type hint and checked-`if` avoidance, single-character-string handling, atoms-only state management, reader conditionals (`#?`, `#?@`, `.cljc`), CLJS-specific aliases (`cljs.core.async`, `goog.object`, `goog.string`, `goog.dom`), and the `cljs.main` project workflow. Defers to the [clojure](https://github.com/brackendev/clojure-skills) baseline for host-neutral style. Pulls deeper detail from `references/project-workflows.md` on demand (`cljs.main` CLI, `deps.edn`, browser REPL, Node REPL, source maps, advanced compilation, npm interop, shadow-cljs and figwheel-main as community alternatives).
- **clojurescript-lenses** (model-invoked): Code-lenses translations layered on top of `clojure-lenses` in [clojure-skills](https://github.com/brackendev/clojure-skills). Records the ClojureScript-specific deltas: JavaScript interop boundaries, externs and advanced compilation, macro stage separation, the JS event loop, and the absence of refs / agents / STM and var reification. Covers the four default code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't Validate) and the two opt-in philosophies (APOSD, Legacy Code) that activate when their lens is added with `+aposd` or `+legacy-code` or invoked directly. Auto-triggers alongside the [code-lenses](https://github.com/brackendev/code-lenses) plugin in ClojureScript work.
- **cljs-new** (user-invoked): Scaffold a new ClojureScript project using the official `cljs.main` entry point. Creates `deps.edn` with `:test`, `:build`, and `:cljfmt` aliases, a browser-targeted `index.html`, a Node-targeted test runner namespace, `.cljfmt.edn`, `.gitignore`, and `.clj-kondo/config.edn` (with `cljs.test` macros lint-as-clojure.test). Fetches the latest ClojureScript version from Maven Central before writing `deps.edn`.
- **cljs-check** (user-invoked): Run the ClojureScript quality pipeline (lint, format, test, advanced, dry). Lint runs `clj-kondo --lint src test`; format runs `clj -M:cljfmt fix`; test runs `clj -M:test` (Node target by default, detects shadow-cljs / figwheel-main); advanced runs `clj -M:build` or `clj -M -m cljs.main --optimizations advanced --compile <main-ns>` as a production-build sanity check that surfaces externs-inference failures; dry runs `clj -M:dry4clj src test`. Each step is also addressable on its own.
- **cljs-upgrade** (user-invoked): Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central. Records the previous version, fetches the latest from Maven Central's `solrsearch` endpoint, updates `deps.edn`, refreshes dependencies with `clj -P`, runs the project's advanced-compile build, and refreshes clj-kondo imports. Reverts the version on compile failure.
- **cljs-smells-review** (user-invoked, placeholder): Reserves the command name for a future ClojureScript-specific smells review. Currently prints a "not yet implemented" notice that points users at `/clj-smells-review` in `clojure-skills` for host-neutral smells in `.cljs` files. Documents the planned categories (advanced-compilation hazards, macro stage confusion, truthiness and number traps, async / event-loop patterns, Reagent / re-frame patterns, JS interop hygiene, build-tool drift).
