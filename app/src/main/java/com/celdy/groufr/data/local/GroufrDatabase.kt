package com.celdy.groufr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        GroupEntity::class,
        UserEntity::class,
        EventEntity::class,
        PollEntity::class,
        MessageEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GroufrDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun pollDao(): PollDao
    abstract fun messageDao(): MessageDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
}
