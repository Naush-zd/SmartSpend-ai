package dev.nausheen.smartspend.ui.theme

import androidx.compose.ui.graphics.Color

/** Soft pastel background + readable foreground per spending category, so
 *  category chips scan at a glance. Falls back to a neutral tint. */
fun categoryColors(category: String?): Pair<Color, Color> = when (category) {
    "Food & Dining" -> Color(0xFFFFEDD5) to Color(0xFF9A3412)   // warm amber
    "Groceries" -> Color(0xFFDCFCE7) to Color(0xFF166534)       // green
    "Transport" -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)       // blue
    "Shopping" -> Color(0xFFFCE7F3) to Color(0xFF9D174D)        // pink
    "Utilities" -> Color(0xFFFEF9C3) to Color(0xFF854D0E)       // yellow
    "Health" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)          // red
    "Entertainment" -> Color(0xFFEDE9FE) to Color(0xFF5B21B6)   // violet
    "Travel" -> Color(0xFFCFFAFE) to Color(0xFF155E75)          // cyan
    else -> Color(0xFFECEEEC) to Color(0xFF3F3F46)              // neutral
}
