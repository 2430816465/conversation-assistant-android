package cn.xindong.junshi

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightPalette=lightColorScheme(
    primary=Color(0xFFB84234), onPrimary=Color.White,
    primaryContainer=Color(0xFFFBE6E0), onPrimaryContainer=Color(0xFF803025),
    secondary=Color(0xFF65594F), onSecondary=Color.White,
    secondaryContainer=Color(0xFFEFE8DF), onSecondaryContainer=Color(0xFF494038),
    tertiary=Color(0xFF596A58), onTertiary=Color.White,
    background=Color(0xFFFAF7F2), onBackground=Color(0xFF302B28),
    surface=Color(0xFFFFFDFC), onSurface=Color(0xFF302B28),
    surfaceDim=Color(0xFFE5DFD7), surfaceBright=Color(0xFFFFFDFC),
    surfaceContainerLowest=Color.White, surfaceContainerLow=Color(0xFFFAF7F2),
    surfaceContainer=Color(0xFFF5EFE9), surfaceContainerHigh=Color(0xFFF0E9E2), surfaceContainerHighest=Color(0xFFECE4DC),
    surfaceVariant=Color(0xFFF5EFE9), onSurfaceVariant=Color(0xFF706259),
    outline=Color(0xFF897B70), outlineVariant=Color(0xFFE8DFD6),
    error=Color(0xFFAD292E), onError=Color.White,
    errorContainer=Color(0xFFFFDAD6), onErrorContainer=Color(0xFF680B13)
)
private val DarkPalette=darkColorScheme(
    primary=Color(0xFFFFB09E), onPrimary=Color(0xFF602016),
    primaryContainer=Color(0xFF63372E), onPrimaryContainer=Color(0xFFFFDED5),
    secondary=Color(0xFFD9C4B3), onSecondary=Color(0xFF3C2E24),
    secondaryContainer=Color(0xFF463B33), onSecondaryContainer=Color(0xFFF3E5D9),
    tertiary=Color(0xFFB9CDB5), onTertiary=Color(0xFF243622),
    background=Color(0xFF1B1715), onBackground=Color(0xFFF1E7DF),
    surface=Color(0xFF25201D), onSurface=Color(0xFFF1E7DF),
    surfaceDim=Color(0xFF1B1715), surfaceBright=Color(0xFF473D36),
    surfaceContainerLowest=Color(0xFF151210), surfaceContainerLow=Color(0xFF201B18),
    surfaceContainer=Color(0xFF25201D), surfaceContainerHigh=Color(0xFF302923), surfaceContainerHighest=Color(0xFF3B322B),
    surfaceVariant=Color(0xFF302923), onSurfaceVariant=Color(0xFFD1BFB1),
    outline=Color(0xFFAF9989), outlineVariant=Color(0xFF4C4036),
    error=Color(0xFFFFB4AB), onError=Color(0xFF650012),
    errorContainer=Color(0xFF832A2C), onErrorContainer=Color(0xFFFFDAD6)
)
private val JunshiTypography=Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=40.sp),
    headlineMedium=TextStyle(fontWeight=FontWeight.Bold,fontSize=28.sp,lineHeight=38.sp,letterSpacing=(-0.5).sp),
    headlineSmall=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=24.sp,lineHeight=32.sp),
    titleLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=21.sp,lineHeight=29.sp),
    titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=25.sp),
    titleSmall=TextStyle(fontWeight=FontWeight.Medium,fontSize=15.sp,lineHeight=23.sp),
    bodyLarge=TextStyle(fontSize=16.sp,lineHeight=25.sp),
    bodyMedium=TextStyle(fontSize=14.sp,lineHeight=22.sp),
    bodySmall=TextStyle(fontSize=12.sp,lineHeight=19.sp),
    labelLarge=TextStyle(fontWeight=FontWeight.Medium,fontSize=14.sp,lineHeight=20.sp),
    labelMedium=TextStyle(fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=18.sp),
    labelSmall=TextStyle(fontWeight=FontWeight.Medium,fontSize=11.sp,lineHeight=16.sp)
)
@Composable internal fun JunshiTheme(content: @Composable ()->Unit) {
    val dark=isSystemInDarkTheme(); val view=LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window,view).apply {
                isAppearanceLightStatusBars=!dark
                isAppearanceLightNavigationBars=!dark
            }
        }
    }
    MaterialTheme(colorScheme=if(dark) DarkPalette else LightPalette,typography=JunshiTypography,
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(32.dp)),content=content)
}
