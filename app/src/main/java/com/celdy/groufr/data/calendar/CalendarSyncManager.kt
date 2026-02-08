package com.celdy.groufr.data.calendar

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.events.EventDto
import com.celdy.groufr.data.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val store: CalendarSyncStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    fun isEnabled(): Boolean = store.isEnabled()

    private fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * @return true if account + calendar were created successfully
     */
    fun enableSync(): Boolean {
        store.setEnabled(true)
        try {
            val accountCreated = ensureAccount()
            if (!accountCreated) {
                Log.e(TAG, "Failed to create or find account")
                store.setEnabled(false)
                return false
            }
            val calendarCreated = ensureCalendar()
            if (!calendarCreated) {
                Log.e(TAG, "Failed to create calendar")
                logAllCalendars()
                store.setEnabled(false)
                return false
            }
            Log.d(TAG, "Calendar sync enabled successfully")
            logAllCalendars()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during enableSync", e)
            store.setEnabled(false)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during enableSync", e)
            store.setEnabled(false)
            return false
        }
        schedulePeriodicWork()
        scope.launch { syncNow() }
        return true
    }

    fun disableSync() {
        store.setEnabled(false)
        try {
            deleteAllEvents()
            deleteCalendar()
            removeAccount()
        } catch (_: SecurityException) {
            // Permission revoked — nothing to clean up
        } catch (e: Exception) {
            Log.e(TAG, "Error during disableSync", e)
        }
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun schedulePeriodicWorkIfEnabled() {
        if (!store.isEnabled()) return
        schedulePeriodicWork()
    }

    fun maybeSyncOnResume() {
        if (!store.isEnabled()) return
        if (!hasCalendarPermissions()) return
        val now = System.currentTimeMillis()
        val lastSync = store.getLastSyncMs()
        if (now - lastSync >= ONE_HOUR_MS) {
            scope.launch { syncNow() }
        }
    }

    suspend fun syncNow() {
        if (!store.isEnabled()) return
        if (!hasCalendarPermissions()) return
        if (!authRepository.hasSession()) return

        syncMutex.withLock {
            if (!store.isEnabled()) return
            val ok = authRepository.ensureValidSession()
            if (!ok) return

            try {
                var calendarId = getCalendarId()
                if (calendarId == null) {
                    Log.w(TAG, "syncNow: calendar not found, recreating")
                    ensureAccount()
                    ensureCalendar()
                    calendarId = getCalendarId()
                }
                if (calendarId == null) {
                    Log.e(TAG, "syncNow: calendar still not found after recreation attempt")
                    return
                }
                val events = fetchAllEvents()
                Log.d(TAG, "syncNow: fetched ${events.size} events from API")
                diffSync(calendarId, events)
                store.setLastSyncMs(System.currentTimeMillis())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during syncNow", e)
                store.setEnabled(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error during syncNow", e)
            }
        }
    }

    private fun schedulePeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    // --- Account management ---

    private fun ensureAccount(): Boolean {
        val am = AccountManager.get(context)
        val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        val existing = am.getAccountsByType(ACCOUNT_TYPE)
        if (existing.isNotEmpty()) {
            Log.d(TAG, "Account already exists")
            enableCalendarSync(existing[0])
            return true
        }
        val added = am.addAccountExplicitly(account, null, null)
        Log.d(TAG, "addAccountExplicitly returned: $added")
        if (added) {
            enableCalendarSync(account)
        }
        return added
    }

    private fun enableCalendarSync(account: Account) {
        val authority = CalendarContract.AUTHORITY
        ContentResolver.setIsSyncable(account, authority, 1)
        ContentResolver.setSyncAutomatically(account, authority, true)
    }

    private fun removeAccount() {
        val am = AccountManager.get(context)
        val accounts = am.getAccountsByType(ACCOUNT_TYPE)
        for (account in accounts) {
            am.removeAccountExplicitly(account)
        }
    }

    // --- Calendar management ---

    private fun ensureCalendar(): Boolean {
        val existingId = getCalendarId()
        if (existingId != null) {
            Log.d(TAG, "Calendar already exists with id=$existingId")
            return true
        }
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val insertUri = syncAdapterUri(CalendarContract.Calendars.CONTENT_URI)
        Log.d(TAG, "Inserting calendar with URI: $insertUri")
        Log.d(TAG, "Calendar values: $values")
        val resultUri = context.contentResolver.insert(insertUri, values)
        Log.d(TAG, "Calendar insert returned uri=$resultUri")

        // Verify the calendar was actually persisted
        val verifyId = getCalendarId()
        Log.d(TAG, "Verify calendar after insert: id=$verifyId")
        return verifyId != null
    }

    private fun getCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val args = arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection, selection, args, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun deleteCalendar() {
        val calendarId = getCalendarId() ?: return
        val uri = ContentUris.withAppendedId(syncAdapterUri(CalendarContract.Calendars.CONTENT_URI), calendarId)
        context.contentResolver.delete(uri, null, null)
    }

    private fun logAllCalendars() {
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.SYNC_EVENTS
            )
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null
            )?.use { cursor ->
                Log.d(TAG, "=== All calendars (${cursor.count} total) ===")
                while (cursor.moveToNext()) {
                    Log.d(TAG, "  id=${cursor.getLong(0)}" +
                        " account=${cursor.getString(1)}" +
                        " type=${cursor.getString(2)}" +
                        " name=${cursor.getString(3)}" +
                        " visible=${cursor.getInt(4)}" +
                        " sync=${cursor.getInt(5)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list calendars", e)
        }
    }

    // --- Event sync ---

    private suspend fun fetchAllEvents(): List<EventDto> {
        val allEvents = mutableListOf<EventDto>()
        var offset = 0
        while (true) {
            val response = apiService.getAllEvents(
                time = "upcoming",
                participation = "joined,maybe",
                limit = 50,
                offset = offset
            )
            allEvents.addAll(response.events)
            val meta = response.meta
            if (meta == null || offset + meta.limit >= meta.total) break
            offset += meta.limit
        }
        return allEvents
    }

    private fun diffSync(calendarId: Long, apiEvents: List<EventDto>) {
        val existing = loadExistingEvents(calendarId)
        val apiMap = mutableMapOf<String, EventDto>()
        for (event in apiEvents) {
            if (event.startAt == null) continue
            val key = "groufr_${event.id}"
            apiMap[key] = event
        }

        // Delete events no longer in API response
        for ((key, localId) in existing) {
            if (key !in apiMap) {
                val uri = ContentUris.withAppendedId(
                    syncAdapterUri(CalendarContract.Events.CONTENT_URI), localId
                )
                context.contentResolver.delete(uri, null, null)
            }
        }

        // Insert or update
        for ((key, event) in apiMap) {
            val values = buildEventValues(calendarId, key, event)
            val localId = existing[key]
            if (localId != null) {
                val uri = ContentUris.withAppendedId(
                    syncAdapterUri(CalendarContract.Events.CONTENT_URI), localId
                )
                context.contentResolver.update(uri, values, null, null)
            } else {
                context.contentResolver.insert(
                    syncAdapterUri(CalendarContract.Events.CONTENT_URI), values
                )
            }
        }
    }

    private fun loadExistingEvents(calendarId: Long): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.SYNC_DATA1)
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val args = arrayOf(calendarId.toString())
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection, selection, args, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val syncKey = cursor.getString(1) ?: continue
                result[syncKey] = id
            }
        }
        return result
    }

    private fun buildEventValues(calendarId: Long, syncKey: String, event: EventDto): ContentValues {
        val isMaybe = event.yourStatus == "maybe"
        val title = if (isMaybe) "? ${event.title}" else event.title

        val startMs = parseIsoToMillis(event.startAt!!)
        val endMs = if (event.endAt != null) {
            parseIsoToMillis(event.endAt)
        } else {
            startMs + ONE_HOUR_MS
        }

        val descriptionParts = mutableListOf<String>()
        event.group?.name?.let { descriptionParts.add("Group: $it") }
        event.description?.let { descriptionParts.add(it) }
        val description = descriptionParts.joinToString("\n").ifEmpty { null }

        return ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_LOCATION, event.place)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.AVAILABILITY,
                if (isMaybe) CalendarContract.Events.AVAILABILITY_TENTATIVE
                else CalendarContract.Events.AVAILABILITY_BUSY
            )
            put(CalendarContract.Events.SYNC_DATA1, syncKey)
        }
    }

    private fun parseIsoToMillis(iso: String): Long {
        val odt = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return odt.toInstant().toEpochMilli()
    }

    private fun deleteAllEvents() {
        val calendarId = getCalendarId() ?: return
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val args = arrayOf(calendarId.toString())
        context.contentResolver.delete(
            syncAdapterUri(CalendarContract.Events.CONTENT_URI),
            selection, args
        )
    }

    // --- Utility ---

    private fun syncAdapterUri(uri: Uri): Uri {
        return uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()
    }

    companion object {
        private const val TAG = "CalendarSyncManager"
        private const val ACCOUNT_NAME = "Groufr"
        private const val ACCOUNT_TYPE = "com.celdy.groufr.account"
        private const val CALENDAR_NAME = "Groufr"
        private const val CALENDAR_COLOR = 0xFF0EA5E9.toInt()
        private const val ONE_HOUR_MS = 60 * 60_000L
        private const val WORK_NAME = "calendar_sync_work"
    }
}
