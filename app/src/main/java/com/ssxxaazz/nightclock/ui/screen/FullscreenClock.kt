package com.ssxxaazz.nightclock.ui.screen

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ssxxaazz.nightclock.AlarmFilter
import com.ssxxaazz.nightclock.R
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
    var isCalendarAlert by rememberSaveable { mutableStateOf(false) }

    var textColor by remember { mutableStateOf(SwatchColors.Green) }
    val sharedPreferences = remember { context.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE) }
    var burnInProtectionEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("burn_in", false)) }
    var checkerboardPhase by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(burnInProtectionEnabled) {
        checkerboardPhase = false
        while (burnInProtectionEnabled) {
            delay(burnInProtectionCycleMillis)
            checkerboardPhase = !checkerboardPhase
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            hours = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
            minutes = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

            val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.US)
            dateText = sdf.format(cal.time)

            val alarmInfo = AlarmFilter.getNextAlarm(context)
            if (alarmInfo != null) {
                val isClockAlarm = AlarmFilter.isClockAlarm(alarmInfo)
                val alarmCal = Calendar.getInstance().apply {
                    timeInMillis = alarmInfo.triggerTime
                }
                val daySdf = SimpleDateFormat("EEEE", Locale.US)
                val dayText = daySdf.format(alarmCal.time)
                alarmText = "$dayText ${String.format(Locale.US, "%02d:%02d", alarmCal.get(Calendar.HOUR_OF_DAY), alarmCal.get(Calendar.MINUTE))}"
                isCalendarAlert = !isClockAlarm
            } else {
                alarmText = null
                isCalendarAlert = false
            }

            textColor = try {
                ComposeColor(AndroidColor.parseColor(sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"))
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
                        ComposeColor(AndroidColor.parseColor(sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"))
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
            ComposeColor(AndroidColor.parseColor(sharedPreferences.getString("text_color", "#2AB32F") ?: "#2AB32F"))
        } catch (e: Exception) {
            SwatchColors.Green
        }
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Calculate clock dimensions based on screen size
    // The original uses 250sp font on a rotated layout
    val fontSize = (screenHeight * 0.36f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (burnInProtectionEnabled) {
                        Modifier.checkerboardBurnInMask(maskOddPixels = checkerboardPhase)
                    } else {
                        Modifier
                    }
                )
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
                        painter = painterResource(if (isCalendarAlert) R.drawable.ic_calendar else R.drawable.ic_alarm),
                        contentDescription = if (isCalendarAlert) "Calendar Alert" else "Alarm",
                        tint = textColor,
                        modifier = Modifier.size((screenWidth * 0.06f).dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = alarm,
                        fontSize = (screenWidth * 0.06f).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
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
                        color = textColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Seconds line between hours and minutes - spanning full width
                SecondsLine(
                    modifier = Modifier.fillMaxWidth(),
                    color = textColor
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
                        color = textColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Date at BOTTOM
            Text(
                text = dateText,
                fontSize = (screenWidth * 0.07f).sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
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
                        tint = textColor
                    )
                }
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

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth(secondsProgress)
                .height(4.dp)
                .background(color)
        )
    }
}

private fun Modifier.checkerboardBurnInMask(maskOddPixels: Boolean): Modifier = composed {
    val checkerboardPaint = remember(maskOddPixels) {
        createCheckerboardMaskPaint(maskOddPixels)
    }

    drawWithContent {
        drawContent()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPaint(checkerboardPaint)
        }
    }
}

private fun createCheckerboardMaskPaint(maskOddPixels: Boolean): Paint {
    val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    for (y in 0 until 2) {
        for (x in 0 until 2) {
            val isOddPixel = (x + y) % 2 == 1
            bitmap.setPixel(
                x,
                y,
                if (isOddPixel == maskOddPixels) AndroidColor.BLACK else AndroidColor.TRANSPARENT
            )
        }
    }

    return Paint().apply {
        shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        isAntiAlias = false
        isFilterBitmap = false
    }
}

private const val burnInProtectionCycleMillis = 300000L
