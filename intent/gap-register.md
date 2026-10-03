# gap-register.md (Plan stage, all nine gaps decided; recorded in intent.md v52 final)

Purpose: candidate requirements found by asking "what is missing for this to be a good app?". Nothing here is a requirement until you decide it. Each item gets a decision, then (if accepted) goes into a new intent.md version.
Source: intent/intent.md v51 (final) at the time of writing; now superseded by v52 (final). spec/spec.md draft v2.
Honest limit: these are my judgment from reading the files. Nothing was tested with real use, and nothing has been built.

Status values: OPEN, ACCEPTED, REJECTED, DEFERRED.

## Accepted direction (not yet written into intent.md)
- Status: ACCEPTED as a direction. Not yet in intent.md.
- You said the backup file is the only way data enters or leaves the app ("App initialization": delete all data, then restore from the file), and that your existing transactions would reach the app through a file you prepare in that format (reading 2). There is no separate CSV import, no CSV export, and no merge.
- This amends intent.md v51 (CSV import scope and decisions, and "replace or merge").
- Timing: intent.md is updated to v52 once, AFTER all gaps (G1 to G9) are discussed, so the accepted changes go in together.
- Update: all nine gaps are discussed. intent.md v52 is written and approved as FINAL (11 hunks approved, with the "Last backup" line on the More tab). ETF look-through (G5) is listed there as a planned later item, and the draft has no open questions.
- Still open inside G1: all-or-nothing loading, validation before deleting, the offline rule for restoring a hand-prepared file, and what the file contains.
- spec.md finding 4 (CSV `lot_id`) and finding 10 stay on hold until v52 is settled. spec.md is brought in line after v52.

## G1. Getting your data in (hand-prepared restore file)
- Status: ACCEPTED (all five decisions below made). To be written into intent.md v52. Decision: see below.
- Gap: you would prepare a whole-app file by hand, with no in-app preview, and errors would reject everything.
- Why it matters: this is the only way your existing data reaches the app, so a failed attempt blocks the first version.
- Ideas: a documented template or sample file; a validation report listing every error with its row and reason; validate the whole file before anything is deleted; rebuild derived data (lots, trade cash lines, holdings) from the records instead of reading it from the file.
- Touches: intent.md (CSV import scope and decisions, replace/merge), spec.md section 8, 9, K6, K7, slices 5 and 6, finding 4, C6 to C8.
- Decisions:
  1. DECIDED: the restore loads all-or-nothing. No partial load of valid rows.
  2. DECIDED: the app validates the whole file first and only then deletes. If validation fails, current data is untouched. The report lists every error with its row and reason, not just the first. If the app already holds data, it offers a backup export of it first.
  3. DECIDED: confirmation step. A summary shows what will be replaced (counts of portfolios, transactions and cash entries, now versus in the file). On an install that already holds data, a typed confirmation is also required. On a fresh install there is nothing to replace, so only the summary of the file is shown. Delete and load are one confirmed action, so the app is never left empty in between.
  4. DECIDED (option a, with sub-point option ii): the restore always requires internet. Every ticker still held at the end of the file (shares above zero) is verified before anything is deleted, and if one cannot be verified the restore is blocked. This replaces the earlier recommendation of an offline restore.
     - Sold-out tickers (zero shares left, for example a delisted stock recorded as a sell with zero proceeds) are not verified. They are accepted as they are and stay unverified, so a later buy of such a ticker is blocked until it is verified online (spec 4.3).
     - Open detail for Design: whether the check must use the currently selected price source (K4).
     - Known drawbacks accepted with this choice: dependence on a free third-party source (C1 unverified), rate limits on a large file, a slower restore, and the restore result depending on the selected price source.
  5. DECIDED: what the file contains.
     - In the file: portfolios, all transactions (buys, sells, splits, and reversals with their links), all cash entries, hidden holdings, manual sector/geography overrides, and ALL settings (auto-lock, price source, tab order).
     - Not in the file: lots, trade cash lines, holdings and prices. The app rebuilds them by replaying the records through the rules, so a hand-made file cannot contradict itself.
     - Lot references in the file are temporary labels that tie each sell to its buys. The app discards them and generates its own unique IDs after a successful restore. When the app writes a backup it generates these labels itself; they have no meaning outside the file.
     - The phone's PIN or biometric is set by Android and is not stored in the file.
     - Consequences accepted: the app's own backup and a hand-made file share one format. Prices are not in the file, so holdings show "no price" until the first successful price refresh after a restore. A restored lock setting could be weaker than the one on the new install.

## G2. Data-loss protection
- Status: ACCEPTED. Decision: last backup indicator plus an in-app reminder. To be written into intent.md v52.
- Gap: data lives only on the phone, backups are manual, and notifications are out of scope. A lost phone or uninstall loses everything since the last backup.
- Decision: 
  1. DECIDED: a "Last backup: date" line (or "never backed up") on the More tab.
  2. DECIDED: an in-app reminder to back up. In-app means it is shown only while the app is open. It is not an Android notification.
