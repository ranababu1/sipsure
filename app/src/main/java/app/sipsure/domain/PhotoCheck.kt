package app.sipsure.domain

/** Outcomes shown to the user (plan.md 5.2). None of them claims proof of drinking. */
enum class CheckInOutcome { ACCEPTED, RETRY_NEEDED, REVIEW_REQUIRED }

/**
 * Cheap features computed on-device from a downscaled photo. The raw image is deleted after
 * analysis; only the [hash] is kept, to spot an identical photo being resubmitted.
 */
data class PhotoFeatures(
    val widthPx: Int,
    val heightPx: Int,
    /** 0..255 mean luminance. */
    val meanLuma: Double,
    /** Standard deviation of luminance; near zero means a blank or covered lens. */
    val lumaStdDev: Double,
    /** 64-bit average hash of an 8x8 grayscale thumbnail. */
    val hash: Long,
    /** Capture time from the file, if available. */
    val capturedAtMs: Long?,
)

data class CheckInVerdict(val outcome: CheckInOutcome, val message: String)

/**
 * Conservative plausibility and replay checks. It cannot tell whether water was drunk, so a photo
 * that passes is recorded as "accepted" only in the sense that it looks like a fresh, real photo.
 */
object PhotoCheck {
    const val MIN_SIDE_PX = 200
    const val MIN_LUMA = 25.0
    const val MAX_LUMA = 240.0
    const val MIN_STD_DEV = 8.0
    const val MAX_AGE_MS = 5 * 60_000L
    const val NEAR_DUPLICATE_BITS = 5

    fun evaluate(photo: PhotoFeatures, previousHashes: List<Long>, nowMs: Long): CheckInVerdict {
        if (photo.widthPx < MIN_SIDE_PX || photo.heightPx < MIN_SIDE_PX) {
            return retry("The photo is too small to check. Please take a new one.")
        }
        if (photo.meanLuma < MIN_LUMA || photo.meanLuma > MAX_LUMA || photo.lumaStdDev < MIN_STD_DEV) {
            return retry("The photo looks too dark, too bright or blank. Please retake it in better light.")
        }
        val capturedAt = photo.capturedAtMs
        if (capturedAt != null && nowMs - capturedAt > MAX_AGE_MS) {
            return retry("This photo wasn't just taken. Please capture a fresh one.")
        }
        if (previousHashes.any { java.lang.Long.bitCount(it xor photo.hash) <= NEAR_DUPLICATE_BITS }) {
            return CheckInVerdict(
                CheckInOutcome.REVIEW_REQUIRED,
                "This looks very similar to an earlier check-in. You can confirm it yourself or retake the photo.",
            )
        }
        return CheckInVerdict(
            CheckInOutcome.ACCEPTED,
            "Check-in recorded. SipSure only checks that the photo looks fresh; it can't confirm how much you drank.",
        )
    }

    private fun retry(message: String) = CheckInVerdict(CheckInOutcome.RETRY_NEEDED, message)
}
