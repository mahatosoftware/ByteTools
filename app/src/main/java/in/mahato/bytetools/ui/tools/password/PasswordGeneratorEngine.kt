package `in`.mahato.bytetools.ui.tools.password

import java.security.SecureRandom
import kotlin.math.min

/**
 * Configuration options for offline cryptographic password generation.
 */
data class PasswordConfig(
    val length: Int = PasswordGeneratorEngine.DEFAULT_LENGTH,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSpecial: Boolean = true,
    val excludeSimilar: Boolean = true
)

/**
 * Result of a password generation attempt.
 */
sealed class PasswordGenerationResult {
    data class Success(val password: String) : PasswordGenerationResult()
    data class Error(val message: String) : PasswordGenerationResult()
}

/**
 * Offline, cryptographically secure password generator backed by [SecureRandom].
 */
class PasswordGeneratorEngine(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 64
        const val DEFAULT_LENGTH = 16

        val SIMILAR_CHARS: Set<Char> = setOf('O', '0', 'I', 'l', '1')

        const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
        const val NUMBER_CHARS = "0123456789"
        const val SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    }

    /**
     * Generates a random password matching [config], ensuring every selected
     * character category is represented and shuffling securely with [SecureRandom].
     */
    fun generate(config: PasswordConfig): PasswordGenerationResult {
        if (config.length !in MIN_LENGTH..MAX_LENGTH) {
            return PasswordGenerationResult.Error(
                "Password length must be between $MIN_LENGTH and $MAX_LENGTH characters."
            )
        }

        val activePools = buildActivePools(config)
        if (activePools.isEmpty()) {
            return PasswordGenerationResult.Error(
                "Select at least one character type to generate a password."
            )
        }

        val combinedPool = activePools.joinToString(separator = "")
        val passwordChars = CharArray(config.length)

        return try {
            // Guarantee at least one character from every selected category.
            val guaranteedCount = min(activePools.size, config.length)
            for (i in 0 until guaranteedCount) {
                val pool = activePools[i]
                passwordChars[i] = pool[unbiasedNextInt(pool.length)]
            }

            // Fill remaining slots from the combined pool.
            for (i in guaranteedCount until config.length) {
                passwordChars[i] = combinedPool[unbiasedNextInt(combinedPool.length)]
            }

            // Secure Fisher-Yates shuffle so guaranteed characters are uniformly distributed.
            for (i in passwordChars.lastIndex downTo 1) {
                val j = unbiasedNextInt(i + 1)
                val temp = passwordChars[i]
                passwordChars[i] = passwordChars[j]
                passwordChars[j] = temp
            }

            PasswordGenerationResult.Success(String(passwordChars))
        } finally {
            // Clear temporary character buffer from memory.
            passwordChars.fill('\u0000')
        }
    }

    /**
     * Returns the list of non-empty character pools for the given [config].
     */
    fun buildActivePools(config: PasswordConfig): List<String> {
        val pools = mutableListOf<String>()
        if (config.includeUppercase) {
            pools.add(filterPool(UPPERCASE_CHARS, config.excludeSimilar))
        }
        if (config.includeLowercase) {
            pools.add(filterPool(LOWERCASE_CHARS, config.excludeSimilar))
        }
        if (config.includeNumbers) {
            pools.add(filterPool(NUMBER_CHARS, config.excludeSimilar))
        }
        if (config.includeSpecial) {
            pools.add(filterPool(SPECIAL_CHARS, config.excludeSimilar))
        }
        return pools.filter { it.isNotEmpty() }
    }

    private fun filterPool(pool: String, excludeSimilar: Boolean): String {
        if (!excludeSimilar) return pool
        return buildString(pool.length) {
            for (ch in pool) {
                if (ch !in SIMILAR_CHARS) {
                    append(ch)
                }
            }
        }
    }

    /**
     * Unbiased integer in [0, bound) using [SecureRandom.nextInt].
     */
    private fun unbiasedNextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive" }
        return secureRandom.nextInt(bound)
    }
}
