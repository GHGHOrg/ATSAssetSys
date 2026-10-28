# plan/spikes/vscode-claude-code.md

Stage: Build. Status: draft, not approved (2026-10-28).
Source: plan/plan.md section 7 (the spike's question list, added with this note), handoff-12.md (written 2026-10-28), the Project instructions (Ruleset A and B, read in the Project on 2026-10-28).
The look-ups were done on 2026-10-28. Versions and behaviour here are facts of that day.

How sources are labelled in this note:
- "I fetched": page text returned by my own fetch in this chat, from code.claude.com (2026-10-28).
- "I read in the Project": the Project's instructions and doc list, read with the Project tool.
- "your file": a repo file you uploaded. Its hash matched the one you pasted.
- "my reading": my interpretation. Not a fact until the spike's run shows it.
- "not checked": not looked at.

## 1. Outcome
Claude Code runs in VS Code on your machine in this repo, with every needed component installed, configured and shown working. Nothing that lives only in the claude.ai Project or in this chat is lost: each item in the inventory (section 3.1) ends in one of three places: in the repo, in the hand-off, or deliberately dropped by you.

## 2. Gate
This is a decision note, not an iteration. It gives no app feature and changes no app code.
Iteration 02b's file cannot start until: the decisions in section 5 are approved, the run in section 4 is done, and its results are written into section 9.
Branch: spike-vscode-claude-code, one pull request, merged by you.

## 3. Findings

### 3.1 Inventory: what exists now and where it would go
| Item | Where it lives now | Visible in Claude Code? | Proposed destination |
|---|---|---|---|
| Ruleset A (9 rules) | Project instructions (I read in the Project) | No | Full text into the repo (decision D2). CLAUDE.md section 1 holds a short version only |
| Ruleset B (3 rules) | Project instructions | No | Same. CLAUDE.md section 2 has 2 of the 3 rules; the web-search rule is missing |
| Playbook copy "in project knowledge" | Does not exist: the Project has 0 docs and 0 files (I read in the Project). The instructions give only a link | n/a | Decision D3 |
| Project knowledge, 74,491 bytes | Synced from GitHub: /plan/, /spec/, /CLAUDE.md (wireframes excluded) | Yes, the same files are in the repo | None |
| Wireframes | spec/wireframes/ in the repo (excluded from the Project sync) | Yes | None |
| Project memory | claude.ai Project. Not readable in this session: the session has no memory access | No | You check it in claude.ai and tell me what is there |
| Working-style rules and lessons | handoff-12.md sections 7 and 8, and this chat | No | CLAUDE.md or a user-level file (D2) |
| Open items and risks | handoff-12.md section 5, and the 02a file sections 13.5 and 15 (already in the repo) | Partly | Decision D4 |
| This chat's history | claude.ai chat and this session's transcript | No | The hand-off is the carrier |
| claude.ai skills and connectors | claude.ai | Not checked | None needed (my reading: the project uses none) |

### 3.2 Answers to the questions in plan.md section 7
| # | Question | Finding | Source |
|---|---|---|---|
| 1 | What must be installed? | VS Code 1.94.0 or later; the extension "Claude Code" (anthropic.claude-code); a paid Claude subscription (Pro, Max, Team or Enterprise) or a Claude Console account, no API key needed. The extension bundles its own copy of the CLI for the chat panel. Running `claude` in VS Code's integrated terminal also needs the standalone CLI. Git for Windows is recommended (without it Claude Code uses PowerShell as its shell tool); WSL is not needed | I fetched: vs-code, overview |
| 2 | How is the CLI installed on Windows? | Three ways: PowerShell `irm https://claude.ai/install.ps1 \| iex` (downloads and runs a script; auto-updates), `winget install Anthropic.ClaudeCode` (no auto-update), CMD. Check with `claude --version` in a new terminal | I fetched: overview |
| 3 | Where do the rules live? | CLAUDE.md at the project root, in directories above the working directory, and `~/.claude/CLAUDE.md` for all projects; `CLAUDE.local.md` for private notes (add to .gitignore); `.claude/rules/` for scoped rules. All found files are concatenated, not overridden. Imports with `@path` (depth up to 4). Target under 200 lines per file. The page says these are "context, not enforced configuration"; to block an action, use a PreToolUse hook | I fetched: memory |
| 4 | What may it do without asking? | Modes: default/Manual (reads only), acceptEdits, plan, auto (everything, with background checks), dontAsk, bypassPermissions. From Claude Code v2.1.283 the built-in start mode is Auto. In VS Code the start mode comes from, in order: claudeCode.initialPermissionMode (read from user settings only), the mode last picked, permissions.defaultMode in ~/.claude/settings.json or managed settings, the built-in default. The extension never reads the project's .claude/settings.json for it, and "auto" set in project or local settings is ignored. Writes to .git, .vscode and .claude always prompt | I fetched: permission-modes, vs-code |
| 5 | Do web search, web fetch, shell commands, file edits and git push prompt? | In Manual mode: Bash commands yes (built-in read-only set excepted); file edits yes; WebFetch yes (except preapproved documentation domains); WebSearch yes. "Yes, and don't ask again" is remembered: for WebSearch permanently per repository, for Bash per repository and command, for file edits until the session ends. Rules are evaluated deny, then ask, then allow, and the first match wins. `Bash(git push *)` and `PowerShell(git push *)` rules exist. A rule matches the command text Claude writes, so it is not a security boundary: `git -C . push` is not matched by `Bash(git push *)` | I fetched: permissions |
| 6 | Where are the settings files, and what wins? | Windows: `~/.claude` means `%USERPROFILE%\.claude`. Files: user (`~/.claude/settings.json`), shared project (`.claude/settings.json`, meant to be committed), project local (`.claude/settings.local.json`), managed. Highest first: managed, command line, project local, shared project, user. Lists such as permissions.allow merge. A shared-file `allow` rule waits until you trust the folder; `deny` and `ask` apply at once. "Don't ask again" writes an allow rule into `.claude/settings.local.json` | I fetched: settings |
| 7 | Is anything saved outside the repo? | Auto memory: on by default in local sessions; notes Claude writes itself, plain markdown, machine-local, shared across worktrees of one repo; switched off with `autoMemoryEnabled: false` (user or project settings) or `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`; browse with `/memory` | I fetched: memory |
| 8 | Does the hand-off work as written? | Not run yet (section 8) | none |

### 3.3 What I did not read or verify
- Read in parts: the VS Code page (prerequisites, install, review, settings table and keyword hits); the permissions and memory pages (keyword hits, not the whole page).
- Not read at all: the last 14,068 characters of permission-modes, the last 7,660 characters of settings-reference, and the pages on hooks, MCP, sandboxing, authentication and troubleshooting.
- Not checked: which Claude Code version and extension version will install on your machine, whether your subscription allows VS Code use, and Claude Code behaviour on your machine.
- Documentation can change: every fact is the page text of 2026-10-28.
- My reading, not shown: the permission prompt displays what Claude is about to do, so a WebSearch prompt would show the query. The page says "a permission prompt shows what Claude is about to do" without naming the search query.

## 4. Plan (on branch spike-vscode-claude-code)
1. Re-check the pages of section 3 on the day of the run (versions, wording). Note any difference.
2. You open the Project in claude.ai and tell me what its memory holds, if anything (section 3.1).
3. Install and sign in, per D8. Show the extension version (and `claude --version` if the CLI is installed).
4. Write the user-level settings (VS Code user settings and ~/.claude/settings.json) per D1, D5 and D6. I show each file's exact text first. You type or paste it; nothing is saved outside the repo by me.
5. Repo changes, in one pull request, each shown and approved before saving:
   - CLAUDE.md: full Ruleset A and B and the working-style rules (D2); known pitfalls for Claude Code (from section 3).
   - .claude/settings.json: the permission rules of D6 (shared project settings, committed).
   - plan/open-items.md (D4), if approved.
   - plan.md: the files list, a section 7 entry for this spike, the status row, the change log.
   - handoff-12.md refreshed so its hashes and state match the merged repo.
6. Run the checks in section 8 in a new VS Code window on the repo. You paste each result.
7. Fix what failed. Write section 9 (results) and section 10 (status). Pull request, CI, your merge.

## 5. Decisions (leanings are mine, not settled)
D1. Start mode. Pin Manual: `claudeCode.initialPermissionMode: "default"` in VS Code user settings, `permissions.defaultMode: "default"` in ~/.claude/settings.json, and `permissions.disableAutoMode: "disable"` so Auto cannot be reached by accident. Leaning: all three. Effect for you: every file edit shows a diff and asks, and every shell command except the read-only ones asks; more clicks, no silent edits. In plan mode, approve with "Yes, manually approve edits".
D2. Where the rules live. Leaning: the repo CLAUDE.md, with Ruleset A and B in full text, and a short working-style section. It is versioned and travels with the repo. It would grow from 72 to about 130 lines, under the page's 200-line target. Alternative: ~/.claude/CLAUDE.md (outside the repo, not versioned, applies to every project). Either way these are instructions Claude reads, not enforced rules.
D3. The playbook. The Project text says a copy is in project knowledge, but none exists. Leaning: keep the link only, record in the note that no copy exists, and leave the Project text alone.
D4. Open items. Leaning: a new file plan/open-items.md holding the carry-over list (the unexplained findings, the deferred checks, Dependabot, the price spike), so nothing depends on the hand-off alone.
D5. Auto memory. Leaning: off (`autoMemoryEnabled: false` in user settings). Project knowledge then lives only in repo files you approve. Cost: Claude Code will not save corrections on its own; I would propose each as a CLAUDE.md edit.
D6. What may run without asking. Leaning:
   - allow: `.\gradlew.bat` build, test, lint and spotlessCheck, and read-only git (status, diff, log).
   - ask (explicit, so a later broad allow cannot override it, since ask is evaluated before allow): `git push *` for Bash and PowerShell.
   - deny: reads of keystore.properties, signing.properties, *.jks, *.keystore.
   - Everything else prompts in Manual mode. Web search and web fetch always prompt, and I never choose "don't ask again" for them.
D7. Commits and pushes. Leaning: Claude Code may stage and commit after showing the diff and asking each time; you push, create and merge pull requests yourself, as today. Effect: no more copying files and pasting hashes for edits Claude Code makes itself; the real output is still shown.
D8. Install. Leaning: the extension only (it bundles the CLI), signed in with your own account; the standalone CLI later, only if you want `claude` in a terminal, via WinGet (no piped script, no auto-update). Which account and plan is your choice; I did not check whether your plan covers VS Code.

## 6. Out of scope
Hooks (beyond naming them as the way to enforce a rule), MCP servers, plugins and skills, GitHub Actions integration, the desktop app and the web version, JetBrains, moving claude.ai skills, any app code, and iteration 02b itself.

## 7. Risks
- The default start mode is Auto, which edits without asking. D1 removes it, but a later update could change the default (not checked).
- CLAUDE.md is context, not enforcement. "Say the stage first", "one question at a time" and "never push" can drift. A hook would enforce a rule; this spike does not add one.
- Permission rules match the command text, so `git -C . push` is not matched by `Bash(git push *)`. The safety net is that Manual mode prompts for any Bash command outside the read-only set and the allow list.
- Choosing "Yes, and don't ask again" writes a standing allow rule into .claude/settings.local.json (WebSearch: permanently per repository). I do not choose it, but you may click it by habit.
- Auto memory (D5) if left on: saved state outside the repo that you do not review.
- JAVA_HOME and ANDROID_HOME are user environment variables. VS Code, started before they were set, may not see them (my reading).
- The VS Code Kotlin extension may start the Gradle daemon on its own Java again (CLAUDE.md pitfall). Fix: `.\gradlew.bat --stop`, then rebuild.
- Two tools will edit the same repo for a time. The Project keeps syncing main; the hand-off and the Project instructions can go stale.
- Plan limits and usage cost are not checked.
- Claude Code in a repo folder can read any file there. Signing keys stay outside the repo (setup.md section 6); the deny rules of D6 are a second guard.

## 8. Proof
By you, in PowerShell and VS Code, each result pasted and written into section 9 as "checked" or "not checked":
1. Extension version in the Extensions view, and `claude --version` if the CLI is installed.
2. A new window on the repo folder: the Claude Code tab opens and shows you signed in.
3. A new conversation, prompt: "State the stage, then list the rules you loaded from CLAUDE.md." Expected: the stage is stated first, and Ruleset A and B are present.
4. The mode indicator reads Manual. Open a second new conversation: it still reads Manual.
5. Ask it to create a scratch file in the repo. Expected: a diff and a prompt. Reject it. `git status` shows no change.
6. Ask it to run `.\gradlew.bat spotlessCheck`. Expected: real output, the same as yours.
7. Ask it to run `git push --dry-run`. Expected: a prompt. Deny it.
8. Ask it for a harmless web search. Expected: a prompt showing the details. Deny it.
9. Ask it to read one file in spec/wireframes/. Expected: it reads it.
10. A new conversation with the hand-off pasted. Expected: it states the stage, reads the repo files, asks the disk question and writes nothing.
11. `/status` shows the setting sources you expect.
12. Section 3.1, line by line: each inventory row has a destination, and you confirm it.
13. CI green on the spike's pull request (your report). `git status` shows no keys and no .claude/settings.local.json tracked.
Not covered: other machines, other plans, and Claude Code versions after the run day.

## 9. Results (not run yet)
Every Claude Code, VS Code, Git, CI and settings result here will be your report of pasted output or of a check you did. I do not see your machine or GitHub. Where something was not run or not verified, it will say so.
9.1 Decisions D1 to D8: settled values and dates (to fill).
9.2 Install: versions and sign-in (to fill).
9.3 Checks 1 to 13 of section 8: result and your report for each (to fill).
9.4 Departures from section 4 (to fill).
9.5 Not run or not verified (to fill).

## 10. Status and notes
Draft 2026-10-28. Not approved. Not run. The note and my leanings are shown to you in three parts; the decisions D1 to D8 are settled one at a time afterwards and recorded in section 5.
