package com.celdy.groufr.data.local

import androidx.room.TypeConverter
import com.celdy.groufr.data.events.EventParticipantDto
import com.celdy.groufr.data.expenses.ExpenseShareDto
import com.celdy.groufr.data.messages.MessageEventRef
import com.celdy.groufr.data.messages.MessagePollRef
import com.celdy.groufr.data.polls.PollOptionDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun toMessageEventRefJson(value: MessageEventRef?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun fromMessageEventRefJson(value: String?): MessageEventRef? =
        value?.let { gson.fromJson(it, MessageEventRef::class.java) }

    @TypeConverter
    fun toMessagePollRefJson(value: MessagePollRef?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun fromMessagePollRefJson(value: String?): MessagePollRef? =
        value?.let { gson.fromJson(it, MessagePollRef::class.java) }

    @TypeConverter
    fun toPollOptionsJson(value: List<PollOptionDto>): String = gson.toJson(value)

    @TypeConverter
    fun fromPollOptionsJson(value: String): List<PollOptionDto> {
        val type = object : TypeToken<List<PollOptionDto>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toLongListJson(value: List<Long>): String = gson.toJson(value)

    @TypeConverter
    fun fromLongListJson(value: String): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toParticipantsJson(value: Map<String, Int>): String = gson.toJson(value)

    @TypeConverter
    fun fromParticipantsJson(value: String): Map<String, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toParticipantsListJson(value: List<EventParticipantDto>): String = gson.toJson(value)

    @TypeConverter
    fun fromParticipantsListJson(value: String): List<EventParticipantDto> {
        val type = object : TypeToken<List<EventParticipantDto>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toExpenseSharesJson(value: List<ExpenseShareDto>): String = gson.toJson(value)

    @TypeConverter
    fun fromExpenseSharesJson(value: String): List<ExpenseShareDto> {
        val type = object : TypeToken<List<ExpenseShareDto>>() {}.type
        return gson.fromJson(value, type)
    }
}
