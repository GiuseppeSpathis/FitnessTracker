package com.example.fitnesstracker
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {

        val sharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastOpened = sharedPreferences.getLong("last_opened", 0)
        val currentTime = System.currentTimeMillis()


        if (currentTime - lastOpened >= ((3 * 60 * 60 * 1000) + (40 * 60 * 1000))) { // 3 ore e 40 minuti
            sendNotification()
        }

        return Result.success()

    }

    private fun sendNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val channel = NotificationChannel("reminder_channel", "Reminder Channel", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Channel for activity tracking reminders"
            }
            notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, "reminder_channel")
            .setContentTitle(applicationContext.getString(R.string.ReminderNotifyTitle))
            .setContentText(applicationContext.getString(R.string.ReminderNotifyContent))
            .setSmallIcon(R.drawable.reminder_notify) // Sostituisci con la tua icona
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        with(notificationManager) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(2, notification.build())
            }
        }

    }
}
