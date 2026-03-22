package com.example.batimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("ba_prefs", Context.MODE_PRIVATE)

        // 1. 获取用户设置的开关和时间点
        val mOn = prefs.getBoolean("m_on", true)  // 早刷新开关
        val h1 = prefs.getInt("h1", 4)           // 早刷新时间点
        val eOn = prefs.getBoolean("e_on", true)  // 晚刷新开关
        val h2 = prefs.getInt("h2", 16)          // 晚刷新时间点

        // 获取全局声音和震动开关
        val useSound = prefs.getBoolean("s_on", true)
        val useVibrate = prefs.getBoolean("v_on", true)

        // 2. 无论是否提醒，只要是整点任务，就预约下一个整点，确保时钟不中断
        if (intent.action == "ACTION_EVERY_HOUR" || intent.action == "android.intent.action.BOOT_COMPLETED") {
            MainActivity.scheduleNextHourAlarm(context)
        }

        // 3. 核心判定逻辑：检查当前小时是否命中“已开启”的设置点
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // 只有满足以下任一条件才会触发提醒：
        // 条件A：现在是早刷新时间点 且 早开关打开
        // 条件B：现在是晚刷新时间点 且 晚开关打开
        val isMorningTime = (currentHour == h1 && mOn)
        val isEveningTime = (currentHour == h2 && eOn)

        if (isMorningTime || isEveningTime) {
            // 执行提醒逻辑
            if (useSound || useVibrate) {
                Toast.makeText(context, "老师，${currentHour}点的刷新时间到啦！", Toast.LENGTH_LONG).show()

                // 播放自定义铃声
                if (useSound) {
                    try {
                        val mp = MediaPlayer.create(context, R.raw.ba_message)
                        mp.setOnCompletionListener { it.release() }
                        mp.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 执行震动
                if (useVibrate) {
                    val vibrator = context.getSystemService(Vibrator::class.java)
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
                }
            }
        }
    }
}