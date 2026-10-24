---
name: cljs-upgrade
description: "Upgrade the org.clojure/clojurescript dependency in deps.edn to the latest released version on Maven Central and verify the project compiles"
argument-hint: "[--report] [all]"
user-invocable: true
disable-model-invocation: true
---

# Upgrade ClojureScript

Upgrade the `org.clojure/clojurescript` dependency in `deps.edn` to the latest released version on Maven Central and verify the project still compiles. See the package's [CONVENTIONS.md](https://github.com/brackendev/clojurescript-skills/blob/master/CONVENTIONS.md) for the argument grammar this skill follows.

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

Verify `deps.edn` exists in the current directory and contains an `org.clojure/clojurescript` dependency. If not, stop and tell the user. When the project has a `shadow-cljs.edn` file and `deps.edn` does not name `org.clojure/clojurescript`, the ClojureScript version comes from the shadow-cljs release, which upstream tests against one set of `org.clojure/clojurescript` and Closure Compiler versions. Tell the user to upgrade `thheller/shadow-cljs` instead, and do not add or edit an `org.clojure/clojurescript` entry in `shadow-cljs.edn`.

## Steps

Parse `$ARGUMENTS` to determine whether `--report` is present.

### 1. Record Current Version

Read `deps.edn` and note the current `:mvn/version` value for `org.clojure/clojurescript`.

### 2. Fetch the Latest Released Version

Query Maven Central for the latest release:

```bash
curl -sSL -A 'Mozilla/5.0' \
  'https://repo1.maven.org/maven2/org/clojure/clojurescript/maven-metadata.xml' \
  | python3 -c "import re,sys; versions=re.findall(r'<version>([^<]+)</version>', sys.stdin.read()); print([v for v in versions if not re.search(r'alpha|beta|rc|snapshot', v, re.I)][-1])"
```

The command prints the newest stable version in Maven Central's metadata. Do not use the `search.maven.org` search index instead: it can lag many releases behind Maven Central.

If the network call fails, ask the user for the target version rather than guessing. Do not invent a version number.

If `--report` is present, print the previous and latest versions and stop without writing `deps.edn` or running the compile:

```
ClojureScript upgrade report:
  Current: <old-version>
  Latest:  <latest-version>
```

### 3. Compare and Update

If the latest version matches the current version, report "Already at the latest version" and stop. If it is older than the current version (compare the dot-separated numbers in order), report both versions and stop without writing. Never downgrade the dependency.

Otherwise, update the `:mvn/version` value in `deps.edn` to the latest version. Preserve formatting and surrounding content.

### 4. Refresh Dependencies and Compile

```bash
clj -P
clj -M -m cljs.main --optimizations advanced --compile <main-ns>
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

- The metadata file lists every published version in publish order, and the command skips names that contain `alpha`, `beta`, `rc`, or `snapshot`. Apply a prerelease only when the user names one.
- Some projects pin ClojureScript via a Git SHA (`{:git/url "https://github.com/clojure/clojurescript" :sha "..."}`) rather than `:mvn/version`. This skill only handles the `:mvn/version` path; if a Git dependency is present, stop and tell the user.
- ClojureScript 1.12.42 and later require Java 21. When the upgrade crosses that version, check `java -version` before compiling. On an older JDK the compile step fails and the skill reverts the version, so report the Java requirement instead of a generic compile failure.
