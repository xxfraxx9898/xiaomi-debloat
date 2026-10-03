package com.xiaomiwatch.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Dopo ogni avvio (e dopo un aggiornamento di questa app) ricontrolla e riavvisa. */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
      intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
    ) return
    Prefs.setLastAlert(ctx, null)
    CheckWorker.ensurePeriodic(ctx)
    CheckWorker.runSoon(ctx, 60)
  }
}
