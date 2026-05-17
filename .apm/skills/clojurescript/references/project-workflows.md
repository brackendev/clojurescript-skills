# ClojureScript Project Workflows

Reference for `cljs.main` (the official ClojureScript CLI entry point), project structure, `deps.edn` configuration for ClojureScript, the browser and Node REPL flows, source maps, advanced compilation, externs handling, npm interop, and the shadow-cljs and figwheel-main community alternatives.

Authoritative source: [clojurescript.org Quick Start](https://clojurescript.org/guides/quick-start). The exact command lines below match the upstream guide.

## CLI

`cljs.main` is the ClojureScript main entry point, invoked through the Clojure CLI:

```bash
clj -M --main cljs.main --compile <main-ns> --repl
clj -M -m cljs.main --help

# Browser REPL with a custom launch option (Linux example)
clj -M --main cljs.main --repl-opts "{:launch-browser false}" --compile <main-ns> --repl

# Production build (advanced optimization)
clj -M -m cljs.main --optimizations advanced -c <main-ns>

# Node target
clj -M -m cljs.main --target node --output-to main.js -c <main-ns>

# Node REPL
clj -M -m cljs.main --repl-env node

# Local development web server
clj -M -m cljs.main --serve
```

The Quick Start notes: "main options like `--compile`, `--main`, `--repl` must always come last."

Windows users without the Clojure CLI run the same flags via the standalone ClojureScript JAR (`java -cp "cljs.jar;src" cljs.main ...`).

## Project Structure

```
hello-world/
  deps.edn                 # Clojure CLI dependencies and aliases
  src/
    hello_world/
      core.cljs            # Entry point
  resources/               # Static assets (HTML, CSS, ...)
  test/
    hello_world/
      core_test.cljs       # Tests mirror src structure
```

File naming uses underscores; namespace declarations use hyphens. `src/hello_world/core.cljs` corresponds to namespace `hello-world.core`.

## deps.edn Configuration

Minimal ClojureScript dependency (from the Quick Start):

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.12.145"}}}
```

The version above matches the upstream Quick Start at the time of writing. Check [the ClojureScript releases page](https://github.com/clojure/clojurescript/releases) for newer builds and update on a fresh project.

## REPL

The host-neutral REPL conventions (`(comment ...)` scratchpads, `in-ns`, fully-qualified references across namespaces) and the REPL-driven development principle live in the [clojure](https://github.com/brackendev/clojure-skills) baseline skill. The ClojureScript-specific additions are below.

### Browser REPL

The default REPL environment for `cljs.main` is the browser. Compiling with `--repl` launches a local web server, opens a browser tab, and starts a REPL that evaluates each form in the browser:

```bash
clj -M --main cljs.main --compile hello-world.core --repl
```

Interactions in the browser REPL behave like any Clojure REPL:

```clojure
(inc 1)
(map inc [1 2 3])
(.getElementById js/document "app")
(require '[hello-world.core :as hello] :reload)
(hello/average 20 13)
```

The browser REPL emits source-mapped stack traces automatically. The compiler must have run a build that produced source maps (the default with `--compile`).

### Node REPL

```bash
clj -M -m cljs.main --repl-env node
```

For source-mapped stack traces in Node, install the Node helper once per project:

```bash
npm install source-map-support
```

### Watch and incremental compile

`cljs.main` does not include a built-in file watcher. For an incremental development loop, use shadow-cljs or figwheel-main (see below), or wrap a watcher around `cljs.main --compile`.

## Source Maps

Source maps are emitted by default in development. In the browser, stack traces are mapped back to ClojureScript source. In Node, install `source-map-support` (npm) to enable the same behavior.

## Advanced Compilation

```bash
clj -M -m cljs.main --optimizations advanced -c <main-ns>
```

The Google Closure Compiler aggressively renames property names that it does not see in an externs file. Enable externs inference and add `^js` / `^js/Foo.Bar` hints to silence inference warnings before promoting a build to advanced optimization. See the main `SKILL.md` for the externs inference workflow.

## NPM Interop

Two patterns are in common use today.

### CLJSJS

Historically the most portable approach: each npm package is wrapped as a Maven jar published to the [CLJSJS](https://github.com/cljsjs/packages) repository, listed as a regular Clojure dependency, and reached through `js/` globals. The Quick Start example illustrates the shape:

```clojure
;; deps.edn
{:deps {cljsjs/react-dom {:mvn/version "16.2.0-3"}}}

;; namespace
(ns hello-world.core
  (:require react-dom))

;; access
js/ReactDOM
```

CLJSJS coverage is uneven for newer packages; many projects migrate to a build tool that consumes `node_modules` directly.

### Build-tool-managed npm modules

[shadow-cljs](https://github.com/thheller/shadow-cljs) and [figwheel-main](https://github.com/bhauman/figwheel-main) both resolve string requires against `node_modules`:

```clojure
(ns my-app.core
  (:require ["react" :as react]
            ["react-dom" :as react-dom]))

(react/createElement "div" nil "Hello")
```

The string require form (`["react" :as react]`) is supported by shadow-cljs and figwheel-main. The plain `cljs.main` CLI also accepts it for projects configured to use the `:bundle` target. Consult each tool's documentation for current behavior before relying on edge cases.

## Community Alternatives

The official toolchain is `cljs.main`. Two community tools are widely used and are referenced where relevant; their conventions are not documented here in depth because they evolve independently.

| Tool | Strengths | Where to look |
|------|-----------|---------------|
| [shadow-cljs](https://github.com/thheller/shadow-cljs) | Tight npm integration via `shadow-cljs.edn`, hot reload, multi-target builds, npm-style invocation (`npx shadow-cljs watch app`). | [shadow-cljs User's Guide](https://shadow-cljs.github.io/docs/UsersGuide.html) |
| [figwheel-main](https://github.com/bhauman/figwheel-main) | Hot reload with a Clojure CLI integration, configured via `figwheel-main.edn` and `:fig` aliases. | [figwheel-main docs](https://figwheel.org/) |

When working in a shadow-cljs or figwheel-main project, defer to that project's tooling for build commands and REPL connection. The host-neutral REPL principle (start a REPL before changing larger code paths) still applies.

## Testing

```bash
# Node-targeted tests via cljs.main
clj -M -m cljs.main --target node --output-to out/tests.js -c my-app.test-runner
node out/tests.js
```

For interactive testing during development, evaluate `(cljs.test/run-tests 'my-app.core-test)` in the REPL after reloading the test namespace:

```clojure
(require '[my-app.core-test] :reload)
(cljs.test/run-tests 'my-app.core-test)
```

shadow-cljs ships a `:test` target with a `karma` runner and a node-test runner; figwheel-main pairs with [cljs-test-display](https://github.com/bhauman/cljs-test-display) for a browser-based runner. Each tool's documentation covers the project layout it expects.