- Intent impact: intent.md v51 lists "Notifications, alerts and reminders" as out of scope. v52 must narrow that to system notifications and alerts, and add the in-app backup reminder to scope.
- DECIDED details of the reminder:
  3. Age: the reminder shows when the last backup is older than 7 days.
  4. Never backed up: it also shows when there is no backup at all and the app holds data.
  5. Dismissal: you can dismiss it until the next time the app opens, so it cannot be silenced permanently.
  6. Placement: a banner on the Overall tab.
  7. What counts as a backup: a successfully completed export. A cancelled or failed export does not update the date.
- Known limit: the app only knows when it last exported. It cannot know whether the file is still safe or has been copied somewhere safe.
- Touches: section 6 (Overall tab, More tab), section 9, intent.md scope.

## G3. Performance over time
- Status: DEFERRED (kept after the first version). Decision: keep performance over time deferred, as intent.md v51 already says. No change to intent.md scope.
- Condition: before the performance milestone, check whether a free source provides historical prices back to the first transaction, and how to reconcile them with as-traded lots, because splits are entered by hand (spec section 7, UNVERIFIED).
- Transactions are stored from day one, so nothing is lost by waiting.
- Gap: it is one of three outcomes in intent.md but deferred. Without it the app is a snapshot tracker.
- Risk: historical prices from a free source are unverified (section 7), and as-traded versus split-adjusted prices must be reconciled.
- Decision needed: keep it deferred, or pull it into the first version.
- Touches: intent.md outcomes, section 7, slice order.

## G4. Fixing a typo takes two steps
- Status: ACCEPTED. Decision: add a "Reverse and re-enter" shortcut. To be written into intent.md v52.
- Gap: a mistake needs a reversal and then a new entry.
- Idea: a "Reverse and re-enter" action that opens a new entry prefilled with the original values. The reversal rules and blocking rules stay the same.
- Touches: 4.6, entry flows (K5). Does not change "saved transactions are permanent".
- DECIDED: the shortcut starts from a transaction or cash entry, opens a new entry prefilled with the original values, and saves the reversal and the new entry together as ONE confirmed action. If either part is blocked, neither is saved, and the message says why.
- Consequences (details for Design):
  - The original and the reversal stay as separate permanent records, so the rule that saved transactions have no edit or delete does not change.
  - The pair is checked together against the cash and share rules (4.1). A block that comes from the original itself still applies, for example a buy whose shares were already sold cannot be reversed (4.6, Example D).
  - For a sell, the prefilled lot picks start from the original picks, and the new sell must still pass the lot rules.
  - No shortcut on an opening balance, on a reversal entry, or on a trade cash line (4.6). A buy or sell is the starting point for a trade.
- Limit: it saves typing, not rules. A buried mistake with later dependent transactions is still blocked until the later ones are reversed.

## G5. Sector/geography: override flow and ETFs
- Status: ACCEPTED. The override flow, the two lists and the source-value rules are accepted, and ETF look-through is deferred to a later version (option a). Written into intent.md v52 (final).
- Gap: criterion 12 mentions a hand override, but section 6 has no screen or flow for it. A global ETF counts as one sector or geography, with no look-through.
- DECIDED: the override is edited on Holding detail, with two fields, sector and geography. A manual value wins over the source's value until you clear it. Clearing it returns to the source's value, or "Unclassified" if there is none.
- DECIDED: the value is picked from a list, not typed freely. The list is maintained in the app settings.
- DECIDED: there are two separate lists, one for sectors and one for geographies.
- DECIDED: renaming a list value updates every holding that uses it. Deleting a list value is blocked while any holding uses it.
- DECIDED (option a): when the source supplies a sector or geography that is not on the list, the app adds it to the list automatically.
- Consequences (details for Design):
  - Settings gain a screen to maintain the sector list and the geography list.
  - The lists are part of the settings in the backup/restore file (G1, decision 5).
  - Auto-adding can fill a list with near-duplicates (for example "Tech" and "Technology"). You clean them up by renaming, and a rename updates the holdings.
  - DECIDED: a rename remembers the source's original text, so the same text keeps mapping to the renamed value on later refreshes. Without this, the source's original text would be added again as a new value.
  - Details for Design: where the remembered texts are kept and whether they are in the backup file (they should be, as part of the settings, so a restored install does not re-add them); what happens to a remembered text when its list value is deleted (a delete is only possible when no holding uses the value, so the remembered text would be removed with it).
- DECIDED (option a): no ETF look-through in the first version. Each holding has one sector and one geography, from the source or your override. A broad ETF can be given a value from your own lists (for example "Diversified" or "Global"). Look-through (manual weights, or from a data source, which is unverified) is listed under "Planned for later versions" in intent.md v52.
- For the later version: a holding would carry several weighted values that add up to 100%, which changes the allocation calculation and the backup file, so the backup file needs a format version (already in the deferred-to-Design list).
- Touches: section 6 (Holding detail, settings), C14, criterion 12, section 9 (backup contents), intent.md scope.

