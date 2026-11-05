# intent-cr-03-screen-tests.md (CR3, final)

Change request CR3, 2026-11-05: screen tests on an emulator (open item 47, second part).
Base: intent.md v52 and CR2. Discussion: plan/discussions/screen-tests.md. Measured in: plan/spikes/screen-tests.md, section 11.

## Problem
- After CR2, figures, texts and messages are unit tested. Layout, windows, the keyboard and TalkBack labels are still checked only by hand on the phone. Three defects of that kind were found there:
  - 10: the header didn't give way at the largest font and in landscape.
  - 08: the keyboard covered a layer.
  - 07: a layer and the cover screen replaced each other in a loop and blocked all input.
  The TalkBack labels are 14c finding 2.
- Spike F replayed these four kinds of defect on an emulator.
  - Defects caught: 10, 08, 07b and the reversal card's label. 07a was caught locally only.
  - On GitHub's runner the tests passed 10 of 10, taking 4m 16s to 5m 45s per run.

## Outcome
- I no longer check by hand, on a covered screen, any of these: the layout at the largest font and in landscape, layers staying above the keyboard, layers and menus without the cover loop, and TalkBack labels. Tests check them on GitHub.
- Covered: the four spike checks (10, 07, 08, TalkBack labels), kept as tests of the app; and from the next iteration on, every new or changed screen with a layer, a menu, a fixed part or a text field.
- When: on every pull request into main, next to the existing checks, and again on main after the merge. A red run means "don't merge" until it's fixed.
- Which phone checks end: for covered screens, the hand checks of the layout at the largest font and in landscape, of layers, menus and the keyboard, and of TalkBack labels. They end only after one iteration in which the tests and the phone check both run and the tests catch everything the phone check finds.
- My goal stays: no manual phone checks, with no loss of reliability. These stay phone checks after CR3: TalkBack speech, the themes (criterion 40), the network (42), installing over my data (35), the lock and Keystore, blocked screenshots and Recents, and Android 17 on the phone.

## Users
- Me.

## Constraints
- No change to what the app does or shows. The release APK stays content-identical.
- No debug-only switch that skips the lock or changes a screen for tests.
- The tests use an emulator only, never the phone. The test PIN is a throwaway one and never the phone's.
- The tests run on GitHub's hosted runner. There's no self-hosted runner, because the repo is public.
- Local runs on my PC aren't required, since they aren't steady here (spike 11.8).

## Open questions
None remaining in Plan.

## Deferred to Design
- Which spec criteria change (for example 39's "on the test phone", and 20 and 51's "checked by hand on the target phone"), and how.

## Deferred to Build
- What is kept from the run branch, the workflow's shape (ci.yml or its own file), where the test PIN lives, the test-count check, and the iteration in which the tests and the phone check both run.
