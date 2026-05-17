---
name: cljs-upgrade
description: >-
  Upgrade ClojureScript to the latest released version. Use when the user asks
  to upgrade, update, or bump ClojureScript, deps.edn ClojureScript version,
  or the `org.clojure/clojurescript` dependency.
user-invocable: true
disable-model-invocation: true
---

# Upgrade ClojureScript

Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central and verify the project still compiles.

## Prerequisites

Verify `deps.edn` exists in the current directory and contains an `org.clojure/clojurescript` dependency. If not, stop and tell the user.

## Steps

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
