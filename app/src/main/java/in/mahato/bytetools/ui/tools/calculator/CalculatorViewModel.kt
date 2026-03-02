package `in`.mahato.bytetools.ui.tools.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.data.local.CalcDao
import `in`.mahato.bytetools.domain.model.CalcResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calcDao: CalcDao
) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result.asStateFlow()

    val history = calcDao.getCalcHistory()

    fun onDigit(digit: String) {
        _expression.value += digit
    }

    fun onOperator(op: String) {
        if (_expression.value.isNotEmpty() && !_expression.value.last().isDigit()) {
            _expression.value = _expression.value.dropLast(1) + op
        } else {
            _expression.value += op
        }
    }

    fun onClear() {
        _expression.value = ""
        _result.value = ""
    }

    fun onCalculate() {
        try {
            val res = evaluate(_expression.value)
            _result.value = res
            saveToHistory(_expression.value, res)
        } catch (e: Exception) {
            _result.value = "Error"
        }
    }

    private fun saveToHistory(expr: String, res: String) {
        viewModelScope.launch {
            calcDao.saveCalcResult(CalcResult(expression = expr, result = res))
        }
    }

    // A very simple evaluator for demonstration
    private fun evaluate(expr: String): String {
        // This is a placeholder for a real evaluator like exp4j or similar
        // For now, let's just do simple arithmetic if possible
        return try {
            val cleanExpr = expr.replace("×", "*").replace("÷", "/")
            // Simplified logic: split by operators and compute
            // Real implementation should use a library or proper parser
            "Result" // Placeholder
        } catch (e: Exception) {
            "Error"
        }
    }
}
