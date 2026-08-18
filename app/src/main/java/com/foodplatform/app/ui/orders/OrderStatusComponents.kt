package com.foodplatform.app.ui.orders

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─── Internal helper ───────────────────────────────────────────────────────────

private data class StatusStyle(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun StatusChipContent(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

// ─── Order Status Chip ─────────────────────────────────────────────────────────

@Composable
fun OrderStatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, color, icon) = when (status.uppercase()) {
        "PENDING"          -> Triple("Pending",          Color(0xFF9E9E9E), Icons.Default.HourglassEmpty)
        "CONFIRMED"        -> Triple("Confirmed",        Color(0xFF1976D2), Icons.Default.ThumbUp)
        "PREPARING"        -> Triple("Preparing",        Color(0xFFF9A825), Icons.Default.Restaurant)
        "OUT_FOR_DELIVERY" -> Triple("Out for Delivery", Color(0xFFE65100), Icons.Default.LocalShipping)
        "DELIVERED"        -> Triple("Delivered",        Color(0xFF388E3C), Icons.Default.CheckCircle)
        "CANCELLED"        -> Triple("Cancelled",        Color(0xFFD32F2F), Icons.Default.Cancel)
        else               -> Triple(status,             Color(0xFF9E9E9E), Icons.Default.HourglassEmpty)
    }
    StatusChipContent(label = label, color = color, icon = icon, modifier = modifier)
}

// ─── Delivery Status Chip ──────────────────────────────────────────────────────

@Composable
fun DeliveryStatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, color, icon) = when (status.uppercase()) {
        "PENDING"    -> Triple("Delivery Pending", Color(0xFF9E9E9E), Icons.Default.HourglassEmpty)
        "PICKED_UP"  -> Triple("Picked Up",        Color(0xFF1976D2), Icons.Default.LocalShipping)
        "IN_TRANSIT" -> Triple("In Transit",       Color(0xFFE65100), Icons.Default.DirectionsBike)
        "DELIVERED"  -> Triple("Delivered",        Color(0xFF388E3C), Icons.Default.CheckCircle)
        "FAILED"     -> Triple("Delivery Failed",  Color(0xFFD32F2F), Icons.Default.Cancel)
        else         -> Triple(status,             Color(0xFF9E9E9E), Icons.Default.HourglassEmpty)
    }
    StatusChipContent(label = label, color = color, icon = icon, modifier = modifier)
}

// ─── Payment Status Chip ───────────────────────────────────────────────────────

@Composable
fun PaymentStatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, color, icon) = when (status.uppercase()) {
        "PENDING"   -> Triple("Payment Pending", Color(0xFF9E9E9E), Icons.Default.Payment)
        "COMPLETED" -> Triple("Paid",            Color(0xFF388E3C), Icons.Default.CheckCircle)
        "FAILED"    -> Triple("Payment Failed",  Color(0xFFD32F2F), Icons.Default.Cancel)
        "REFUNDED"  -> Triple("Refunded",        Color(0xFF7B1FA2), Icons.Default.Replay)
        else        -> Triple(status,            Color(0xFF9E9E9E), Icons.Default.Payment)
    }
    StatusChipContent(label = label, color = color, icon = icon, modifier = modifier)
}
