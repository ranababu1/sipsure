package app.sipsure.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.sipsure.MainActivity
import app.sipsure.R
import app.sipsure.domain.Phase

/** Posts check-in notifications. Wording stays neutral: a missed check-in is not treated as wrongdoing. */
class Notifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_description) },
        )
    }

    fun show(phase: Phase) {
        val (title, text) = when (phase) {
            Phase.CHECK_IN_DUE -> R.string.notif_due_title to R.string.notif_due_text
            Phase.GRACE_PERIOD -> R.string.notif_grace_title to R.string.notif_grace_text
            Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE ->
                R.string.notif_restricted_title to R.string.notif_restricted_text
            else -> return cancel()
        }
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setStyle(Notification.BigTextStyle().bigText(context.getString(text)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(ID, notification)
    }

    fun cancel() = manager.cancel(ID)

    private fun canPost() = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL = "checkins"
        const val ID = 1
    }
}
