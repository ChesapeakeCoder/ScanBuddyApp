package com.example.scanbuddy
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt


enum class ScanType(
    val title: String,
    val emoji: String,
    val soundResName: String,
    @DrawableRes val imageRes: Int
) {
    MRI("MRI", "🧲", "mri_demo", R.drawable.bear_mri),
    CT("CT Scan", "💫", "ct_demo", R.drawable.bear_ct),
    XRAY("X-Ray", "⚡", "xray_demo", R.drawable.bear_xray),
    ULTRASOUND("Ultrasound", "\uD83D\uDC42", "ultrasound_demo", R.drawable.bear_ultrasound);
}

val scanSlides: Map<ScanType, List<String>> = mapOf(
    ScanType.MRI to listOf(
        "An MRI uses a magnet to take pictures inside your body. It does make loud sounds like BEEP and WHOOSH! You can tap the Play/Stop Sounds button to hear what it’s like so you know what to expect.",
        "You’ll lie very still on a bed while it moves into the tunnel. The machine takes pictures without touching you. Staying still helps the pictures turn out clear. Practice staying still with the Stay Still Challenge!",
        "You’ve got this! The sounds might seem funny or loud, but they can’t hurt you. Close your eyes, think of something happy, and remember, your ScanBuddy is proud of you!"
    ),
    ScanType.CT to listOf(
        "A CT scanner looks like a big donut! It takes pictures of the inside of your body using soft whirring sounds. Tap the Play/Stop Sounds button to hear what the machine might sound like.",
        "You’ll lie on a small bed that slides through the donut. It’s super quick! The best way to help is to stay still, just like in the Stay Still Challenge.",
        "Nice work! You’ll be in and out before you know it. Keep calm, stay still, and you’ll be helping your care team do their best work!"
    ),
    ScanType.XRAY to listOf(
        "An X-ray takes quick pictures of your bones using a special camera. It’s fast and pretty quiet, no loud noises! You can press the Play/Stop Sounds button to hear a soft camera click.",
        "You might be asked to stand up, sit, or lie down while the picture is taken. Try not to wiggle, just like in the Stay Still Challenge. It only takes a moment!",
        "X-rays are quick and safe. You are doing a great job being brave!"
    ),
    ScanType.ULTRASOUND to listOf(
        "An ultrasound uses sound waves to make pictures of the inside of your body. It’s quiet and gentle. You can press the Play/Stop Sounds button to hear a soft humming sound like the machine makes.",
        "A slippery gel goes on your skin so the camera can glide around. You might see wavy shapes on the screen! Stay still, just like in the Stay Still Challenge, to help make clear pictures.",
        "Ultrasounds don’t hurt, and the gel gets cleaned up quickly. You’ll do amazing, just relax and know ScanBuddy is cheering you on!"
    )
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScanBuddyApp() }
    }
}


