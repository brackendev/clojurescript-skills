# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.21] - 2026-10-25

### Added

- The `clojurescript` skill opens with Iron Laws (CLJS-1 through CLJS-5): numbered rules that prevent production-only bugs, silent data loss, and security holes on JavaScript hosts. The agent does not write code that breaks a law, and when existing code breaks one, it reports the law ID, the location, and the correct pattern, then asks before changing code outside the task. Key Rules that guarded against bugs or security holes moved into the laws, and the remaining Key Rules are renumbered.
- The `clojurescript` interop reference covers inserting HTML safely: `textContent` for plain text and a sanitizer for content that must stay HTML.
- The interop reference explains that `^js` hints need externs inference, which the stock compiler leaves off by default, and covers `js->clj` key conversion and unsafe URL values.

### Fixed

- The project workflow reference points to `references/interop.md` for the externs guidance, which moved out of `SKILL.md` in 0.1.20.

## [0.1.20] - 2026-10-25

### Added

- The `clojurescript` skill's project workflow reference covers the shadow-cljs REPL: the `.shadow-cljs/nrepl.port` file, switching an nREPL session to a build with `(shadow/repl :build-id)`, returning with `:cljs/quit`, and the running `watch` and connected JavaScript runtime that a build REPL needs.

### Changed

- The `clojurescript` skill loads a short entry file that keeps the Key Rules and routes each task to a reference file: JavaScript interop and externs, async functions and exceptions, compilation stages and reader conditionals, host semantics, and testing. The detailed guidance moved into those files without changes, so less text loads on every matching task and the agent reads the detail for the topic at hand.

## [0.1.19] - 2026-10-25

### Changed

- The Companion packages table describes biff-skills as covering Biff 2 conventions and deployment, because that package no longer scaffolds projects.

### Removed

- The `/cljs-new` scaffolding skill is removed. It wrote a fixed project layout with pinned versions, and the agent could not run it from a natural-language request. The `/cljs-fix` skill still shows the `:test` and `:dry4clj` alias shapes it expects.

## [0.1.18] - 2026-10-24

### Added

