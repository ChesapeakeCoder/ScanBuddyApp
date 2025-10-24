// ScanBuddy: Helping Kids Prepare for Medical Imaging
package com.example.scanbuddy

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.delay

// -----------------------------
// Data
// -----------------------------
enum class ScanType(val title: String, val emoji: String, val soundResName: String) {
    MRI("MRI", "🧲", "mri_demo"),
    CT("CT Scan", "💫", "ct_demo"),
    XRAY("X-Ray", "⚡", "xray_demo"),
    ULTRASOUND("Ultrasound", "\uD83D\uDC42", "ultrasound_demo");
}

// Simple copy for per-scan text slides
val scanSlides: Map<ScanType, List<String>> = mapOf(
    ScanType.MRI to listOf(
        "You’ll lie very still like a statue while pictures are taken.",
        "The machine makes loud noises like BEEP and WHOOSH. That’s normal!",
        "It doesn’t hurt. Your job is to stay still."
    ),
    ScanType.CT to listOf(
        "You’ll ride through a big donut-shaped machine.",
        "It’s quick! You might hear soft whirring sounds.",
        "Just like a photo, stay still."
    ),
    ScanType.XRAY to listOf(
        "This is typically fast, just a quick picture of your bones.",
        "You might be asked to stand up or lie down for the picture.",
        "Hold still for as long as asked. Click!"
    ),
    ScanType.ULTRASOUND to listOf(
        "Gel on your skin helps the camera slide.",
        "You might be able to see wavy pictures on screen!",
        "It’s quiet and gentle. Just relax."
    )
)

// -----------------------------
// Activity
// -----------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScanBuddyApp() }
    }
}

// -----------------------------
// App + Navigation
// -----------------------------
@Composable
fun ScanBuddyApp() {
    val nav = rememberNavController()
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            NavHost(navController = nav, startDestination = "welcome") {

                composable("welcome") {
                    WelcomeScreen(onStart = { nav.navigate("choose") })
                }

                composable("choose") {
                    ChooseScanScreen(onSelect = { type ->
                        nav.navigate("story/${type.name}")  // Pass scan type name
                    })
                }

                composable("story/{type}") { backStack ->
                    val typeArg = backStack.arguments?.getString("type") ?: "MRI"
                    val type = runCatching { ScanType.valueOf(typeArg) }.getOrDefault(ScanType.MRI)
                    StoryScreen(
                        type = type,
                        onPractice = { nav.navigate("practice/${type.name}") },
                        onDone = { nav.navigate("congrats") }
                    )
                }

                composable("practice/{type}") { backStack ->
                    val typeArg = backStack.arguments?.getString("type") ?: "MRI"
                    val type = runCatching { ScanType.valueOf(typeArg) }.getOrDefault(ScanType.MRI)
                    StillnessPracticeScreen(
                        title = "Stay Still Challenge",
                        secondsGoal = 15,
                        hint = when (type) {
                            ScanType.MRI -> "Be still like a statue in the tunnel!"
                            ScanType.CT -> "Ride the donut like a statue!"
                            ScanType.XRAY -> "Freeze for a quick photo!"
                            ScanType.ULTRASOUND -> "Relax and watch the wavy movie!"
                        },
                        onFinished = { nav.navigate("congrats") }
                    )
                }

                composable("congrats") {
                    CongratsScreen(onRestart = { nav.navigate("choose") })
                }
            }
        }
    }
}

// -----------------------------
// Screens
// -----------------------------
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hi! I’m ScanBuddy 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("I’ll help you get ready for your scan.", fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart) { Text("Let’s Begin!") }
    }
}

@Composable
fun ChooseScanScreen(onSelect: (ScanType) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose your scan", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ScanType.entries.toList()) { type ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { onSelect(type) } // <-- this makes the cards work!
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(type.emoji, fontSize = 28.sp)
                            Text(type.title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryScreen(type: ScanType, onPractice: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    var currentSlide by remember { mutableStateOf(0) }
    val slides = scanSlides[type] ?: emptyList()

    // Audio player (play/stop demo sound)
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.release(); mediaPlayer = null } }

    fun toggleSound() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        } else {
            val resId = context.resources.getIdentifier(type.soundResName, "raw", context.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${type.title} Guide", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ElevatedCard(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize().padding(20.dp)) {
                Text(slides.getOrNull(currentSlide) ?: "", fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { if (currentSlide > 0) currentSlide-- }, enabled = currentSlide > 0) { Text("Back") }
            FilledTonalButton(onClick = { toggleSound() }) { Text("Play/Stop Sounds") }
            Button(onClick = { if (currentSlide < slides.lastIndex) currentSlide++ else onDone() }) {
                Text(if (currentSlide < slides.lastIndex) "Next" else "Finish")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onPractice) { Text("Try the Stay Still Challenge →") }
    }
}

@Composable
fun StillnessPracticeScreen(title: String, secondsGoal: Int, hint: String, onFinished: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var stillSeconds by remember { mutableStateOf(0f) }
    var isStill by remember { mutableStateOf(true) }
    val movementThreshold = 0.7f // sensitivity

    DisposableEffect(accelerometer) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (ax, ay, az) = event.values
                val magnitude = sqrt(ax*ax + ay*ay + az*az) - 9.81f
                isStill = abs(magnitude) < movementThreshold
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(isStill) {
        while (true) {
            delay(1000)
            if (isStill) {
                stillSeconds += 1f
                if (stillSeconds >= secondsGoal) {
                    onFinished()
                    break
                }
            } else stillSeconds = 0f
        }
    }

    val progress by animateFloatAsState(
        targetValue = (stillSeconds / secondsGoal).coerceIn(0f, 1f),
        animationSpec = tween(400, easing = LinearEasing), label = "progress"
    )

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(hint, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Stay still: ${stillSeconds.toInt()} / $secondsGoal s", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(if (isStill) "Great job. Keep going!" else "Oops! Try to freeze like a statue.")
    }
}

@Composable
fun CongratsScreen(onRestart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("You did it! 🎉", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("You’re ready for your scan.", fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRestart) { Text("Start Over") }
    }
}