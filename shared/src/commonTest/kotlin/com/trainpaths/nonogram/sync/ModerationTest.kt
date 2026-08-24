package com.trainpaths.nonogram.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModerationTest {

    @Test
    fun denialAdvancesTheStreak() {
        assertEquals(1, nextDenialStreak(current = 0, approved = false))
        assertEquals(5, nextDenialStreak(current = 4, approved = false))
    }

    @Test
    fun approvalResetsTheStreak() {
        assertEquals(0, nextDenialStreak(current = 4, approved = true))
    }

    @Test
    fun banOnlyAppliesFromTheThresholdOnwards() {
        assertFalse(isPublishBanned(DENIAL_BAN_THRESHOLD - 1))
        assertTrue(isPublishBanned(DENIAL_BAN_THRESHOLD))
        assertTrue(isPublishBanned(DENIAL_BAN_THRESHOLD + 1))
    }

    @Test
    fun fiveDenialsInARowBan() {
        var streak = 0
        repeat(5) { streak = nextDenialStreak(streak, approved = false) }
        assertTrue(isPublishBanned(streak))
    }

    @Test
    fun anApprovalBeforeTheFifthDenialSavesTheUser() {
        var streak = 0
        repeat(4) { streak = nextDenialStreak(streak, approved = false) }
        streak = nextDenialStreak(streak, approved = true)
        streak = nextDenialStreak(streak, approved = false)
        assertFalse(isPublishBanned(streak))
    }
}
