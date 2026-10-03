package com.xiaomiwatch

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.xiaomiwatch.watch.CheckWorker
import com.xiaomiwatch.watch.Checker
import com.xiaomiwatch.watch.NativeXiaomiWatchPackage

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          add(NativeXiaomiWatchPackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    loadReactNative(this)
    // Crea la cartella in cui ./invia-lista.sh deposita la lista
    Checker.watchlistFile(this)
    CheckWorker.ensurePeriodic(this)
  }
}
