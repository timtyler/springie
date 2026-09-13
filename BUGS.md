# Known bugs

Things we know are wrong and haven't fixed. Most issues found during
development get fixed the same day; this file is for the ones that
didn't.

## Open

From `source/ToDo.txt` (still present there too):

- Modern renderer in fast single-buffer mode sometimes does an incomplete
  redraw.
- The original renderer runs faster while a drag box is showing (nobody
  knows why).
- The original renderer's distance colour-fade has a cosmetic flaw.
- Grabbing a link doesn't immobilise both ends the way grabbing a node
  does.

## Deliberately left alone

- The tifsoft SAX driver doesn't handle QName/URI attributes. Pre-existing,
  no functional impact on the app. Decision: leave the driver as is.
- The old deprecated message queue
  (`com.springie.messages.{Message,MessageObj,MessageManager}`) is still
  live. Migrating every handler to the new message classes is a real
  refactor, not a bug fix.
- Muscle period 2 is an identically zero drive
  (`sin(pi * t) = 0` at every integer tick), so 3 is the effective slider
  minimum. Cosmetic quirk, not worth hiding.

## Fixed

(Kept brief — full history is in git.)

- 2026-09-13: muscle amplitude scrollbar arrows were swallowed by
  fixed-point truncation on the display readback (right arrow did nothing,
  left arrow skipped by twos). Fixed by rounding to nearest.
