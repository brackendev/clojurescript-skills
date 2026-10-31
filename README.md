# clojurescript-skills

ClojureScript development skills packaged as an [APM](https://github.com/microsoft/apm) plugin. One install deploys the full set to every runtime in APM's default target set: Claude Code, Codex, OpenCode, Cursor, Copilot, Gemini, Windsurf, Kiro, and Grok Build. Antigravity is supported by naming it explicitly with `--target antigravity`.

Skills follow the [Agent Skills](https://agentskills.io) open standard. One auto-triggers from conversation context (`clojurescript`). The rest appear as slash commands. The agent can also run `/cljs-fix` from a natural-language request. `/cljs-upgrade` runs only as a typed slash command. Runtimes differ in how they offer these commands: Codex lists them under `/skills` and runs them as `$name`, OpenCode attaches them with `@name`, and Gemini CLI does not read the user-only setting, so its agent can start any of them.

This package layers on top of the host-neutral [clojure-skills](https://github.com/brackendev/clojure-skills) baseline. Install both together so the `clojure` skill handles general Clojure family style (naming, threading, collections, atoms, dispatch, formatting, namespaces, testing) and this package covers JavaScript interop, externs inference, macro stage separation, host-typed exceptions, JS-flavored numerics and truthiness, the `cljs.main` CLI workflow, and the production-build sanity checks that come with advanced compilation.

## Companion packages

Eight sibling APM packages. This package layers on top of [clojure-skills](https://github.com/brackendev/clojure-skills) and covers ClojureScript-specific style and tooling. Install both together for ClojureScript work. For Electric or Fulcro projects, also install [electric-skills](https://github.com/brackendev/electric-skills) or [fulcro-skills](https://github.com/brackendev/fulcro-skills), which layer on top of this package. The JVM, Biff, ClojureDart, and Babashka packages are not required for ClojureScript projects.

| Package | Focus | Layers on |
|---------|-------|-----------|
| [clojure-skills](https://github.com/brackendev/clojure-skills) | Host-neutral [Clojure](https://clojure.org/) family baseline: style, naming, threading, collections, atoms, dispatch, formatting, namespaces, and testing. Triggers on `.clj`, `.cljs`, `.cljc`, `.cljd`. | — |
| [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) | [Clojure](https://clojure.org/) on the JVM: Java interop, refs / agents / STM, `with-open`, JVM-typed exceptions, `alter-var-root`, and the Clojure CLI / `tools.build` / `clj-kondo` / `cljfmt` / `test-runner` / nREPL workflow. | `clojure-skills` |
| [clojurescript-skills](https://github.com/brackendev/clojurescript-skills) (this package) | [ClojureScript](https://clojurescript.org/) style and tooling: JavaScript interop, externs inference, macro stage separation, `catch :default`, JS-flavored numbers and truthiness, and the `cljs.main` workflow. Triggers on `.cljs`, `.cljc` compiled to JS, `shadow-cljs.edn`, `figwheel-main.edn`. | `clojure-skills` |
| [babashka-skills](https://github.com/brackendev/babashka-skills) | [Babashka](https://babashka.org/) scripting host: built-in namespaces (`babashka.fs`, `babashka.process`, `babashka.http-client`), `bb.edn` tasks, subprocess exit checks, threads and process exit, records and types without Java classes, testing, pods, and packaging. Triggers on `.bb`, `bb.edn`. | `clojure-skills` |
| [biff-skills](https://github.com/brackendev/biff-skills) | [Biff](https://biffweb.com/) 2 web framework on the JVM: conventions and deployment. | `clojure-skills` + `clojure-jvm-skills` |
| [electric-skills](https://github.com/brackendev/electric-skills) | [Electric](https://github.com/hyperfiddle/electric) v3 full-stack reactive framework: `e/defn` components, `e/client` / `e/server` site transfer, `e/for` over `e/diff-by`, `e/watch` state, `e/Token` transactions, `e/Offload`, the `electric-dom3` and `electric-forms5` APIs, and the single-JVM shadow-cljs workflow. Triggers on `hyperfiddle.electric3`, `hyperfiddle.electric-dom3`, `e/defn`, `e/client`, `e/server`, and `com.hyperfiddle/electric`. | `clojure-skills` + `clojurescript-skills` + `clojure-jvm-skills` |
| [fulcro-skills](https://github.com/brackendev/fulcro-skills) | [Fulcro](https://github.com/fulcrologic/fulcro) 3 full-stack framework: `defsc` components, idents and the normalized client database, mutations, `df/load!`, dynamic routing, forms, UI state machines, Fulcro Inspect, and the Pathom 3 server. Triggers on `com.fulcrologic.fulcro.*`, `com.fulcrologic.rad.*`, `com.wsscode.pathom3.*`, `defsc`, `defmutation`, `defrouter`, `df/load!`, and ident vectors. | `clojure-skills` + `clojurescript-skills` + `clojure-jvm-skills` |
| [clojuredart-skills](https://github.com/brackendev/clojuredart-skills) | [ClojureDart](https://github.com/Tensegritics/ClojureDart) on [Flutter](https://flutter.dev/): Dart interop, type hints, `cljd.flutter` directives, async, FFI, REPL, and the Flutter project workflow. Triggers on `.cljd`, `cljd.flutter`. | `clojure-skills` |

## Install

Install [APM](https://github.com/microsoft/apm) 0.34.0 first. Homebrew offers only the newest release, so remove a Homebrew copy with `brew uninstall apm` and install from PyPI with [uv](https://docs.astral.sh/uv/getting-started/installation/): `uv tool install apm-cli==0.34.0`. APM 0.35.0 is not supported because it can delete installed skill files ([microsoft/apm#3372](https://github.com/microsoft/apm/issues/3372)). Then, in a project:

```bash
apm install brackendev/clojurescript-skills --target all
apm install brackendev/clojure-skills --target all
```

Globally for your user account:

```bash
apm install brackendev/clojurescript-skills -g --target all
apm install brackendev/clojure-skills -g --target all
```

Update later with `apm update [-g]`. Remove with `apm uninstall brackendev/clojurescript-skills [-g]`. A local filesystem path can replace the shorthand at either scope.

## Requirements

- A ClojureScript toolchain. The reference workflow uses the official [`cljs.main`](https://clojurescript.org/guides/quick-start) entry point via the [Clojure CLI](https://clojure.org/guides/install_clojure). ClojureScript requires Java 21 or higher. [shadow-cljs](https://github.com/thheller/shadow-cljs) and [figwheel-main](https://github.com/bhauman/figwheel-main) are common community alternatives and are referenced where relevant.
- [clojure-skills](https://github.com/brackendev/clojure-skills) installed alongside, for the host-neutral baseline.
- [clj-kondo](https://github.com/clj-kondo/clj-kondo) for the lint steps in the user-invoked skills below. Install the latest release, because the lint step relies on its support for `^:async` functions and `await`.
- The `cljs-fix` format step runs [cljfmt](https://github.com/weavejester/cljfmt) through the first of these that the project has: a `:cljfmt` alias in `deps.edn`, the Leiningen plugin, the `cljfmt` binary, or a Clojure CLI tool installed as `cljfmt`. Without any of them, the format step reports that it was skipped and shows the alias to add.
- The `cljs-fix` dry step requires a [dry4clj](https://github.com/unclebob/dry4clj) `:dry4clj` alias in `deps.edn`. dry4clj is not published to Clojars, so the alias uses a Git coordinate. Without the alias, the dry step reports that it was skipped and shows the alias to add.
- [Babashka](https://babashka.org) (`bb`) to run the `cljs-rules` hook. Without it, the hook does nothing and prints a notice on standard error.

## Command guide

A quick guide to every slash command. The detailed entries under [Skills](#skills) cover arguments and examples.

| Command | Use it when | What it does |
|---------|-------------------|--------------|
| `/cljs-fix` | Lint, format, tests, or the advanced build need attention | Runs the fix pipeline, rewriting files in the format step with `cljfmt fix` |
| `/cljs-upgrade` | The ClojureScript version is behind | Upgrades `org.clojure/clojurescript` to the latest release and verifies the build |

## Skills

### Quality and upgrades

#### `/cljs-fix [lint|format|test|advanced|dry] [--report] [all|<path>|<glob>]`

Tidy a ClojureScript project. Defaults to the full sequence (lint, format, test, advanced, dry) on the source and test paths that the project's build configuration declares. Each step is also addressable on its own, and naming files or directories restricts the lint, format, and dry steps to them. The `format` step writes by default (`cljfmt fix`). Pass `--report` to replace it with `cljfmt check`, which writes nothing. The `advanced` step runs the project under `:advanced` optimizations as a production-build sanity check and surfaces externs-inference failures. See [CONVENTIONS.md](CONVENTIONS.md) for the argument grammar this skill follows.

```bash
/cljs-fix
/cljs-fix lint
/cljs-fix format src/app
/cljs-fix advanced
/cljs-fix --report
```

#### `/cljs-upgrade [--report] [all]`

Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central. Before it writes anything, it reads the ClojureScript release notes between the two versions and lists the entries that affect the project, with the files that use them. It then updates the version, applies the changes from the notes that have a mechanical rewrite, refreshes dependencies, runs the project's advanced-compile build, and refreshes clj-kondo imports. It reverts the version and those changes on compile failure, and never replaces the current version with an older one. In an Electric project it changes nothing and reports the ClojureScript version that the project's Electric release declares, because a newer ClojureScript stops Electric code from loading on the JVM. Pass `--report` to print the current and latest versions and the affecting release-note entries without writing or compiling.

```bash
/cljs-upgrade
/cljs-upgrade --report
```

### Auto-triggered

These skills activate from conversation context. They cannot be invoked directly.

| Skill | Triggers |
|-------|----------|
| **clojurescript** | `.cljs` files, `.cljc` files compiled to JS, `shadow-cljs.edn`, `figwheel-main.edn`, ClojureScript `deps.edn` projects, `cljs.test`, JavaScript interop (`js/`, `.-prop`, `(.method obj ...)`, `set!` on properties, `js-obj`, `clj->js`, `js->clj`, `goog.object/get`, `goog.object/set`, `aget`, `aset`), global references (`:refer-global`, `:require-global`), method values (`String/.toUpperCase`, `Date/new`), async functions (`^:async`, `await`, `deftest ^:async`, `js/Promise`), externs inference (`^js`, `^js/Foo`, `*warn-on-infer*`, `:infer-externs`), macro stage separation (`:require-macros`, `:refer-macros`, `:include-macros`), reader conditionals (`#?`, `#?@`, `:cljs`, `:default`), JS-flavored numerics and truthiness, `^boolean` type hints, JavaScript exceptions (`catch :default`, `js/Error`), browser REPL, Node REPL, source maps, npm interop, the `cljs.main` CLI, shadow-cljs, or figwheel-main. Covers JavaScript interop, async functions, host-typed exception handling (`catch :default`, `js/Error`), externs and advanced compilation, macro stage separation, JS-flavored numerics, host-specific aliases (`goog.object`, `goog.string`, `goog.dom`), and the ClojureScript project workflow. Defers to the [clojure](https://github.com/brackendev/clojure-skills) skill for host-neutral style. The skill opens with Iron Laws (CLJS-1 through CLJS-5), rules that prevent production-only bugs, silent data loss, and security holes on JavaScript hosts. The agent does not write code that breaks them, and it reports a violation it finds in existing code before changing it. |

## Hooks

The package includes one hook, `cljs-rules`.

### `cljs-rules`

Agents seldom load an auto-triggered skill on their own, so this hook gives the agent the CLJS-1 through CLJS-5 Iron Laws of the `clojurescript` skill as text. In a project with `shadow-cljs.edn` or `figwheel-main.edn`, or a `deps.edn` or `project.clj` that names `org.clojure/clojurescript`, it puts the condensed laws into the agent's context when a session starts, and again after `/clear` or a compaction, with an instruction to load the `clojurescript` skill along with the `clojure` skill. When the session starts outside such a project, for example in a parent directory, the hook delivers the laws on the first edit of a `.cljs`, `.cljc`, or `.edn` file in a matching project instead, once per session. The hook never edits a file. Without Babashka it does nothing and prints a notice on standard error.

The hook has been run only under Claude Code, where it registers `SessionStart` and `PostToolUse`. The package ships empty Copilot and Cursor hook files, so APM writes no Copilot hook and no native Cursor hook. APM also installs the generic copy for Codex, Gemini CLI, Kiro, and Windsurf, where its effect has not been tested. OpenCode and Grok Build have no hook support.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT. See [LICENSE](LICENSE).