- The Companion packages table lists [electric-skills](https://github.com/brackendev/electric-skills), the new companion package for the [Electric](https://github.com/hyperfiddle/electric) v3 full-stack framework, and `CONTRIBUTING.md` cites its Hyperfiddle lavender brand color (`#B7A4C2`).
- The ClojureScript workflows reference notes that shadow-cljs 3.x requires Java 21, and that `:deps` in `shadow-cljs.edn` makes shadow-cljs take its source paths and dependencies from `deps.edn`.

### Changed

- The `/cljs-fix` description points Electric projects to `/electric-fix` when it is installed, alongside `/fulcro-fix` for Fulcro projects.
- The Companion packages table names the major version each framework package targets: Biff 2, Electric v3, and Fulcro 3. The skills in those packages do not apply to other major versions.
- `/cljs-new` adds a `:dry4clj` alias, so `/cljs-fix` runs its duplicate-form step on a new project. When a project has no such alias, `/cljs-fix` reports the dry step as `SKIP` and shows the alias to add, instead of reporting `ERROR`.
- `/cljs-new` states the date its fixed versions were last verified (`org.clojure/clojure` 1.12.6 and cljfmt 0.16.6 on 2026-10-24). The ClojureScript version is still fetched at run time.
- `CONTRIBUTING.md` asks for a changelog entry and a version increment for every change to a published file, including documentation-only changes, which matches the release history. It asked for entries only for user-facing changes.

### Removed

- The `/cljs-smells-fix` placeholder is removed. It only printed a not-implemented notice. Run `/clj-smells-fix` from clojure-skills for host-neutral smells in `.cljs` and `.cljc` files.

### Fixed

- The `CONTRIBUTING.md` Layout table names the local `AGENTS.md`, `GEMINI.md`, and `TODO.md` files. It named `CLAUDE.md`, which the package does not use.
- `/cljs-fix`, `/cljs-new`, and `/cljs-upgrade` link to the package's `CONVENTIONS.md` on GitHub. They pointed to a `CONVENTIONS.md` in the repository root, which is not installed alongside the skills.
- `/cljs-fix` reads source paths from `deps.edn` when `shadow-cljs.edn` sets `:deps`, because shadow-cljs then ignores its own `:source-paths`.
- The README explains how Codex, OpenCode, and Gemini CLI offer the commands: Codex runs a skill as `$name` from its `/skills` list, OpenCode attaches one with `@name`, and Gemini CLI does not read the user-only setting. It said every command appears as a slash command and that user-only commands run only when typed.
- `CONTRIBUTING.md` states that `agents/openai.yaml` sets `allow_implicit_invocation` to `true` for every skill the model can run, including slash commands that the model can also run. It named only model-invoked and user-only skills.
- `/cljs-fix` reports the dry step as `ERROR` when dry4clj stops on an alias-qualified auto-resolved keyword such as `::str/ok`, which the pinned dry4clj commit cannot read, and keeps the step advisory. It said that dry4clj always exits 0.
- `/cljs-new` and `/cljs-upgrade` read the latest ClojureScript release from Maven Central's `maven-metadata.xml`. The `search.maven.org` index they queried reported 1.12.42 on 2026-10-24, when 1.12.145 was current, so `/cljs-new` pinned an old release and `/cljs-upgrade` could replace a newer version with it. `/cljs-upgrade` also stops when the fetched version is older than the current one.

## [0.1.17] - 2026-10-24

### Fixed

- `/cljs-upgrade` stops on a shadow-cljs project whose `deps.edn` does not name `org.clojure/clojurescript` and tells the user to upgrade `thheller/shadow-cljs`, because each shadow-cljs release is tested with one ClojureScript version. It no longer suggests editing `shadow-cljs.edn`.
- The `clojurescript` skill says that `^boolean` is the only type hint that changes generated code, matching its Key Rules and the upstream "Differences from Clojure" page.
- `CONVENTIONS.md` describes the `/cljs-upgrade --report` output that the skill prints: the current and latest versions, with no diff.
- `/cljs-fix` adds a "skipped N vendored or generated paths" line to the report when the vendored-path filter excludes any path, and list those paths under `--report`, as Rule 4 of `CONVENTIONS.md` requires.

### Changed

- The `clojurescript` skill states each rule once. The Gotchas for `:advanced` property renaming, `aget` on objects, `(= 0.0 0)`, macro and function name sharing, and `await` inside a nested `fn` are now part of the sections they repeated, with their symptoms and examples. `/cljs-fix`, `/cljs-new`, `/cljs-smells-fix`, and `clojurescript-lenses` follow the same pattern.

## [0.1.16] - 2026-10-24

### Removed

- OpenCode 1 is no longer supported. The repository no longer keeps a copy of each `SKILL.md` under `.opencode/skills/` or a per-skill allow list in `opencode.jsonc`. `.opencode/skills` is now a symlink to `.apm/skills/` instead, so an OpenCode 2 session started anywhere in the repository loads the canonical skills together with their supporting files. The `.opencode/package.json` file, which pinned the OpenCode 1 plugin package, is removed as well. APM deploys the same files as before.

### Changed

- The `CONTRIBUTING.md` acceptance steps list the skills OpenCode discovers with `opencode api GET "/api/skill?location[directory]=$PWD"`, because OpenCode 2 has no `--pure` flag or `debug skill` command.

## [0.1.15] - 2026-10-24

### Fixed

- The `CONTRIBUTING.md` acceptance steps install with `apm install --target all` in the clean temporary project. On APM 0.34.0, a bare `apm install` in that project stops with "Multiple harnesses detected" because the steps create several runtime folders first. `apm update` reuses the targets that the install records in the project's `apm.yml`.

## [0.1.14] - 2026-10-17

### Changed

- In Claude Code and Codex, the agent can now run `/cljs-fix` from a natural-language request, such as "lint this ClojureScript project", as well as from the slash command. Its description now states when to use it. `/cljs-new`, `/cljs-upgrade`, and `/cljs-smells-fix` remain user-only.

## [0.1.13] - 2026-10-04

### Added

- The `clojurescript` skill covers async functions from ClojureScript 1.12.145: `^:async` placement, `await`, `deftest ^:async`, waiting on several Promises with `Promise/all`, and the compile error raised when `await` appears inside a nested non-async `fn`.
- The `clojurescript` skill covers `:refer-global`, `:require-global`, and Clojure 1.12 method value syntax (`String/.toUpperCase`, `Date/new`) from ClojureScript 1.12.116, and notes the experimental `cljs.proxy` namespace. The project workflow reference adds script-tag globals through `:require-global` and the experimental `:lite-mode` and `:elide-to-string` compiler options.
- The `clojurescript` and `cljs-fix` skills explain that clj-kondo versions before 2026.05.25 report `Unresolved symbol: await`, and direct an upgrade instead of a suppression.
- The `cljs-upgrade` skill checks for Java 21 when an upgrade crosses ClojureScript 1.12.42.

### Changed

- The `cljs-new` prerequisites and the README now require Java 21, which ClojureScript 1.12.42 and later need. They previously stated Java 17.
- The `cljs-new` scaffold pins `org.clojure/clojure` 1.12.6 and `cljfmt` 0.16.6.
- The `clojurescript` skill no longer describes the `goog.isXxx` helpers as deprecated. The Clojure-maintained Closure Library fork restored them in ClojureScript 1.12.42, and the skill still recommends the core predicates.

### Fixed

- The `.cljfmt.edn` that `cljs-new` writes no longer sets `:indents`. That key replaces all of cljfmt's default indentation rules, so `cljfmt fix` misindented `let`, `try`, and `deftest` bodies in scaffolded projects.
- The `cljs-new` gotcha and the `clojurescript` macro rule no longer claim that `cljs.test` macros require `:refer-macros`. `cljs.test` loads its own macros, so a plain `:refer` works.

## [0.1.12] - 2026-09-26

### Fixed

- The `cljs-fix` test step now runs the compiled test runner with `node out/test.js`. The `:test` alias only compiles the runner, so the step previously reported a pass without executing any tests. The `cljs-new` next-steps output shows the same command.
- The `cljs-upgrade` compile check now builds under `:advanced` optimizations when the project has no `:build` alias, matching the advanced-compile verification the skill already described.
- The `cljs-new` browser page now loads `out/main.js`, which is the file `cljs.main` produces, instead of `cljs-out/dev-main.js`.
- The `cljs-smells-fix` placeholder no longer points to a `TODO.md` file that is not part of the published package. Its notice links to the repository instead.

## [0.1.11] - 2026-09-16

### Changed

- The README's runtime sentence now names Grok Build after Kiro. APM 0.28.0 added `grok-build` as a canonical target that `apm install --target all` includes and that APM auto-detects from a `.grok/` directory, so the default target set now has nine runtimes. Grok Build keeps a target-native skill directory (`.grok/skills/` at project scope, `~/.grok/skills/` at user scope), like Claude Code and Kiro, rather than the shared `.agents/skills/` directory.

## [0.1.10] - 2026-07-29

### Changed

- The README's runtime sentence now scopes its list to APM's default target set rather than claiming every runtime APM supports. APM 0.26.0 also supports Antigravity, IntelliJ, and several experimental runtimes, none of which `apm install --target all` includes. The eight runtimes named are unchanged, and the sentence adds that Antigravity works when named explicitly with `--target antigravity`.

## [0.1.9] - 2026-07-29

### Removed

- The package manifest no longer declares the top-level `target: all` field. The APM manifest schema deprecates the `all` value: a parser treats the field as though it were absent and falls through to the `--target` flag or filesystem auto-detection, and the value is scheduled to become a hard parse error in a future APM release. Removing the field makes that fall-through behavior permanent. Installation behavior is unchanged, because APM already resolved targets by auto-detection rather than from this field. The separate `compilation.target` setting is not affected.

## [0.1.8] - 2026-06-23

### Changed

- The `cljs-fix` dry step is now advisory and scoped to production source, matching `clj-fix`. It scans the source paths declared by the project's build configuration (`deps.edn` `:paths` or `shadow-cljs.edn` `:source-paths`) rather than a hardcoded `src test`, reads dry4clj's `--edn` output instead of matching a status string, and reports candidates without failing the run or halting the pipeline. The step reports `PASS` when no candidates are found, `REVIEW` when it lists candidates for inspection, `ERROR` when the scan cannot run or parse, or `SKIPPED`. Test directories are excluded by default because repeated test scaffolding is usually intentional.
- `CONVENTIONS.md` Rule 3 now states that mutating skills apply safe edits and report findings that require judgment, and that `--report` suppresses intentional source and configuration edits while verification steps such as advanced compilation may still write build artifacts.

## [0.1.7] - 2026-06-15

### Changed

- Add Kiro to the README's runtime list. APM 0.20.0 added Kiro as a first-class install target included in `apm install --target all`, so the README now lists it alongside Claude Code, Codex, OpenCode, Cursor, Copilot, Gemini, and Windsurf.

## [0.1.6] - 2026-05-28

### Added

- `CONVENTIONS.md` gains Rule 4: vendored and generated paths are excluded by default from mutating skills that walk the workspace. The rule sits alongside the existing three rules (renamed from "The three rules" to "The four rules"). Two filters apply together (`.gitignore` matches plus a hardcoded floor of dependency directories, build outputs, and lock files). The override rides on Rule 1's existing `<path>` `<glob>` grammar; no new flag is introduced. Applies to `/cljs-fix` and `/cljs-smells-fix`. Exempt: `/cljs-new` is scaffolding, `/cljs-upgrade` rewrites a dependency declaration. The `advanced` step of `/cljs-fix` produces compiler output that is outside the source-mutation scope of the rule.
- `/cljs-fix` and `/cljs-smells-fix` each extend their `## Mutation` (or placeholder body) section with a paragraph restating Rule 4 in context. Operators see the policy without leaving the skill's page.

## [0.1.5] - 2026-05-26

### Fixed

- Quote the YAML `description` and `argument-hint` frontmatter in all skills that had unquoted values. Prevents potential YAML misinterpretation of special characters (angle brackets in `argument-hint`, semicolons and parentheses in `description`).

## [0.1.4] - 2026-05-22

### Changed

- The `cljs-smells-review` placeholder is renamed to `cljs-smells-fix` to match the mutating contract adopted by the new `clj-smells-fix` skill in [clojure-skills](https://github.com/brackendev/clojure-skills). The placeholder still prints a "not yet implemented" notice; the rename, argument grammar (`[path|all] [--report]`), and frontmatter all reflect the eventual fix-by-default behavior. The "not yet implemented" notice now points operators at `/clj-smells-fix` for host-neutral smells in `.cljs` files. Operators with a saved `/cljs-smells-review` invocation should replace it with `/cljs-smells-fix`.

## [0.1.3] - 2026-05-20

### Changed

- `CONTRIBUTING.md` is aligned to the family-wide structural template. The Layout row for `CONVENTIONS.md` uses the canonical phrasing; step 1 of "Adding or modifying a skill" no longer carries an inline `CONVENTIONS.md` reminder (the dedicated Skill conventions section now opens with that pointer); the Skill conventions table cites package-specific example skills (`cljs-fix`, `cljs-new`).

## [0.1.2] - 2026-05-20

### Changed

- The `cljs-tidy` skill is renamed to `cljs-fix` to adopt the noun-first canonical naming pattern (`<target>-<verb>`) shared across the agent-skills family. The verb suffix `-fix` consistently signals a mutating quality pipeline (lint, format, test, advanced, dry). Operators with a saved `/cljs-tidy` invocation should replace it with `/cljs-fix`. The skill's behavior is unchanged; only the name moves.

## [0.1.1] - 2026-05-19

### Added

- A repo-root `CONVENTIONS.md` that defines the argument grammar, scope vocabulary, and mutation defaults every user-invocable skill in this package follows. Three rules cover argument grammar (one sanctioned flag, `--report`), scope vocabulary (`(no argument)`, `all`, `<path>`), and mutation-as-default. The document lists `/cljs-new` as the standard's positional-required exemption and includes an author checklist that runs against every migrated skill.

### Changed

- The `cljs-check` skill is renamed to `cljs-tidy`. The verb now matches the default behavior: `cljfmt fix` runs by default and rewrites files in the `format` step. Operators with a saved `/cljs-check` invocation should replace it with `/cljs-tidy`. The new `--report` flag swaps the format step for `cljfmt check`, which previews diffs without writing; `lint`, `test`, `advanced`, and `dry` are pure-read of source regardless.
- The `cljs-upgrade` skill adds a `--report` flag that prints the current `:mvn/version` for `org.clojure/clojurescript` and the latest released version from Maven Central without writing `deps.edn` or running the advanced compile. The frontmatter description and `argument-hint` document the new grammar.
- The `cljs-smells-review` skill renames its prior section structure to the canonical `## Arguments` table and clarifies in the frontmatter that the skill is pure-report (never writes). The opt-in `fix` modifier never existed for this skill, so no operator-facing invocations change.
- The `cljs-new` skill adds a `## Arguments` section that documents its positional `<project-name>` exemption and a `## Mutation` section that lists the files it writes.

## [0.1.0] - 2026-05-17

### Added

- Initial release. ClojureScript skills layered on top of [clojure-skills](https://github.com/brackendev/clojure-skills) so the baseline can stay host-neutral across `.clj`, `.cljs`, `.cljc`, and `.cljd`.
- **clojurescript** (model-invoked): JavaScript interop (`js/`, `.-prop`, method calls, `set!` on properties, `js-obj`, `clj->js`, `js->clj`, `goog.object/get` and `goog.object/set`), host-typed exception handling (`catch :default`, `js/Error`), externs inference (`:infer-externs`, `*warn-on-infer*`, `^js`, `^js/Foo`), macro stage separation (`:require-macros`, `:refer-macros`, `:include-macros`), namespace forms (`:import` for Google Closure classes only, no `gen-class`), JS-flavored numbers (no `Ratio`, `BigInt`, `BigDecimal`; `(= 0.0 0) ⇒ true`), `^boolean` type hint and checked-`if` avoidance, single-character-string handling, atoms-only state management, reader conditionals (`#?`, `#?@`, `.cljc`), CLJS-specific aliases (`cljs.core.async`, `goog.object`, `goog.string`, `goog.dom`), and the `cljs.main` project workflow. Defers to the [clojure](https://github.com/brackendev/clojure-skills) baseline for host-neutral style. Pulls deeper detail from `references/project-workflows.md` on demand (`cljs.main` CLI, `deps.edn`, browser REPL, Node REPL, source maps, advanced compilation, npm interop, shadow-cljs and figwheel-main as community alternatives).
- **clojurescript-lenses** (model-invoked): Code-lenses translations layered on top of `clojure-lenses` in [clojure-skills](https://github.com/brackendev/clojure-skills). Records the ClojureScript-specific deltas: JavaScript interop boundaries, externs and advanced compilation, macro stage separation, the JS event loop, and the absence of refs / agents / STM and var reification. Covers the four default code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't Validate) and the two opt-in philosophies (APOSD, Legacy Code) that activate when their lens is added with `+aposd` or `+legacy-code` or invoked directly. Auto-triggers alongside the [code-lenses](https://github.com/brackendev/code-lenses) plugin in ClojureScript work.
- **cljs-new** (user-invoked): Scaffold a new ClojureScript project using the official `cljs.main` entry point. Creates `deps.edn` with `:test`, `:build`, and `:cljfmt` aliases, a browser-targeted `index.html`, a Node-targeted test runner namespace, `.cljfmt.edn`, `.gitignore`, and `.clj-kondo/config.edn` (with `cljs.test` macros lint-as-clojure.test). Fetches the latest ClojureScript version from Maven Central before writing `deps.edn`.
- **cljs-check** (user-invoked): Run the ClojureScript quality pipeline (lint, format, test, advanced, dry). Lint runs `clj-kondo --lint src test`; format runs `clj -M:cljfmt fix`; test runs `clj -M:test` (Node target by default, detects shadow-cljs / figwheel-main); advanced runs `clj -M:build` or `clj -M -m cljs.main --optimizations advanced --compile <main-ns>` as a production-build sanity check that surfaces externs-inference failures; dry runs `clj -M:dry4clj src test`. Each step is also addressable on its own.
- **cljs-upgrade** (user-invoked): Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central. Records the previous version, fetches the latest from Maven Central's `solrsearch` endpoint, updates `deps.edn`, refreshes dependencies with `clj -P`, runs the project's advanced-compile build, and refreshes clj-kondo imports. Reverts the version on compile failure.
- **cljs-smells-review** (user-invoked, placeholder): Reserves the command name for a future ClojureScript-specific smells review. Currently prints a "not yet implemented" notice that points users at `/clj-smells-review` in `clojure-skills` for host-neutral smells in `.cljs` files. Documents the planned categories (advanced-compilation hazards, macro stage confusion, truthiness and number traps, async / event-loop patterns, Reagent / re-frame patterns, JS interop hygiene, build-tool drift).
