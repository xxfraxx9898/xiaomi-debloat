package com.xiaomiwatch.watch

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xiaomiwatch.MainActivity
import com.xiaomiwatch.R

object Notifier {
  private const val CHANNEL = "watch"
  private const val NOTIFICATION_ID = 1

  /** Notifica solo se i problemi sono diversi dall'ultimo avviso. */
  fun maybeNotify(ctx: Context, r: CheckResult) {
    val pkgs = r.alertPackages
    if (pkgs.isEmpty()) {
      Prefs.setLastAlert(ctx, null)
      NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
      return
    }
    val signature = pkgs.joinToString(",")
    if (signature == Prefs.lastAlert(ctx)) return
    if (show(ctx, r)) Prefs.setLastAlert(ctx, signature)
  }

  private fun show(ctx: Context, r: CheckResult): Boolean {
    if (Build.VERSION.SDK_INT >= 33 &&
      ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false

    val nm = ctx.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26) {
      nm.createNotificationChannel(
        NotificationChannel(CHANNEL, "App tornate", NotificationManager.IMPORTANCE_DEFAULT)
      )
    }

    val lines = buildList {
      if (r.returned.isNotEmpty()) add("Tornate: " + r.returned.joinToString { it.label })
      if (r.unsuspended.isNotEmpty()) add("Non più bloccate: " + r.unsuspended.joinToString { it.label })
      if (r.newSystem.isNotEmpty()) add("Nuove di sistema: " + r.newSystem.size)
    }
    val count = r.returned.size + r.unsuspended.size + r.newSystem.size
    val open = PendingIntent.getActivity(
      ctx, 0,
      Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val n = NotificationCompat.Builder(ctx, CHANNEL)
      .setSmallIcon(R.drawable.ic_stat_watch)
      .setContentTitle(if (count == 1) "1 app da ricontrollare" else "$count app da ricontrollare")
      .setContentText(lines.first())
      .setStyle(NotificationCompat.BigTextStyle().bigText(
        lines.joinToString("\n") + "\n\nCollega il PC e lancia ./controlla.sh --ripara"
      ))
      .setContentIntent(open)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, n)
    return true
  }
}
