import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFFFFFFF),         // Pure white text/icons
            secondary = Color(0xFFB0B0B0),       // Mid-gray for secondary accents
            background = Color(0xFF000000),      // Jet black background
            surface = Color(0xFF121212),         // Slightly lighter black for surfaces
            surfaceVariant = Color(0xFF1E1E1E),  // Even lighter for variants
            primaryContainer = Color(0xFF333333),// Container for primary elements
            onPrimary = Color.Black              // Black on white (if ever used)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF000000),         // Pure black text/icons
            secondary = Color(0xFF505050),       // Gray for accents
            background = Color(0xFFFFFFFF),      // Clean white background
            surface = Color(0xFFF5F5F5),         // Slightly darker white for surfaces
            surfaceVariant = Color(0xFFEEEEEE),  // Even darker for variants
            primaryContainer = Color(0xFFE0E0E0),// Container for primary elements
            onPrimary = Color.White              // White on black (if ever used)
        )
    }

    val typography = Typography(
        displayLarge = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary
        ),
        titleMedium = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary
        ),
        bodyMedium = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = colorScheme.secondary
        ),
        labelSmall = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = colorScheme.secondary
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
