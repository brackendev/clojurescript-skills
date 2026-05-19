---
name: cljs-upgrade
description: Upgrade the org.clojure/clojurescript dependency in deps.edn to the latest released version on Maven Central and verify the project compiles
argument-hint: "[--report] [all]"
user-invocable: true
disable-model-invocation: true
---

# Upgrade ClojureScript

Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central and verify the project still compiles. See `CONVENTIONS.md` in the repo root for the argument grammar this skill follows.

## Arguments

| Input         | Target                                                                       |
|---------------|------------------------------------------------------------------------------|
| (no argument) | Upgrade `org.clojure/clojurescript` `:mvn/version` in `deps.edn` and run the advanced-compile build to verify |
| `all`         | Same as (no argument); accepted for family consistency                       |
| `--report`    | Print the current `:mvn/version` and the latest released version from Maven Central without writing `deps.edn` or running the compile |

The skill rewrites a single dependency in `deps.edn`. `<path>` rows are not part of the standard scope vocabulary for this skill because the target is fixed.

## Mutation

Mutates `deps.edn` by default, replacing the `:mvn/version` value under `org.clojure/clojurescript`. Also runs `clj -P` to refresh dependencies and the project's advanced-compile build (`clj -M:build` if defined, otherwise `clj -M -m cljs.main --optimizations advanced --compile <main-ns>`), which writes generated JavaScript under `out/` or the project's compiler `:output-dir` (build artifact). On compile failure, the skill reverts `:mvn/version` to the recorded original value. The skill also runs `clj-kondo --copy-configs --dependencies` to refresh cached exports under `.clj-kondo/imports/`. With `--report`, the skill writes nothing and runs no compile or import refresh.

## Prerequisites

Verify `deps.edn` exists in the current directory and contains an `org.clojure/clojurescript` dependency. If not, stop and tell the user.

## Steps

Parse `$ARGUMENTS` to determine whether `--report` is present.

### 1. Record Current Version

Read `deps.edn` and note the current `:mvn/version` value for `org.clojure/clojurescript`.

### 2. Fetch the Latest Released Version

Query Maven Central for the latest release:

```bash
curl -sSL -A 'Mozilla/5.0' \
  "https://search.maven.org/solrsearch/select?q=g:%22org.clojure%22+AND+a:%22clojurescript%22&rows=1&core=gav" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['response']['docs'][0]['v'])"
```

If the network call fails, ask the user for the target version rather than guessing. Do not invent a version number.

If `--report` is present, print the previous and latest versions and stop without writing `deps.edn` or running the compile:

```
ClojureScript upgrade report:
  Current: <old-version>
  Latest:  <latest-version>
```

### 3. Compare and Update

If the latest version matches the current version, report "Already at the latest version" and stop.

Otherwise, update the `:mvn/version` value in `deps.edn` to the latest version. Preserve formatting and surrounding content.

### 4. Refresh Dependencies and Compile

```bash
clj -P
clj -M -m cljs.main --compile <main-ns>
```

Where `<main-ns>` is the project's main namespace. Inspect `deps.edn` or the project README to determine it. If the project defines a `:build` alias, use that instead:

```bash
clj -M:build
```

If compilation fails, revert the `:mvn/version` in `deps.edn` to the original value, report the error, and stop.

### 5. Refresh clj-kondo Imports

clj-kondo ships with cached exports that reference specific versions. After upgrading, refresh them:

```bash
clj-kondo --copy-configs --dependencies --lint "$(clj -Spath)"
```

### 6. Report

Print a summary:

```
ClojureScript upgraded:
  Previous: <old-version>
  Current:  <new-version>
  Compile:  PASS
  clj-kondo imports refreshed
```

## Gotchas

- Maven Central's `solrsearch` endpoint occasionally returns no rows when the index is being refreshed. Re-run the query if the response has zero results before falling back to asking the user.
- Pre-release versions appear in the search results with suffixes like `-rc1` or `-alpha1`. The `&rows=1` query returns the most recent regardless of stability. Confirm with the user before applying a non-stable version.
- Some projects pin ClojureScript via a Git SHA (`{:git/url "https://github.com/clojure/clojurescript" :sha "..."}`) rather than `:mvn/version`. This skill only handles the `:mvn/version` path; if a Git dependency is present, stop and tell the user.
- shadow-cljs projects often pin ClojureScript through `shadow-cljs.edn` rather than `deps.edn`. Detect `shadow-cljs.edn` and update there if `deps.edn` does not name `org.clojure/clojurescript`.
