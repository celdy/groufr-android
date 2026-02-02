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

fun NotificationDto.invitedGroupNameFromPayload(): String? {
    val payload = payload ?: return null
    val raw = payload["group_name"] ?: payload["groupName"] ?: return null
    return raw as? String ?: raw.toString()
}

fun NotificationDto.invitationTokenFromPayload(): String? {
    val payload = payload ?: return null
    val raw = payload["invitation_token"] ?: payload["invitationToken"] ?: return null
    return raw as? String ?: raw.toString()
}
