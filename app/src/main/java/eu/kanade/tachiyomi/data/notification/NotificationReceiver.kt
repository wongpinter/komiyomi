package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Global [BroadcastReceiver] that runs on UI thread
 * Pending Broadcasts should be made from here.
 * NOTE: Use local broadcasts if possible.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHARE_IMAGE -> intent.getStringExtra(EXTRA_URI)?.toUri()?.let { uri ->
                context.startActivity(uri.toShareIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_CANCEL_RESTORE -> BackupRestoreJob.stop(context)
        }
    }

    companion object {
        private const val ACTION_SHARE_IMAGE = "$ID.NotificationReceiver.SHARE_IMAGE"
        private const val ACTION_CANCEL_RESTORE = "$ID.NotificationReceiver.CANCEL_RESTORE"
        private const val EXTRA_URI = "$ID.NotificationReceiver.URI"
        private const val EXTRA_NOTIFICATION_ID = "$ID.NotificationReceiver.NOTIFICATION_ID"

        // KMK -->
        internal fun dismissNotification(context: Context, notificationId: Int, groupId: Int? = null) {
            val groupKey = context.notificationManager.activeNotifications
                .firstOrNull { it.id == notificationId }
                ?.groupKey
            if (groupId != null && groupId != 0 && !groupKey.isNullOrEmpty()) {
                val groupCount = context.notificationManager.activeNotifications.count { it.groupKey == groupKey }
                if (groupCount == 2) context.cancelNotification(groupId)
            }
            context.cancelNotification(notificationId)
        }

        internal fun shareImagePendingBroadcast(context: Context, uri: Uri): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHARE_IMAGE
                putExtra(EXTRA_URI, uri.toString())
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun shareBackupPendingActivity(context: Context, uri: Uri): PendingIntent {
            val intent = uri.toShareIntent(context, "application/x-protobuf+gzip").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun openErrorLogPendingActivity(context: Context, uri: Uri): PendingIntent {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/plain")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        internal fun cancelRestorePendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_RESTORE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        // KMK <--
    }
}
