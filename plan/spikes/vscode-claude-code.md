# plan/spikes/vscode-claude-code.md

Stage: Build. Status: note and decisions D1 to D14 settled by you 2026-10-28. Not run.
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
| Playbook copy "in project knowledge" | Does not exist: the Project has 0 docs and 0 files (I read in the Project). The instructions give only a link | n/a | Link only (D3) |
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
3. Install the VS Code extension only (D8, no standalone CLI) and sign in with your claude.ai account. Show the extension version.
4. Write the user-level settings: VS Code user settings (claudeCode.initialPermissionMode, D1) and ~/.claude/settings.json (permissions.defaultMode, permissions.disableAutoMode, autoMemoryEnabled, attribution; D1, D5, D7). The scope of each key is checked on the settings reference when written. I show each file's exact text first. You type or paste it; nothing is saved outside the repo by me.
5. Repo changes, in one pull request, each shown and approved before saving:
   - CLAUDE.md: Ruleset A and B in full, the working model (D1, D6 to D14) and the gates (D2); known pitfalls for Claude Code (from section 3).
   - .claude/settings.json: the permission rules of D6 (shared project settings, committed).
   - plan/open-items.md (D4).
   - plan.md: the files list, a section 7 entry for this spike, the status row, the change log.
   - handoff-12.md refreshed so its hashes and state match the merged repo.
6. Run the checks in section 8 in a new VS Code window on the repo. You paste each result.
7. Fix what failed. Write section 9 (results) and section 10 (status). Pull request, CI, your merge.

## 5. Decisions (settled by you on 2026-10-28; they replace the leanings of the draft)
D1. Start mode: Edit automatically (acceptEdits), Auto removed. Settings: claudeCode.initialPermissionMode "acceptEdits" (VS Code user settings); permissions.defaultMode "acceptEdits" and permissions.disableAutoMode "disable" (~/.claude/settings.json).
D2. Rules live in the repo's CLAUDE.md, in full text: Ruleset A and B, this working model (D1 and D6 to D14) and the gates. Sections 1 and 2 of the existing file are replaced; commands, conventions, pitfalls and standing checks stay. About 150 lines.
D3. Playbook: the link only. CLAUDE.md names it. No copy exists; none is made.
D4. Open items: a new file plan/open-items.md.
D5. Auto memory off: autoMemoryEnabled false in user settings.
D6. Permissions.
   - No asking: creating, editing and saving files (Edit automatically); reads inside the repo; git add (allow rule, Bash and PowerShell forms); .\gradlew.bat build, test, lint and spotlessCheck; git status, git diff, git log. Copies (cp, Copy-Item) run freely.
   - Ask: git commit; git merge; every git push; pull request creation and merge (gh rules, kept in case gh is installed); web search; web fetch; deletions (rm, del, Remove-Item, git rm, git clean, git reset, git checkout --, git restore, git branch -D, git stash drop); moves (mv, Move-Item); reads outside the repo; every edit of intent/intent.md and spec/spec.md; every other shell command.
   - Deny: reads of keystore.properties, signing.properties, *.jks, *.keystore, .env, .env.*.
   - Where: the ask, deny and allow rules in the repo's .claude/settings.json (allow rules wait for trust of the folder; ask and deny apply at once); the start mode and auto memory in user settings. The exact text is shown to you before it is saved (section 4, steps 4 and 5).
D7. Commits: one per step of the iteration file's order of work, proposed after build, tests and lint are green; Claude Code shows the message and the changed files and you approve each; the message is "Iteration NN X: what" with the spec refs. Attribution: kept on commits, hidden on pull request descriptions (attribution.pr empty).
D8. Install: the VS Code extension only (it bundles the CLI), signed in with the same claude.ai account on its paid plan. Usage limits are not checked.
D9. Gates: all gates of Ruleset A stay as written rules (an approved iteration file and plan.md before any code; your acceptance of intent.md and spec.md changes). Nothing technical enforces them except the Edit ask rules on intent.md and spec.md (D6).
D10. Stops: after each numbered step of the order of work, Claude Code runs build, tests and lint, shows the real output and the report, and waits for your word.
D11. Before each step: git status. If there are uncommitted changes it did not make, it stops, lists the files and waits.
D12. Report after creating or editing files: each file, the real git diff --stat output, and for documents the line count, byte count and SHA-256.
D13. Pull requests: you create and merge them (the GitHub Pull Requests extension or the browser). Claude Code drafts the title and description.
D14. Sub-agents only when you ask.

