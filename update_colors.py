import re

files = [
    "app/src/main/java/in/mahato/bytetools/ui/tools/decision/DecisionDashboardScreen.kt",
    "app/src/main/java/in/mahato/bytetools/ui/tools/image/ImageToolsDashboardScreen.kt",
    "app/src/main/java/in/mahato/bytetools/ui/tools/pdf/PDFDashboardScreen.kt",
    "app/src/main/java/in/mahato/bytetools/ui/tools/qr/QRBarcodeDashboardScreen.kt"
]

colors = [
    "Color(0xFFE3F2FD), Color(0xFF2196F3)",
    "Color(0xFFF3E5F5), Color(0xFF9C27B0)",
    "Color(0xFFE8F5E9), Color(0xFF4CAF50)",
    "Color(0xFFFBE9E7), Color(0xFFFF5722)",
    "Color(0xFFFFF3E0), Color(0xFFFF9800)",
    "Color(0xFFF1F8E9), Color(0xFF8BC34A)",
    "Color(0xFFE0F7FA), Color(0xFF00BCD4)",
    "Color(0xFFFFEBEE), Color(0xFFF44336)",
    "Color(0xFFE8EAF6), Color(0xFF3F51B5)",
    "Color(0xFFFFF8E1), Color(0xFFFFC107)",
    "Color(0xFFECEFF1), Color(0xFF607D8B)"
]

card_to_replace = """        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }"""

new_card = """        colors = CardDefaults.cardColors(
            containerColor = item.bgColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                modifier = Modifier.size(32.dp),
                tint = item.accentColor
            )
            
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black,
                    maxLines = 1
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    maxLines = 1
                )
            }
        }
    }"""

for path in files:
    with open(path, "r") as f:
        content = f.read()

    # Find the data class definition
    data_class_match = re.search(r"data class (\w+)\(val name: String, val description: String, val icon: ImageVector, val route: String\)", content)
    if not data_class_match:
        print(f"Data class not found in {path}")
        continue
    
    class_name = data_class_match.group(1)
    
    # Replace data class
    content = content.replace(
        f"data class {class_name}(val name: String, val description: String, val icon: ImageVector, val route: String)",
        f"data class {class_name}(val name: String, val description: String, val icon: ImageVector, val route: String, val bgColor: androidx.compose.ui.graphics.Color, val accentColor: androidx.compose.ui.graphics.Color)"
    )

    # Find all tools instances and add colors
    pattern = rf"({class_name}\(\"[^\"]+\", \"[^\"]+\", [^,]+, [^\)]+)\)"
    
    def repl(match):
        idx = getattr(repl, "counter", 0)
        color_pair = colors[idx % len(colors)]
        repl.counter = idx + 1
        return f"{match.group(1)}, {color_pair})"
    
    repl.counter = 0
    content = re.sub(pattern, repl, content)

    # Replace Card UI
    content = content.replace(card_to_replace, new_card)

    with open(path, "w") as f:
        f.write(content)
        print(f"Updated {path}")

