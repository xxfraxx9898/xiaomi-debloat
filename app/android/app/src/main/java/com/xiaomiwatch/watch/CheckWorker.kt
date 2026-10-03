package com.xiaomiwatch.watch

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CheckWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
  override fun doWork(): Result {
    val r = Checker.run(applicationContext)
    Prefs.setLastBackgroundCheck(applicationContext, r.time)
    Notifier.maybeNotify(applicationContext, r)
    return Result.success()
  }

  companion object {
    /** Controllo ogni 12 ore; KEEP non resetta il timer se è già programmato. */
    fun ensurePeriodic(ctx: Context) {
      WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
        "periodic-check",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequest.Builder(CheckWorker::class.java, 12, TimeUnit.HOURS).build(),
      )
    }

    fun runSoon(ctx: Context, delaySeconds: Long) {
      WorkManager.getInstance(ctx).enqueueUniqueWork(
        "check-soon",
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequest.Builder(CheckWorker::class.java)
          .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
          .build(),
      )
    }
  }
}
