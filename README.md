# clojurescript-skills

ClojureScript development skills packaged as an [APM](https://github.com/microsoft/apm) plugin. One install deploys the full set to every runtime APM supports: Claude Code, Codex, OpenCode, Cursor, Copilot, Gemini, and Windsurf.

Skills follow the [Agent Skills](https://agentskills.io) open standard. Two auto-trigger from conversation context (`clojurescript`, `clojurescript-lenses`); the rest appear as slash commands.

This package layers on top of the host-neutral [clojure-skills](https://github.com/brackendev/clojure-skills) baseline. Install both together so the `clojure` skill handles general Clojure family style (naming, threading, collections, atoms, dispatch, formatting, namespaces, testing) and this package covers JavaScript interop, externs inference, macro stage separation, host-typed exceptions, JS-flavored numerics and truthiness, the `cljs.main` CLI workflow, and the production-build sanity checks that come with advanced compilation.

## Companion packages

Six sibling APM packages. This package layers on top of [clojure-skills](https://github.com/brackendev/clojure-skills) and covers ClojureScript-specific style and tooling. Install both together for ClojureScript work. For Fulcro projects, also install [fulcro-skills](https://github.com/brackendev/fulcro-skills), which layers on top of this package. The JVM, Biff, and ClojureDart packages are not required for ClojureScript projects.

| Package | Focus | Layers on |
|---------|-------|-----------|
| [clojure-skills](https://github.com/brackendev/clojure-skills) | Host-neutral [Clojure](https://clojure.org/) family baseline: style, naming, threading, collections, atoms, dispatch, formatting, namespaces, and testing. Triggers on `.clj`, `.cljs`, `.cljc`, `.cljd`. | — |
| [clojure-jvm-skills](https://github.com/brackendev/clojure-jvm-skills) | [Clojure](https://clojure.org/) on the JVM: Java interop, refs / agents / STM, `with-open`, JVM-typed exceptions, `alter-var-root`, and the Clojure CLI / `tools.build` / `clj-kondo` / `cljfmt` / `test-runner` / nREPL workflow. | `clojure-skills` |
| [clojurescript-skills](https://github.com/brackendev/clojurescript-skills) (this package) | [ClojureScript](https://clojurescript.org/) style and tooling: JavaScript interop, externs inference, macro stage separation, `catch :default`, JS-flavored numbers and truthiness, and the `cljs.main` workflow. Triggers on `.cljs`, `.cljc` compiled to JS, `shadow-cljs.edn`, `figwheel-main.edn`. | `clojure-skills` |
| [biff-skills](https://github.com/brackendev/biff-skills) | [Biff](https://biffweb.com/) web framework on the JVM: scaffolding, conventions, deployment. | `clojure-skills` + `clojure-jvm-skills` |
| [fulcro-skills](https://github.com/brackendev/fulcro-skills) | [Fulcro](https://github.com/fulcrologic/fulcro) full-stack framework: `defsc` components, idents and the normalized client database, mutations, `df/load!`, dynamic routing, forms, UI state machines, Fulcro Inspect, and the Pathom 3 server. Triggers on `com.fulcrologic.fulcro.*`, `com.fulcrologic.rad.*`, `com.wsscode.pathom3.*`, `defsc`, `defmutation`, `defrouter`, `df/load!`, and ident vectors. | `clojure-skills` + `clojurescript-skills` + `clojure-jvm-skills` |
| [clojuredart-skills](https://github.com/brackendev/clojuredart-skills) | [ClojureDart](https://github.com/Tensegritics/ClojureDart) on [Flutter](https://flutter.dev/): Dart interop, type hints, `cljd.flutter` directives, async, FFI, REPL, and the Flutter project workflow. Triggers on `.cljd`, `cljd.flutter`. | `clojure-skills` |

## Install

Install [APM](https://github.com/microsoft/apm) first if you don't already have it. Then, in a project:

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

- A ClojureScript toolchain. The reference workflow uses the official [`cljs.main`](https://clojurescript.org/guides/quick-start) entry point via the [Clojure CLI](https://clojure.org/guides/install_clojure) (Java 17 or higher). [shadow-cljs](https://github.com/thheller/shadow-cljs) and [figwheel-main](https://github.com/bhauman/figwheel-main) are common community alternatives and are referenced where relevant.
- [clojure-skills](https://github.com/brackendev/clojure-skills) installed alongside, for the host-neutral baseline.
- [clj-kondo](https://github.com/clj-kondo/clj-kondo) for the lint steps in the user-invoked skills below.
- The `cljs-fix` dry step requires a [dry4clj](https://github.com/unclebob/dry4clj) `:dry4clj` alias in `deps.edn`.

## Command guide

A quick guide to every slash command. The detailed entries under [Skills](#skills) cover arguments and examples.

| Command | Use it when | What it does |
|---------|-------------------|--------------|
| `/cljs-new` | Starting a new ClojureScript project | Scaffolds a project with `deps.edn`, a browser entry point, a Node test runner, and clj-kondo setup |
| `/cljs-fix` | Lint, format, tests, or the advanced build need attention | Runs the fix pipeline, rewriting files in the format step with `cljfmt fix` |
| `/cljs-upgrade` | The ClojureScript version is behind | Upgrades `org.clojure/clojurescript` to the latest release and verifies the build |
| `/cljs-smells-fix` | Reserved for a future ClojureScript smells fix | Placeholder that prints a not-yet-implemented notice pointing to `/clj-smells-fix` |

## Skills

### Scaffolding and quality

#### `/cljs-new <project-name>`

Scaffold a new ClojureScript project with `deps.edn`, browser entry point, Node-targeted test runner, and clj-kondo configuration. Fetches the latest ClojureScript version from Maven Central before writing `deps.edn`.

```bash
/cljs-new hello-world
```

#### `/cljs-fix [lint|format|test|advanced|dry] [--report] [all]`

Tidy a ClojureScript project. Defaults to the full sequence (lint, format, test, advanced, dry). Each step is also addressable on its own. The `format` step writes by default (`cljfmt fix`); pass `--report` to swap it for non-writing `cljfmt check`. The `advanced` step runs the project under `:advanced` optimizations as a production-build sanity check and surfaces externs-inference failures. See [CONVENTIONS.md](CONVENTIONS.md) for the argument grammar this skill follows.

```bash
/cljs-fix
/cljs-fix lint
/cljs-fix advanced
/cljs-fix --report
```

#### `/cljs-upgrade [--report] [all]`

Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central. Refreshes dependencies, runs the project's advanced-compile build, and refreshes clj-kondo imports. Reverts the version on compile failure. Pass `--report` to print the current and latest versions without writing or compiling.

```bash
/cljs-upgrade
/cljs-upgrade --report
```

#### `/cljs-smells-fix [path|all] [--report]` (placeholder)

Reserves the command name for a future ClojureScript-specific smells fix pipeline. When implemented, will mirror the mutation contract of `/clj-smells-fix` in `clojure-skills`: auto-apply Stage 1 mechanical findings and the Stage 2 `DEFECT`-tier safety band; report `SMELL` and `HINT` findings; honor `--report` to disable all writes. Currently prints a "not yet implemented" notice that points users at `/clj-smells-fix` in `clojure-skills` for host-neutral smells in `.cljs` files. See [TODO.md](TODO.md).

### Auto-triggered

These skills activate from conversation context. They cannot be invoked directly.

| Skill | Triggers |
|-------|----------|
| **clojurescript** | `.cljs` files, `.cljc` files compiled to JS, `shadow-cljs.edn`, `figwheel-main.edn`, ClojureScript `deps.edn` projects, `cljs.test`, JavaScript interop (`js/`, `.-prop`, `(.method obj ...)`, `set!` on properties, `js-obj`, `clj->js`, `js->clj`, `goog.object/get`, `goog.object/set`, `aget`, `aset`), externs inference (`^js`, `^js/Foo`, `*warn-on-infer*`, `:infer-externs`), macro stage separation (`:require-macros`, `:refer-macros`, `:include-macros`), reader conditionals (`#?`, `#?@`, `:cljs`, `:default`), JS-flavored numerics and truthiness, `^boolean` type hints, browser REPL, Node REPL, source maps, npm interop, the `cljs.main` CLI, shadow-cljs, or figwheel-main. Covers JavaScript interop, host-typed exception handling (`catch :default`, `js/Error`), externs and advanced compilation, macro stage separation, JS-flavored numerics, host-specific aliases (`goog.object`, `goog.string`, `goog.dom`), and the ClojureScript project workflow. Defers to the [clojure](https://github.com/brackendev/clojure-skills) skill for host-neutral style. |
| **clojurescript-lenses** | Auto-triggers alongside the [code-lenses](https://github.com/brackendev/code-lenses) plugin in ClojureScript work. Layers on top of `clojure-lenses` and records only the ClojureScript-specific deltas: JavaScript interop boundaries, externs and advanced compilation, macro stage separation, the JS event loop, and the absence of refs / agents / STM and var reification. Covers the four default code-lenses philosophies (grug, Honest Code, Tidy First, Parse Don't Validate) and the two opt-in philosophies (APOSD, Legacy Code) that activate when their lens is added with `+aposd` or `+legacy-code` or invoked directly. |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT. See [LICENSE](LICENSE).
