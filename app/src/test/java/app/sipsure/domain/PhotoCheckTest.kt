package app.sipsure.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PhotoCheckTest {
    private val now = 1_000_000_000L
    private fun photo(
        w: Int = 640, h: Int = 480, luma: Double = 120.0, sd: Double = 40.0,
        hash: Long = 0x0F0F0F0F0F0F0F0FL, at: Long? = now - 10_000,
    ) = PhotoFeatures(w, h, luma, sd, hash, at)

    private fun outcome(p: PhotoFeatures, prev: List<Long> = emptyList()) = PhotoCheck.evaluate(p, prev, now).outcome

    @Test
    fun freshVariedPhotoIsAccepted() = assertEquals(CheckInOutcome.ACCEPTED, outcome(photo()))

    @Test
    fun tinyDarkBrightOrBlankPhotosNeedRetry() {
        assertEquals(CheckInOutcome.RETRY_NEEDED, outcome(photo(w = 100)))
        assertEquals(CheckInOutcome.RETRY_NEEDED, outcome(photo(luma = 5.0)))
        assertEquals(CheckInOutcome.RETRY_NEEDED, outcome(photo(luma = 250.0)))
        assertEquals(CheckInOutcome.RETRY_NEEDED, outcome(photo(sd = 1.0)))
    }

    @Test
    fun oldPhotoNeedsRetryButMissingTimestampIsTolerated() {
        assertEquals(CheckInOutcome.RETRY_NEEDED, outcome(photo(at = now - 10 * 60_000)))
        assertEquals(CheckInOutcome.ACCEPTED, outcome(photo(at = null)))
    }

    @Test
    fun identicalOrNearIdenticalHashNeedsReview() {
        val h = 0x0F0F0F0F0F0F0F0FL
        assertEquals(CheckInOutcome.REVIEW_REQUIRED, outcome(photo(hash = h), listOf(h)))
        assertEquals(CheckInOutcome.REVIEW_REQUIRED, outcome(photo(hash = h xor 0b111), listOf(h)))
        assertEquals(CheckInOutcome.ACCEPTED, outcome(photo(hash = h), listOf(h.inv())))
    }

    @Test
    fun acceptedMessageDoesNotClaimProofOfDrinking() {
        val msg = PhotoCheck.evaluate(photo(), emptyList(), now).message
        assertFalse(msg.contains("proof", ignoreCase = true) && !msg.contains("can't", ignoreCase = true))
    }
}
