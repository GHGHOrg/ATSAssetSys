# intent-cr-02-screen-models.md (CR2, final)

Change request CR2, 2026-11-03: screen models (open item 47, first part).
Base: intent.md v52. Discussion: plan/discussions/screen-tests.md.

## Problem
- What a screen shows (figures, texts, messages) is checked only by hand on the phone; no test draws a screen.
- In 14c, Holding detail's Transactions row showed a buy's stored quantity ("Buy 1 share" for 0.33333333) while 611 tests passed, and the phone check did not ask about that row (14c Deploy finding 1). The same kind of defect was found on the phone in 14b (the review showed the stand-in note "check") and 05 (a date refusal hidden behind "note required").

## Outcome
- I no longer have to read figures, texts and messages on a covered screen by hand to trust them: tests check them on every build.
- Covered: new and changed screens from iteration 16 on, plus three screens where defects were found: Holding detail's Transactions rows, the Reverse and re-enter combined review, and the cash entry review's messages.
- My goal stays: no manual phone checks, with no loss of reliability. This change is the first part. Layout, windows and the keyboard, and the platform (lock, screenshots, TalkBack speech, installing over my data) stay phone checks for now (open item 47).

## Users
- Me.

## Constraints
- No new library.
- No change to what the app does or shows.
- Exact arithmetic and formatting as now (spec 4.10, section 6).

## Open questions
None remaining in Plan.

## Deferred to Design
- Confirm that spec.md needs no change (it describes what the app does, not how it is tested).

## Deferred to Build
- The shape of a screen model, and the iteration that builds the three screens.
