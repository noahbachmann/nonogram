package com.trainpaths.nonogram.screens.viewModel

import com.trainpaths.nonogram.classes.PublishStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PublishActionTest {

    private fun action(
        publishStatus: PublishStatus = PublishStatus.NONE,
        isValid: Boolean? = true,
        isSignedIn: Boolean = true,
        isBanned: Boolean = false,
        isSaving: Boolean = false,
    ) = publishAction(publishStatus, isValid, isSignedIn, isBanned, isSaving)

    @Test
    fun validSignedInPuzzleCanBeRequested() {
        assertEquals(PublishAction.REQUEST, action())
    }

    @Test
    fun invalidPuzzleCannotBeRequested() {
        assertEquals(PublishAction.REQUEST_DISABLED, action(isValid = false))
    }

    @Test
    fun unvalidatedPuzzleCannotBeRequested() {
        assertEquals(PublishAction.REQUEST_DISABLED, action(isValid = null))
    }

    @Test
    fun guestCannotRequest() {
        assertEquals(PublishAction.REQUEST_DISABLED, action(isSignedIn = false))
    }

    @Test
    fun savingPuzzleCannotBeRequested() {
        assertEquals(PublishAction.REQUEST_DISABLED, action(isSaving = true))
    }

    @Test
    fun bannedUserSeesTheBanInsteadOfTheButton() {
        assertEquals(PublishAction.BANNED, action(isBanned = true))
    }

    @Test
    fun pendingRequestShowsSentEvenWhenBanned() {
        assertEquals(PublishAction.SENT, action(publishStatus = PublishStatus.PENDING, isBanned = true))
    }

    @Test
    fun approvedPuzzleOffersTheVisibilityToggle() {
        assertEquals(
            PublishAction.APPROVED_TOGGLE,
            action(publishStatus = PublishStatus.APPROVED, isValid = null),
        )
    }

    @Test
    fun unlistedPuzzleOffersTheSameToggle() {
        assertEquals(
            PublishAction.APPROVED_TOGGLE,
            action(publishStatus = PublishStatus.UNLISTED, isValid = null),
        )
    }

    @Test
    fun deniedPuzzleStaysDeniedUntilItIsEdited() {
        assertEquals(PublishAction.DENIED, action(publishStatus = PublishStatus.DENIED))
    }
}
