package com.xiaomiwatch.watch

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.xiaomiwatch.specs.NativeXiaomiWatchSpec

class NativeXiaomiWatchModule(reactContext: ReactApplicationContext) :
  NativeXiaomiWatchSpec(reactContext) {

  override fun getName() = NAME

  override fun check(promise: Promise) {
    Thread {
      try {
        val r = Checker.run(reactApplicationContext)
        // Dall'app non notifichiamo (la stai già guardando): togliamo solo un avviso ormai risolto
        if (r.alertPackages.isEmpty()) Notifier.maybeNotify(reactApplicationContext, r)
        promise.resolve(r.toJson())
      } catch (e: Exception) {
        promise.reject("CHECK_FAILED", e)
      }
    }.start()
  }

  override fun acknowledgeNew(packages: ReadableArray) {
    Prefs.acknowledge(reactApplicationContext, (0 until packages.size()).mapNotNull { packages.getString(it) })
  }

  override fun lastBackgroundCheck(): Double =
    Prefs.lastBackgroundCheck(reactApplicationContext).toDouble()

  companion object {
    const val NAME = "NativeXiaomiWatch"
  }
}
