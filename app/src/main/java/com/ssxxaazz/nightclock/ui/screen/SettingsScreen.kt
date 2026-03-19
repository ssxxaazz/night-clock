package com.ssxxaazz.nightclock.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.ssxxaazz.nightclock.R

private val Green = SwatchColors.Green
private val Red = SwatchColors.Red
private val Yellow = SwatchColors.Yellow
private val Blue = SwatchColors.Blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val colorEntries = listOf("Green", "Red", "Yellow", "Blue")
    val colorValues = listOf("#2AB32F", "#CC0E00", "#D7C631", "#0000FF")
    val colorObjects = listOf(Green, Red, Yellow, Blue)

    var selectedColorIndex by remember {
        val currentColor = sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"
        mutableStateOf(colorValues.indexOf(currentColor).coerceAtLeast(0))
    }

    var burnInEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("burn_in", false))
    }

    var brightness by remember {
        mutableFloatStateOf(sharedPreferences.getInt("night_mode_brightness", 0).toFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Black,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = Black
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorSelectionSetting(
                title = stringResource(R.string.text_color_title),
                entries = colorEntries,
                colors = colorObjects,
                selectedIndex = selectedColorIndex,
                onSelectionChange = { index ->
                    selectedColorIndex = index
                    sharedPreferences.edit()
                        .putString("text_color", colorValues[index])
                        .apply()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SwitchSetting(
                title = stringResource(R.string.burn_in_title),
                checked = burnInEnabled,
                onCheckedChange = {
                    burnInEnabled = it
                    sharedPreferences.edit()
                        .putBoolean("burn_in", it)
                        .apply()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            BrightnessSetting(
                title = stringResource(R.string.night_mode_brightness_title),
                value = brightness,
                onValueChange = {
                    brightness = it
                    sharedPreferences.edit()
                        .putInt("night_mode_brightness", it.toInt())
                        .apply()
                }
            )
        }
    }
}

@Composable
private fun ColorSelectionSetting(
    title: String,
    entries: List<String>,
    colors: List<Color>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ColorLens,
                contentDescription = null,
                tint = White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEachIndexed { index, entry ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectionChange(index) }
                        .padding(8.dp)
                ) {
                    ColorCircle(
                        color = colors[index],
                        isSelected = index == selectedIndex
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == selectedIndex) White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean
) {
    val borderColor = if (isSelected) White else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Canvas(
        modifier = Modifier.size(40.dp)
    ) {
        drawCircle(
            color = borderColor,
            radius = (size.minDimension / 2) + borderWidth.toPx()
        )
        drawCircle(
            color = color,
            radius = size.minDimension / 2
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = White
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Green,
                checkedTrackColor = Green.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun BrightnessSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BrightnessMedium,
                contentDescription = null,
                tint = White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Green,
                    activeTrackColor = Green,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${value.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = White
            )
        }
    }
}
