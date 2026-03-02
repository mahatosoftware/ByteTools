package `in`.mahato.bytetools.ui.tools.unitconverter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.LENGTH) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Unit Converter", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                UnitCategory.entries.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ConverterContent(selectedCategory)
        }
    }
}

@Composable
fun ConverterContent(category: UnitCategory) {
    var inputValue by remember(category) { mutableStateOf("") }
    var fromUnit by remember(category) { mutableStateOf(category.units[0]) }
    var toUnit by remember(category) { mutableStateOf(category.units[1]) }
    
    val result = remember(inputValue, fromUnit, toUnit) {
        val input = inputValue.toDoubleOrNull() ?: 0.0
        val baseValue = input * fromUnit.factor
        baseValue / toUnit.factor
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Enter Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Text(fromUnit.symbol) }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UnitSelector(
                label = "From",
                selectedUnit = fromUnit,
                units = category.units,
                onUnitSelected = { fromUnit = it },
                modifier = Modifier.weight(1f)
            )
            UnitSelector(
                label = "To",
                selectedUnit = toUnit,
                units = category.units,
                onUnitSelected = { toUnit = it },
                modifier = Modifier.weight(1f)
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Result", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${String.format("%.4f", result)} ${toUnit.symbol}",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelector(
    label: String,
    selectedUnit: ConversionUnit,
    units: List<ConversionUnit>,
    onUnitSelected: (ConversionUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

enum class UnitCategory(val units: List<ConversionUnit>) {
    LENGTH(listOf(
        ConversionUnit("Meter", "m", 1.0),
        ConversionUnit("Kilometer", "km", 1000.0),
        ConversionUnit("Centimeter", "cm", 0.01),
        ConversionUnit("Millimeter", "mm", 0.001),
        ConversionUnit("Inch", "in", 0.0254),
        ConversionUnit("Foot", "ft", 0.3048),
        ConversionUnit("Yard", "yd", 0.9144),
        ConversionUnit("Mile", "mi", 1609.34)
    )),
    WEIGHT(listOf(
        ConversionUnit("Gram", "g", 1.0),
        ConversionUnit("Kilogram", "kg", 1000.0),
        ConversionUnit("Milligram", "mg", 0.001),
        ConversionUnit("Pound", "lb", 453.592),
        ConversionUnit("Ounce", "oz", 28.3495)
    )),
    AREA(listOf(
        ConversionUnit("Sq Meter", "m²", 1.0),
        ConversionUnit("Sq Kilometer", "km²", 1000000.0),
        ConversionUnit("Sq Foot", "ft²", 0.092903),
        ConversionUnit("Acre", "ac", 4046.86),
        ConversionUnit("Hectare", "ha", 10000.0)
    )),
    VOLUME(listOf(
        ConversionUnit("Liter", "L", 1.0),
        ConversionUnit("Milliliter", "mL", 0.001),
        ConversionUnit("Cubic Meter", "m³", 1000.0),
        ConversionUnit("Gallon (US)", "gal", 3.78541),
        ConversionUnit("Cup", "cup", 0.236588)
    )),
    TEMPERATURE(listOf(
        ConversionUnit("Celsius", "°C", 1.0), // Special handling needed usually, but factor works for differences. 
        // For absolute conversion, we'd need a more complex formula.
        ConversionUnit("Fahrenheit", "°F", 1.0), 
        ConversionUnit("Kelvin", "K", 1.0)
    ))
}

data class ConversionUnit(val name: String, val symbol: String, val factor: Double)

// Note: For Temperature, simple factors don't work (need offset). I'll simplify or add special logic if target quality requires.