## G6. Privacy in the recent-apps preview and screenshots
- Status: ACCEPTED. To be written into intent.md v52. Decision: the app's content is hidden by default, with a temporary screenshot window.
- Gap: the spec does not say whether balances are hidden in Android's recent-apps preview or in screenshots.
- DECIDED:
  1. By default the app blocks screenshots and screen recording, and hides its content in the recent-apps preview.
  2. A setting turns on a screenshot window for a fixed 3 minutes. The recent-apps preview stays hidden during the window.
  3. Turning the setting on needs the biometric or the phone PIN (my reading: each time it is turned on).
  4. After 3 minutes the default protection is back in effect.
- Conditions (accepted):
  - Leaving the app ends the window early, so it ends after 3 minutes or when you leave the app, whichever comes first.
  - A restart ends it. The app always starts protected. The window is never saved and is not part of the backup/restore file.
  - It is not selective. During the 3 minutes anything can capture the screen, including screen-recording apps and screen sharing.
  - It works together with the auto-lock. The default auto-lock locks the app each time you leave it, so it relocks then.
- The 3 minutes is fixed. It is not adjustable in settings.
- UNVERIFIED (platform behavior from my knowledge, not tested in this session): Android's secure-window flag blocks screenshots, recording and the recent-apps preview together. Android 13 and newer have a separate switch for hiding only the preview. On older versions the app would turn the flag back on when it goes to the background. Using a biometric or the phone PIN in one prompt has restrictions before Android 11. The minimum Android version is decided in plan.md and settles these.
- Limit: this protects against casual exposure only. It does not protect against someone who has already unlocked your phone and your app.
- Touches: section 9 (lock), section 6 (settings), K8, intent.md constraints.

## G7. First launch and empty states
- Status: ACCEPTED. To be written into intent.md v52. Decision: a guided empty state.
- Gap: nothing says what you see with no portfolios and no cash entries, on a fresh install or after deleting everything.
- DECIDED:
  1. When the app holds no data, the Overall screen shows a short sequence of steps instead of zeros: (a) set the opening balance (a cash entry), since buys are blocked without cash; (b) create a portfolio; (c) enter a buy.
  2. The lock is set up first: the app asks you to confirm your biometric or phone PIN before showing anything.
  3. "App initialization from backup file" (the restore) appears ONLY in More, not on the empty state.
  4. Once the app holds data, the empty state is replaced by the normal screens.
- Consequence: a fresh install that you want to restore from a backup must go to More first, because the empty state does not offer it.
- Details for Design: exactly when the guide ends (suggestion: when there is any cash entry or portfolio); what Overall shows when there is cash but no portfolio yet; the wording of the steps.
- Touches: section 6 (Overall tab, navigation outline), section 9 (lock), intent.md views.

## G8. No manual price
- Status: REJECTED (as a feature). Decision: option (a), accept "no price". No change to intent.md or spec.md.
- Gap: if the source cannot price a ticker, the holding stays valued at cost and flagged "no price". Section 7 says manual entry is for transactions only.
- DECIDED: no manual price. A holding the source cannot price keeps the existing rule (4.8): valued at cost and flagged "no price" until the source can price it. Manual entry stays for transactions only.
- Reason: a ticker the source cannot price should be rare for US stocks and ETFs on NYSE and NASDAQ, and the first version is already large (C2).
- Revisit if it happens in real use. A manual price per holding would be a new entry in the register.
- Touches: nothing to change (4.8 and section 7 already say this).

## G9. Dividends and realized-gains filtering
- Status: PARTLY ACCEPTED. The realized gains date-range filter is accepted (to be written into intent.md v52). Per-holding dividends are DEFERRED to a future app version.
- Gap: dividends are cash-only, so holding gain/loss excludes them and understates returns for dividend payers. The realized gains view has no date-range or year filter.
- DECIDED: option (b) for dividends, but deferred to a future version. The idea is that a dividend entry can optionally name a ticker and portfolio, still goes to the shared cash account, and Holding detail then shows dividends received next to the gain/loss. In the first version dividends stay cash-only, as in intent.md v51.
- DECIDED: the realized gains view gets a date-range filter, like the cash account. The date input note and the "ET" suffix rules apply (spec section 6). Realized gains stay informational only. Tax reporting stays out of scope.
- Notes for later:
  - Per-holding dividend tracking stays out of scope in v52 for the first version and is listed as a planned future item.
  - Open for that future version: how a dividend with a ticker fits the rule that imported or direct cash rows carry no link to a portfolio (C6), and whether the backup/restore file needs a format version so a new optional field can be added without breaking old files.
- Touches: intent.md scope (planned future item), section 6 (view 5, Realized gains), criterion 11.
