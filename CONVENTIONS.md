# Skill Conventions

This document defines the argument grammar, scope vocabulary, and mutation defaults that every user-invocable skill in `clojurescript-skills` follows. A reader who learns one skill should be able to predict every other skill. New skills follow this document.

These conventions apply to user-invocable skills (`user-invocable: true` in frontmatter). Model-invocable reference skills with no argument surface (currently `clojurescript` and `clojurescript-lenses`) carry no argument grammar and are exempt from rules 1 and 2.

## The three rules

### Rule 1: argument grammar

User-invocable skills accept natural-language keywords and bare paths. The single sanctioned flag is `--report`. No other `--name` flags exist.

Skill-specific modifiers are bare phrases, not flags. Examples used in this plugin:

- `lint`, `format`, `test`, `advanced`, `dry`: step keywords for `/cljs-tidy`
- `all`, `<path>`, `<glob>`: shared scope keywords listed in rule 2

Each skill documents its own modifiers in its `## Arguments` table.

Exemptions are listed in the [Exemptions](#exemptions) section with a reason. The standard exemption pattern is a scaffolding skill that needs a required positional argument because no useful default exists.

### Rule 2: scope vocabulary

Skills that operate on files, diffs, pull requests, or commit messages share these core rows in their `## Arguments` table:

| Input             | Target                                                          |
|-------------------|-----------------------------------------------------------------|
| (no argument)     | The skill's narrowest useful default                            |
| `all`             | Widen the selected scope to its maximum                         |
| `<path>` `<glob>` | Operate on those files or directories                           |

Each skill states what `all` resolves to in concrete terms (the whole project, the full codebase, every detected step). The keyword has one uniform meaning: widen the selected scope to the maximum. The unit varies per skill.

A skill whose narrowest useful default is already maximum scope still accepts `all` for family consistency. The table row reads "Same as (no argument); accepted for family consistency."

Opt-in rows (`#N` or PR URL, `pr`, `commit`) appear only when the skill genuinely supports them. None of the current `clojurescript-skills` skills operate on pull requests or commit messages, so the opt-in rows do not appear in this plugin today.

### Rule 3: mutation is the default

Skills that can mutate the workspace apply changes when invoked. The operator passes `--report` to receive a description of what the skill would do, without modifying any files.

Only the literal token `--report` enables report-only mode. Natural-language phrases ("preview", "dry run", "rehearse") are scope input or step keywords, not mode triggers. A skill that conflates them is wrong.

Command verbs reinforce the default. Skills named `/cljs-tidy`, `/cljs-new`, `/cljs-upgrade` (verbs that imply action) mutate by default. Skills named `/cljs-smells-review` (`review` is a reading verb) are pure-report.

## Classification

Every user-invocable skill falls into one of two classes.

**Mutating skill** -- default behavior. May carry `--report` when preview is useful. May omit `--report` when preview is meaningless (the operator inspects `git diff` after the fact) or the action is small and reversible.

**Pure report** -- never mutates. No `--report` flag because there is nothing to invert.

A skill is classified by its actual behavior, not by its name. If the name and behavior disagree, the rename is the fix. The classification appears in the skill's own description so the operator knows what to expect.

### Skills in this plugin

| Skill                  | Class           | `--report` available? | Notes |
|------------------------|-----------------|------------------------|-------|
| `/cljs-tidy`           | Mutating        | Yes                    | `format` step runs `cljfmt fix` by default. `--report` swaps it for `cljfmt check`. `lint`, `test`, `advanced`, and `dry` are pure-read of source regardless. |
| `/cljs-new`            | Mutating        | No                     | Scaffolds the project tree. Preview the side effects by reading `SKILL.md`. |
| `/cljs-upgrade`        | Mutating        | Yes                    | Rewrites `:mvn/version` for `org.clojure/clojurescript` in `deps.edn`. `--report` prints the previous and remote version without writing. |
| `/cljs-smells-review`  | Pure report     | No flag (no inverse)   | Placeholder until the ClojureScript smells catalog ships. The eventual review never writes source files. |

Model-invocable skills (`clojurescript`, `clojurescript-lenses`) have no argument surface and are not classified here.

## Section structure

User-invocable skills with an argument surface use these section conventions:

- `## Arguments` -- required. Contains the canonical scope table.
- `## Scope` -- optional. Add only when detection order, fallback, or base-branch resolution exceeds what the table can express.
- `## Mutation` -- optional. Add only when mutation behavior needs clarification beyond a single table row (for example, when only one of several steps writes, or when a build-artifact write is distinct from a source mutation).

The section name `## Customization` is retired.

## Worked examples

### `/cljs-tidy` -- mutating skill with `--report`

```
/cljs-tidy                    # all five steps; format writes
/cljs-tidy lint               # lint only (pure-read; no writes anywhere)
/cljs-tidy format             # format step; writes via cljfmt fix
/cljs-tidy advanced dry       # combined step keywords
/cljs-tidy --report           # all five steps; format reads via cljfmt check
/cljs-tidy format --report    # format step; no writes
/cljs-tidy all                # synonym for (no argument)
```

The skill writes when the `format` step runs without `--report`. With `--report`, the format step runs `cljfmt check`, which reports diffs without writing. The other four steps (`lint`, `test`, `advanced`, `dry`) are pure-read of source regardless. The `advanced` step writes generated JavaScript under `out/` or the project's compiler `:output-dir`; that is a build artifact, not a source mutation, and is unaffected by `--report`. The `## Mutation` section in the skill body documents this asymmetry.

### `/cljs-new` -- mutating skill, exemption from `all`/path rows

```
/cljs-new my-app             # creates project tree at ./my-app
/cljs-new                    # prompts the operator for a project name
```

`<project-name>` is a required positional argument. The skill omits the `all` and `<path>` rows because they would not be meaningful for scaffolding. It also omits `--report` because preview is meaningless: the operator reads `SKILL.md` to see what files will be written. The exemption is listed below.

### `/cljs-upgrade` -- mutating skill with `--report`

```
/cljs-upgrade                # upgrade deps.edn :mvn/version and run the advanced build
/cljs-upgrade --report       # print current :mvn/version and latest from Maven Central; no writes
/cljs-upgrade all            # synonym for (no argument)
```

The default rewrites `:mvn/version` for `org.clojure/clojurescript` in `deps.edn` and verifies with an advanced compile. With `--report`, the skill prints the current `:mvn/version`, the latest released version on Maven Central, and the would-be diff. No writes. No compile.

### `/cljs-smells-review` -- pure-report skill

```
/cljs-smells-review                      # review changed files
/cljs-smells-review src/my_app           # review files under directory
/cljs-smells-review src/my_app/core.cljs # review specific file
/cljs-smells-review all                  # review the full codebase
```

No `--report` flag, because the skill never writes. Operators apply suggestions themselves. The body remains a placeholder until the ClojureScript smells catalog ships; the argument grammar and pure-report classification land now so the eventual implementation has a contract to honor.

## Exemptions

`/cljs-new` accepts a required positional `<project-name>` because no useful default exists for scaffolding. It omits the `all` and `<path>` scope rows and the `--report` flag for the same reason. The `## Arguments` table in its `SKILL.md` documents the positional grammar.

## Ambiguity notes

**`(no argument)` outside a git worktree.** A skill whose narrowest useful default depends on git state (for example, "review changed files") must define the fallback when no git worktree is present. The expected fallback is to ask the operator what to review rather than to widen silently to `all`. In this plugin, `/cljs-smells-review` will document this fallback when its body lands; `/cljs-tidy`, `/cljs-upgrade`, and `/cljs-new` derive scope from `deps.edn` and the project tree rather than from a diff, so the question does not apply.

**`commit` versus staged-and-unstaged state.** When a future skill accepts `commit` as a scope keyword, it must state whether `commit` means the most recent commit, the staged tree, or the staged-plus-unstaged working tree. The expected default is the most recent commit. None of the current skills carry this scope.

**`--report` versus natural-language synonyms.** "Preview", "dry run", "rehearse", and similar phrases are scope input or step keywords (or operator chatter), never mode triggers. Only the literal `--report` token disables writes. The `/cljs-tidy` step keyword `dry` is the dry4clj duplicate-form scan, not a dry-run mode; the conflict is named here so operators do not read `dry` as "dry run".

**Step keywords versus scope keywords.** A skill like `/cljs-tidy` accepts step keywords (`lint`, `format`, `test`, `advanced`, `dry`) that select work to run, and scope keywords (`all`) that widen scope. Step keywords are skill-specific and listed in the skill's own table. Scope keywords are shared and listed here. When a skill has both, the `## Arguments` table lists both with clearly distinct rows.

**Tool-level flags the skill calls internally.** A skill may invoke a tool that itself uses POSIX flags (for example, `clj-kondo --lint`, `cljfmt fix`, `cljfmt check`, `cljs.main --optimizations`, `clj -P`). Those are tool-level flags, not skill flags, and do not count against rule 1. The skill body should disambiguate when a tool flag could be mistaken for a skill flag.

## Author checklist

When adding or modifying a user-invocable skill, confirm each item before committing.

- [ ] Skill has a `## Arguments` section (or is listed under [Exemptions](#exemptions)).
- [ ] Scope rows match the canonical table; opt-in rows appear only where the skill genuinely supports them.
- [ ] If the skill mutates, the command verb signals it (`/cljs-tidy`, `/cljs-new`, `/cljs-upgrade`).
- [ ] If the skill mutates and preview is useful, `--report` is documented.
- [ ] If the skill is pure-report, the verb signals it (`/cljs-smells-review`) and the skill has no `--report` flag.
- [ ] No `## Customization` section.
- [ ] No `--name` flags other than `--report`. Tool-level flags the skill calls internally (for example, `cljfmt check`, `clj-kondo --lint`) are not skill flags and do not count.
- [ ] Frontmatter `name` matches the skill's directory name.
- [ ] OpenCode mirror under `.opencode/skills/<name>/SKILL.md` is byte-identical to the canonical source.
- [ ] `agents/openai.yaml` `default_prompt` references the current command name.
- [ ] `CHANGELOG.md` records the change under `[Unreleased]` when the change is user-facing.
