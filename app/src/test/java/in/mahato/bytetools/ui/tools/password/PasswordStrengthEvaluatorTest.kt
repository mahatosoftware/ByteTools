package `in`.mahato.bytetools.ui.tools.password

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordStrengthEvaluatorTest {

    @Test
    fun emptyAndShortPasswordsAreVeryWeak() {
        assertEquals(StrengthLevel.VERY_WEAK, PasswordStrengthEvaluator.evaluate("").level)
        assertEquals(StrengthLevel.VERY_WEAK, PasswordStrengthEvaluator.evaluate("Ab1!").level)
    }

    @Test
    fun longSingleCategoryPasswordIsNotStrongBasedOnLengthAlone() {
        val longLowerOnly = "abcdefghijklmnopqrstuvwxzyabcdefghijklmnopqrstuvwxzy"
        val strength = PasswordStrengthEvaluator.evaluate(longLowerOnly)
        assertTrue(
            "Single-category password should not be classified as Strong or Very Strong, was ${strength.level}",
            strength.level <= StrengthLevel.WEAK
        )
    }

    @Test
    fun repeatedAndSequentialPatternsDowngradeStrength() {
        val repeated = PasswordStrengthEvaluator.evaluate("AaaaBbbbCccc1111")
        val sequential = PasswordStrengthEvaluator.evaluate("Abcd1234!@#\$Wxyz")

        assertTrue(repeated.hasPredictablePattern)
        assertTrue(sequential.hasPredictablePattern)
        assertTrue(repeated.level < StrengthLevel.VERY_STRONG)
    }

    @Test
    fun diverseHighEntropyPasswordIsVeryStrong() {
        val strength = PasswordStrengthEvaluator.evaluate("K9#mP2\$vQx7!nR4@L8wZ")
        assertEquals(StrengthLevel.VERY_STRONG, strength.level)
        assertTrue(strength.entropyBits >= 84)
        assertEquals(4, strength.diversityCount)
    }
}
