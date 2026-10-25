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

ClojureScript 1.12.42 and later require Java 21 or higher, because the bundled Google Closure Compiler requires it. The version above matches the upstream Quick Start at the time of writing. Check [the ClojureScript releases page](https://github.com/clojure/clojurescript/releases) for newer builds and update on a fresh project.

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

### Node REPL

```bash
clj -M -m cljs.main --repl-env node
```

Stack traces in both REPLs map back to ClojureScript source as described in "Source Maps" below.

### Watch and incremental compile

`cljs.main` does not include a built-in file watcher. For an incremental development loop, use shadow-cljs or figwheel-main (see below), or wrap a watcher around `cljs.main --compile`.

## Source Maps

Source maps are emitted by default in development, including by a `--compile` build. In the browser, including the browser REPL, stack traces are mapped back to ClojureScript source automatically. In Node, including the Node REPL, install the `source-map-support` npm package once per project to enable the same behavior:

```bash
npm install source-map-support
```

## Advanced Compilation

```bash
clj -M -m cljs.main --optimizations advanced -c <main-ns>
```

Resolve every externs inference warning before promoting a build to advanced optimization. The main `SKILL.md` ("Type Hints and Externs") describes the property-renaming hazard and the externs inference workflow.

ClojureScript 1.12.116 added two experimental compiler options for very small programs. `:lite-mode true` makes data literals use lighter copy-on-write collections, and `:elide-to-string true` removes the collection `toString` implementations so dead code elimination can drop the printer. Upstream describes them as useful only for simple programs: once a program has enough dependencies or uses enough of `cljs.core`, the size savings disappear. Do not enable them on an application build without measuring the output.

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

### Script-tag globals

`:require-global` (ClojureScript 1.12.116 and later) aliases a library that the page loads as a global through a `<script>` tag, with no npm or bundler configuration. The main `SKILL.md` ("Referred globals and method values") gives the syntax. This suits pages with one or two JavaScript dependencies, such as hypermedia-style applications. Projects that already consume `node_modules` should keep their build tool's npm support.

## Community Alternatives

The official toolchain is `cljs.main`. Two community tools are widely used and are referenced where relevant; their conventions are not documented here in depth because they evolve independently.

| Tool | Strengths | Where to look |
|------|-----------|---------------|
| [shadow-cljs](https://github.com/thheller/shadow-cljs) | Tight npm integration via `shadow-cljs.edn`, hot reload, multi-target builds, npm-style invocation (`npx shadow-cljs watch app`). | [shadow-cljs User's Guide](https://shadow-cljs.github.io/docs/UsersGuide.html) |
| [figwheel-main](https://github.com/bhauman/figwheel-main) | Hot reload with a Clojure CLI integration, configured via `figwheel-main.edn` and `:fig` aliases. | [figwheel-main docs](https://figwheel.org/) |

When working in a shadow-cljs or figwheel-main project, defer to that project's tooling for build commands and REPL connection. The host-neutral REPL guidance in the `clojure` skill still applies.

Two shadow-cljs facts affect these projects:

- **shadow-cljs 3.x requires Java 21.** Release 3.0.0 (2025-04-24) raised the minimum to Java 21 when it updated the Closure Compiler. Check `java -version` before building a shadow-cljs 3 project.
- **`:deps` moves source paths and dependencies to `deps.edn`.** When `shadow-cljs.edn` sets `:deps` (`:deps true` or `:deps {:aliases [...]}`), shadow-cljs resolves its dependencies and source paths through `deps.edn` instead of its own `:dependencies` and `:source-paths`. Put the source paths in `deps.edn` `:paths` (or an alias's `:extra-paths`), list the aliases in `:deps {:aliases [...]}`, and add `thheller/shadow-cljs` to `deps.edn` yourself.

### shadow-cljs REPL

The shadow-cljs server runs an nREPL server and writes its port to `.shadow-cljs/nrepl.port`. It also writes `.nrepl-port` when `shadow-cljs.edn` sets `:nrepl {:write-port-file true}`, and `:nrepl {:port 9000}` fixes the port. An nREPL session starts in Clojure, in the `shadow.user` namespace, where `shadow` aliases `shadow.cljs.devtools.api`:

```clojure
(shadow/active-builds)   ; builds with a running watch
(shadow/repl :app)       ; switch this session to the :app build's ClojureScript REPL
:cljs/quit               ; return to the Clojure REPL; the watch keeps running
```

From a terminal, `npx shadow-cljs cljs-repl app` opens the same build REPL. A build REPL needs two things: a running `watch` for the build, and a JavaScript runtime that has loaded the build's output and connected to the server (the browser page for `:browser` builds, the `node` process for `:node-script` builds). Without the runtime, every evaluation prints "No available JS runtime." and returns `nil`. Open the app or start the process, then evaluate again. Reloading the page clears the runtime's REPL state.

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
