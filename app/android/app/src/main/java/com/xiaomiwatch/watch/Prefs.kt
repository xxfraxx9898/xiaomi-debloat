package com.xiaomiwatch.watch

import android.content.Context

object Prefs {
  private const val FILE = "watch"
  private const val ACK = "acknowledged"
  private const val LAST_ALERT = "lastAlert"
  private const val LAST_BG = "lastBackgroundCheck"

  private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

  /** Pacchetti nuovi che hai già visto e accettato: non vengono più segnalati. */
  fun acknowledged(ctx: Context): Set<String> = sp(ctx).getStringSet(ACK, emptySet())!!.toSet()

  fun acknowledge(ctx: Context, pkgs: Collection<String>) =
    sp(ctx).edit().putStringSet(ACK, acknowledged(ctx) + pkgs).apply()

  /** Firma dell'ultima notifica, per non ripetere lo stesso avviso a ogni controllo. */
  fun lastAlert(ctx: Context): String? = sp(ctx).getString(LAST_ALERT, null)

  fun setLastAlert(ctx: Context, signature: String?) =
    sp(ctx).edit().putString(LAST_ALERT, signature).apply()

  fun lastBackgroundCheck(ctx: Context): Long = sp(ctx).getLong(LAST_BG, 0)

  fun setLastBackgroundCheck(ctx: Context, time: Long) = sp(ctx).edit().putLong(LAST_BG, time).apply()
}
