package com.celdy.groufr.data.calendar

import android.accounts.Account
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder

class StubSyncService : Service() {
    private lateinit var syncAdapter: StubSyncAdapter

    override fun onCreate() {
        super.onCreate()
        syncAdapter = StubSyncAdapter(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = syncAdapter.syncAdapterBinder

    private class StubSyncAdapter(context: Context) :
        AbstractThreadedSyncAdapter(context, true, false) {

        override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult
        ) {
            // Sync is handled by CalendarSyncManager via WorkManager, not by SyncAdapter framework
        }
    }
}
