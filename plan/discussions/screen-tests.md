# plan/discussions/screen-tests.md

Status: discussed 2026-11-03; decided 2026-11-04 (section 8). Open item 47 stays open until spike F's results are in.
Source: the conversation after iteration 15's merge (pull request #117). "My reading" is an interpretation, not a test or a measurement.

## 1. Why this came up
- In 14c, Holding detail's Transactions row showed a buy's stored quantity instead of its exact shares ("Buy 1 share" for 0.33333333). 611 tests passed; no test draws a screen, and the phone check did not ask about that row's text (14c file, section 14, items 9 to 11).
- Open item 47 was opened from it: decide how screen content is tested before 16, where the screens gain many figures.

## 2. Your goal
Eliminate the manual phone checks done in every iteration, with no loss of reliability.

## 3. The options discussed
- A. Screen models, no new library: from 16 on, each new or changed screen gets its figures and texts from a plain Kotlin model the ViewModel builds (for example a Transactions row as ticker, shares and amount, already chosen and formatted); the screen only draws it. Ordinary unit tests check the models. Existing screens change only when touched anyway.
- B. JVM screen tests: Robolectric with Compose's test library draws a screen on the computer and in CI and checks its text and TalkBack labels. For layout, a screenshot library rendered at the largest font, in landscape and in dark mode. New test libraries, versions looked up on the day (a web search, asked first). Whether Robolectric works with this project's lock-screen activity and SQLCipher's native library is [UNVERIFIED]; a spike first.
- C. Tests on a phone or emulator (instrumented Compose tests): what B does, plus real windows, focus and the keyboard. CI would need an emulator, or the tests run by hand. The heaviest option.
- D. Keep the manual phone checks, with the CLAUDE.md pitfall from 14c as the guard.

Correction made during the discussion: test-only libraries are not certain to stay out of the APK. Compose's test setup usually needs ui-test-manifest as a debug dependency, and the debug build is what is on the phone (an empty test activity entry, no behaviour). Whether Robolectric tests can do without it is [UNVERIFIED].

## 4. What the phone checks have caught, and the best option for each
| Found on the phone | Kind | Could automation catch it? | Best option |
|---|---|---|---|
| 14c: a row showed the stored quantity | wrong figure on a screen | Yes | A: the row model's shares are unit tested (B would catch it too, at a higher cost) |
| 14b: the review showed the stand-in note "check" | wrong text on a screen | Yes | A: the review model carries the typed note |
| 05: the date refusal hidden behind "note required" | which messages a screen shows | Yes | A: the set of messages is part of the model |
| 12: picks rounding up and down | figures and rules | Yes | None needed: core unit tests already cover it |
| 10: the header does not give way at the largest font | layout at a large font or in landscape | Mostly | B, with screenshot tests at the largest font and in landscape |
| 08: the keyboard covers a layer | window and keyboard insets | Partly | C, on an emulator with a keyboard; the computer cannot show it |
| 07: a dialog and the cover screen replace each other in a loop | window focus, the platform | Partly | C, on an emulator; D if the emulator does not behave like the phone [UNVERIFIED] |
| 09: the redirect, and the token forwarded | network behaviour | Partly | A-level unit test with a fake server for the redirect and header rules; D for the live Finnhub |
| Phone-only: lock, Keystore, blocked screenshots, Recents, TalkBack speech, install over real data, Android 17 on the phone | the platform and your data | No | D, as a short smoke check, rechecked only when that code changes |

Several spec criteria say "checked by hand on the target phone, because the platform behaviour is unverified" (for example 20 and 51). Removing those hand checks would be a spec change (Design).

## 5. Does a higher level cover the lower ones?
In what each can observe, yes: D includes C, C includes B, B includes A.
- A checks the figures and texts the ViewModel prepares, not whether the screen draws them.
- B adds the drawing: text on screen, TalkBack labels, layout at a font size or orientation, on a simulated Android.
- C adds the real Android: windows, focus, the keyboard and insets.
- D adds what only a person and the phone have: biometrics, the Keystore, real TalkBack speech, your data, and "does this look right?"

