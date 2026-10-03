package com.xiaomiwatch.watch

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class AppEntry(val pkg: String, val label: String)

data class CheckResult(
  val time: Long,
  val returned: List<AppEntry>,
  val unsuspended: List<AppEntry>,
  val manual: List<AppEntry>,
  val newSystem: List<AppEntry>,
  val watchedRemoved: Int,
  val watchedSuspended: Int,
  /** false finché dal PC non è arrivata la lista con ./invia-lista.sh. */
  val configured: Boolean = true,
) {
  /** Problemi che meritano una notifica (le app "manuali" no: sono note e non cambiano). */
  val alertPackages: List<String>
    get() = (returned + unsuspended + newSystem).map { it.pkg }.sorted()

  fun toJson(): String {
    fun list(l: List<AppEntry>) =
      JSONArray(l.map { JSONObject().put("pkg", it.pkg).put("label", it.label) })
    return JSONObject()
      .put("time", time)
      .put("returned", list(returned))
      .put("unsuspended", list(unsuspended))
      .put("manual", list(manual))
      .put("newSystem", list(newSystem))
      .put("watchedRemoved", watchedRemoved)
      .put("watchedSuspended", watchedSuspended)
      .put("configured", configured)
      .toString()
  }
}

object Checker {
  private class Watchlist(
    val removed: Set<String>,
    val suspended: Set<String>,
    val disabled: Set<String>,
    val manual: Set<String>,
    val original: Set<String>,
  )

  /** La lista arriva dal PC: ./invia-lista.sh la deposita con adb nella cartella dell'app. */
  fun watchlistFile(ctx: Context): File? =
    ctx.getExternalFilesDir(null)?.let { File(it, "watchlist.json") }

  private fun loadWatchlist(ctx: Context): Watchlist? {
    val file = watchlistFile(ctx)?.takeIf { it.isFile } ?: return null
    val json = JSONObject(file.readText())
    fun set(key: String): Set<String> {
      val a = json.optJSONArray(key) ?: return emptySet()
      return (0 until a.length()).map { a.getString(it) }.toSet()
    }
    return Watchlist(set("removed"), set("suspended"), set("disabled"), set("manual"), set("original"))
  }

  @Suppress("DEPRECATION")
  private fun installedApps(pm: PackageManager): List<ApplicationInfo> =
    if (Build.VERSION.SDK_INT >= 33) {
      pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
      pm.getInstalledApplications(0)
    }

  /** Preinstallata dal sistema e non da te: flag di sistema o APK fuori da /data/app. */
  private fun isPreinstalled(ai: ApplicationInfo): Boolean =
    (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || ai.sourceDir?.startsWith("/data/app/") != true

  fun run(ctx: Context): CheckResult {
    // Senza lista non c'è niente da confrontare (e ogni app di sistema sembrerebbe "nuova")
    val w = loadWatchlist(ctx) ?: return CheckResult(
      System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), emptyList(), 0, 0,
      configured = false,
    )
    val pm = ctx.packageManager
    val apps = installedApps(pm).associateBy { it.packageName }
    fun entry(ai: ApplicationInfo) = AppEntry(ai.packageName, ai.loadLabel(pm).toString())
    val acknowledged = Prefs.acknowledged(ctx)

    return CheckResult(
      time = System.currentTimeMillis(),
      returned = w.removed.mapNotNull { apps[it] }.map(::entry),
      // Sospese che non lo sono più, o disattivate che sono state riattivate
      unsuspended = w.suspended.mapNotNull { apps[it] }
        .filter { (it.flags and ApplicationInfo.FLAG_SUSPENDED) == 0 }.map(::entry) +
        w.disabled.mapNotNull { apps[it] }.filter { it.enabled }.map(::entry),
      manual = w.manual.mapNotNull { apps[it] }.filter { it.enabled }.map(::entry),
      newSystem = apps.values
        .filter { isPreinstalled(it) && it.packageName !in w.original && it.packageName !in acknowledged }
        .map(::entry),
      watchedRemoved = w.removed.size,
      watchedSuspended = w.suspended.size + w.disabled.size,
    ).let { r -> r.copy(
      returned = r.returned.sortedBy { it.label.lowercase() },
      unsuspended = r.unsuspended.sortedBy { it.label.lowercase() },
      manual = r.manual.sortedBy { it.label.lowercase() },
      newSystem = r.newSystem.sortedBy { it.pkg },
    ) }
  }
}
