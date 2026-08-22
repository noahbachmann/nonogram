# Publish moderation (admin review)

A puzzle used to become public the moment its author flipped a switch — the only gates were
`Nonogram.isValid` and being signed in. Now the author *requests* publication and an admin accepts
or denies it. This doc exists so the transition table, which is spread across a ViewModel, two sync
implementations and `firestore.rules`, can be read in one place.

## Moderation state lives on the puzzle

`Nonogram` carries a `publishState` — `NONE`, `PENDING`, `APPROVED`, `UNLISTED`, `DENIED` —
persisted as the ordinal in the existing `status` column of `NonogramData` and as the state's
*name* in a `publishStatus` field on the `nonograms/{id}` Firestore doc. **It is the only visibility
field there is.** An earlier cut of this feature also kept a stored `isPublic` /
`status` flag that `publishState` had to be held consistent with; that flag is gone, and `isPublic`
survives only as `val isPublic get() = publishState == APPROVED`. `UNLISTED` is what makes that
possible: it means *approved, but the author has taken it down*, so the author's visibility switch
moves between `APPROVED` and `UNLISTED` and never costs a new review.

**There is no separate requests collection.** One would have had to
duplicate the solution and carry its own rules; keeping the state on the puzzle means the admin
queue is a plain query — `nonograms where publishStatus == 'PENDING' order by updatedAt` — and the
author's verdict flows home through the *existing* owned-nonogram pull with no new sync stream.
Filing a request is itself a write, so `updatedAt` doubles as the request time and the queue needs
no extra field.

`status` already existed as the 0/1 public flag, so widening it to an ordinal needs **no migration
at all** — the column is already an `INTEGER`, and `1` is the only value ever written to it. It now
means `PENDING`, which is wrong for rows written by an older build, but nothing has shipped and a
local install can simply be cleared.

Firestore is different, because those documents *do* exist. Reusing the remote `status` field would
silently reinterpret every live public puzzle as `PENDING`, so the remote state moved to a new
`publishStatus` field instead, and both parsers fall back to reading the old numeric `status` when a
doc has no `publishStatus`. That fallback is the only place remote `status` is still read; nothing
writes it. Local and remote therefore use different field names on purpose: the local one had no
data worth protecting, the remote one did.

## One rule covers denial and un-publishing

The two behaviours asked for — *a denied puzzle can be requested again after it is edited*, and
*an approved puzzle can be flipped public and private freely as long as nothing changed* — are the
same rule seen from two sides: **any content change resets `publishState` to `NONE` and forces the
puzzle private.** `GenViewModel.onSave` reads `isDirty` for this, which is trustworthy because
`resizeNonogram` and `updateName` both early-return on a no-op, so saving the config screen without
touching anything does not count as an edit.

Because that reset is silent and destructive, every path that saves an edited *public* puzzle goes
through `PublicEditConfirmDialog` first — the board's Save button, the leave-confirm dialog's Save,
the config screen's Save, and the wrench, whose `onConfig` saves before it navigates. `GenScreen`
gates them with a local `requestSave`; `GenConfScreen` runs the check **before** applying
`updateName`/`resizeNonogram`, so cancelling leaves the in-memory board untouched. The predicate,
`needsPublicEditConfirmation`, is pure and tested.

`visibilityAfterToggle` then applies the author's switch — `APPROVED` ↔ `UNLISTED`, ignored in every
other state, because publication is the admin's call — and
`publishAction` maps the state plus validity, sign-in, ban and saving flags onto the six things the
generator's publish control can be: request, request-disabled, sent, approved-toggle, denied,
banned. Both are pure functions next to the existing `canSaveNonogram`, and both are unit-tested.

## The rules are the gate, not the client

The client checks are cosmetic on their own — an author has always been able to write their own
`nonograms` doc directly. The transitions above were chosen so `firestore.rules` can enforce all of
them with field comparisons against `resource.data`, with **no Cloud Function anywhere in the
design**. An author may only create a doc at `publishStatus = 'NONE'`; on update, a changed `solution`,
`name` or `difficulty` must come with `publishStatus = 'NONE'`, while unchanged content permits
`NONE → PENDING` (when not banned) and free movement between `APPROVED` and `UNLISTED`. Admins are
unrestricted. Collapsing the two fields into one shortened these rules considerably — there is now
a single field to reason about rather than an invariant between two.

This is why `pushNonogram` is a **merge** write that omits `publishStatus` entirely: a full `set`
would clobber a pending or approved state on every ordinary save, and sending the client's own idea
of the state would let a stale client reset a request it doesn't know about — which the rules would
then reject, breaking an innocent save. The one exception is its `resetModeration` flag, set when
the content changed, which writes `publishStatus = 'NONE'` explicitly.

The ban lives in `users/{uid}` as `denialStreak` and `publishBanned`, admin-write and self-read.
The rules consult it with a `get()` on the `NONE → PENDING` transition, so a banned user's request
is refused at the server, not merely greyed out. `nextDenialStreak` resets the streak on any
approval and `isPublishBanned` trips at `DENIAL_BAN_THRESHOLD` (5); lifting a ban is a manual edit
of the user doc.

Admin identity is an `admins/{uid}` doc — chosen over a hardcoded UID constant so a second admin
needs no app release, and over a custom auth claim so no Admin SDK script is needed. The client
reads its own row to decide whether Settings shows the *Admin panel* button, and caches the answer
in `multiplatform-settings` through `AuthRepository` so the button survives a cold start offline.

## Sync surface

`SyncService` gained five methods — `requestPublish`, `fetchModerationGate`, `isAdmin`,
`pullPendingReviews`, `decideReview` — implemented twice, on gitlive in `androidMain` and on the
handwritten externals in `webMain`. The web side needed three new external shapes, all following
the rules in `docs/web-architecture.md`: `@JsName`-aliased overloads for the variadic `query` and
`setDoc`, `orderBy`/`limit` constraints, and per-shape `getDoc` aliases, because Kotlin externals
cannot express variadics or generic document data.

`decideReview` writes the verdict onto the puzzle and then the streak onto the author's user doc.
`AdminViewModel` deliberately does **not** touch the local database afterwards: the verdict bumps
`updatedAt`, so the ordinary public and owned pulls bring it home on both the admin's device and
the author's.

## Setup outside the repo

`firestore.rules` and `firestore.indexes.json` are checked in and deployable with
`firebase deploy --only firestore`, or pasteable into the console. Two things are not automated:
creating `admins/{yourUid}` with any payload, and a one-off backfill of `publishStatus = 'APPROVED'`
onto existing `nonograms` docs with `status = 1`.

**The backfill is mandatory and must run before this ships.** Both the public-pull query and the
read rule now key on `publishStatus == 'APPROVED'`, so any doc still carrying only the old `status`
field becomes invisible to everyone but its author. Two composite indexes cover all three queries:
`(publishStatus, updatedAt)` serves both the public pull and the pending-review queue, and
`(authorUid, updatedAt)` serves the owned pull.

## Limitations

- The rules make publishing without approval impossible, and make it impossible to swap the content
  under an approved puzzle. They cannot stop a hand-crafted client from *spamming* requests; the
  five-denial ban is the only backstop, and a banned user can still make a new account.
- A decision is two non-atomic writes. A failure between them lands the verdict but leaves the
  streak un-incremented. Acceptable with a single admin; a transaction would need a function.
- There is no email and no push. Requests are seen only when the admin panel is opened. A
  Firestore-triggered Cloud Function on `publishStatus == 'PENDING'` could add that later without any
  change to the app.