@Composable
fun WelcomeLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val logoWidth = (screenWidthDp * 0.75f).dp
    val maxHeight = 340.dp

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(R.drawable.scanbuddy_logo)
            .allowHardware(false)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "Animated ScanBuddy Logo",
        modifier = Modifier
            .width(logoWidth)
            .heightIn(max = maxHeight)
            .then(modifier)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBuddyApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "welcome"

    val title = when {
        route == "welcome" -> "ScanBuddy"
        route == "choose" -> "Select your scan:"
        route.startsWith("story/") -> "Scan Guide"
        route.startsWith("practice/") -> "Stay Still Challenge"
        route == "congrats" -> "Great job!"
        else -> "ScanBuddy"
    }

    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(
            topBar = {
                if (route != "welcome") {
                    CenterAlignedTopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    nav.navigate("welcome") {
                                        popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = "Home"
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Surface(Modifier.fillMaxSize().padding(innerPadding)) {
                NavHost(navController = nav, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(onStart = { nav.navigate("choose") }) }
                    composable("choose") {
                        ChooseScanScreen(onSelect = { type -> nav.navigate("story/${type.name}") })
                    }
                    composable("story/{type}") { backStackEntry ->
                        val typeArg = backStackEntry.arguments?.getString("type") ?: "MRI"
                        val type = runCatching { ScanType.valueOf(typeArg) }.getOrDefault(ScanType.MRI)
                        StoryScreen(
                            type = type,
                            onPractice = { nav.navigate("practice/${type.name}") },
                            onDone = { nav.navigate("congrats") }
                        )
                    }
                    composable("practice/{type}") { backStackEntry ->
                        val typeArg = backStackEntry.arguments?.getString("type") ?: "MRI"
                        val type = runCatching { ScanType.valueOf(typeArg) }.getOrDefault(ScanType.MRI)
                        StillnessPracticeScreen(
                            title = "Stay Still Challenge",
                            secondsGoal = 15,
                            hint = when (type) {
                                ScanType.MRI -> "Be still like a statue in the tunnel!"
                                ScanType.CT -> "Be still like a statue in the donut!"
                                ScanType.XRAY -> "Freeze for a quick photo!"
                                ScanType.ULTRASOUND -> "Relax and watch the wavy movie!"
                            },
                            onFinished = { nav.navigate("congrats") }
                        )
                    }
                    composable("congrats") { CongratsScreen(onRestart = { nav.navigate("choose") }) }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        WelcomeLogo(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Hi, I’m Your ScanBuddy!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "I’ll help you get ready for your scan.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onStart) { Text("Let’s Begin!") }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "ScanBuddy is designed to help children learn what to expect during medical imaging.\nIt’s for educational and comfort purposes only and does not replace medical advice or care from your healthcare provider.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun ChooseScanScreen(onSelect: (ScanType) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ScanType.entries.toList()) { type ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable { onSelect(type) }
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

        Image(
            painter = painterResource(id = R.drawable.scanbuddybear),
            contentDescription = "ScanBuddy bear",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.5f)
                .aspectRatio(1.5f)
                .offset(y = 40.dp)
        )
    }
}

@Composable
fun StoryScreen(type: ScanType, onPractice: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    var currentSlide by remember { mutableIntStateOf(0) }
    val slides = scanSlides[type] ?: emptyList()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.release(); mediaPlayer = null } }

    fun toggleSound() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        } else {
            val resId =
                context.resources.getIdentifier(type.soundResName, "raw", context.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${type.title}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        ElevatedCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(type.imageRes),
                    contentDescription = "${type.title} demo image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Text(
                    slides.getOrNull(currentSlide) ?: "",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (currentSlide == 0) {
                    FilledTonalButton(onClick = { toggleSound() }) {
                        Text("Play/Stop Sounds")
                    }
                }
                if (currentSlide == 1) {
                    FilledTonalButton(onClick = onPractice) {
                        Text("Try the Stay Still Challenge →")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { if (currentSlide > 0) currentSlide-- },
                enabled = currentSlide > 0
            ) { Text("Back") }

            Button(onClick = { if (currentSlide < slides.lastIndex) currentSlide++ else onDone() }) {
                Text(if (currentSlide < slides.lastIndex) "Next" else "Finish")
            }
        }
    }
}

@Composable
fun StillnessPracticeScreen(
    title: String,
    secondsGoal: Int,
    hint: String,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var stillSeconds by remember { mutableFloatStateOf(0f) }
    var isStill by remember { mutableStateOf(true) }
    var started by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    val movementThreshold = 0.7f

    DisposableEffect(accelerometer) {
        if (accelerometer == null) return@DisposableEffect onDispose {}
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (ax, ay, az) = event.values
                val magnitude = sqrt(ax * ax + ay * ay + az * az) - 9.81f
                isStill = abs(magnitude) < movementThreshold
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(started, isStill) {
        while (started && !finished) {
            delay(1000)
            if (accelerometer == null) {
                stillSeconds += 1f
            } else if (isStill) {
                stillSeconds += 1f
            } else {
                stillSeconds = 0f
            }
            if (stillSeconds >= secondsGoal) {
                finished = true
                onFinished()
            }
        }
    }

    val progress by animateFloatAsState(
        targetValue = (stillSeconds / secondsGoal).coerceIn(0f, 1f),
        animationSpec = tween(400, easing = LinearEasing),
        label = "progress"
    )

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(hint, textAlign = TextAlign.Center)
        if (accelerometer == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "(No motion sensor detected — we’ll use a simple $secondsGoal‑second timer)",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Stay still: ${stillSeconds.toInt()} / $secondsGoal s",
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(if (isStill || accelerometer == null) "Great job. Keep going!" else "Oops! Try to stay as still as a statue.")
    }
}

@Composable
fun CongratsScreen(onRestart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("You did it, Buddy!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("You’re ready for your scan.", fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRestart) { Text("Start Over") }
    }
}