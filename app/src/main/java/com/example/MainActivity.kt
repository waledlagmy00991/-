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
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryContainerTeal
import com.example.ui.theme.OnPrimaryTeal
import com.example.ui.theme.OnPrimaryContainerTeal
import com.example.ui.theme.NeutralDark
import com.example.ui.theme.NeutralMedium
import com.example.ui.theme.NeutralLight
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.LightGrayContainer
import com.example.ui.theme.MutedDivider
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorContainerRed
import androidx.compose.foundation.Canvas
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
                        PrimaryTeal,
                        PrimaryTeal,
                        NeutralDark
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
                        .background(PrimaryContainerTeal, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryTeal,
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
                    color = PrimaryContainerTeal,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Step Content
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, NeutralBorder),
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
                            color = PrimaryTeal,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "أدخل رقمًا سريًا للتطبيق لمنع إيقاف التنبيه أو إلغاء التثبيت من قبل الآخرين.",
                            fontSize = 12.sp,
                            color = NeutralMedium,
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
                                focusedBorderColor = PrimaryTeal,
                                unfocusedBorderColor = NeutralBorder,
                                focusedLabelColor = PrimaryTeal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "تفعيل حمايات الأمان لمنع الحذف الكلي",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "للحفاظ على أمان أداء المنبه وعدم إلغاء تثبيته يدويًا، يُرجى تمكين وضع مدير جهاز النظام.",
                            fontSize = 12.sp,
                            color = NeutralMedium,
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
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
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إذن المنبهات الدقيقة", color = PrimaryTeal)
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
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
            .background(NeutralLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Geometric Design Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                        .background(PrimaryTeal, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "مؤذن المنشاوي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(PrimaryTeal)
                        }
                        Text(
                            text = "نظام الحماية نشط",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryContainerTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = OnPrimaryContainerTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Clock / Countdown Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(PrimaryTeal)
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
                            color = PrimaryContainerTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "وقت الأذان " + nextPrayerTime,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x1BFFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (nextPrayerName == "الفجر" || nextPrayerName == "العشاء") Icons.Default.Bedtime else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color(0x20FFFFFF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x20FFFFFF), RoundedCornerShape(100.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "متبقي $countdown",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "بصوت الشيخ",
                            color = PrimaryContainerTeal,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "محمد صديق المنشاوي",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
        
        when (downloadState) {
            is DownloadState.Downloading -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainerTeal),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryTeal,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Column {
                            Text(
                                text = "جاري تحميل أذان الشيخ المنشاوي...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                            Text(
                                text = "يتم الآن تأمين مكتبة الصوت لتعمل بدون إنترنت وبأقصى دقة.",
                                fontSize = 11.sp,
                                color = NeutralMedium
                            )
                        }
                    }
                }
            }
            is DownloadState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEEBEE)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تعذر تحميل ملف الأذان",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorCrimson
                            )
                            Text(
                                text = "سيتم استخدام التنبيه الافتراضي مؤقتًا. اضغط للتحميل.",
                                fontSize = 11.sp,
                                color = NeutralMedium
                            )
                        }
                        Button(
                            onClick = { viewModel.downloadAthanIfMissing() },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("إعادة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is DownloadState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryContainerTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مكتبة أذان الشيخ المنشاوي نشطة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutralDark
                            )
                            Text(
                                text = "ملف الصوت محمل وجاهز للعمل بالكامل بدون الاتصال بالإنترنت.",
                                fontSize = 11.sp,
                                color = NeutralMedium
                            )
                        }
                    }
                }
            }
            else -> {}
        }

        // Geometric Symmetry Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Volume lock shortcut card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, NeutralBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LightGrayContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = NeutralMedium,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "مستوى الصوت",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralDark
                        )
                        Text(
                            text = "مُثبَّت على الأقصى",
                            fontSize = 11.sp,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Anti-deletion / system stabilization card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, NeutralBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LightGrayContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeutralMedium,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تثبيت النظام",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralDark
                        )
                        Text(
                            text = "غير قابل للحذف",
                            fontSize = 11.sp,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Secondary feature lock switches strip
        Card(
            colors = CardDefaults.cardColors(containerColor = LightGrayContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(PrimaryTeal)
                        }
                        Text(
                            text = "إغلاق التطبيق برقم سري",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeutralDark
                        )
                    }
                    Switch(
                        checked = config.pinCode.isNotEmpty(),
                        onCheckedChange = {},
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryTeal,
                            disabledCheckedTrackColor = PrimaryTeal.copy(alpha = 0.5f),
                            disabledCheckedThumbColor = Color.White
                        )
                    )
                }

                Divider(color = MutedDivider, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .border(1.dp, NeutralMedium, CircleShape)
                        )
                        Text(
                            text = "منع إطفاء الهاتف وقت الأذان",
                            fontSize = 14.sp,
                            color = NeutralMedium
                        )
                    }
                    Switch(
                        checked = false,
                        onCheckedChange = {},
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor = MutedDivider,
                            uncheckedTrackColor = LightGrayContainer
                        )
                    )
                }
            }
        }

        // Test Speaker Playback Feedback Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, NeutralBorder, RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "مستوى التنبيه ومستوى الرنين",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralDark
                )
            }
            Button(
                onClick = {
                    val triggerIntent = Intent(context, AlMinshawiAlarmReceiver::class.java).apply {
                        putExtra("PRAYER_NAME", "تجربة")
                    }
                    context.sendBroadcast(triggerIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerTeal),
                shape = RoundedCornerShape(50.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("تجربة التنبيه", fontSize = 11.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold)
            }
        }

        // Location & calculation details strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, NeutralBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal)
                Column {
                    Text("الموقع والوقت الحالي", fontSize = 12.sp, color = NeutralMedium)
                    Text(config.locationName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeutralDark)
                }
            }
            Text(
                text = if (config.calculationMethod == "UmmAlQura") "أم القرى" else "الهيئة المصرية",
                color = PrimaryTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Timing list header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مواقيت الصلاة لليوم",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NeutralDark
            )
            Text(
                text = todayFormatted,
                fontSize = 12.sp,
                color = NeutralMedium
            )
        }

        // Timing Table Card Layout
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeutralBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                times?.let { t ->
                    PrayerRow(name = "الفجر", time = t.fajr, isEnabled = config.isFajrEnabled, onToggle = { viewModel.togglePrayerAlarm("الفجر", it) })
                    Divider(color = NeutralBorder)
                    PrayerRow(name = "الشروق", time = t.sunrise, isEnabled = false, onToggle = {}, showToggle = false)
                    Divider(color = NeutralBorder)
                    PrayerRow(name = "الظهر", time = t.dhuhr, isEnabled = config.isDhuhrEnabled, onToggle = { viewModel.togglePrayerAlarm("الظهر", it) })
                    Divider(color = NeutralBorder)
                    PrayerRow(name = "العصر", time = t.asr, isEnabled = config.isAsrEnabled, onToggle = { viewModel.togglePrayerAlarm("العصر", it) })
                    Divider(color = NeutralBorder)
                    PrayerRow(name = "المغرب", time = t.maghrib, isEnabled = config.isMaghribEnabled, onToggle = { viewModel.togglePrayerAlarm("المغرب", it) })
                    Divider(color = NeutralBorder)
                    PrayerRow(name = "العشاء", time = t.isha, isEnabled = config.isIshaEnabled, onToggle = { viewModel.togglePrayerAlarm("العشاء", it) })
                } ?: Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            }
        }

        // Spiritual Quote
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryContainerTeal.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .border(1.dp, PrimaryContainerTeal, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = PrimaryTeal)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "«أَقِمِ الصَّلَاةَ لِدُلُوكِ الشَّمْسِ إِلَىٰ غَسَقِ اللَّيْلِ وَقُرْآنَ الْفَجْرِ ۖ إِنَّ قُرْآنَ الْفَجْرِ كَانَ مَشْهُودًا»",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "سورة الإسراء - آية ٧٨",
                    fontSize = 12.sp,
                    color = NeutralMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    .background(if (isEnabled) PrimaryContainerTeal else LightGrayContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (name == "الفجر" || name == "العشاء") Icons.Default.Bedtime else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = if (isEnabled) PrimaryTeal else NeutralMedium,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralDark
                )
                Text(
                    text = if (name == "الفجر") "Fajr" else if (name == "الظهر") "Dhuhr" else if (name == "العصر") "Asr" else if (name == "المغرب") "Maghrib" else if (name == "العشاء") "Isha" else "Sunrise",
                    fontSize = 10.sp,
                    color = NeutralMedium
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
                color = NeutralDark
            )
            if (showToggle) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryTeal,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = LightGrayContainer
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
            .background(NeutralLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "الإعدادات العامة",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryTeal
        )

        // Muadhan choice card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeutralBorder)
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
                            .background(PrimaryContainerTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = PrimaryTeal)
                    }
                    Column {
                        Text("القارئ المختار للتنبيه", fontSize = 14.sp, color = NeutralMedium)
                        Text("الشيخ محمد صديق المنشاوي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeutralDark)
                    }
                }
                Text("ثابت", color = NeutralMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Commitment mode toggle card
        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryTeal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = PrimaryContainerTeal)
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
                    color = PrimaryContainerTeal,
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
                            checkedThumbColor = PrimaryTeal,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0x30FFFFFF)
                        )
                    )
                }
            }
        }

        // Coordinates & calculations headings
        Text("الموقع وطريقة الحساب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeutralBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Method choose options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("طريقة الحساب الرياضية", fontWeight = FontWeight.Bold, color = NeutralDark)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateCalculationMethod("UmmAlQura") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.calculationMethod == "UmmAlQura") PrimaryTeal else LightGrayContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                "أم القرى",
                                color = if (config.calculationMethod == "UmmAlQura") Color.White else NeutralMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { viewModel.updateCalculationMethod("Egyptian") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.calculationMethod == "Egyptian") PrimaryTeal else LightGrayContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                "الهيئة المصرية",
                                color = if (config.calculationMethod == "Egyptian") Color.White else NeutralMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = NeutralBorder)

                // Custom Coordinate Setter (Quick Riyadh / Cairo Quick switchers)
                Text("تعديل الموقع الجغرافي يدويًا", fontWeight = FontWeight.Bold, color = NeutralDark)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.updateCoords(24.7136, 46.6753, "الرياض، المملكة العربية السعودية") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.customLatitude == 24.7136) PrimaryTeal else LightGrayContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "الرياض",
                            color = if (config.customLatitude == 24.7136) Color.White else NeutralMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { viewModel.updateCoords(30.0444, 31.2357, "القاهرة، جمهورية مصر العربية") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.customLatitude == 30.0444) PrimaryTeal else LightGrayContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "القاهرة",
                            color = if (config.customLatitude == 30.0444) Color.White else NeutralMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Security Configurations card
        Text("حمايات وتأمينات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeutralBorder)
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
                    Text("تغيير رقم الحماية السري", color = NeutralDark, fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = NeutralMedium, modifier = Modifier.size(18.dp))
                }

                Divider(color = NeutralBorder)

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
                    Text("التحقق من حماية التطبيق ضد الحذف", color = NeutralDark, fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
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
                    Text("تحقق", color = ErrorCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pinPromptOpen = false }) {
                    Text("إلغاء", color = NeutralDark)
                }
            },
            title = { Text("مطلوب رمز الحماية للتعطيل", fontWeight = FontWeight.Bold, color = NeutralDark) },
            text = {
                Column {
                    Text("الرجاء إدخال رقم PIN لتأكيد رغبتك في تعطيل وضع الالتزام الكلي.", fontSize = 13.sp, color = NeutralMedium)
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = NeutralBorder,
                            focusedLabelColor = PrimaryTeal,
                            errorBorderColor = ErrorCrimson
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinVerifyError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الرمز أدخل خاطئ!", color = ErrorCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    Text("حفظ", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { changePinOpen = false }) {
                    Text("إلغاء", color = NeutralDark)
                }
            },
            title = { Text("تغير رمز الحماية PIN", fontWeight = FontWeight.Bold, color = NeutralDark) },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 8) newPinInput = it },
                    label = { Text("الرمز السري الجديد") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = NeutralBorder,
                        focusedLabelColor = PrimaryTeal
                    ),
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
            .background(NeutralLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "الأذكار والتحصينات",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryTeal
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
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NeutralBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, fontSize = 14.sp, color = NeutralDark, lineHeight = 22.sp, textAlign = TextAlign.Justify)
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
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                indicatorColor = PrimaryContainerTeal,
                unselectedIconColor = NeutralMedium,
                unselectedTextColor = NeutralMedium
            )
        )
        NavigationBarItem(
            selected = currentTab == "dua",
            onClick = { onTabSelected("dua") },
            icon = { Icon(imageVector = Icons.Default.MenuBook, contentDescription = null) },
            label = { Text("الأذكار") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                indicatorColor = PrimaryContainerTeal,
                unselectedIconColor = NeutralMedium,
                unselectedTextColor = NeutralMedium
            )
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
            label = { Text("الإعدادات") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryTeal,
                selectedTextColor = PrimaryTeal,
                indicatorColor = PrimaryContainerTeal,
                unselectedIconColor = NeutralMedium,
                unselectedTextColor = NeutralMedium
            )
        )
    }
}
