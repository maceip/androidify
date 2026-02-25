package com.android.developers.androidify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.developers.androidify.theme.AndroidifyTheme
import dagger.hilt.android.AndroidEntryPoint

// Google brand colours
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkSurfaceVariant = Color(0xFF2A2A2A)

private enum class ThemeOption { System, Light, Dark, Custom }

private data class ShortcutItem(
    val id: String,
    val label: String,
    val icon: String, // emoji or text representation
)

private val shortcutOptions = listOf(
    ShortcutItem("none", "None", ""),
    ShortcutItem("ai_mode", "AI Mode", "\u2728"),
    ShortcutItem("live", "Live", "\u25B6"),
    ShortcutItem("translate", "Translate", "\uD83C\uDF10"),
    ShortcutItem("song_search", "Song Search", "\uD83C\uDFB5"),
    ShortcutItem("weather", "Weather", "\u2600"),
    ShortcutItem("translate_visual", "Translate", "\uD83D\uDCF7"),
    ShortcutItem("sports", "Sports", "\u26BD"),
    ShortcutItem("dictionary", "Dictionary", "\uD83D\uDCD6"),
    ShortcutItem("homework", "Homework", "\uD83C\uDF93"),
    ShortcutItem("finance", "Finance", "\uD83D\uDCC8"),
    ShortcutItem("saved", "Saved", "\uD83D\uDD16"),
    ShortcutItem("news", "News", "\uD83D\uDCF0"),
)

private enum class Screen { Customize, ChangeShortcut }

