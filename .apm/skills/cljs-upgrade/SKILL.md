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
| (no argument) | Upgrade `org.clojure/clojurescript` `:mvn/version` in `deps.edn`, apply the mechanical changes that the release notes require, and run the advanced-compile build to verify |
| `all`         | Same as (no argument); accepted for family consistency                       |
| `--report`    | Print the current `:mvn/version`, the latest released version from Maven Central, and the release-note changes that affect the project, without writing `deps.edn`, editing files, or running the compile |

The skill rewrites a single dependency in `deps.edn`. `<path>` rows are not part of the standard scope vocabulary for this skill because the target is fixed.

## Mutation

Mutates `deps.edn` by default, replacing the `:mvn/version` value under `org.clojure/clojurescript`. Also runs `clojure -P` to refresh dependencies and the project's advanced-compile build (`clojure -M:build` if defined, otherwise `clojure -M -m cljs.main --optimizations advanced --compile <main-ns>`), which writes generated JavaScript under `out/` or the project's compiler `:output-dir` (build artifact). When the release notes between the two versions require a change to the project's build configuration or source, the skill applies it before compiling and lists it in the report. On compile failure, the skill reverts `:mvn/version` to the recorded original value and undoes those release-note changes. The skill also runs `clj-kondo --copy-configs --dependencies` to refresh cached exports under `.clj-kondo/imports/`. With `--report`, the skill writes nothing and runs no compile or import refresh. It still fetches the latest version and reads the release notes.

## Prerequisites

Verify `deps.edn` exists in the current directory and contains an `org.clojure/clojurescript` dependency. If not, stop and tell the user. When the project has a `shadow-cljs.edn` file and `deps.edn` does not name `org.clojure/clojurescript`, the ClojureScript version comes from the shadow-cljs release, which upstream tests against one set of `org.clojure/clojurescript` and Closure Compiler versions. Tell the user to upgrade `thheller/shadow-cljs` instead, and do not add or edit an `org.clojure/clojurescript` entry in `shadow-cljs.edn`.

When `deps.edn` depends on `com.hyperfiddle/electric`, stop before writing anything, with or without `--report`. Electric loads on the JVM only with the ClojureScript version that its release declares, and a newer ClojureScript stops every Electric namespace from loading there. Read the declared version from the `org.clojure/clojurescript` dependency in the POM of the project's Electric release:

```bash
curl -sSL "https://repo.clojars.org/com/hyperfiddle/electric/<electric-version>/electric-<electric-version>.pom"
```

Report that version next to the project's pin, and tell the user that the `electric` skill (electric-skills) describes how to move ClojureScript and shadow-cljs together with an Electric release.

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

### 3. Compare the Versions

Compare the dot-separated numbers in order. If the latest version matches the current version, report "Already at the latest version" and stop. If it is older than the current version, report both versions and stop without writing. Never downgrade the dependency. These checks run with `--report` too.

### 4. Read the Release Notes

Read what changed between the recorded version and the latest version before writing anything. ClojureScript GitHub releases have empty bodies, so read `changes.md` at the latest release tag, which never includes unreleased work:

```bash
curl -sSL "https://raw.githubusercontent.com/clojure/clojurescript/r<latest-version>/changes.md"
```

Each section starts with `## <version>`, newest first. Read every section whose version is newer than the recorded version, comparing the dot-separated numbers. Do not search for the exact release number: some releases have no section, and a heading can differ from the Maven version (at tag `r1.12.145`, the 1.12.145 changes sit under `## 1.12.144`).

The release announcements on clojurescript.org explain dependency and Java requirement changes in more detail. List them, then fetch each one whose first line (`= <version> Release`) names a version newer than the recorded one:

```bash
curl -sSL 'https://api.github.com/repos/clojure/clojurescript-site/contents/content/news' \
  | python3 -c "import json,sys; print('\n'.join(f['name'] for f in json.load(sys.stdin) if f['name'].endswith('-release.adoc')))"
curl -sSL "https://raw.githubusercontent.com/clojure/clojurescript-site/master/content/news/<file>"
```

List the entries that can affect the project: a changed compiler default or option that the build configuration sets or relies on, a changed or removed function, macro, or `ns` clause that the source uses, and a new dependency or Java requirement. Search the project's `.cljs` and `.cljc` sources and its build configuration (`deps.edn`, build `.edn` files, `figwheel-main.edn`, and `<build>.cljs.edn`) for each affected name, and note the files and lines where it occurs. Check a new Java requirement against `java -version`. Mark each entry as mechanical, when the matching edit to the build configuration or source is a deterministic rewrite, or as needing a decision. If neither source can be fetched, say so in the `Changes:` line and continue.

If `--report` is present, print the versions and the list, then stop without writing `deps.edn`, editing files, or running the compile:

```
ClojureScript upgrade report:
  Current: <current-version>
  Latest:  <latest-version>
  Changes: <each entry that affects the project, with the files that use it, marked mechanical or needs a decision, or "none found">
```

### 5. Update deps.edn

Update the `:mvn/version` value in `deps.edn` to the latest version. Preserve formatting and surrounding content.

### 6. Apply the Mechanical Changes

Make the edit for each entry from step 4 that is marked mechanical. Leave every entry that needs a decision for the report.

### 7. Refresh Dependencies and Compile

```bash
clojure -P
clojure -M -m cljs.main --optimizations advanced --compile <main-ns>
```

Where `<main-ns>` is the project's main namespace. Inspect `deps.edn` or the project README to determine it. If the project defines a `:build` alias, use that instead:

```bash
clojure -M:build
```

If compilation fails, revert the `:mvn/version` in `deps.edn` to the recorded value, undo the changes made in step 6, report the error together with any step 4 entry that explains it, and stop.

### 8. Refresh clj-kondo Imports

clj-kondo copies library configurations into `.clj-kondo/imports/` and caches its analysis of the dependencies, including `cljs.core`, under `.clj-kondo/.cache/`. It copies configurations only when the project already has a `.clj-kondo` directory. After upgrading, refresh both:

```bash
clj-kondo --copy-configs --dependencies --lint "$(clojure -Spath)"
```

### 9. Report

Print a summary:

```
ClojureScript upgraded:
  Previous: <old-version>
  Current:  <new-version>
  Compile:  PASS
  Changes:  <each entry that affects the project, with the files that use it, marked applied or needs a decision, or "none found">
  clj-kondo imports refreshed
```

## Gotchas

- The metadata file lists every published version in publish order, and the command skips names that contain `alpha`, `beta`, `rc`, or `snapshot`. Apply a prerelease only when the user names one.
- Some projects pin ClojureScript with a Git coordinate (`{:git/url "https://github.com/clojure/clojurescript" :git/sha "..."}`, or the synonym `:sha`) rather than `:mvn/version`. This skill only handles the `:mvn/version` path. If a Git dependency is present, stop and tell the user.
- ClojureScript requires Java 21 or later, because its Google Closure Compiler dependency requires Java 21. Check `java -version` before compiling. When the JDK is below 21, the compile step fails and the skill reverts the version, so report the Java requirement instead of a generic compile failure.
