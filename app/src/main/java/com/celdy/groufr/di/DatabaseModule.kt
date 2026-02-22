package com.celdy.groufr.di

import android.content.Context
import androidx.room.Room
import com.celdy.groufr.data.local.EventDao
import com.celdy.groufr.data.local.ExpenseDao
import com.celdy.groufr.data.local.GroufrDatabase
import com.celdy.groufr.data.local.GroupDao
import com.celdy.groufr.data.local.MessageDao
import com.celdy.groufr.data.local.PollDao
import com.celdy.groufr.data.local.SettlementDao
import com.celdy.groufr.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GroufrDatabase {
        return Room.databaseBuilder(context, GroufrDatabase::class.java, "groufr.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideGroupDao(database: GroufrDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideUserDao(database: GroufrDatabase): UserDao = database.userDao()

    @Provides
    fun provideEventDao(database: GroufrDatabase): EventDao = database.eventDao()

    @Provides
    fun providePollDao(database: GroufrDatabase): PollDao = database.pollDao()

    @Provides
    fun provideMessageDao(database: GroufrDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideExpenseDao(database: GroufrDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideSettlementDao(database: GroufrDatabase): SettlementDao = database.settlementDao()
}
