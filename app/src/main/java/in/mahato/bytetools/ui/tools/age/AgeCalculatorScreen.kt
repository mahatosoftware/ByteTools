package `in`.mahato.bytetools.ui.tools.age

import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(navController: NavController) {
    val birthDate = remember { Calendar.getInstance().apply { set(2000, 0, 1) } }
    var year by remember { mutableStateOf(birthDate.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(birthDate.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(birthDate.get(Calendar.DAY_OF_MONTH)) }
    
    val today = Calendar.getInstance()
    
    // Calculate Age
    var years = today.get(Calendar.YEAR) - year
    var months = today.get(Calendar.MONTH) - month
    var days = today.get(Calendar.DAY_OF_MONTH) - day

    if (days < 0) {
        val prevMonth = (today.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        months--
    }
    if (months < 0) {
        months += 12
        years--
    }

    // Next Birthday
    val nextBday = Calendar.getInstance().apply {
        set(Calendar.YEAR, today.get(Calendar.YEAR))
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        if (before(today)) add(Calendar.YEAR, 1)
    }
    val diffMillis = nextBday.timeInMillis - today.timeInMillis
    val daysToNextBday = diffMillis / (24 * 60 * 60 * 1000)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Age Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val context = LocalContext.current
            val datePickerDialog = DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    year = selectedYear
                    month = selectedMonth
                    day = selectedDayOfMonth
                    birthDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
                }, year, month, day
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Date of Birth", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(String.format("%02d/%02d/%d", day, month + 1, year), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Tap to change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AgeCard("Years", years.toString(), Modifier.weight(1f))
                AgeCard("Months", months.toString(), Modifier.weight(1f))
                AgeCard("Days", days.toString(), Modifier.weight(1f))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Next Birthday in: $daysToNextBday days", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AgeCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.displaySmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
// Note: LocalDate requires API 26. The minSdk is 24. 
// I should use ThreeTenABP or check API level.
// Let's check minSdk in build.gradle.kts. It was 24.
// So I should use Calendar for compatibility if not using desugaring.
