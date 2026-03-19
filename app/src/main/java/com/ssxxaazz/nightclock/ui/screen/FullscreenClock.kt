package com.ssxxaazz.nightclock.ui.screen

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager
import com.ssxxaazz.nightclock.SettingsActivity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Black = ComposeColor.Black

@Composable
fun FullscreenClock(
    systemUiVisible: MutableState<Boolean>,
    onSettingsClick: () -> Unit,
    onSystemUiShow: () -> Unit = {},
    onSystemUiAutoHide: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    var hours by rememberSaveable { mutableStateOf("00") }
    var minutes by rememberSaveable { mutableStateOf("00") }
    var dateText by rememberSaveable { mutableStateOf("") }
    var alarmText by rememberSaveable { mutableStateOf<String?>(null) }

    var textColor by remember { mutableStateOf(SwatchColors.Green) }
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var burnInProtectionEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("burn_in", false)) }

    val infiniteTransition = rememberInfiniteTransition(label = "burn_in")
    val burnInFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "burn_in_animation"
    )

    val currentTextColor by remember {
        derivedStateOf {
            if (!burnInProtectionEnabled) {
                textColor
            } else {
                val phase = burnInFraction % 1f
                val (from, to) = if (phase < 0.5f) {
                    textColor to Black
                } else {
                    Black to textColor
                }
                val localPhase = if (phase < 0.5f) phase * 2f else (phase - 0.5f) * 2f
                val fraction = if (localPhase < 0.05f) {
                    localPhase / 0.05f
                } else {
                    1f
                }
                blendColorsCompose(from, to, fraction)
            }
        }
    }

    val currentBackgroundColor by remember {
        derivedStateOf {
            if (!burnInProtectionEnabled) {
                Black
            } else {
                val phase = burnInFraction % 1f
                val (from, to) = if (phase < 0.5f) {
                    Black to textColor
                } else {
                    textColor to Black
                }
                val localPhase = if (phase < 0.5f) phase * 2f else (phase - 0.5f) * 2f
                val fraction = if (localPhase < 0.05f) {
                    localPhase / 0.05f
                } else {
                    1f
                }
                blendColorsCompose(from, to, fraction)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            hours = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
            minutes = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

            val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.US)
            dateText = sdf.format(cal.time)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmClockInfo = alarmManager.nextAlarmClock
            if (alarmClockInfo != null) {
                val alarmCal = Calendar.getInstance().apply {
                    timeInMillis = alarmClockInfo.triggerTime
                }
                val daySdf = SimpleDateFormat("EEEE", Locale.US)
                val dayText = daySdf.format(alarmCal.time)
                alarmText = "$dayText ${String.format(Locale.US, "%02d:%02d", alarmCal.get(Calendar.HOUR_OF_DAY), alarmCal.get(Calendar.MINUTE))}"
            } else {
                alarmText = null
            }

            textColor = try {
                ComposeColor(Color.parseColor(sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"))
            } catch (e: Exception) {
                SwatchColors.Green
            }

            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "text_color" -> {
                    textColor = try {
                        ComposeColor(Color.parseColor(sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"))
                    } catch (e: Exception) {
                        SwatchColors.Green
                    }
                }
                "burn_in" -> {
                    burnInProtectionEnabled = sharedPreferences.getBoolean("burn_in", false)
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        textColor = try {
            ComposeColor(Color.parseColor(sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"))
        } catch (e: Exception) {
            SwatchColors.Green
        }
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val actualTextColor = if (burnInProtectionEnabled) currentTextColor else textColor
    val actualBackgroundColor = if (burnInProtectionEnabled) currentBackgroundColor else Black

    // Calculate clock dimensions based on screen size
    // The original uses 250sp font on a rotated layout
    val fontSize = (screenHeight * 0.36f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(actualBackgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onSystemUiShow()
                        systemUiVisible.value = !systemUiVisible.value
                    },
                    onLongPress = { onLongPress() }
                )
            }
            .semantics { testTag = "clock_root" }
    ) {
        // Alarm section at TOP
        alarmText?.let { alarm ->
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Alarm",
                    tint = actualTextColor,
                    modifier = Modifier.size((screenWidth * 0.06f).dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = alarm,
                    fontSize = (screenWidth * 0.06f).sp,
                    fontWeight = FontWeight.Bold,
                    color = actualTextColor,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Clock in CENTER - Hours : SecondsLine : Minutes (all rotated 90 degrees)
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hours - ROTATED 90 DEGREES, both digits as a unit
            Box(
                modifier = Modifier.graphicsLayer { rotationZ = 90f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hours,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    color = actualTextColor,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Seconds line between hours and minutes - spanning full width
            SecondsLine(
                modifier = Modifier.fillMaxWidth(),
                color = actualTextColor
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Minutes - ROTATED 90 DEGREES, both digits as a unit
            Box(
                modifier = Modifier.graphicsLayer { rotationZ = 90f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = minutes,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    color = actualTextColor,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Date at BOTTOM
        Text(
            text = dateText,
            fontSize = (screenWidth * 0.07f).sp,
            fontWeight = FontWeight.Bold,
            color = actualTextColor,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )

        // Settings button - TOP RIGHT
        if (systemUiVisible.value) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = actualTextColor
                )
            }
        }
    }
}

@Composable
private fun SecondsLine(
    modifier: Modifier = Modifier,
    color: ComposeColor
) {
    var secondsProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            val seconds = cal.get(Calendar.SECOND)
            val milliseconds = cal.get(Calendar.MILLISECOND)
            secondsProgress = (seconds + milliseconds / 1000f) / 60f
            delay(100)
        }
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(secondsProgress)
                .height(3.dp)
                .background(color)
        )
    }
}

private fun blendColorsCompose(color1: ComposeColor, color2: ComposeColor, ratio: Float): ComposeColor {
    val inverseRatio = 1 - ratio
    val r = color1.red * inverseRatio + color2.red * ratio
    val g = color1.green * inverseRatio + color2.green * ratio
    val b = color1.blue * inverseRatio + color2.blue * ratio
    return ComposeColor(r, g, b, 1f)
}
