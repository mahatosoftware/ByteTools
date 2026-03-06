package `in`.mahato.bytetools.ui.tools.dateduration

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDurationScreen(navController: NavController) {
    val initialStartDate = remember { Calendar.getInstance().apply { clearTime(this) } }
    val initialEndDate = remember { Calendar.getInstance().apply { 
        clearTime(this)
        add(Calendar.DAY_OF_MONTH, 1) 
    } }

    var startYear by remember { mutableStateOf(initialStartDate.get(Calendar.YEAR)) }
    var startMonth by remember { mutableStateOf(initialStartDate.get(Calendar.MONTH)) }
    var startDay by remember { mutableStateOf(initialStartDate.get(Calendar.DAY_OF_MONTH)) }

    var endYear by remember { mutableStateOf(initialEndDate.get(Calendar.YEAR)) }
    var endMonth by remember { mutableStateOf(initialEndDate.get(Calendar.MONTH)) }
    var endDay by remember { mutableStateOf(initialEndDate.get(Calendar.DAY_OF_MONTH)) }

    val startDate = remember(startYear, startMonth, startDay) {
        Calendar.getInstance().apply {
            clearTime(this)
            set(startYear, startMonth, startDay)
        }
    }

    val endDate = remember(endYear, endMonth, endDay) {
        Calendar.getInstance().apply {
            clearTime(this)
            set(endYear, endMonth, endDay)
        }
    }

    // Since startDate and endDate change when state changes, the calculation below will be re-executed 
    // because remember(startYear, ...) READS the state, causing the whole function to recompose!
    val actualStart = if (startDate.before(endDate)) startDate else endDate
    val actualEnd = if (startDate.before(endDate)) endDate else startDate

    val tempDate = actualStart.clone() as Calendar
    var years = actualEnd.get(Calendar.YEAR) - tempDate.get(Calendar.YEAR)
    tempDate.add(Calendar.YEAR, years)
    if (tempDate.after(actualEnd)) {
        years--
        tempDate.add(Calendar.YEAR, -1)
    }

    var months = 0
    while (true) {
        tempDate.add(Calendar.MONTH, 1)
        if (tempDate.after(actualEnd)) {
            tempDate.add(Calendar.MONTH, -1)
            break
        }
        months++
    }

    val daysMillis = abs(actualEnd.timeInMillis - tempDate.timeInMillis)
    val days = ((daysMillis + 12L * 60 * 60 * 1000) / (24L * 60 * 60 * 1000)).toInt()

    val diffMillis = abs(endDate.timeInMillis - startDate.timeInMillis)
    // Adding half a day (12 hours) prevents DST jump issues where difference could be 23 hours instead of 24
    val totalDays = (diffMillis + 12L * 60 * 60 * 1000) / (24L * 60 * 60 * 1000)
    
    val totalWeeks = totalDays / 7
    val remainingDaysOfWeek = totalDays % 7

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Date Duration", fontWeight = FontWeight.Bold) },
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
            
            val startDatePickerDialog = DatePickerDialog(
                context,
                { _, sYear, sMonth, sDay ->
                    startYear = sYear
                    startMonth = sMonth
                    startDay = sDay
                }, startYear, startMonth, startDay
            )

            val endDatePickerDialog = DatePickerDialog(
                context,
                { _, eYear, eMonth, eDay ->
                    endYear = eYear
                    endMonth = eMonth
                    endDay = eDay
                }, endYear, endMonth, endDay
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f).clickable { startDatePickerDialog.show() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Start Date", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format("%02d/%02d/%d", startDay, startMonth + 1, startYear), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).clickable { endDatePickerDialog.show() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("End Date", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format("%02d/%02d/%d", endDay, endMonth + 1, endYear), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text("Duration Between Dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DurationCard("Years", years.toString(), Modifier.weight(1f))
                DurationCard("Months", months.toString(), Modifier.weight(1f))
                DurationCard("Days", days.toString(), Modifier.weight(1f))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Total Days: $totalDays", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Or $totalWeeks weeks and $remainingDaysOfWeek days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DurationCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.displaySmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun clearTime(cal: Calendar) {
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
}
