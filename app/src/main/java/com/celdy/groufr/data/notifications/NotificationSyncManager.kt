package com.celdy.groufr.data.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.celdy.groufr.data.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NotificationsRepository,
    private val authRepository: AuthRepository,
    private val store: NotificationSyncStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var foregroundJob: Job? = null
    private var startedActivities = 0

    fun schedulePeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun onAppForeground() {
        startedActivities += 1
        if (startedActivities == 1) {
            startForegroundScheduler()
        }
    }

    fun onAppBackground() {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) {
            stopForegroundScheduler()
        }
    }

    fun maybeSyncOnResume() {
        val now = System.currentTimeMillis()
        val lastCheck = store.getLastCheckAtMs()
        if (now - lastCheck >= ONE_HOUR_MS) {
            forceSync()
        }
    }

    fun onUserAction() {
        forceSync()
    }

    fun forceSync() {
        scope.launch {
            performSync()
        }
    }

    private fun startForegroundScheduler() {
        if (foregroundJob?.isActive == true) return
        foregroundJob = scope.launch {
            while (isActive) {
                val delayMs = nextDelayMs()
                if (delayMs > 0) {
                    delay(delayMs)
                }
                if (!isActive) break
                performSyncIfDue()
            }
        }
    }

    private fun stopForegroundScheduler() {
        foregroundJob?.cancel()
        foregroundJob = null
    }

    private suspend fun performSyncIfDue() {
        val now = System.currentTimeMillis()
        val lastCheck = store.getLastCheckAtMs()
        val interval = currentIntervalMs()
        if (lastCheck == 0L || now - lastCheck >= interval) {
            performSync()
        }
    }

    private suspend fun performSync() {
        if (!authRepository.hasSession()) return
        val ok = authRepository.ensureValidSession()
        if (!ok) return
        try {
            val notifications = repository.loadNotifications(unreadOnly = true, limit = 1)
            val hasNew = notifications.isNotEmpty()
            val now = System.currentTimeMillis()
            store.setLastCheckAtMs(now)
            store.setIntervalIndex(nextIntervalIndex(hasNew))
        } catch (exception: Exception) {
            // Ignore errors; try again later based on current interval.
        }
    }

    private fun nextIntervalIndex(hasNew: Boolean): Int {
        return if (hasNew) {
            0
        } else {
            val next = store.getIntervalIndex() + 1
            next.coerceAtMost(INTERVALS_MS.lastIndex)
        }
    }

    private fun currentIntervalMs(): Long {
        val index = store.getIntervalIndex().coerceIn(0, INTERVALS_MS.lastIndex)
        return INTERVALS_MS[index]
    }

    private fun nextDelayMs(): Long {
        val lastCheck = store.getLastCheckAtMs()
        val interval = currentIntervalMs()
        if (lastCheck == 0L) return 0L
        val nextCheckAt = lastCheck + interval
        val now = System.currentTimeMillis()
        return (nextCheckAt - now).coerceAtLeast(0L)
    }

    companion object {
        private val INTERVALS_MS = longArrayOf(
            60_000L,
            5 * 60_000L,
            10 * 60_000L,
            15 * 60_000L,
            30 * 60_000L,
            ONE_HOUR_MS
        )
        private const val ONE_HOUR_MS = 60 * 60_000L
        private const val WORK_NAME = "notification_sync_work"
    }
}