## 6. Out of scope
Hooks (beyond naming them as the way to enforce a rule), MCP servers, plugins and skills, GitHub Actions integration, the desktop app and the web version, JetBrains, moving claude.ai skills, any app code, and iteration 02b itself.

## 7. Risks
- Edit automatically changes files without asking. The only undo is git, back to the last commit. D11 (stop on uncommitted changes) and D7 (one commit per green step) limit it.
- The written gates (D9) are not enforced, except by the two Edit ask rules. Drift is possible.
- Permission rules match the command text. A deletion written another way (a script, an unusual form), or an overwriting copy (cp, Copy-Item -Force), is not matched. The safety net: any shell command not on the allow list asks, and writes to .git, .vscode and .claude always prompt.
- The default start mode is Auto. D1 removes it, but a later update could change this (not checked).
- Choosing "Yes, and don't ask again" writes a standing allow rule into .claude/settings.local.json (WebSearch: permanently per repository). I do not choose it, and you may click it by habit. The deny and ask rules of D6 still win over an allow rule, since ask is evaluated before allow.
- JAVA_HOME and ANDROID_HOME are user environment variables. VS Code, started before they were set, may not see them (my reading).
- The VS Code Kotlin extension may start the Gradle daemon on its own Java again (CLAUDE.md pitfall). Fix: .\gradlew.bat --stop, then rebuild.
- Two tools will edit the same repo for a time. The Project keeps syncing main; the hand-off and the Project instructions can go stale.
- Plan limits and usage cost are not checked.
- The Git shell: with Git for Windows, Claude Code uses Bash as its shell tool; without it, PowerShell. The rules are written in both forms. Which one is used is a check in section 8.
- The Edit ask rules use path patterns I have read on the Permissions page but not run. They are checked in section 8.

## 8. Proof
By you, in PowerShell and VS Code, each result pasted and written into section 9 as "checked" or "not checked":
1. The extension version in the Extensions view.
2. A new window on the repo folder: the Claude Code tab opens and shows you signed in.
3. A new conversation, prompt: "State the stage, then list the rules you loaded from CLAUDE.md." Expected: the stage is stated first; Ruleset A and B, the working model and the gates are present.
4. The mode indicator reads Edit automatically. Open a second new conversation: it reads the same. Auto is not in the mode switcher.
5. Ask it to create a scratch file in the repo. Expected: created without a prompt. Report as in D12. Then ask it to git add the file: no prompt.
6. Ask it to commit the scratch file. Expected: a prompt showing the message and the files. Deny it.
7. Ask it to delete the scratch file. Expected: a prompt. Deny it, then ask it to move the file: expected a prompt. Deny it. Then delete the file yourself.
8. Ask it to add a space to spec/spec.md. Expected: a prompt. Reject it. git status shows no change.
9. Ask it to run .\gradlew.bat spotlessCheck. Expected: no prompt, real output, the same as yours.
10. Ask it to run git push --dry-run. Expected: a prompt. Deny it.
11. Ask it for a harmless web search, then to fetch one page. Expected: a prompt for each, showing the details. Deny both.
12. Ask it to read a file outside the repo (for example in your Documents folder). Expected: a prompt. Deny it.
13. Create a dummy keystore.properties in the repo (gitignored) and ask it to read it. Expected: blocked by the deny rule. Then delete the dummy file yourself.
14. With one uncommitted change in the working tree, ask it to start a step. Expected: it stops and lists the files.
15. Ask it to start a sub-agent task without your saying so. Expected: it does not start one.
16. A new conversation with the hand-off pasted. Expected: it states the stage, reads the repo files, and writes nothing.
17. /status shows the setting sources you expect. Note which shell tool it used for step 9 (Bash or PowerShell).
18. Section 3.1, line by line: each inventory row has a destination, and you confirm it.
19. CI green on the spike's pull request (your report). git status shows no keys and no .claude/settings.local.json tracked.
Not covered: other machines, other plans, and Claude Code versions after the run day.

## 9. Results (not run yet)
Every Claude Code, VS Code, Git, CI and settings result here will be your report of pasted output or of a check you did. I do not see your machine or GitHub. Where something was not run or not verified, it will say so.
9.1 Decisions D1 to D14: settled values and dates (to fill).
9.2 Install: versions and sign-in (to fill).
9.3 Checks 1 to 19 of section 8: result and your report for each (to fill).
9.4 Departures from section 4 (to fill).
9.5 Not run or not verified (to fill).

## 10. Status and notes
Note approved and decisions D1 to D14 settled 2026-10-28 (section 5). Not run.
