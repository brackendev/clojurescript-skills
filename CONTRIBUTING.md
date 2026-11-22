# Contributing

For people working on the plugin source. End-user install instructions live in [README.md](README.md).

## Layout

| Path | Purpose |
|------|---------|
| `apm.yml` | Package manifest (`type: skill`, no MCP dependencies). `target` limits installs to Claude Code, Codex, and OpenCode. |
| `.apm/skills/<name>/` | Canonical skill source. APM deploys from here. Each directory holds `SKILL.md` plus optional `agents/openai.yaml`, `references/`, `scripts/`, `examples/`. |
| `.apm/hooks/` | Hook definitions. `cljs-rules.json` defines the hook. `scripts/` holds the bash launcher, the Babashka script, and `cljs-rules.md`, the condensed Iron Laws that the hook delivers. |
| `tests/hooks/` | Hook test suites. They are repository files, not package content, and APM does not deploy them. |
| `.opencode/skills` | Symlink to `../.apm/skills`. OpenCode finds it from any directory in the repository, so an OpenCode session loads the canonical skills and their supporting files without an APM install. |
| `opencode.jsonc` | Local OpenCode configuration. |
| `README.md` | End-user documentation. |
| `CHANGELOG.md` | User-facing changes per version. |
| `CONVENTIONS.md` | Canonical argument grammar, scope vocabulary, and mutation defaults for every user-invocable skill. |
| `AGENTS.md`, `GEMINI.md`, `TODO.md` | Local agent instructions (`GEMINI.md` is a symlink to `AGENTS.md`) and working notes. Gitignored globally and never committed. |

## APM lockfile rule

Do not add `.claude-plugin/`, `.codex-plugin/`, `.agents/plugins/marketplace.json`, or a root `plugin.json`. In APM 0.36.1 the package's `apm.yml` decides the package type before any plugin marker is considered, so a local install with one of these markers still records `package_type: apm_package`. Remote installs from GitHub were not tested with these markers, so keep all of them out.

## Adding or modifying a skill

1. Edit `.apm/skills/<name>/SKILL.md`.
2. Update `README.md` if the change is user-facing.
3. Add a `CHANGELOG.md` entry for every change to a published file, including documentation-only changes, under a `## [x.y.z] - YYYY-MM-DD` heading for the new `apm.yml` version. Leave `## [Unreleased]` empty above it.
4. Increment the `version` field in `apm.yml`.

## Adding or modifying a hook

1. Edit the hook JSON in `.apm/hooks/<name>.json`, keyed by lifecycle event (`SessionStart`, `PostToolUse`). Use the matcher `"*"` for tool events and choose the tool inside the script, because APM passes matchers through unchanged.
2. Do not add a hook file for one runtime. APM sends every hook file to each runtime that `target` in `apm.yml` allows and that has hooks, which is Claude Code and Codex. An install fails when a hook also reaches Cursor, so keep Cursor out of `target`.
3. Write the hook as a Babashka script, `.apm/hooks/scripts/<name>.bb`, started by a bash launcher, `.apm/hooks/scripts/<name>.sh`. When `bb` is missing, the launcher prints that the hook is inactive to standard error and exits 0. Reference the launcher as `${PLUGIN_ROOT}/.apm/hooks/scripts/<name>.sh`. The `.apm/` segment is required, or APM logs "Hook script not found". Keep the launcher executable.
4. Add cases to the suites in `tests/hooks/` and run them (see "Hook tests" below).
5. Update `README.md`, increment the `version` field in `apm.yml`, and add a `CHANGELOG.md` entry under a `## [x.y.z] - YYYY-MM-DD` heading for that version.

## Changing an Iron Law

The `cljs-rules` hook delivers `.apm/hooks/scripts/cljs-rules.md`, which condenses each Iron Law in the skill's `SKILL.md` into one `- <ID>: ...` line. When a law changes, update its line in the same change, keeping the law's exceptions. The coordinator's drift check compares the law IDs in both files but cannot compare their meaning.

## Validation

Configuration files:

```bash
python3 -m json.tool opencode.jsonc >/dev/null
python3 -c "import yaml, pathlib; yaml.safe_load(pathlib.Path('apm.yml').read_text())"
python3 -c "import yaml, pathlib; [yaml.safe_load(p.read_text()) for p in pathlib.Path('.apm/skills').glob('*/agents/openai.yaml')]"
```

Runtime install (requires `apm` and the runtime CLIs you want to verify: `claude`, `codex`, `opencode`):

- Run `apm install --target claude,codex,opencode`, `apm update`, and `apm uninstall` in a clean temporary project. Pre-create the runtime roots `.claude`, `.codex`, and `.opencode`. With several runtime roots present, APM stops with "Multiple harnesses detected" unless the install names a target. `apm update` reuses the targets that the install records in the project's `apm.yml`.
- Exercise user scope with `apm install brackendev/clojurescript-skills -g [--target ...]` and `apm uninstall brackendev/clojurescript-skills -g`. A local filesystem path (`apm install /absolute/path -g`) is also accepted.
- Confirm OpenCode deployment from the temporary project with `opencode api GET "/api/skill?location[directory]=$PWD"`, which lists the skills OpenCode discovers and their source paths. The first request for a directory can return an empty list while OpenCode loads the skills, so repeat it before reading the result.

### Hook tests

Run every hook suite with `bb tests/hooks/run.bb`. It needs `bb`, `git`, and `apm`, and a missing requirement fails the run. `cljs-rules-hook.bb` checks when the hook delivers the rules: at session start, on the first matching edit, after a clear or compaction, and never twice or outside a matching project. `install-check.bb` installs the package with APM at project and user scope, with `--target claude` and with `--target all`. With every target it checks that Claude Code and Codex receive the hook, that OpenCode receives the skills, and that no other runtime receives a file.

## Skill conventions

For the argument grammar, scope vocabulary, and mutation defaults that every user-invocable skill follows, see [CONVENTIONS.md](CONVENTIONS.md).

| Setting | When to use |
|---------|-------------|
| `user-invocable: true`, `disable-model-invocation: true` | User-only slash command, for scaffolding, dependency upgrades, and any skill whose changes reach beyond the working tree or are hard to undo (for example `cljs-upgrade`). |
| `user-invocable: true`, no `disable-model-invocation` | Slash command the model can also run from a natural-language request, for a fixer whose edits stay in the working tree (for example `cljs-fix`). Write the description as a trigger. |
| `user-invocable: false` | Model-invoked from conversation context (for example `clojurescript`). |

Every skill carries `agents/openai.yaml` whose `policy.allow_implicit_invocation` matches the table above: `false` for a user-only skill and `true` for every skill the model can run. Skills in this package use the JavaScript yellow brand color, `#F7DF1E`, so the runtimes can distinguish ClojureScript-specific guidance from the other Clojure-family packages. Every `agents/openai.yaml` in this package sets `brand_color` to the same value. The coordinator's `scripts/drift-check.bb` checks that it does and that no other Clojure-family package uses this color.
