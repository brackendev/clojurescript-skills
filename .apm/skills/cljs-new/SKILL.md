---
name: cljs-new
description: "Scaffold a new ClojureScript project with deps.edn and cljs.main"
argument-hint: "<project-name>"
user-invocable: true
disable-model-invocation: true
---

# Scaffold a ClojureScript Project

Create a new ClojureScript project that uses [`cljs.main`](https://clojurescript.org/guides/quick-start) as its build and REPL entry point. The scaffold follows the official ClojureScript Quick Start layout and adds an `:test` alias, a `:cljfmt` alias, and clj-kondo configuration. See `CONVENTIONS.md` in the repo root for the argument grammar this skill follows.

## Arguments

| Input             | Target                                                                       |
|-------------------|------------------------------------------------------------------------------|
| `<project-name>`  | Required. Use hyphens (for example, `hello-world`); the skill maps to underscores for file paths (`hello_world/`) and keeps hyphens in namespace symbols (`hello-world.core`). |
| (no argument)     | Prompt the operator for a project name.                                      |

This skill is exempt from the `all` and `<path>` rows of the standard scope vocabulary because scaffolding has no useful default scope. See `CONVENTIONS.md` for the standard.

## Mutation

Mutates by default: creates the project directory and writes `deps.edn`, the entry-point source file `src/<project_name>/core.cljs`, the test source and test-runner files under `test/<project_name>/`, `resources/public/index.html`, `.cljfmt.edn`, `.gitignore`, and `.clj-kondo/config.edn` (plus the upstream clj-kondo exports under `.clj-kondo/imports/`). No `--report` flag; preview the side effects by reading this `SKILL.md`.

## Prerequisites

Verify these are installed before proceeding. If any are missing, stop and tell the user.

- `clj` (Clojure CLI / tools.deps)
- `clj-kondo` (Clojure linter; lints `.cljs` and `.cljc` too)
- Java 17 or higher

Check with:

```bash
clj --version
clj-kondo --version
java -version
```

## Steps

### 1. Get Project Name

Use `$ARGUMENTS` as the project name. If empty, ask the user for a project name.

The project name uses hyphens (for example, `hello-world`). File paths use underscores (for example, `hello_world/`). The namespace mirrors the project name (`hello-world.core`).

### 2. Resolve the Latest ClojureScript Version

Fetch the latest released version from Maven Central:

```bash
curl -sSL -A 'Mozilla/5.0' \
  "https://search.maven.org/solrsearch/select?q=g:%22org.clojure%22+AND+a:%22clojurescript%22&rows=1&core=gav" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['response']['docs'][0]['v'])"
```

If the network call fails, fall back to the version named in the [Quick Start](https://clojurescript.org/guides/quick-start) (currently `1.12.145`) and warn the user that the value may be stale.

### 3. Create Project Directory and deps.edn

```bash
mkdir <project-name>
```

Create `<project-name>/deps.edn`:

```clojure
{:paths   ["src" "resources"]
 :deps    {org.clojure/clojure       {:mvn/version "1.12.0"}
           org.clojure/clojurescript {:mvn/version "<latest-cljs-version>"}}
 :aliases
 {:dev     {:extra-paths ["dev"]}
  :test    {:extra-paths ["test"]
            :main-opts   ["-m" "cljs.main"
                          "--target" "node"
                          "--output-to" "out/test.js"
                          "--compile" "<namespace>.test-runner"]}
  :build   {:main-opts   ["-m" "cljs.main"
                          "--optimizations" "advanced"
                          "--compile" "<namespace>.core"]}
  :cljfmt  {:extra-deps  {dev.weavejester/cljfmt {:mvn/version "0.13.0"}}
            :main-opts   ["-m" "cljfmt.main"]}}}
```

Where `<namespace>` matches the project name with hyphens (e.g., `hello-world` for project `hello-world`).

### 4. Create Entry Point

Create `src/<project_name>/core.cljs` where `<project_name>` uses underscores:

```clojure
(ns <namespace>.core)

(defn -main
  [& _args]
  (println "Hello from <project-name>!"))
```

### 5. Create Test File and Test Runner

Create `test/<project_name>/core_test.cljs`:

```clojure
(ns <namespace>.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [<namespace>.core :as core]))

(deftest hello-test
  (testing "main runs without error"
    (is (nil? (core/-main)))))
```

Create `test/<project_name>/test_runner.cljs`:

```clojure
(ns <namespace>.test-runner
  (:require
   [cljs.test :as t]
   [<namespace>.core-test]))

(defn -main [& _args]
  (t/run-tests '<namespace>.core-test))

(set! *main-cli-fn* -main)
```

### 6. Create Index HTML for the Browser REPL

Create `resources/public/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title><Project Name></title>
  </head>
  <body>
    <div id="app">Loading...</div>
    <script src="cljs-out/dev-main.js"></script>
  </body>
</html>
```

### 7. Create `.cljfmt.edn`

```clojure
{:paths   ["src" "test"]
 :indents {ns   [[:inner 0]]
           defn [[:inner 0]]
           fn   [[:inner 0]]}}
```

### 8. Create `.gitignore`

```
out/
target/
.cpcache/
.nrepl-port
node_modules/
*.js.map
```

### 9. Create `.clj-kondo/config.edn`

Set up clj-kondo to lint `.cljs` and `.cljc` consistently:

```clojure
{:linters {:unresolved-symbol {:level :warning}
           :unresolved-namespace {:level :warning}}
 :lint-as {cljs.test/deftest clojure.test/deftest
           cljs.test/testing clojure.test/testing
           cljs.test/is      clojure.test/is}}
```

Import upstream exports so well-known macros (Reagent, re-frame, etc.) lint cleanly:

```bash
clj-kondo --copy-configs --dependencies --lint "$(clj -Spath)"
```

Commit `.clj-kondo/` to version control.

### 10. First Compile and REPL

```bash
cd <project-name>
clj -M --main cljs.main --compile <namespace>.core --repl
```

The default `--repl-env` is the browser. The compiler launches a local web server, opens a browser tab, and starts a REPL that evaluates each form in the browser. For a Node REPL instead:

```bash
clj -M -m cljs.main --repl-env node
```

### 11. Report

Print the created files and next steps:

```
Created:
  deps.edn
  src/<project_name>/core.cljs
  test/<project_name>/core_test.cljs
  test/<project_name>/test_runner.cljs
  resources/public/index.html
  .cljfmt.edn
  .gitignore
  .clj-kondo/config.edn
  .clj-kondo/imports/ (upstream exports)

Next steps:
  cd <project-name>
  clj -M --main cljs.main --compile <namespace>.core --repl   # Browser REPL
  clj -M -m cljs.main --repl-env node                          # Node REPL
  clj -M:test                                                  # Run tests
  clj -M:build                                                 # Advanced build
  clj -M:cljfmt fix                                            # Format source
  clj-kondo --lint src test                                    # Lint source
```

## Gotchas

- File names use underscores; namespace names use hyphens. `src/hello_world/core.cljs` corresponds to `(ns hello-world.core)`. Mismatches surface as "could not locate" errors at compile time.
- `cljs.test/deftest`, `cljs.test/testing`, and `cljs.test/is` are macros. They must be brought in via `:refer-macros` (or `:include-macros true` on a single `:require`), not via plain `:refer`.
- Node REPL stack traces are only source-mapped if `npm install source-map-support` is run inside the project. Browser REPL source mapping is automatic.
- The `:build` alias above produces an advanced-compiled bundle. Enable externs inference (`:infer-externs true` in the compiler options, `(set! *warn-on-infer* true)` per namespace) before relying on advanced builds; see the main `clojurescript` skill for details.
- `cljs.main` does not include a file watcher. For an incremental development loop with hot reload, install [shadow-cljs](https://github.com/thheller/shadow-cljs) or [figwheel-main](https://github.com/bhauman/figwheel-main) and migrate the project layout to that tool.
