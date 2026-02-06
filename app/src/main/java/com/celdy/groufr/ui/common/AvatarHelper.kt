package com.celdy.groufr.ui.common

import android.widget.TextView

object AvatarHelper {

    fun initials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.isEmpty() || parts[0].isEmpty() -> "?"
            parts.size == 1 -> parts[0].first().uppercaseChar().toString()
            else -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
        }
    }

    fun bindAvatar(textView: TextView, name: String) {
        textView.text = initials(name)
    }
}
