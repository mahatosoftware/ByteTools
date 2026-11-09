package `in`.mahato.bytetools.ui.tools.quickorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.PaymentStatus
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.ReconciliationStatus

val PurplePrimary = Color(0xFF673AB7)
val PurpleBackground = Color(0xFFEDE7F6)
val PurpleDark = Color(0xFF512DA8)
val GreenSuccess = Color(0xFF2E7D32)
val GreenBackground = Color(0xFFE8F5E9)
val AmberWarning = Color(0xFFED6C02)
val AmberBackground = Color(0xFFFFF4E5)
val RedDanger = Color(0xFFD32F2F)
val RedBackground = Color(0xFFFFEBEE)

fun navigateToWizardStep(
    navController: NavController,
    orderId: Long,
    targetStep: Int,
    isFinalized: Boolean = false
) {
    if (orderId <= 0L) return
    val route = when (targetStep) {
        0 -> null
        1 -> "collect_orders/$orderId"
        2 -> "consolidated_list/$orderId"
        3 -> "enter_bill/$orderId"
        4 -> if (isFinalized || targetStep <= 4) "reconciliation/$orderId" else "reconciliation/$orderId"
        5 -> if (isFinalized || targetStep == 5) "settlement_dashboard/$orderId" else null
        6 -> "payment_tracking/$orderId"
        else -> null
    } ?: return
    navController.navigate(route) {
        launchSingleTop = true
    }
}

@Composable
fun OrderStepIndicator(
    currentStep: Int,
    steps: List<String> = listOf("Create", "Collect", "List", "Bill", "Allocate", "Split", "Track"),
    onStepClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stepTitle = when (currentStep) {
        0 -> "Create Order & Add Members"
        1 -> "Collect Member Orders"
        2 -> "Consolidated Shopping List"
        3 -> "Enter Shop Bill & Payers"
        4 -> "Reconcile & Allocate Items"
        5 -> "Smart Split Settlements"
        6 -> "Payment Tracking & Receipts"
        else -> steps.getOrElse(currentStep) { "Order Wizard" }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = ((currentStep + 1).toFloat() / steps.size.toFloat()).coerceIn(0f, 1f),
        label = "wizard_step_progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header Row: Step Badge + Title + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PurpleBackground,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "STEP ${currentStep + 1} OF ${steps.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurplePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${((currentStep + 1) * 100) / steps.size}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurplePrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Smooth Progress Track
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PurplePrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Connected Step Node Trail
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, stepName ->
                    val isCompleted = index < currentStep
                    val isCurrent = index == currentStep
                    val isClickable = onStepClick != null && (isCompleted || isCurrent)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isClickable) {
                                onStepClick?.invoke(index)
                            }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 26.dp else 22.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> GreenSuccess
                                        isCurrent -> PurplePrimary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .then(
                                    if (isCurrent) Modifier.border(2.dp, PurpleBackground, CircleShape)
                                    else if (!isCompleted) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = if (isCurrent) 11.sp else 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = stepName,
                            fontSize = 9.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                isCurrent -> PurplePrimary
                                isCompleted -> GreenSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .offset(y = (-8).dp)
                                .background(
                                    if (index < currentStep) GreenSuccess.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun QuantityStepper(
    quantity: Double,
    unit: String = "pcs",
    onQuantityChange: (Double) -> Unit,
    minQuantity: Double = 0.0,
    step: Double = 1.0,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (enabled) {
                IconButton(
                    onClick = {
                        if (quantity - step >= minQuantity) {
                            onQuantityChange(quantity - step)
                        }
                    },
                    modifier = Modifier.size(32.dp),
                    enabled = quantity > minQuantity
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }
            }

            val displayQty = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else String.format(java.util.Locale.US, "%.1f", quantity)
            Text(
                text = "$displayQty $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = if (enabled) 0.dp else 10.dp)
                    .widthIn(min = 54.dp)
            )

            if (enabled) {
                IconButton(
                    onClick = { onQuantityChange(quantity + step) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text, icon) = when (status) {
        PaymentStatus.PAID -> Quadruple(GreenBackground, GreenSuccess, "PAID", Icons.Default.CheckCircle)
        PaymentStatus.PARTIALLY_PAID -> Quadruple(AmberBackground, AmberWarning, "PARTIAL", Icons.Default.Schedule)
        PaymentStatus.PENDING -> Quadruple(RedBackground, RedDanger, "PENDING", Icons.Default.Pending)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun ReconciliationStatusBadge(status: ReconciliationStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when (status) {
        ReconciliationStatus.MATCHED -> Triple(GreenBackground, GreenSuccess, "MATCHED")
        ReconciliationStatus.SHORTFALL -> Triple(AmberBackground, AmberWarning, "SHORTFALL")
        ReconciliationStatus.SURPLUS -> Triple(Color(0xFFE1F5FE), Color(0xFF0288D1), "SURPLUS")
        ReconciliationStatus.UNAVAILABLE -> Triple(RedBackground, RedDanger, "UNAVAILABLE")
        ReconciliationStatus.NEW_ITEM -> Triple(PurpleBackground, PurplePrimary, "EXTRA ITEM")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EmptyQuickOrderView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PurpleBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionButtonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
