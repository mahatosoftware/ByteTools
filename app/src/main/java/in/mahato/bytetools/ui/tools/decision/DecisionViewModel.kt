package `in`.mahato.bytetools.ui.tools.decision

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.random.Random

import `in`.mahato.bytetools.data.local.WheelOptionDao
import `in`.mahato.bytetools.domain.model.WheelOption
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DecisionViewModel @Inject constructor(
    private val wheelOptionDao: WheelOptionDao
) : ViewModel() {

    // Spin Wheel
    val wheelOptions: StateFlow<List<WheelOption>> = wheelOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addOption(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                wheelOptionDao.insertOption(WheelOption(name = name))
            }
        }
    }

    fun removeOption(option: WheelOption) {
        viewModelScope.launch {
            wheelOptionDao.deleteOption(option)
        }
    }

    // Dice Roller
    private val _diceResults = MutableStateFlow<List<Int>>(listOf(1))
    val diceResults = _diceResults.asStateFlow()

    fun rollDice(count: Int) {
        _diceResults.value = List(count) { Random.nextInt(1, 7) }
    }

    // Coin Flip
    private val _coinResult = MutableStateFlow<String?>(null)
    val coinResult = _coinResult.asStateFlow()

    fun flipCoin() {
        _coinResult.value = if (Random.nextBoolean()) "Heads" else "Tails"
    }

    // RNG
    private val _rngResults = MutableStateFlow<List<Int>>(emptyList())
    val rngResults = _rngResults.asStateFlow()

    fun generateNumbers(min: Int, max: Int, count: Int, unique: Boolean) {
        if (min >= max) return
        val results = mutableListOf<Int>()
        if (unique && (max - min + 1) < count) {
            // Cannot generate unique numbers
            return
        }
        
        while (results.size < count) {
            val num = Random.nextInt(min, max + 1)
            if (!unique || !results.contains(num)) {
                results.add(num)
            }
        }
        _rngResults.value = results
    }

    // Name Picker
    private val _pickedNames = MutableStateFlow<List<String>>(emptyList())
    val pickedNames = _pickedNames.asStateFlow()

    fun pickNames(names: List<String>, count: Int) {
        val validNames = names.filter { it.isNotBlank() }
        if (validNames.isEmpty()) return
        val itemsCount = minOf(count, validNames.size)
        _pickedNames.value = validNames.shuffled().take(itemsCount)
    }

    // Truth or Dare
    private val _truthOrDareResult = MutableStateFlow<Pair<String, String>?>(null)
    val truthOrDareResult = _truthOrDareResult.asStateFlow()

    private val truths = listOf(
        "What is your biggest fear?",
        "What is the most embarrassing thing you've ever done?",
        "If you could have any superpower, what would it be?",
        "Who is your secret crush?",
        "What's one thing you've never told anyone?",
        "What's the best piece of advice you've ever received?",
        "If you could meet anyone, dead or alive, who would it be?"
    )

    private val dares = listOf(
        "Sing a song out loud.",
        "Do 10 pushups.",
        "Eat a spoonful of something spicy.",
        "Call a friend and say something funny.",
        "Dance without music for 30 seconds.",
        "Impersonate a famous person.",
        "Tell a funny joke."
    )

    fun getTruth() {
        _truthOrDareResult.value = "Truth" to truths.random()
    }

    fun getDare() {
        _truthOrDareResult.value = "Dare" to dares.random()
    }
}
