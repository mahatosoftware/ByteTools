package `in`.mahato.bytetools.ui.tools.password

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UI state for the offline Password Generator screen.
 * Passwords are held only in volatile memory for active display and never persisted.
 */
data class PasswordGeneratorUiState(
    val config: PasswordConfig = PasswordConfig(),
    val password: String = "",
    val strength: PasswordStrength = PasswordStrengthEvaluator.evaluate(""),
    val validationMessage: String? = null
)

@HiltViewModel
class PasswordGeneratorViewModel @Inject constructor() : ViewModel() {

    private val engine = PasswordGeneratorEngine()

    private val _uiState = MutableStateFlow(PasswordGeneratorUiState())
    val uiState: StateFlow<PasswordGeneratorUiState> = _uiState.asStateFlow()

    init {
        regenerate()
    }

    fun onLengthChange(length: Int) {
        val clamped = length.coerceIn(
            PasswordGeneratorEngine.MIN_LENGTH,
            PasswordGeneratorEngine.MAX_LENGTH
        )
        if (clamped == _uiState.value.config.length) return
        updateConfig(_uiState.value.config.copy(length = clamped))
    }

    fun onUppercaseToggle(enabled: Boolean) {
        val current = _uiState.value.config
        updateCategoryToggle(current.copy(includeUppercase = enabled))
    }

    fun onLowercaseToggle(enabled: Boolean) {
        val current = _uiState.value.config
        updateCategoryToggle(current.copy(includeLowercase = enabled))
    }

    fun onNumbersToggle(enabled: Boolean) {
        val current = _uiState.value.config
        updateCategoryToggle(current.copy(includeNumbers = enabled))
    }

    fun onSpecialToggle(enabled: Boolean) {
        val current = _uiState.value.config
        updateCategoryToggle(current.copy(includeSpecial = enabled))
    }

    fun onExcludeSimilarToggle(enabled: Boolean) {
        updateConfig(_uiState.value.config.copy(excludeSimilar = enabled))
    }

    fun clearValidationMessage() {
        _uiState.update { it.copy(validationMessage = null) }
    }

    fun regenerate() {
        val config = _uiState.value.config
        when (val result = engine.generate(config)) {
            is PasswordGenerationResult.Success -> {
                _uiState.update {
                    it.copy(
                        password = result.password,
                        strength = PasswordStrengthEvaluator.evaluate(result.password),
                        validationMessage = null
                    )
                }
            }
            is PasswordGenerationResult.Error -> {
                _uiState.update { it.copy(validationMessage = result.message) }
            }
        }
    }

    private fun updateCategoryToggle(candidate: PasswordConfig) {
        val hasAnyCategory = candidate.includeUppercase ||
            candidate.includeLowercase ||
            candidate.includeNumbers ||
            candidate.includeSpecial

        if (!hasAnyCategory) {
            _uiState.update {
                it.copy(validationMessage = "At least one character type must be selected.")
            }
            return
        }
        updateConfig(candidate)
    }

    private fun updateConfig(newConfig: PasswordConfig) {
        when (val result = engine.generate(newConfig)) {
            is PasswordGenerationResult.Success -> {
                _uiState.update {
                    it.copy(
                        config = newConfig,
                        password = result.password,
                        strength = PasswordStrengthEvaluator.evaluate(result.password),
                        validationMessage = null
                    )
                }
            }
            is PasswordGenerationResult.Error -> {
                _uiState.update { it.copy(validationMessage = result.message) }
            }
        }
    }
}
