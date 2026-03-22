package com.example.batimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.batimer.ui.theme.BatimerTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_Batimer)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            scheduleNextHourAlarm(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDark) }
            var showHelpDialog by remember { mutableStateOf(false) }

            val bgRes = if (isDarkMode) R.drawable.bg_night else R.drawable.bg_day

            BatimerTheme(darkTheme = isDarkMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. 背景图
                    Image(
                        painter = painterResource(id = bgRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 2. 半透明蒙版层
                    val scrimColor = if (isDarkMode) Color.Black.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
                    Spacer(modifier = Modifier.fillMaxSize().background(scrimColor))

                    // 3. UI 内容
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        // 使用 Box 实现左下角定位
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

                            // 主体内容居中
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TimerScreen(
                                    context = this@MainActivity,
                                    showHelpDialog = showHelpDialog,
                                    onShowHelpChange = { showHelpDialog = it }
                                )
                            }

                            // 右上角：切换模式按钮
                            IconButton(
                                onClick = { isDarkMode = !isDarkMode },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // 左下角：帮助按钮
                            val helpBtnColor = MaterialTheme.colorScheme.onSurface
                            TextButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = helpBtnColor.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "使用说明",
                                        fontSize = 14.sp,
                                        color = helpBtnColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // 帮助弹窗
                    if (showHelpDialog) {
                        AlertDialog(
                            onDismissRequest = { showHelpDialog = false },
                            confirmButton = {
                                TextButton(onClick = { showHelpDialog = false }) {
                                    Text("了解", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            title = { Text(text = "使用说明", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("1. 设置早晚两个摸头刷新点（国内为3点和15点）。", fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("2. 咖啡店摸完头点击开始计时。app 会在下次可以摸头时发出提醒。", fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("3. 请确保系统允许本应用显示通知并忽略电池优化，以保证提醒准时。", fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("4. 点击“已摸完”即可实装计划。", fontSize = 14.sp)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun scheduleNextHourAlarm(context: Context) {
            val am = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = "ACTION_EVERY_HOUR" }
            val pi = PendingIntent.getBroadcast(
                context, 20, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextHour = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextHour.timeInMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextHour.timeInMillis, pi)
            }
        }
    }
}

@Composable
fun TimerScreen(context: Context, showHelpDialog: Boolean, onShowHelpChange: (Boolean) -> Unit) {
    // 1. 这里确实没有 var showHelpDialog，很好
    val prefs = remember { context.getSharedPreferences("ba_prefs", Context.MODE_PRIVATE) }
    var mOn by remember { mutableStateOf(prefs.getBoolean("m_on", true)) }
    var eOn by remember { mutableStateOf(prefs.getBoolean("e_on", true)) }
    var sOn by remember { mutableStateOf(prefs.getBoolean("s_on", true)) }
    var vOn by remember { mutableStateOf(prefs.getBoolean("v_on", true)) }
    var h1 by remember { mutableIntStateOf(prefs.getInt("h1", 4)) }
    var h2 by remember { mutableIntStateOf(prefs.getInt("h2", 16)) }
    var target by remember { mutableLongStateOf(prefs.getLong("target", 0L)) }
    var text by remember { mutableStateOf("00:00:00") }

    LaunchedEffect(target) {
        while (target > 0) {
            val remain = target - System.currentTimeMillis()
            if (remain > 0) {
                val h = (remain / 3600000) % 24
                val m = (remain / 60000) % 60
                val s = (remain / 1000) % 60
                text = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            } else {
                target = 0L; prefs.edit { putLong("target", 0L) }
            }
            delay(1000)
        }
    }

    // --- 以下是 UI 部分 ---
    Text("蔚蓝档案 摸头闹钟", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
    Spacer(modifier = Modifier.height(16.dp))
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("距离下次提醒剩余", style = MaterialTheme.typography.labelLarge)
            Text(
                text = text,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary ,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))

    val isRunning = target > 0

    RefreshRow(label = "早刷新点开关", isOn = mOn, hour = h1, enabled = !isRunning,
        onToggle = { mOn = it; prefs.edit { putBoolean("m_on", it) } },
        onVal = { h1 = it; prefs.edit { putInt("h1", it) } }
    )
    RefreshRow(label = "晚刷新点开关", isOn = eOn, hour = h2, enabled = !isRunning,
        onToggle = { eOn = it; prefs.edit { putBoolean("e_on", it) } },
        onVal = { h2 = it; prefs.edit { putInt("h2", it) } }
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = sOn, onCheckedChange = { checked ->
            sOn = checked
            prefs.edit { putBoolean("s_on", checked) }
            if (checked) {
                try {
                    val mp = MediaPlayer.create(context, R.raw.ba_message)
                    mp.setOnCompletionListener { it.release() }
                    mp.start()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }, enabled = !isRunning)
        Text("铃声提醒", color = MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.width(16.dp))

        Checkbox(checked = vOn, onCheckedChange = { checked ->
            vOn = checked
            prefs.edit { putBoolean("v_on", checked) }
            if (checked) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }, enabled = !isRunning)
        Text("震动提醒", color = MaterialTheme.colorScheme.onSurface)
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = {
            if (!isRunning) {
                val next = calculateNextTime(h1, mOn, h2, eOn)
                if (startAlarmTask(context, next)) {
                    target = next; prefs.edit { putLong("target", next) }
                }
            } else {
                cancelAlarmTask(context); target = 0L; text = "00:00:00"; prefs.edit { putLong("target", 0L) }
            }
        },
        modifier = Modifier.height(70.dp).width(220.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    ) { Text(if (isRunning) "取消提醒" else "已摸完，开始计时", fontSize = 20.sp) }

    // --- 重点：在这里使用参数，灰色就会消失 ---
    if (showHelpDialog) { // 👈 这里读取了参数，第一个灰色消失
        AlertDialog(
            onDismissRequest = { onShowHelpChange(false) }, // 👈 这里调用了参数，第二个灰色消失
            confirmButton = {
                TextButton(onClick = { onShowHelpChange(false) }) { // 👈 这里也调用了参数
                    Text("了解", color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text(text = "使用说明", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("1. 设置早晚两个摸头刷新点（通常为4点和16点）。", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("2. 开启开关后，App 会在对应时间发送提醒。", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("3. 请确保系统允许本应用显示通知并忽略电池优化。", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("4. 点击“已摸完”即可实装计划。", fontSize = 14.sp)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Composable
fun RefreshRow(label: String, isOn: Boolean, hour: Int, enabled: Boolean, onToggle: (Boolean) -> Unit, onVal: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Switch(checked = isOn, onCheckedChange = onToggle, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = 8.dp), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hour.toString(),
                onValueChange = { input -> input.toIntOrNull()?.let { onVal(it) } },
                modifier = Modifier.width(60.dp).padding(vertical = 4.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 18.sp),
                singleLine = true
            )
            Text(text = "点", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
    }
}

fun calculateNextTime(h1: Int, e1: Boolean, h2: Int, e2: Boolean): Long {
    val now = Calendar.getInstance()
    val t3h = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 3) }.timeInMillis
    val list = mutableListOf<Long>()
    list.add(t3h)

    fun getMs(h: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        if (cal.before(now)) cal.add(Calendar.DATE, 1)
        return cal.timeInMillis
    }
    if (e1) list.add(getMs(h1))
    if (e2) list.add(getMs(h2))

    var result = list[0]
    for (i in 1 until list.size) { if (list[i] < result) result = list[i] }
    return result
}

fun startAlarmTask(ctx: Context, time: Long): Boolean {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, AlarmReceiver::class.java).apply { action = "ACTION_TIMER_REMIND" }
    val pi = PendingIntent.getBroadcast(ctx, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        return false
    }
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
    Toast.makeText(ctx, "提醒已开启", Toast.LENGTH_SHORT).show()
    return true
}

fun cancelAlarmTask(ctx: Context) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, AlarmReceiver::class.java).apply { action = "ACTION_TIMER_REMIND" }
    val pi = PendingIntent.getBroadcast(ctx, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    am.cancel(pi)
}