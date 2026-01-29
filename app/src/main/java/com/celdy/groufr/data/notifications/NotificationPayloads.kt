package com.celdy.groufr.data.notifications

fun NotificationDto.eventIdFromPayload(): Long? {
    val payload = payload ?: return null
    val raw = payload["event_id"] ?: payload["eventId"] ?: return null
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> raw.toString().toLongOrNull()
    }
}
