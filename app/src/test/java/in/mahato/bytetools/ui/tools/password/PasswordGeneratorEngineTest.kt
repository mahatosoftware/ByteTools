package `in`.mahato.bytetools.ui.tools.password

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PasswordGeneratorEngineTest {

    private class TrackingSecureRandom : SecureRandom() {
        var nextIntCalls = 0
            private set

        override fun nextInt(bound: Int): Int {
            nextIntCalls++
            return super.nextInt(bound)
        }
    }

    @Test
    fun defaultConfigGenerates16CharacterPassword() {
        val engine = PasswordGeneratorEngine()
        val result = engine.generate(PasswordConfig())

        assertTrue(result is PasswordGenerationResult.Success)
        val password = (result as PasswordGenerationResult.Success).password
        assertEquals(16, password.length)
    }

    @Test
    fun lengthBoundariesMin4AndMax64() {
        val engine = PasswordGeneratorEngine()

        val minResult = engine.generate(PasswordConfig(length = 4))
        assertTrue(minResult is PasswordGenerationResult.Success)
        assertEquals(4, (minResult as PasswordGenerationResult.Success).password.length)

        val maxResult = engine.generate(PasswordConfig(length = 64))
        assertTrue(maxResult is PasswordGenerationResult.Success)
        assertEquals(64, (maxResult as PasswordGenerationResult.Success).password.length)
    }

    @Test
    fun outOfRangeLengthReturnsError() {
        val engine = PasswordGeneratorEngine()

        assertTrue(engine.generate(PasswordConfig(length = 3)) is PasswordGenerationResult.Error)
        assertTrue(engine.generate(PasswordConfig(length = 65)) is PasswordGenerationResult.Error)
    }

    @Test
    fun allSelectedCharacterCategoriesAreRepresented() {
        val engine = PasswordGeneratorEngine()
        val config = PasswordConfig(
            length = 4,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSpecial = true,
            excludeSimilar = true
        )

        repeat(50) {
            val result = engine.generate(config) as PasswordGenerationResult.Success
            val password = result.password
            assertTrue("Missing uppercase in $password", password.any { it in 'A'..'Z' })
            assertTrue("Missing lowercase in $password", password.any { it in 'a'..'z' })
            assertTrue("Missing digit in $password", password.any { it in '0'..'9' })
            assertTrue(
                "Missing special in $password",
                password.any { it in PasswordGeneratorEngine.SPECIAL_CHARS }
            )
        }
    }

    @Test
    fun excludedSimilarCharactersNeverAppearWhenEnabled() {
        val engine = PasswordGeneratorEngine()
        val config = PasswordConfig(
            length = 64,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSpecial = true,
            excludeSimilar = true
        )

        repeat(50) {
            val result = engine.generate(config) as PasswordGenerationResult.Success
            val password = result.password
            for (similar in PasswordGeneratorEngine.SIMILAR_CHARS) {
                assertFalse(
                    "Excluded character '$similar' appeared in $password",
                    password.contains(similar)
                )
            }
        }
    }

    @Test
    fun emptyCharacterSelectionIsHandledCorrectly() {
        val engine = PasswordGeneratorEngine()
        val emptyConfig = PasswordConfig(
            length = 16,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = false,
            includeSpecial = false
        )

        val result = engine.generate(emptyConfig)
        assertTrue(result is PasswordGenerationResult.Error)
    }

    @Test
    fun secureRandomIsUsedForRandomGeneration() {
        val trackingRandom = TrackingSecureRandom()
        val engine = PasswordGeneratorEngine(secureRandom = trackingRandom)

        val result = engine.generate(PasswordConfig(length = 16))
        assertTrue(result is PasswordGenerationResult.Success)
        assertTrue(
            "Expected SecureRandom.nextInt(bound) to be invoked for generation and shuffling",
            trackingRandom.nextIntCalls >= 16 + 15
        )
    }

    @Test
    fun viewModelUpdatesLengthInstantlyAndBlocksDisablingLastCategory() {
        val viewModel = PasswordGeneratorViewModel()

        val initial = viewModel.uiState.value
        assertEquals(16, initial.config.length)
        assertEquals(16, initial.password.length)
        assertTrue(initial.config.excludeSimilar)

        viewModel.onLengthChange(32)
        assertEquals(32, viewModel.uiState.value.config.length)
        assertEquals(32, viewModel.uiState.value.password.length)

        // Disable 3 categories, leaving only lowercase enabled.
        viewModel.onUppercaseToggle(false)
        viewModel.onNumbersToggle(false)
        viewModel.onSpecialToggle(false)
        assertTrue(viewModel.uiState.value.config.includeLowercase)

        // Attempting to disable the last remaining category should be rejected.
        viewModel.onLowercaseToggle(false)
        assertTrue(viewModel.uiState.value.config.includeLowercase)
        assertNotNull(viewModel.uiState.value.validationMessage)
    }
}
