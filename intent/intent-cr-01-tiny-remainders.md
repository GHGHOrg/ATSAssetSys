# intent-cr-01-tiny-remainders.md (CR1, final)

Change request CR1, 2026-11-03: leftovers too small to type (open items 44 and 46).
Base: intent.md v52. Settled before iteration 15 (hide holdings).

## Problem
- After a reverse split, a lot can keep a remainder below 0.000000005 share. It shows as 0, cannot be picked, and its holding never reaches exactly 0, so it cannot be hidden (item 44).
- Reverse and re-enter of a buy after a split such as 1 for 3 fills in the quantity rounded to 8 places, so a pair saved unchanged leaves the new lot short of the original by less than 0.00000001 share (item 46).

## Outcome
- A leftover smaller than the smallest quantity I can type (0.00000001), for example after a reverse split, never keeps a holding from being sold out completely and hidden.
- When I reverse and re-enter a buy and leave its quantity as filled in, the new entry has exactly the original's shares, even when a split made that quantity longer than 8 decimal places.

## Users
- Me.

## Constraints
- Exact arithmetic (spec 4.10).
- Quantities are typed to 8 places.
- Records already saved on the phone keep working.

## Open questions
None remaining in Plan.

## Deferred to Design
- How the leftover is removed (show it as the smallest typed quantity, count "0 at 8 places" as sold out, or another way) and how the re-entered buy keeps the exact quantity.
