package com.example

import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.AppConfig
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PrayerTimesCalculator
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule alarms on start
        AlMinshawiAlarmReceiver.scheduleAlarms(applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel by viewModels()
                val config by viewModel.configState.collectAsStateWithLifecycle()
                
                var currentTab by remember { mutableStateOf("prayers") } // prayers, settings, dua

                if (!config.isOnboardingCompleted) {
                    OnboardingScreen(
                        onGetStarted = { pinInput ->
                            viewModel.updatePinCode(pinInput)
                            viewModel.completeOnboarding()
                            AlMinshawiAlarmReceiver.scheduleAlarms(applicationContext)
                        }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            BottomNavigationBar(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                "prayers" -> PrayersDashboard(viewModel = viewModel)
                                "settings" -> SettingsScreen(viewModel = viewModel)
                                "dua" -> DuaScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== COMPONENTS ====================

@Composable
fun OnboardingScreen(onGetStarted: (String) -> Unit) {
    val context = LocalContext.current
    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, AlMinshawiAdminReceiver::class.java)

    var pinText by remember { mutableStateOf("1234") }
    var step by remember { mutableStateOf(1) } // 1: Welcome/Pin, 2: Permissions (Device Admin, Alarm)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF004D4D),
                        Color(0xFF006A6A),
                        Color(0xFF191C1C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            // Header visual
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFCCE8E8), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF006A6A),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "مرحبًا بك في تطبيق المنشاوي",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "منبه صلوات ذكي متكامل بصوت الشيخ المنشاوي مع نظام حماية كلي لمنع تفويت الصلاة وتعطيل الحذف اليدوي.",
                    fontSize = 14.sp,
                    color = Color(0xFFCCE8E8),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Step Content
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color(0xFFDCE5E5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (step == 1) {
                        Text(
                            text = "تعيين رقم حماية سري",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006A6A),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "أدخل رقمًا سريًا للتطبيق لمنع إيقاف التنبيه أو إلغاء التثبيت من قبل الآخرين.",
                            fontSize = 12.sp,
                            color = Color(0xFF3F4948),
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { if (it.length <= 8) pinText = it },
                            label = { Text("الرقم السري للتثبيت") },
                            placeholder = { Text("1234") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF006A6A),
                                unfocusedBorderColor = Color(0xFFDCE5E5),
                                focusedLabelColor = Color(0xFF006A6A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "تفعيل حمايات الأمان لمنع الحذف الكلي",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006A6A),
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "للحفاظ على أمان أداء المنبه وعدم إلغاء تثبيته يدويًا، يُرجى تمكين وضع مدير جهاز النظام.",
                            fontSize = 12.sp,
                            color = Color(0xFF3F4948),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "يرجى تمكين هذه الحماية لجعل تطبيق المنشاوي تطبيقًا أساسيًا محميًا لعدم حذفه يدويًا."
                                    )
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE932C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل حماية النظام (إجباري)", color = Color.White)
                        }

                        // Exact Alarm configurations guidance for modern Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Button(
                                onClick = {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    if (!alarmManager.canScheduleExactAlarms()) {
                                        val intent = Intent().apply {
                                            action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "إذن التنبيه الدقيق مفعّل بالفعل!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003527)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إذن المنبهات الدقيقة", color = Color.White)
                            }
                        }
                    }
                }
            }

            // CTAs
            Button(
                onClick = {
                    if (step == 1) {
                        if (pinText.isEmpty()) {
                            Toast.makeText(context, "الرجاء تحديد رقم سري متفرد!", Toast.LENGTH_SHORT).show()
                        } else {
                            step = 2
                        }
                    } else {
                        onGetStarted(pinText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE932C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (step == 1) "التالي" else "ابدأ الآن",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PrayersDashboard(viewModel: MainViewModel) {
    val times by viewModel.prayerTimesToday.collectAsStateWithLifecycle()
    val countdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val nextPrayerName by viewModel.nextPrayerName.collectAsStateWithLifecycle()
    val nextPrayerTime by viewModel.nextPrayerTime.collectAsStateWithLifecycle()
    val config by viewModel.configState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Formatting date
    val sdf = SimpleDateFormat("EEEE، d MMMM", Locale("ar"))
    val todayFormatted = sdf.format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9F9FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Digital Clock Countdown Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF003527), Color(0xFF064E3B))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الصلاة القادمة: $nextPrayerName",
                            color = Color(0xFF80BEA6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "وقت الأذان " + nextPrayerTime,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x20FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFE932C),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color(0x30FFFFFF))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "الوقت المتبقي للأذان الكلي",
                            color = Color(0xFF80BEA6),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = countdown,
                            color = Color(0xFFFE932C),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Button(
                        onClick = {
                            val triggerIntent = Intent(context, AlMinshawiAlarmReceiver::class.java).apply {
                                putExtra("PRAYER_NAME", "تجربة")
                            }
                            context.sendBroadcast(triggerIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE932C)),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تجربة الصوت الأقصى", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Location & calculation methods bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF003527))
                Column {
                    Text("الموقع والوقت الحالي", fontSize = 12.sp, color = Color.Gray)
                    Text(config.locationName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003527))
                }
            }
            Text(
                text = if (config.calculationMethod == "UmmAlQura") "أم القرى" else "الهيئة المصرية",
                color = Color(0xFFFE932C),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Prayer Timings List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مواقيت الصلاة لليوم",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF003527)
            )
            Text(
                text = todayFormatted,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Table List of Prayers loaded offline
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                times?.let { t ->
                    PrayerRow(name = "الفجر", time = t.fajr, isEnabled = config.isFajrEnabled, onToggle = { viewModel.togglePrayerAlarm("الفجر", it) })
                    Divider(color = Color(0x10000000))
                    PrayerRow(name = "الشروق", time = t.sunrise, isEnabled = false, onToggle = {}, showToggle = false)
                    Divider(color = Color(0x10000000))
                    PrayerRow(name = "الظهر", time = t.dhuhr, isEnabled = config.isDhuhrEnabled, onToggle = { viewModel.togglePrayerAlarm("الظهر", it) })
                    Divider(color = Color(0x10000000))
                    PrayerRow(name = "العصر", time = t.asr, isEnabled = config.isAsrEnabled, onToggle = { viewModel.togglePrayerAlarm("العصر", it) })
                    Divider(color = Color(0x10000000))
                    PrayerRow(name = "المغرب", time = t.maghrib, isEnabled = config.isMaghribEnabled, onToggle = { viewModel.togglePrayerAlarm("المغرب", it) })
                    Divider(color = Color(0x10000000))
                    PrayerRow(name = "العشاء", time = t.isha, isEnabled = config.isIshaEnabled, onToggle = { viewModel.togglePrayerAlarm("العشاء", it) })
                } ?: Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF003527))
                }
            }
        }

        // Daily spiritual quote & shortcuts (Asymmetric cards)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE7EEFF), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = Color(0xFF003527))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "«أَقِمِ الصَّلَاةَ لِدُلُوكِ الشَّمْسِ إِلَىٰ غَسَقِ اللَّيْلِ وَقُرْآنَ الْفَجْرِ ۖ إِنَّ قُرْآنَ الْفَجْرِ كَانَ مَشْهُودًا»",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003527),
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "سورة الإسراء - آية ٧٨",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerRow(
    name: String,
    time: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    showToggle: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isEnabled) Color(0xFFE7EEFF) else Color(0x10000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (name == "الفجر" || name == "العشاء") Icons.Default.Bedtime else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = if (isEnabled) Color(0xFF003527) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003527)
                )
                Text(
                    text = if (name == "الفجر") "Fajr" else if (name == "الظهر") "Dhuhr" else if (name == "العصر") "Asr" else if (name == "المغرب") "Maghrib" else if (name == "العشاء") "Isha" else "Sunrise",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = time,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF003527)
            )
            if (showToggle) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFE932C),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0x20000000)
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val config by viewModel.configState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pinPromptOpen by remember { mutableStateOf(false) }
    var changePinOpen by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var targetCommitmentState by remember { mutableStateOf(false) }
    var pinVerifyError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9F9FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "الإعدادات العامة",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF003527)
        )

        // Muadhan choice card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE7EEFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF003527))
                    }
                    Column {
                        Text("القارئ المختار للتنبيه", fontSize = 14.sp, color = Color.Gray)
                        Text("الشيخ محمد صديق المنشاوي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003527))
                    }
                }
                Text("ثابت", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Commitment mode toggle card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF003527)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFFFE932C))
                    Text(
                        text = "وضع الالتزام الكلي",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "يضمن وصول الصوت لأقصى درجة، ويمنع إغلاق التنبيه أو إلغاء تثبيت التطبيق بسهولة لضمان عدم تفويت أي صلاة.",
                    color = Color(0xFF80BEA6),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تنشيط الحماية الكلية", color = Color.White, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = config.isCommitmentModeEnabled,
                        onCheckedChange = { isChecked ->
                            if (!isChecked) {
                                // Enforce password checking to disable the state
                                targetCommitmentState = false
                                pinPromptOpen = true
                            } else {
                                viewModel.toggleCommitmentMode(true)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFE932C),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0x30FFFFFF)
                        )
                    )
                }
            }
        }

        // Coordinates & calculations
        Text("الموقع وطريقة الحساب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003527))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Method choose
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("طريقة الحساب الرياضية", fontWeight = FontWeight.Bold, color = Color(0xFF003527))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateCalculationMethod("UmmAlQura") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.calculationMethod == "UmmAlQura") Color(0xFF003527) else Color(0x10000000)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                "أم القرى",
                                color = if (config.calculationMethod == "UmmAlQura") Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { viewModel.updateCalculationMethod("Egyptian") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.calculationMethod == "Egyptian") Color(0xFF003527) else Color(0x10000000)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                "الهيئة المصرية",
                                color = if (config.calculationMethod == "Egyptian") Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = Color(0x10000000))

                // Custom Coordinate Setter (Quick Riyadh / Cairo Quick switchers)
                Text("تعديل الموقع الجغرافي يدويًا", fontWeight = FontWeight.Bold, color = Color(0xFF003527))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.updateCoords(24.7136, 46.6753, "الرياض، المملكة العربية السعودية") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.customLatitude == 24.7136) Color(0xFFFE932C) else Color(0x10000000)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "الرياض",
                            color = if (config.customLatitude == 24.7136) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { viewModel.updateCoords(30.0444, 31.2357, "القاهرة، جمهورية مصر العربية") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.customLatitude == 30.0444) Color(0xFFFE932C) else Color(0x10000000)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "القاهرة",
                            color = if (config.customLatitude == 30.0444) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Security Configurations card
        Text("حمايات وتأمينات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003527))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Change PIN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { changePinOpen = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تغيير رقم الحماية السري", color = Color(0xFF003527), fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }

                Divider(color = Color(0x10000000))

                // Request device policy manager active checks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                val adminComponent = ComponentName(context, AlMinshawiAdminReceiver::class.java)
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "تفعيل مدير الجهاز يمنع أي شخص من حذف تطبيق المنشاوي للحفاظ على استمرار نداء الصلاة."
                                )
                            }
                            context.startActivity(intent)
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التحقق من حماية التطبيق ضد الحذف", color = Color(0xFF003527), fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFFFE932C), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal PIN verification to disable Commitment mode features
    if (pinPromptOpen) {
        AlertDialog(
            onDismissRequest = { pinPromptOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentPinInput == config.pinCode) {
                            viewModel.toggleCommitmentMode(false)
                            pinPromptOpen = false
                            currentPinInput = ""
                            pinVerifyError = false
                        } else {
                            pinVerifyError = true
                        }
                    }
                ) {
                    Text("تحقق", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pinPromptOpen = false }) {
                    Text("إلغاء")
                }
            },
            title = { Text("مطلوب رمز الحماية للتعطيل", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("الرجاء إدخال رقم PIN لتأكيد رغبتك في تعطيل وضع الالتزام الكلي.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = {
                            currentPinInput = it
                            pinVerifyError = false
                        },
                        label = { Text("الرقم السري") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinVerifyError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinVerifyError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الرمز أدخل خاطئ!", color = Color(0xFFBA1A1A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // Modal to change current security PIN
    if (changePinOpen) {
        AlertDialog(
            onDismissRequest = { changePinOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPinInput.isNotEmpty()) {
                            viewModel.updatePinCode(newPinInput)
                            changePinOpen = false
                            newPinInput = ""
                            Toast.makeText(context, "تم تغيير رقم الحماية بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("حفظ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { changePinOpen = false }) {
                    Text("إلغاء")
                }
            },
            title = { Text("تغير رمز الحماية PIN", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 8) newPinInput = it },
                    label = { Text("الرمز السري الجديد") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
fun DuaScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9F9FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "الأذكار والتحصينات",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF003527)
        )

        DuaCard(title = "أذكار الصباح", body = "أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.")
        DuaCard(title = "أذكار المساء", body = "أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.")
        DuaCard(title = "دعاء بعد الأذان بصوت المنشاوي", body = "اللهم رب هذه الدعوة التامة، والصلاة القائمة، آت محمدًا الوسيلة والفضيلة، وابعثه مقامًا محمودًا الذي وعدته.")
    }
}

@Composable
fun DuaCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFE932C))
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, fontSize = 14.sp, color = Color(0xFF003527), lineHeight = 22.sp, textAlign = TextAlign.Justify)
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "prayers",
            onClick = { onTabSelected("prayers") },
            icon = { Icon(imageVector = Icons.Default.AccessTime, contentDescription = null) },
            label = { Text("الصلوات") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFE932C),
                selectedTextColor = Color(0xFFFE932C),
                indicatorColor = Color(0xFFE7EEFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = currentTab == "dua",
            onClick = { onTabSelected("dua") },
            icon = { Icon(imageVector = Icons.Default.MenuBook, contentDescription = null) },
            label = { Text("الأذكار") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFE932C),
                selectedTextColor = Color(0xFFFE932C),
                indicatorColor = Color(0xFFE7EEFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
            label = { Text("الإعدادات") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFE932C),
                selectedTextColor = Color(0xFFFE932C),
                indicatorColor = Color(0xFFE7EEFF),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}
