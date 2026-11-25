package `in`.mahato.bytetools.ui.tools.password

import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Five-tier password strength classification.
 */
enum class StrengthLevel(val label: String, val fraction: Float) {
    VERY_WEAK("Very Weak", 0.2f),
    WEAK("Weak", 0.4f),
    MODERATE("Moderate", 0.6f),
    STRONG("Strong", 0.8f),
    VERY_STRONG("Very Strong", 1.0f)
}

/**
 * Result of evaluating a password's strength and estimated entropy.
 */
data class PasswordStrength(
    val level: StrengthLevel,
    val entropyBits: Int,
    val poolSize: Int,
    val diversityCount: Int,
    val hasPredictablePattern: Boolean
)

/**
 * Evaluates password strength using length, character pool size, character diversity,
 * estimated entropy in bits, and detection of common or predictable patterns.
 */
object PasswordStrengthEvaluator {

    private val COMMON_PATTERNS = listOf(
        "password", "qwerty", "asdfgh", "zxcvbn",
        "1234", "4321", "admin", "letmein", "welcome", "abc123"
    )

    fun evaluate(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(
                level = StrengthLevel.VERY_WEAK,
                entropyBits = 0,
                poolSize = 0,
                diversityCount = 0,
                hasPredictablePattern = false
            )
        }

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSpecial = false

        for (ch in password) {
            when {
                ch in 'A'..'Z' -> hasUpper = true
                ch in 'a'..'z' -> hasLower = true
                ch in '0'..'9' -> hasDigit = true
                else -> hasSpecial = true
            }
        }

        var poolSize = 0
        var diversityCount = 0
        if (hasUpper) {
            poolSize += PasswordGeneratorEngine.UPPERCASE_CHARS.length
            diversityCount++
        }
        if (hasLower) {
            poolSize += PasswordGeneratorEngine.LOWERCASE_CHARS.length
            diversityCount++
        }
        if (hasDigit) {
            poolSize += PasswordGeneratorEngine.NUMBER_CHARS.length
            diversityCount++
        }
        if (hasSpecial) {
            poolSize += PasswordGeneratorEngine.SPECIAL_CHARS.length
            diversityCount++
        }

        if (poolSize <= 1) {
            return PasswordStrength(
                level = StrengthLevel.VERY_WEAK,
                entropyBits = 0,
                poolSize = poolSize,
                diversityCount = diversityCount,
                hasPredictablePattern = true
            )
        }

        val rawEntropy = password.length * log2(poolSize.toDouble())
        val uniqueRatio = password.toSet().size.toDouble() / password.length.toDouble()

        val hasRepeatedRun = hasRepeatedCharRun(password, minRun = 3)
        val hasSequentialRun = hasSequentialCharRun(password, minRun = 3)
        val hasCommonSubstring = hasCommonPattern(password)
        val hasLowUniqueness = password.length >= 6 && uniqueRatio < 0.45

        val hasPredictablePattern =
            hasRepeatedRun || hasSequentialRun || hasCommonSubstring || hasLowUniqueness

        var effectiveEntropy = rawEntropy
        if (hasLowUniqueness) {
            effectiveEntropy *= max(0.35, uniqueRatio)
        }
        if (hasRepeatedRun) {
            effectiveEntropy *= 0.65
        }
        if (hasSequentialRun) {
            effectiveEntropy *= 0.70
        }
        if (hasCommonSubstring) {
            effectiveEntropy *= 0.50
        }

        val entropyBits = effectiveEntropy.roundToInt().coerceAtLeast(0)

        var level = when {
            entropyBits < 28 -> StrengthLevel.VERY_WEAK
            entropyBits < 45 -> StrengthLevel.WEAK
            entropyBits < 64 -> StrengthLevel.MODERATE
            entropyBits < 84 -> StrengthLevel.STRONG
            else -> StrengthLevel.VERY_STRONG
        }

        // Do not classify a password as strong based on length alone.
        if (password.length < 6) {
            level = minOf(level, StrengthLevel.VERY_WEAK)
        } else if (password.length < 8) {
            level = minOf(level, StrengthLevel.WEAK)
        }

        if (diversityCount <= 1) {
            level = minOf(level, StrengthLevel.WEAK)
        } else if (diversityCount == 2 && entropyBits < 72) {
            level = minOf(level, StrengthLevel.MODERATE)
        }

        if (hasPredictablePattern && level.ordinal > StrengthLevel.VERY_WEAK.ordinal) {
            level = StrengthLevel.entries[level.ordinal - 1]
        }

        return PasswordStrength(
            level = level,
            entropyBits = entropyBits,
            poolSize = poolSize,
            diversityCount = diversityCount,
            hasPredictablePattern = hasPredictablePattern
        )
    }

    private fun hasRepeatedCharRun(password: String, minRun: Int): Boolean {
        if (password.length < minRun) return false
        var run = 1
        for (i in 1 until password.length) {
            if (password[i] == password[i - 1]) {
                run++
                if (run >= minRun) return true
            } else {
                run = 1
            }
        }
        return false
    }

    private fun hasSequentialCharRun(password: String, minRun: Int): Boolean {
        if (password.length < minRun) return false
        var ascRun = 1
        var descRun = 1
        for (i in 1 until password.length) {
            val prev = password[i - 1]
            val curr = password[i]
            val bothAlnum = prev.isLetterOrDigit() && curr.isLetterOrDigit()
            if (bothAlnum && curr.code - prev.code == 1) {
                ascRun++
                descRun = 1
                if (ascRun >= minRun) return true
            } else if (bothAlnum && prev.code - curr.code == 1) {
                descRun++
                ascRun = 1
                if (descRun >= minRun) return true
            } else {
                ascRun = 1
                descRun = 1
            }
        }
        return false
    }

    private fun hasCommonPattern(password: String): Boolean {
        val lower = password.lowercase()
        return COMMON_PATTERNS.any { lower.contains(it) }
    }
}
