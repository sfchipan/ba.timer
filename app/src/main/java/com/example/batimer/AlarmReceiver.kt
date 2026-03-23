package com.example.batimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("ba_prefs", Context.MODE_PRIVATE)

        val mOn = prefs.getBoolean("m_on", false)
        val h1 = prefs.getInt("h1", 3)
        val eOn = prefs.getBoolean("e_on", true)
        val h2 = prefs.getInt("h2", 15)

        val useSound = prefs.getBoolean("s_on", true)
        val useVibrate = prefs.getBoolean("v_on", true)

        if (intent.action == "ACTION_EVERY_HOUR" || intent.action == "android.intent.action.BOOT_COMPLETED") {
            MainActivity.scheduleNextHourAlarm(context)
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isMorningTime = (currentHour == h1 && mOn && (intent.action == "ACTION_EVERY_HOUR"))
        val isEveningTime = (currentHour == h2 && eOn && (intent.action == "ACTION_EVERY_HOUR"))
        val isTimerRemind = (intent.action == "ACTION_TIMER_REMIND")

        if (isMorningTime || isEveningTime || isTimerRemind) {
            val message = if (isTimerRemind) "可以去咖啡厅摸头了~" else "${currentHour}点了，咖啡厅刷新啦！"

            sendSystemNotification(context, message)

            if (useSound || useVibrate) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                if (useSound) {
                    playAlarmSoundTwice(context)
                }

                if (useVibrate) {
                    val vibrator = context.getSystemService("vibrator") as Vibrator
                    vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        }
    }

    private fun playAlarmSoundTwice(context: Context) {
        try {
            val mp = MediaPlayer.create(context, R.raw.ba_message)
            var playCount = 1
            mp.setOnCompletionListener {
                if (playCount < 2) {
                    playCount++
                    it.start()
                } else {
                    it.release()
                }
            }
            mp.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendSystemNotification(context: Context, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ba_timer_notifications"

        // 重要：创建渠道时将声音设为 null，防止系统声音冲突
        val channel = NotificationChannel(
            channelId,
            "定时提醒服务",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "用于显示BA计时的到达提醒"
            setSound(null, null) // 禁用系统默认通知音
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.peroro1)
            .setContentTitle("蔚蓝档案提醒")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setSound(null) // 再次确保 Builder 级别没有声音
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}