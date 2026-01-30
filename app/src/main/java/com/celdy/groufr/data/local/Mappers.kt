package com.celdy.groufr.data.local

import com.celdy.groufr.data.events.EventDetailDto
import com.celdy.groufr.data.events.EventDto
import com.celdy.groufr.data.events.EventGroupRef
import com.celdy.groufr.data.events.EventUserDto
import com.celdy.groufr.data.groups.GroupDto
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.data.messages.MessageUserRef
import com.celdy.groufr.data.notifications.NotificationActorDto
import com.celdy.groufr.data.polls.PollDto

fun GroupDto.toEntity(): GroupEntity = GroupEntity(
    id = id,
    slug = slug,
    name = name,
    description = description
)

fun GroupEntity.toDto(): GroupDto = GroupDto(
    id = id,
    slug = slug,
    name = name,
    description = description
)

fun UserEntity.toDto(): com.celdy.groufr.data.auth.UserDto = com.celdy.groufr.data.auth.UserDto(
    id = id,
    name = name,
    email = email.orEmpty()
)

fun com.celdy.groufr.data.auth.UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email
)

fun MessageUserRef.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = null
)

fun EventUserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = null
)

fun NotificationActorDto.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = null
)

fun UserEntity.toMessageUserRef(): MessageUserRef = MessageUserRef(
    id = id,
    name = name
)

fun EventDto.toEntity(): EventEntity = EventEntity(
    id = id,
    groupId = groupId,
    groupName = group?.name,
    groupSlug = group?.slug,
    title = title,
    description = description,
    place = place,
    state = state,
    startAt = startAt,
    endAt = endAt,
    deadlineJoinAt = deadlineJoinAt,
    minParticipants = null,
    maxParticipants = null,
    yourStatus = yourStatus,
    yourRole = null,
    participants = participants,
    participantsList = emptyList(),
    createdAt = createdAt,
    updatedAt = null
)

fun EventDetailDto.toEntity(): EventEntity = EventEntity(
    id = id,
    groupId = groupId,
    groupName = groupName,
    groupSlug = null,
    title = title,
    description = description,
    place = place,
    state = state,
    startAt = startAt,
    endAt = endAt,
    deadlineJoinAt = deadlineJoinAt,
    minParticipants = minParticipants,
    maxParticipants = maxParticipants,
    yourStatus = yourStatus,
    yourRole = yourRole,
    participants = participants,
    participantsList = participantsList,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun EventEntity.toDto(): EventDto = EventDto(
    id = id,
    groupId = groupId,
    group = groupName?.let { name ->
        EventGroupRef(id = groupId, name = name, slug = groupSlug ?: "")
    },
    title = title,
    description = description,
    place = place,
    state = state,
    startAt = startAt,
    endAt = endAt,
    deadlineJoinAt = deadlineJoinAt,
    yourStatus = yourStatus,
    participants = participants,
    createdAt = createdAt
)

fun EventEntity.toDetailDto(): EventDetailDto = EventDetailDto(
    id = id,
    groupId = groupId,
    groupName = groupName,
    title = title,
    description = description,
    place = place,
    state = state,
    startAt = startAt,
    endAt = endAt,
    deadlineJoinAt = deadlineJoinAt,
    minParticipants = minParticipants,
    maxParticipants = maxParticipants,
    yourStatus = yourStatus,
    yourRole = yourRole,
    participants = participants,
    participantsList = participantsList,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PollDto.toEntity(): PollEntity = PollEntity(
    id = id,
    groupId = groupId,
    question = question,
    description = description,
    multiselect = multiselect,
    options = options,
    yourVotes = yourVotes,
    totalVoters = totalVoters,
    status = status,
    deadlineAt = deadlineAt,
    createdAt = createdAt
)

fun PollEntity.toDto(): PollDto = PollDto(
    id = id,
    groupId = groupId,
    question = question,
    description = description,
    multiselect = multiselect,
    options = options,
    yourVotes = yourVotes,
    totalVoters = totalVoters,
    status = status,
    deadlineAt = deadlineAt,
    createdAt = createdAt
)

fun MessageDto.toEntity(
    threadType: String,
    threadId: Long
): MessageEntity = MessageEntity(
    id = id,
    threadType = threadType,
    threadId = threadId,
    userId = user?.id,
    messageType = messageType,
    body = body,
    createdAt = createdAt,
    refUserId = refUser?.id,
    refEvent = refEvent,
    refPoll = refPoll
)

fun MessageEntity.toDto(userMap: Map<Long, UserEntity>): MessageDto {
    val userRef = userId?.let { userMap[it]?.toMessageUserRef() }
    val refUser = refUserId?.let { userMap[it]?.toMessageUserRef() }
    return MessageDto(
        id = id,
        user = userRef,
        messageType = messageType,
        body = body,
        createdAt = createdAt,
        refUser = refUser,
        refEvent = refEvent,
        refPoll = refPoll
    )
}