In what each reliably catches in practice, no:

| | A | B | C | D |
|---|---|---|---|---|
| Checks the same thing every time | yes | yes | mostly (emulators can be flaky) | no: only what is listed, and what a person notices that day |
| Runs on every change (CI) | yes, seconds | yes, seconds to a minute | needs an emulator; minutes | no; you, each iteration |
| Says exactly what broke | yes | mostly | less (timing, device state) | depends on the description |
| Cost to write and keep up | low | medium | high | your time, every iteration |

The 14c example: D could have seen "Buy 1 share" in the row, but no check asked about it, so D missed it; a row-model test (A) would have failed on every build. Coverage in principle is not reliability.

Where the levels do not overlap, so one cannot stand in for another:
- A without B: a screen could draw the wrong field of a correct model (small if screens only draw prepared text).
- B without C: window, focus and keyboard behaviour is not real on the simulated Android (07, 08).
- C without D: biometrics, the Keystore, real TalkBack speech, your data, and Android 17 on the phone.

## 6. All four together
All four give the largest coverage, on one condition: each is used only for what it alone can see.
- Overlap adds cost, not reliability: if B re-checks every figure A tests, one change breaks two tests.
- Flaky tests lower reliability: if red builds become normal, real failures get ignored.
- D stays, but small: a five-minute smoke check is done carefully; a long checklist invites the kind of miss of 14c.

| Level | Checks only | Size |
|---|---|---|
| A | figures, texts, messages, rules, flows | most tests |
| B | that screens draw them; layout at the largest font, in landscape and dark; TalkBack labels | a few per screen |
| C | windows, focus, keyboard, layers over screens | a handful, only where those occur |
| D | lock, Keystore, blocked screenshots, TalkBack speech, install over real data | a short smoke check; the platform items only when that code changes |

My reading: built that way, reliability would be higher than today's phone checks, because most checks run on every build instead of once by eye. Not measured.

## 7. How it could be measured before deciding (proposed, not started)
A spike, like the earlier ones (plan/spikes/): set up B (Robolectric, Compose's test library, screenshot tests at the largest font, in landscape and in dark) and a database migration test; then bring back each past phone-found defect in section 4 on a test branch and see which the automated tests catch. The share caught measures "no loss of reliability"; the misses show which phone checks must stay. Cost: new test libraries (versions looked up on the day, web search asked first), possibly a debug-only manifest entry [UNVERIFIED], about a day's work, no change to the app's behaviour.

## 8. Where it stands
- 2026-11-03: option A was taken as change request CR2 and built as iteration 15a (intent/intent-cr-02-screen-models.md; merged in pull request #120). Existing screens without models are open item 48.
- 2026-11-04, your decision F (Plan) for the rest: a spike for level C only (plan/spikes/screen-tests.md, to be written and approved before anything runs), then change request CR3 from its results. Level B is added only if level C cannot run on every change. The spike replays 10 (the header measured, not compared as a picture), 07, 08 and TalkBack labels on an emulator, and measures what each catches, the run time and flakiness.
- Running level C on every change: first try a self-hosted GitHub Actions runner on your PC; fall back to an emulator on GitHub's runner. Conditions for the self-hosted runner:
  - never triggered by pull_request (fork pull requests could run code on your PC); only by pushes to your branches or by hand;
  - run under a separate Windows user that cannot read the keystore, signing files or ~/.claude;
  - the emulator only, never the phone: the build tools uninstall the app after instrumented tests by default [UNVERIFIED], which would delete the app's data;
  - the repo's visibility and fork settings checked first (not yet checked).
- Options not taken: a spike for level B only; levels B and C together (two test setups, the most upkeep); level B for every screen without a spike; postponing past 16; closing the rest of item 47.
- The spike changes no app behaviour, no spec and not the release APK. Dropping hand checks that criteria require (for example 20 and 51) is a spec change, made in CR3 after the results.
