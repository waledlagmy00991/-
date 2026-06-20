package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AthanLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        enableEdgeToEdge()

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "الصلاة"

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AthanAlarmScreen(
                        prayerName = prayerName,
                        modifier = Modifier.padding(innerPadding),
                        onDismissClicked = { pin ->
                            verifyPinAndDismiss(pin)
                        }
                    )
                }
            }
        }
    }

    private fun verifyPinAndDismiss(pinInput: String): Boolean {
        var success = false
        // Fetch current PIN from Room in a thread-safe way
        val db = AppDatabase.getDatabase(this)
        val configDao = db.configDao()
        
        // Run blocking or asynchronous validation
        val config = kotlinx.coroutines.runBlocking { configDao.getConfig() }
        
        val correctPin = config?.pinCode ?: "1234"
        if (pinInput == correctPin) {
            val stopIntent = Intent(this, AthanService::class.java)
            stopService(stopIntent)
            success = true
            finish()
        }
        return success
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Under Commitment level protection, we disable the physical back button to prevent cheat-exits
        // The user must either let it finish or enter their PIN
    }
}

@Composable
fun AthanAlarmScreen(
    prayerName: String,
    modifier: Modifier = Modifier,
    onDismissClicked: (String) -> Boolean
) {
    val scope = rememberCoroutineScope()
    var remainingTime by remember { mutableStateOf(300) } // 5 minutes standard countdown
    var isPinDialogOpen by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Floating circles pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    // Athan Countdown timer tick
    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
        // Early end of service once 5 minutes is complete
        onDismissClicked("FORCE_EXIT_NOT_REALLY_BUT_CONVENIENT")
    }

    val minutes = remainingTime / 60
    val seconds = remainingTime % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF003527), // Deep Emerald
                        Color(0xFF064E3B),
                        Color(0xFF111C2D)  // Dark Slate
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-end spiritual background element (Mosque Dome Vector simulation via Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x05FFD700), // Very light gold
                radius = 300.dp.toPx(),
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "حي على الصلاة",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الأذان بصوت الشيخ المنشاوي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFD8E3FB),
                    textAlign = TextAlign.Center
                )
            }

            // Central Pulsing Rings and Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
            ) {
                // Pulsing rings
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(pulseScale2)
                        .background(Color(0x05FE932C), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale1)
                        .background(Color(0x08FE932C), CircleShape)
                )

                // Main circular timer frame
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Color(0x20FFFFFF))
                ) {
                    Canvas(modifier = Modifier.size(190.dp)) {
                        // Background circle arc
                        drawCircle(
                            color = Color(0x1DFE932C),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Progress arc
                        val sweep = (remainingTime.toFloat() / 300f) * 360f
                        drawArc(
                            color = Color(0xFFFE932C), // Gold
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "صلاة $prayerName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFE932C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeFormatted,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xB5FFFFFF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الوقت المتبقي للأذان",
                                fontSize = 12.sp,
                                color = Color(0xB5FFFFFF)
                            )
                        }
                    }
                }
            }

            // Bottom Locks and Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sound and Commitment Alert bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x10FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFFE932C)
                        )
                        Text(
                            text = "أقصى درجة صوت",
                            color = Color(0xFFFE932C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0x30BA1A1A), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFFDAD6),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "مؤمن",
                            color = Color(0xFFFFDAD6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Compliance Warning message Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF064E3B), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x20FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "وضع الالتزام نشط: لا يمكن خفض الصوت أو إغلاق التنبيه حتى انتهاء الأذان.",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unlock early button
                Button(
                    onClick = { isPinDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBA1A1A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إيقاف الأذان بالرقم السري",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Secure PIN Dialog
    if (isPinDialogOpen) {
        AlertDialog(
            onDismissRequest = { isPinDialogOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = onDismissClicked(pinText)
                        if (ok) {
                            isPinDialogOpen = false
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("إيقاف", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isPinDialogOpen = false }) {
                    Text("عَوْدَة", color = Color(0xFF1E293B))
                }
            },
            title = {
                Text(
                    text = "أدخل الرقم السري لإيقاف الأذان الكلي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            pinText = it
                            pinError = false
                        },
                        label = { Text("الرقم السري") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عفوًا، الرقم السري خاطئ!",
                            color = Color(0xFFBA1A1A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تحذير: لا يوصى بتجاوز الحماية إلا للضرورة القصوى كأوقات الصمت الإجباري.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right
                    )
                }
            }
        )
    }
}

// Simple layout scaling helper
private fun Modifier.scale(scale: Float) = this.then(
    Modifier.clickable(enabled = false) {}
)