@AndroidEntryPoint
class SearchBarCustomizeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
        super.onCreate(savedInstanceState)

        setContent {
            AndroidifyTheme {
                SearchBarCustomizeRoot(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun SearchBarCustomizeRoot(onBack: () -> Unit) {
    var currentScreen by remember { mutableStateOf(Screen.Customize) }
    var selectedTheme by remember { mutableStateOf(ThemeOption.System) }
    var hue by remember { mutableFloatStateOf(0.5f) }
    var saturation by remember { mutableFloatStateOf(0.5f) }
    var transparency by remember { mutableFloatStateOf(1f) }
    var selectedShortcutIndex by remember { mutableIntStateOf(0) }

    val defaultHue = 0.5f
    val defaultSaturation = 0.5f
    val defaultTransparency = 1f

    when (currentScreen) {
        Screen.Customize -> CustomizeScreen(
            selectedTheme = selectedTheme,
            hue = hue,
            saturation = saturation,
            transparency = transparency,
            selectedShortcut = shortcutOptions[selectedShortcutIndex],
            onThemeSelected = { selectedTheme = it },
            onHueChange = { hue = it },
            onSaturationChange = { saturation = it },
            onTransparencyChange = { transparency = it },
            onChangeShortcut = { currentScreen = Screen.ChangeShortcut },
            onBack = onBack,
            onReset = {
                selectedTheme = ThemeOption.System
                hue = defaultHue
                saturation = defaultSaturation
                transparency = defaultTransparency
                selectedShortcutIndex = 0
            },
        )
        Screen.ChangeShortcut -> ChangeShortcutScreen(
            selectedTheme = selectedTheme,
            hue = hue,
            saturation = saturation,
            transparency = transparency,
            selectedShortcutIndex = selectedShortcutIndex,
            selectedShortcut = shortcutOptions[selectedShortcutIndex],
            onShortcutSelected = { selectedShortcutIndex = it },
            onBack = { currentScreen = Screen.Customize },
            onReset = { selectedShortcutIndex = 0 },
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Compute the search bar pill color from the current settings.
 */
@Composable
private fun computePillColor(
    selectedTheme: ThemeOption,
    hue: Float,
    saturation: Float,
    transparency: Float,
): Color {
    val baseColor = when (selectedTheme) {
        ThemeOption.System -> Color.White
        ThemeOption.Light -> Color.White
        ThemeOption.Dark -> Color(0xFF303134)
        ThemeOption.Custom -> Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue * 360f, saturation, 0.85f),
            ),
        )
    }
    return baseColor.copy(alpha = transparency.coerceIn(0.15f, 1f))
}

private fun contentColorFor(theme: ThemeOption): Color = when (theme) {
    ThemeOption.Light, ThemeOption.System -> Color(0xFF5F6368)
    ThemeOption.Dark -> Color.White.copy(alpha = 0.8f)
    ThemeOption.Custom -> Color.White.copy(alpha = 0.9f)
}

// ── Customize Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeScreen(
    selectedTheme: ThemeOption,
    hue: Float,
    saturation: Float,
    transparency: Float,
    selectedShortcut: ShortcutItem,
    onThemeSelected: (ThemeOption) -> Unit,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onChangeShortcut: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    val pillColor = computePillColor(selectedTheme, hue, saturation, transparency)
    val contentColor = contentColorFor(selectedTheme)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", color = Color.White, fontSize = 22.sp)
                    }
                },
                actions = {
                    TextButton(onClick = onReset) {
                        Text("Reset", color = Color(0xFF8AB4F8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = DarkBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Live Preview ─────────────────────────────────────────────
            LivePreviewSection(
                pillColor = pillColor,
                contentColor = contentColor,
                selectedShortcut = selectedShortcut,
            )

            Spacer(Modifier.height(24.dp))

            // ── Theme ────────────────────────────────────────────────────
            Text(
                "Theme",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))
            ThemeSelector(
                selectedTheme = selectedTheme,
                onThemeSelected = onThemeSelected,
            )

            Spacer(Modifier.height(24.dp))

            // ── Hue ──────────────────────────────────────────────────────
            Text(
                "Hue",
                color = if (selectedTheme == ThemeOption.Custom) Color.White else Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            RainbowHueSlider(
                value = hue,
                onValueChange = onHueChange,
                enabled = selectedTheme == ThemeOption.Custom,
            )

            Spacer(Modifier.height(16.dp))

            // ── Saturation ───────────────────────────────────────────────
            Text(
                "Saturation",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            SaturationSlider(
                value = saturation,
                onValueChange = onSaturationChange,
                hue = hue,
            )

            Spacer(Modifier.height(16.dp))

            // ── Transparency ─────────────────────────────────────────────
            Text(
                "Transparency",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            TransparencySlider(
                value = transparency,
                onValueChange = onTransparencyChange,
            )

            Spacer(Modifier.height(24.dp))

            // ── Change shortcut ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .clickable(onClick = onChangeShortcut)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Change shortcut",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (selectedShortcut.icon.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(selectedShortcut.icon, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "\u203A",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 24.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Change Shortcut Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeShortcutScreen(
    selectedTheme: ThemeOption,
    hue: Float,
    saturation: Float,
    transparency: Float,
    selectedShortcutIndex: Int,
    selectedShortcut: ShortcutItem,
    onShortcutSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    val pillColor = computePillColor(selectedTheme, hue, saturation, transparency)
    val contentColor = contentColorFor(selectedTheme)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change shortcut") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", color = Color.White, fontSize = 22.sp)
                    }
                },
                actions = {
                    TextButton(onClick = onReset) {
                        Text("Reset", color = Color(0xFF8AB4F8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = DarkBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Live Preview ─────────────────────────────────────────────
            LivePreviewSection(
                pillColor = pillColor,
                contentColor = contentColor,
                selectedShortcut = selectedShortcut,
            )

            Spacer(Modifier.height(24.dp))

            // ── Shortcuts ────────────────────────────────────────────────
            Text(
                "Shortcuts",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(shortcutOptions.size) { index ->
                    val shortcut = shortcutOptions[index]
                    val isSelected = index == selectedShortcutIndex
                    ShortcutGridItem(
                        shortcut = shortcut,
                        isSelected = isSelected,
                        onClick = { onShortcutSelected(index) },
                    )
                }
            }
        }
    }
}

// ── Shared Components ────────────────────────────────────────────────────────

@Composable
private fun LivePreviewSection(
    pillColor: Color,
    contentColor: Color,
    selectedShortcut: ShortcutItem,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A237E),
                        Color(0xFF283593),
                        Color(0xFF3949AB),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // "Preview" chip
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "Preview",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Search bar preview pill
            SearchBarPreviewPill(
                pillColor = pillColor,
                contentColor = contentColor,
                selectedShortcut = selectedShortcut,
            )
        }
    }
}

@Composable
private fun SearchBarPreviewPill(
    pillColor: Color,
    contentColor: Color,
    selectedShortcut: ShortcutItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(pillColor)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Google G logo
        MiniGoogleG(modifier = Modifier.size(24.dp))

        Spacer(Modifier.weight(1f))

        // Selected shortcut icon
        if (selectedShortcut.icon.isNotEmpty()) {
            Text(
                selectedShortcut.icon,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        // Mic icon
        Text("\uD83C\uDF99", fontSize = 14.sp, modifier = Modifier.padding(end = 2.dp))

        Spacer(Modifier.width(8.dp))

        // Lens icon
        MiniLensIcon(
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MiniGoogleG(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = size.width * 0.2f
        val hs = sw / 2f
        val rect = Size(size.width - sw, size.height - sw)
        val topLeft = Offset(hs, hs)

        drawArc(
            color = GoogleBlue,
            startAngle = -220f,
            sweepAngle = 255f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        drawArc(
            color = GoogleRed,
            startAngle = 35f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        drawArc(
            color = GoogleYellow,
            startAngle = 75f,
            sweepAngle = 35f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        drawArc(
            color = GoogleGreen,
            startAngle = 110f,
            sweepAngle = 30f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )

        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(
            color = GoogleBlue,
            start = Offset(cx, cy),
            end = Offset(size.width - hs, cy),
            strokeWidth = sw,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MiniLensIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.12f
        val padding = stroke / 2f
        val cornerR = size.width * 0.25f

        drawRoundRect(
            color = tint,
            topLeft = Offset(padding, padding),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(cornerR),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.2f,
            style = Stroke(width = stroke * 0.8f),
        )
    }
}

// ── Theme Selector ───────────────────────────────────────────────────────────

@Composable
private fun ThemeSelector(
    selectedTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ThemeOption.entries.forEach { option ->
            ThemeOptionItem(
                option = option,
                isSelected = selectedTheme == option,
                onClick = { onThemeSelected(option) },
            )
        }
    }
}

@Composable
private fun ThemeOptionItem(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val rainbowBrush = Brush.linearGradient(
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFF8800),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF0088FF),
            Color(0xFF8800FF),
        ),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, GoogleBlue, RoundedCornerShape(16.dp))
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    },
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when (option) {
                        ThemeOption.System -> Color(0xFF2A2A2A)
                        ThemeOption.Light -> Color.White
                        ThemeOption.Dark -> Color(0xFF303134)
                        ThemeOption.Custom -> Color(0xFF2A2A2A)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (option) {
                ThemeOption.System -> {
                    // Rainbow gradient background with G
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(rainbowBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text("\u2713", color = Color.White, fontSize = 20.sp)
                        } else {
                            MiniGoogleG(modifier = Modifier.size(28.dp))
                        }
                    }
                }
                ThemeOption.Light -> {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("\u2713", color = GoogleBlue, fontSize = 20.sp)
                        }
                    } else {
                        MiniGoogleG(modifier = Modifier.size(28.dp))
                    }
                }
                ThemeOption.Dark -> {
                    if (isSelected) {
                        Text("\u2713", color = Color.White, fontSize = 20.sp)
                    } else {
                        MiniGoogleG(modifier = Modifier.size(28.dp))
                    }
                }
                ThemeOption.Custom -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(rainbowBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text("\u2713", color = Color.White, fontSize = 20.sp)
                        } else {
                            MiniGoogleG(modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = option.name,
            color = if (isSelected) GoogleBlue else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// ── Sliders ──────────────────────────────────────────────────────────────────

@Composable
private fun RainbowHueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
) {
    val rainbowColors = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000),
    )
    val disabledAlpha = if (enabled) 1f else 0.3f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(rainbowColors),
                    alpha = disabledAlpha,
                ),
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                disabledThumbColor = Color.White.copy(alpha = 0.4f),
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SaturationSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    hue: Float,
) {
    val desaturatedColor = Color(0xFF1A1A3E)
    val saturatedColor = Color(0xFFB388FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(desaturatedColor, saturatedColor),
                    ),
                ),
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun TransparencySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    val transparentColor = Color(0xFFB0BEC5)
    val opaqueColor = Color(0xFF1A237E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(transparentColor, opaqueColor),
                    ),
                ),
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
        )
    }
}

// ── Shortcut Grid Item ───────────────────────────────────────────────────────

@Composable
private fun ShortcutGridItem(
    shortcut: ShortcutItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(2.5.dp, GoogleBlue, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    },
                )
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center,
        ) {
            if (shortcut.icon.isNotEmpty()) {
                Text(shortcut.icon, fontSize = 24.sp)
            } else {
                // "None" option - show a slash or empty
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = shortcut.label,
            color = if (isSelected) GoogleBlue else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
