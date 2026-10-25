# ClojureScript Project Workflows

Reference for `cljs.main` (the official ClojureScript CLI entry point), project structure, `deps.edn` configuration for ClojureScript, the browser and Node REPL flows, source maps, advanced compilation, externs handling, npm interop, and the shadow-cljs and figwheel-main community alternatives.

Authoritative source: [clojurescript.org Quick Start](https://clojurescript.org/guides/quick-start). The flags below match the upstream guide, which starts every command with `clj`. `clj` runs `clojure` through `rlwrap` for line editing, and without `rlwrap` it prints "Please install rlwrap for command editing or use "clojure" instead." and exits 1. The commands below keep `clj` where a person starts an interactive REPL, and use `clojure` for every command that the agent runs.

## CLI

`cljs.main` is the ClojureScript main entry point, invoked through the Clojure CLI:

```bash
clj -M --main cljs.main --compile <main-ns> --repl
clojure -M -m cljs.main --help

# Browser REPL with a custom launch option (Linux example)
clj -M --main cljs.main --repl-opts "{:launch-browser false}" --compile <main-ns> --repl

# Production build (advanced optimization)
clojure -M -m cljs.main --optimizations advanced -c <main-ns>

# Node target
clojure -M -m cljs.main --target node --output-to main.js -c <main-ns>

# Node REPL
clj -M -m cljs.main --repl-env node

# Local development web server
clojure -M -m cljs.main --serve
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

ClojureScript requires Java 21 or later, because its Google Closure Compiler dependency requires Java 21. `/cljs-upgrade` moves an existing project to the latest release on Maven Central.

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

`--watch` rebuilds when a file under the listed paths changes. It works only with `--compile`, and, like every init option, comes before the main option:

```bash
clojure -M -m cljs.main --watch src --compile <main-ns>
```

`cljs.main` does not reload code into a running page. For hot reloading, use shadow-cljs or figwheel-main (see below).

## Source Maps

Source maps are emitted by default in development, including by a `--compile` build. In the browser, including the browser REPL, stack traces are mapped back to ClojureScript source automatically. In Node, including the Node REPL, install the `source-map-support` npm package once per project to enable the same behavior:

```bash
npm install source-map-support
```

## Advanced Compilation

```bash
clojure -M -m cljs.main --optimizations advanced -c <main-ns>
```

Resolve every externs inference warning before promoting a build to advanced optimization, and test the release artifact before reporting success (Iron Law CLJS-3). shadow-cljs release builds use `:advanced` by default and also elide assertions. To debug a release-only failure, use `shadow-cljs check`, `shadow-cljs release app --debug`, or `:pseudo-names true`. "Type Hints and Externs" in `references/interop.md` describes the property-renaming hazard and the externs inference workflow.

Two experimental compiler options target very small programs. `:lite-mode true` makes data literals use lighter copy-on-write collections, and `:elide-to-string true` removes the collection `toString` implementations so dead code elimination can drop the printer. Upstream describes them as useful only for simple programs: once a program has enough dependencies or uses enough of `cljs.core`, the size savings disappear. Do not enable them on an application build without measuring the output.

## NPM Interop

Add JavaScript libraries from npm and require them by package name. The string form (`["react" :as react]`) works with shadow-cljs, and with `cljs.main` and figwheel-main under `:target :bundle`.

```clojure
(ns my-app.core
  (:require ["react" :as react]
            ["react-dom/client" :as react-dom]))

(react/createElement "div" nil "Hello")
```

### `:target :bundle` with `cljs.main`

The official toolchain hands npm modules to a JavaScript bundler. With `:target :bundle`, the compiler emits Node-style `require` calls for those modules and runs `:bundle-cmd` after each build. The target also turns on externs inference, so `:advanced` builds keep the property names of npm values. The build file from the upstream [webpack guide](https://clojurescript.org/guides/webpack):

```clojure
;; build.edn
{:main my-app.core
 :output-to "out/index.js"
 :output-dir "out"
 :target :bundle
 :bundle-cmd {:none ["npx" "webpack" "./out/index.js" "-o" "out" "--mode=development"]
              :default ["npx" "webpack" "./out/index.js" "-o" "out"]}
 :closure-defines {cljs.core/*global* "window"}} ;; needed for advanced
```

```bash
npm install react react-dom
npm install --save-dev webpack webpack-cli
clojure -M -m cljs.main -co build.edn -c                  # development build
clojure -M -m cljs.main -co build.edn -O advanced -c      # release build
```

webpack writes the page's script to `out/main.js`. Add `-r` after `-c` for a browser REPL. figwheel-main uses the same `:target :bundle` option and fills in the `:bundle-cmd` paths itself (see [Using NPM](https://figwheel.org/docs/npm.html) in the figwheel-main docs).

### shadow-cljs

shadow-cljs reads `node_modules` directly and needs no bundler or `:target :bundle`. Install the package with npm and require it.

### CLJSJS

Do not add new `cljsjs/*` dependencies. CLJSJS repackages npm releases as jars by hand, so its packages trail npm, and shadow-cljs does not support CLJSJS at all. The shadow-cljs User's Guide concludes: "Use npm directly."

### Script-tag globals

`:require-global` aliases a library that the page loads as a global through a `<script>` tag, with no npm or bundler configuration. "Referred globals and method values" in `references/interop.md` gives the syntax. This suits pages with one or two JavaScript dependencies, such as hypermedia-style applications. Projects that already consume `node_modules` should keep their npm setup.

## Community Alternatives

The official toolchain is `cljs.main`. Two community tools are widely used and are referenced where relevant. Their conventions are not documented here in depth because they evolve independently.

| Tool | Strengths | Where to look |
|------|-----------|---------------|
| [shadow-cljs](https://github.com/thheller/shadow-cljs) | Tight npm integration via `shadow-cljs.edn`, hot reload, multi-target builds, npm-style invocation (`npx shadow-cljs watch app`). | [shadow-cljs User's Guide](https://shadow-cljs.github.io/docs/UsersGuide.html) |
| [figwheel-main](https://github.com/bhauman/figwheel-main) | Hot reload with a Clojure CLI integration. Figwheel options live in `figwheel-main.edn` and compiler options in a `<build>.cljs.edn` file, started with `clojure -M -m figwheel.main -b dev -r`. Projects from figwheel-main-template wrap this in a `:fig` alias. | [figwheel-main docs](https://figwheel.org/) |

When working in a shadow-cljs or figwheel-main project, defer to that project's tooling for build commands and REPL connection. The host-neutral REPL guidance in the `clojure` skill still applies.

Two shadow-cljs facts affect these projects:

- **shadow-cljs requires Java 21 or later.** Check `java -version` before building a shadow-cljs project. Each shadow-cljs release also pins its own `org.clojure/clojurescript` and Closure Compiler versions, and its User's Guide calls matching versions "very important", so upgrade `thheller/shadow-cljs` instead of overriding the ClojureScript version.
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
clojure -M -m cljs.main --target node --output-to out/tests.js -c my-app.test-runner
node out/tests.js
```

For interactive testing during development, evaluate `(cljs.test/run-tests 'my-app.core-test)` in the REPL after reloading the test namespace:

```clojure
(require '[my-app.core-test] :reload)
(cljs.test/run-tests 'my-app.core-test)
```

shadow-cljs has `:node-test`, `:browser-test`, and `:karma` build targets. For a `:node-test` build named `:test` with `:output-to "out/node-tests.js"`, run `npx shadow-cljs compile test && node out/node-tests.js`. The Node process exits 1 on any failure. figwheel-main serves [cljs-test-display](https://github.com/bhauman/cljs-test-display) results when its `:auto-testing` option is on. Each tool's documentation covers the project layout it expects.
