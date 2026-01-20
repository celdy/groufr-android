# Groufr Mobile Application - Development Instructions

This document provides comprehensive instructions for building a Flutter mobile application that connects to the Groufr API. The application will be available for both Android and iOS platforms.

## Table of Contents

1. [Application Overview](#application-overview)
2. [Features](#features)
3. [API Reference](#api-reference)
4. [Authentication Flow](#authentication-flow)
5. [Data Models](#data-models)
6. [Implementation Guidelines](#implementation-guidelines)
7. [Recommended Architecture](#recommended-architecture)
8. [UI/UX Guidelines](#uiux-guidelines)

---

## Application Overview

**Groufr** is a group organization application that helps friends plan events, communicate, and make decisions together. The mobile app allows users to:

- View and participate in group chats
- Create and vote on polls
- Create and RSVP to events
- Receive push notifications for updates
- Stay synced with the latest group activity

### Target Platforms
- Android (minimum SDK 21)
- iOS (minimum iOS 12)

### Technology Stack
- **Framework:** Flutter 3.x
- **State Management:** Provider, Riverpod, or BLoC (your choice)
- **HTTP Client:** Dio or http package
- **Local Storage:** SharedPreferences for tokens, Hive or SQLite for offline cache
- **Push Notifications:** Firebase Cloud Messaging (FCM) for Android, APNs for iOS

---

## Features

### Core Features

1. **Authentication**
   - Email/password login
   - Secure token storage (access + refresh tokens)
   - Automatic token refresh
   - Logout from current or all devices

2. **Groups**
   - View list of user's groups
   - Select group to view details
   - Group chat with real-time updates

3. **Messages**
   - View message history (paginated)
   - Send text messages
   - View system messages (user joined, event created, poll created)
   - Pull-to-refresh and infinite scroll

4. **Polls**
   - View active and closed polls
   - Vote on polls (single or multi-select)
   - See vote counts and who voted
   - Create new polls

5. **Events**
   - View upcoming and past events
   - Join or decline events
   - View event details and participants
   - Create new events

6. **Notifications**
   - Push notifications for new messages, events, polls
   - In-app notification list
   - Unread count badge
   - Mark as read functionality

7. **Sync**
   - Efficient delta sync for updates
   - Background sync when app is active
   - Immediate sync on push notification

---

## API Reference

### Base URL
```
https://your-domain.com/api/v1
```

### Authentication

All authenticated endpoints require the `Authorization` header:
```
Authorization: Bearer <access_token>
```

### Endpoints

#### Authentication

##### POST /auth/login
Login and receive tokens.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "device": {
    "uuid": "unique-device-identifier",
    "platform": "android",
    "name": "Pixel 7 Pro",
    "app_version": "1.0.0",
    "os_version": "14"
  }
}
```

**Response (200):**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "expires_in": 900,
  "token_type": "Bearer",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "locale": "en",
    "timezone": "Europe/Prague"
  }
}
```

**Error (401):**
```json
{
  "error": "invalid_credentials",
  "message": "Invalid email or password"
}
```

##### POST /auth/refresh
Get new access token using refresh token.

**Request:**
```json
{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response (200):**
```json
{
  "access_token": "new_access_token...",
  "refresh_token": "new_refresh_token...",
  "expires_in": 900,
  "token_type": "Bearer",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "locale": "en",
    "timezone": "Europe/Prague"
  }
}
```

##### POST /auth/logout
Revoke current tokens. Requires authentication.

**Response:** 204 No Content

##### POST /auth/logout-all
Revoke all tokens (logout from all devices). Requires authentication.

**Response (200):**
```json
{
  "revoked_count": 3
}
```

---

#### Device Management

##### PUT /device/push-token
Register or update push notification token. Requires authentication.

**Request:**
```json
{
  "push_token": "fcm_token_here...",
  "provider": "fcm"
}
```

**Response (200):**
```json
{
  "success": true
}
```

##### DELETE /device/push-token
Remove push token. Requires authentication.

**Response:** 204 No Content

---

#### Notifications

##### GET /notifications
Get user's notifications. Requires authentication.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| limit | int | 50 | Max 100 |
| offset | int | 0 | Pagination offset |
| unread_only | bool | false | Filter to unread |
| group_id | int | - | Filter by group |

**Response (200):**
```json
{
  "notifications": [
    {
      "id": 123,
      "event_type": "new_message",
      "group_id": 5,
      "group_name": "Weekend Trip Crew",
      "actor": {
        "id": 2,
        "name": "Jane Doe"
      },
      "entity_type": "message",
      "entity_id": 456,
      "payload": {
        "preview": "Hey everyone..."
      },
      "is_read": false,
      "created_at": "2026-01-01T10:30:00+00:00"
    }
  ],
  "meta": {
    "total": 42,
    "unread_count": 5,
    "limit": 50,
    "offset": 0
  }
}
```

##### GET /notifications/count
Get unread notification count. Requires authentication.

**Response (200):**
```json
{
  "unread_count": 5,
  "by_group": {
    "5": 3,
    "12": 2
  }
}
```

##### POST /notifications/{id}/read
Mark notification as read. Requires authentication.

**Response:** 204 No Content

##### POST /notifications/read-all
Mark all notifications as read. Requires authentication.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| group_id | int | Only mark for this group |

**Response (200):**
```json
{
  "marked_count": 15
}
```

---

#### Sync

##### GET /sync
Get all updates since last sync. Requires authentication.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| since | ISO 8601 | Get updates since this time |
| include | string | Comma-separated: messages,polls,events,notifications |

**Response (200):**
```json
{
  "sync_timestamp": "2026-01-01T12:00:00+00:00",
  "updates": {
    "groups": [
      {
        "id": 5,
        "slug": "weekend-crew",
        "name": "Weekend Trip Crew",
        "description": "Planning our adventures"
      }
    ],
    "messages": {
      "created": [
        {
          "id": 567,
          "chat_id": 10,
          "user": {"id": 2, "name": "Jane"},
          "message_type": "text",
          "body": "Great idea!",
          "created_at": "2026-01-01T11:55:00+00:00"
        }
      ],
      "deleted": [123, 124]
    },
    "polls": {
      "created": [],
      "updated": [
        {
          "id": 12,
          "group_id": 5,
          "question": "Where should we go?",
          "options": [
            {"id": 1, "label": "Beach", "vote_count": 3},
            {"id": 2, "label": "Mountains", "vote_count": 5}
          ],
          "your_votes": [2],
          "total_voters": 8,
          "status": "open",
          "deadline_at": "2026-01-05T18:00:00+00:00"
        }
      ],
      "closed": [8, 9]
    },
    "events": {
      "created": [],
      "updated": [
        {
          "id": 45,
          "group_id": 5,
          "title": "Beach Day",
          "state": "preparing",
          "start_at": "2026-01-15T10:00:00+00:00",
          "your_status": "joined",
          "participants": {"joined": 5, "declined": 2, "invited": 3}
        }
      ],
      "deleted": []
    },
    "notifications": {
      "new_count": 3
    }
  }
}
```

##### GET /sync/group/{groupId}
Sync specific group. Requires authentication.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| since | ISO 8601 | - | Get updates since |
| message_limit | int | 50 | Max messages to return |

---

#### Messages

##### GET /groups/{groupId}/messages
Get messages for a group. Requires authentication.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| limit | int | 50 | Max 100 |
| before_id | int | - | Pagination: get older messages |
| after_id | int | - | Sync: get newer messages |

**Response (200):**
```json
{
  "messages": [
    {
      "id": 567,
      "user": {"id": 2, "name": "Jane Doe"},
      "message_type": "text",
      "body": "Hello everyone!",
      "reply_to_id": null,
      "created_at": "2026-01-01T11:55:00+00:00"
    },
    {
      "id": 568,
      "user": {"id": 1, "name": "John Doe"},
      "message_type": "event_created",
      "body": null,
      "ref_event": {"id": 45, "title": "Beach Day"},
      "created_at": "2026-01-01T11:56:00+00:00"
    }
  ],
  "meta": {
    "has_more": true,
    "oldest_id": 567
  }
}
```

##### POST /groups/{groupId}/messages
Create a message. Requires authentication.

**Request:**
```json
{
  "body": "This is my message"
}
```

**Response (201):**
```json
{
  "id": 569,
  "user": {"id": 1, "name": "John Doe"},
  "message_type": "text",
  "body": "This is my message",
  "created_at": "2026-01-01T12:00:00+00:00"
}
```

---

#### Polls

##### GET /groups/{groupId}/polls
List polls. Requires authentication.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| status | string | all | open, closed, or all |
| limit | int | 20 | Max 100 |
| offset | int | 0 | Pagination |

**Response (200):**
```json
{
  "polls": [
    {
      "id": 12,
      "group_id": 5,
      "created_by": {"id": 2, "name": "Jane Doe"},
      "question": "Where should we go?",
      "description": "Vote for our destination",
      "multiselect": false,
      "options": [
        {"id": 1, "label": "Beach", "vote_count": 3},
        {"id": 2, "label": "Mountains", "vote_count": 5}
      ],
      "your_votes": [2],
      "total_voters": 8,
      "status": "open",
      "deadline_at": "2026-01-05T18:00:00+00:00",
      "created_at": "2026-01-01T10:00:00+00:00"
    }
  ],
  "meta": {
    "total": 5,
    "limit": 20,
    "offset": 0
  }
}
```

##### POST /groups/{groupId}/polls
Create a poll. Requires authentication.

**Request:**
```json
{
  "question": "What time should we meet?",
  "description": "Please vote by Friday",
  "multiselect": true,
  "options": ["9:00 AM", "10:00 AM", "11:00 AM"],
  "deadline_at": "2026-01-05T18:00:00+00:00"
}
```

**Response (201):** Poll object as above.

##### POST /polls/{pollId}/vote
Vote on a poll. Requires authentication.

**Request:**
```json
{
  "option_ids": [2]
}
```

For multiselect polls, multiple IDs are allowed:
```json
{
  "option_ids": [1, 3]
}
```

**Response (200):** Updated poll object.

##### DELETE /polls/{pollId}/vote
Remove all votes from a poll. Requires authentication.

**Response (200):** Updated poll object.

---

#### Events

##### GET /groups/{groupId}/events
List events. Requires authentication.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| filter | string | upcoming | upcoming, past, or all |
| limit | int | 20 | Max 100 |
| offset | int | 0 | Pagination |

**Response (200):**
```json
{
  "events": [
    {
      "id": 45,
      "group_id": 5,
      "created_by": {"id": 2, "name": "Jane Doe"},
      "title": "Beach Day",
      "description": "Let's go to the beach!",
      "state": "offered",
      "start_at": "2026-01-15T10:00:00+00:00",
      "end_at": "2026-01-15T18:00:00+00:00",
      "deadline_join_at": "2026-01-10T18:00:00+00:00",
      "your_status": "invited",
      "participants": {
        "joined": 5,
        "declined": 2,
        "invited": 3
      },
      "created_at": "2026-01-01T10:00:00+00:00"
    }
  ],
  "meta": {
    "total": 10,
    "limit": 20,
    "offset": 0
  }
}
```

##### GET /events/{eventId}
Get event details. Requires authentication.

**Response (200):**
```json
{
  "id": 45,
  "group_id": 5,
  "group_name": "Weekend Trip Crew",
  "created_by": {"id": 2, "name": "Jane Doe"},
  "title": "Beach Day",
  "description": "Let's go to the beach!",
  "state": "offered",
  "start_at": "2026-01-15T10:00:00+00:00",
  "end_at": "2026-01-15T18:00:00+00:00",
  "deadline_join_at": "2026-01-10T18:00:00+00:00",
  "your_status": "joined",
  "participants": {"joined": 5, "declined": 2, "invited": 3},
  "participants_list": [
    {"user": {"id": 1, "name": "John"}, "status": "joined", "role": "owner"},
    {"user": {"id": 2, "name": "Jane"}, "status": "joined", "role": "participant"}
  ],
  "chat_id": 25,
  "created_at": "2026-01-01T10:00:00+00:00",
  "updated_at": "2026-01-01T11:30:00+00:00"
}
```

##### POST /groups/{groupId}/events
Create an event. Requires authentication.

**Request:**
```json
{
  "title": "Hiking Trip",
  "description": "Mountain hiking adventure",
  "start_at": "2026-02-01T08:00:00+00:00",
  "end_at": "2026-02-01T18:00:00+00:00",
  "deadline_join_at": "2026-01-25T18:00:00+00:00"
}
```

**Response (201):** Event object.

##### POST /events/{eventId}/join
Join an event. Requires authentication.

**Response (200):**
```json
{
  "success": true,
  "your_status": "joined"
}
```

##### POST /events/{eventId}/decline
Decline an event. Requires authentication.

**Response (200):**
```json
{
  "success": true,
  "your_status": "declined"
}
```

---

## Authentication Flow

### Initial Login

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   App       │     │   API       │     │   Storage   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │  POST /auth/login │                   │
       │──────────────────>│                   │
       │                   │                   │
       │  access_token +   │                   │
       │  refresh_token    │                   │
       │<──────────────────│                   │
       │                   │                   │
       │  Store tokens securely               │
       │──────────────────────────────────────>│
       │                   │                   │
```

### Token Refresh

```
┌─────────────┐     ┌─────────────┐
│   App       │     │   API       │
└──────┬──────┘     └──────┬──────┘
       │                   │
       │  API Request      │
       │──────────────────>│
       │                   │
       │  401 Unauthorized │
       │<──────────────────│
       │                   │
       │  POST /auth/refresh
       │──────────────────>│
       │                   │
       │  New tokens       │
       │<──────────────────│
       │                   │
       │  Retry original   │
       │  request          │
       │──────────────────>│
       │                   │
```

### Implementation Notes

1. **Token Storage:**
   - Use `flutter_secure_storage` for storing tokens securely
   - Never store tokens in SharedPreferences (not encrypted)

2. **Token Refresh Logic:**
   - Check if access token is expired before making requests
   - If expired, refresh automatically
   - If refresh fails with 401, redirect to login

3. **Device UUID:**
   - Generate once on first launch using `uuid` package
   - Store persistently in secure storage
   - Use same UUID for all sessions on this device

---

## Data Models

### Dart Model Examples

```dart
class User {
  final int id;
  final String email;
  final String name;
  final String locale;
  final String timezone;

  User({
    required this.id,
    required this.email,
    required this.name,
    required this.locale,
    required this.timezone,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      email: json['email'],
      name: json['name'],
      locale: json['locale'],
      timezone: json['timezone'],
    );
  }
}

class Group {
  final int id;
  final String slug;
  final String name;
  final String? description;

  Group({
    required this.id,
    required this.slug,
    required this.name,
    this.description,
  });

  factory Group.fromJson(Map<String, dynamic> json) {
    return Group(
      id: json['id'],
      slug: json['slug'],
      name: json['name'],
      description: json['description'],
    );
  }
}

class Message {
  final int id;
  final UserRef? user;
  final String messageType;
  final String? body;
  final int? replyToId;
  final EventRef? refEvent;
  final PollRef? refPoll;
  final UserRef? refUser;
  final DateTime createdAt;

  Message({
    required this.id,
    this.user,
    required this.messageType,
    this.body,
    this.replyToId,
    this.refEvent,
    this.refPoll,
    this.refUser,
    required this.createdAt,
  });

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'],
      user: json['user'] != null ? UserRef.fromJson(json['user']) : null,
      messageType: json['message_type'],
      body: json['body'],
      replyToId: json['reply_to_id'],
      refEvent: json['ref_event'] != null ? EventRef.fromJson(json['ref_event']) : null,
      refPoll: json['ref_poll'] != null ? PollRef.fromJson(json['ref_poll']) : null,
      refUser: json['ref_user'] != null ? UserRef.fromJson(json['ref_user']) : null,
      createdAt: DateTime.parse(json['created_at']),
    );
  }

  bool get isSystemMessage => messageType != 'text';
}

class Poll {
  final int id;
  final int groupId;
  final UserRef? createdBy;
  final String question;
  final String? description;
  final bool multiselect;
  final List<PollOption> options;
  final List<int> yourVotes;
  final int totalVoters;
  final String status;
  final DateTime? deadlineAt;
  final DateTime createdAt;

  Poll({
    required this.id,
    required this.groupId,
    this.createdBy,
    required this.question,
    this.description,
    required this.multiselect,
    required this.options,
    required this.yourVotes,
    required this.totalVoters,
    required this.status,
    this.deadlineAt,
    required this.createdAt,
  });

  factory Poll.fromJson(Map<String, dynamic> json) {
    return Poll(
      id: json['id'],
      groupId: json['group_id'],
      createdBy: json['created_by'] != null ? UserRef.fromJson(json['created_by']) : null,
      question: json['question'],
      description: json['description'],
      multiselect: json['multiselect'],
      options: (json['options'] as List).map((o) => PollOption.fromJson(o)).toList(),
      yourVotes: List<int>.from(json['your_votes'] ?? []),
      totalVoters: json['total_voters'],
      status: json['status'],
      deadlineAt: json['deadline_at'] != null ? DateTime.parse(json['deadline_at']) : null,
      createdAt: DateTime.parse(json['created_at']),
    );
  }

  bool get isOpen => status == 'open';
  bool get isExpired => deadlineAt != null && deadlineAt!.isBefore(DateTime.now());
}

class Event {
  final int id;
  final int groupId;
  final UserRef? createdBy;
  final String title;
  final String? description;
  final String state;
  final DateTime startAt;
  final DateTime? endAt;
  final DateTime? deadlineJoinAt;
  final String yourStatus;
  final Map<String, int> participants;
  final DateTime createdAt;

  Event({
    required this.id,
    required this.groupId,
    this.createdBy,
    required this.title,
    this.description,
    required this.state,
    required this.startAt,
    this.endAt,
    this.deadlineJoinAt,
    required this.yourStatus,
    required this.participants,
    required this.createdAt,
  });

  factory Event.fromJson(Map<String, dynamic> json) {
    return Event(
      id: json['id'],
      groupId: json['group_id'],
      createdBy: json['created_by'] != null ? UserRef.fromJson(json['created_by']) : null,
      title: json['title'],
      description: json['description'],
      state: json['state'],
      startAt: DateTime.parse(json['start_at']),
      endAt: json['end_at'] != null ? DateTime.parse(json['end_at']) : null,
      deadlineJoinAt: json['deadline_join_at'] != null ? DateTime.parse(json['deadline_join_at']) : null,
      yourStatus: json['your_status'],
      participants: Map<String, int>.from(json['participants']),
      createdAt: DateTime.parse(json['created_at']),
    );
  }

  bool get canRespond => state == 'offered' || state == 'preparing';
  bool get isJoined => yourStatus == 'joined';
  bool get isDeclined => yourStatus == 'declined';
}
```

---

## Implementation Guidelines

### Recommended Packages

```yaml
dependencies:
  flutter:
    sdk: flutter

  # State Management (choose one)
  provider: ^6.0.0
  # OR riverpod: ^2.0.0
  # OR flutter_bloc: ^8.0.0

  # Networking
  dio: ^5.0.0

  # Secure Storage
  flutter_secure_storage: ^9.0.0

  # Local Database (for offline cache)
  hive: ^2.0.0
  hive_flutter: ^1.0.0

  # Push Notifications
  firebase_messaging: ^14.0.0
  firebase_core: ^2.0.0

  # Utilities
  uuid: ^4.0.0
  intl: ^0.18.0
  timeago: ^3.0.0

  # UI
  cached_network_image: ^3.0.0
  shimmer: ^3.0.0
```

### Project Structure

```
lib/
├── main.dart
├── app.dart
├── config/
│   ├── api_config.dart
│   └── app_config.dart
├── core/
│   ├── api/
│   │   ├── api_client.dart
│   │   ├── api_interceptor.dart
│   │   └── api_exceptions.dart
│   ├── auth/
│   │   ├── auth_service.dart
│   │   └── token_storage.dart
│   └── sync/
│       └── sync_service.dart
├── models/
│   ├── user.dart
│   ├── group.dart
│   ├── message.dart
│   ├── poll.dart
│   ├── event.dart
│   └── notification.dart
├── repositories/
│   ├── auth_repository.dart
│   ├── group_repository.dart
│   ├── message_repository.dart
│   ├── poll_repository.dart
│   ├── event_repository.dart
│   └── notification_repository.dart
├── providers/
│   ├── auth_provider.dart
│   ├── groups_provider.dart
│   ├── messages_provider.dart
│   ├── polls_provider.dart
│   ├── events_provider.dart
│   └── notifications_provider.dart
├── screens/
│   ├── auth/
│   │   └── login_screen.dart
│   ├── home/
│   │   └── home_screen.dart
│   ├── groups/
│   │   ├── groups_list_screen.dart
│   │   └── group_detail_screen.dart
│   ├── chat/
│   │   └── chat_screen.dart
│   ├── polls/
│   │   ├── polls_list_screen.dart
│   │   ├── poll_detail_screen.dart
│   │   └── create_poll_screen.dart
│   ├── events/
│   │   ├── events_list_screen.dart
│   │   ├── event_detail_screen.dart
│   │   └── create_event_screen.dart
│   └── notifications/
│       └── notifications_screen.dart
├── widgets/
│   ├── common/
│   │   ├── loading_indicator.dart
│   │   ├── error_widget.dart
│   │   └── empty_state.dart
│   ├── messages/
│   │   ├── message_bubble.dart
│   │   ├── system_message.dart
│   │   └── message_input.dart
│   ├── polls/
│   │   ├── poll_card.dart
│   │   └── poll_option_tile.dart
│   └── events/
│       ├── event_card.dart
│       └── participant_avatar.dart
└── utils/
    ├── date_utils.dart
    ├── validators.dart
    └── extensions.dart
```

### API Client with Token Refresh

```dart
class ApiClient {
  late Dio _dio;
  final TokenStorage _tokenStorage;

  ApiClient(this._tokenStorage) {
    _dio = Dio(BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: Duration(seconds: 30),
      receiveTimeout: Duration(seconds: 30),
    ));

    _dio.interceptors.add(AuthInterceptor(_tokenStorage, _dio));
  }
}

class AuthInterceptor extends Interceptor {
  final TokenStorage _tokenStorage;
  final Dio _dio;
  bool _isRefreshing = false;

  AuthInterceptor(this._tokenStorage, this._dio);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await _tokenStorage.getAccessToken();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode == 401 && !_isRefreshing) {
      _isRefreshing = true;

      try {
        final refreshToken = await _tokenStorage.getRefreshToken();
        if (refreshToken != null) {
          final response = await _dio.post('/auth/refresh', data: {
            'refresh_token': refreshToken,
          });

          await _tokenStorage.saveTokens(
            response.data['access_token'],
            response.data['refresh_token'],
          );

          // Retry original request
          final opts = err.requestOptions;
          opts.headers['Authorization'] = 'Bearer ${response.data['access_token']}';

          final retryResponse = await _dio.fetch(opts);
          handler.resolve(retryResponse);
          return;
        }
      } catch (e) {
        // Refresh failed, logout user
        await _tokenStorage.clearTokens();
      } finally {
        _isRefreshing = false;
      }
    }

    handler.next(err);
  }
}
```

---

## UI/UX Guidelines

### Color Scheme
Use the Groufr brand colors:
- Primary: #4F46E5 (Indigo)
- Secondary: #10B981 (Emerald)
- Background: #F9FAFB (Light Gray)
- Text: #111827 (Dark Gray)
- Error: #EF4444 (Red)

### Key Screens

1. **Login Screen**
   - Email and password fields
   - "Remember me" option
   - Error messages for invalid credentials

2. **Groups List**
   - Cards showing group name and description
   - Unread message count badge
   - Pull-to-refresh

3. **Group Chat**
   - Message bubbles (own messages on right, others on left)
   - System messages centered (user joined, event created, poll created)
   - Clickable references to events/polls
   - Message input at bottom
   - Infinite scroll for older messages

4. **Polls**
   - Poll question as title
   - Options as selectable list items
   - Vote counts shown after voting
   - Deadline countdown
   - Multi-select checkbox style for multiselect polls

5. **Events**
   - Event cards with title, date, location
   - Participant avatars
   - Join/Decline buttons
   - Status badge (offered, preparing, closed, cancelled)

6. **Notifications**
   - Grouped by date
   - Icon based on notification type
   - Unread indicator
   - Tap to navigate to related content

### Sync Strategy

1. **Initial Load:**
   - Call `/sync` without `since` parameter
   - Store `sync_timestamp` for next sync

2. **Periodic Sync (when app is active):**
   - Every 30 seconds: call `/sync?since={last_sync_timestamp}`
   - Update local data with changes

3. **Group-Specific Sync:**
   - When opening a group: call `/sync/group/{id}`
   - Get latest messages, polls, events for that group

4. **Push Notification Received:**
   - Immediate sync to get latest data
   - Update UI accordingly

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Process response |
| 201 | Created | Process response, show success |
| 204 | No Content | Success, no body |
| 400 | Bad Request | Show validation errors |
| 401 | Unauthorized | Try refresh token, or logout |
| 403 | Forbidden | Show access denied message |
| 404 | Not Found | Show not found message |
| 422 | Validation Error | Show field errors |
| 429 | Rate Limited | Retry after delay |
| 500 | Server Error | Show generic error |

### Error Response Format

```json
{
  "error": "error_code",
  "message": "Human readable message",
  "details": {
    "field_name": "Field-specific error"
  }
}
```

---

## Testing the API

### Test User
Create a test user on the web application first, then use those credentials in the mobile app.

### cURL Examples

```bash
# Login
curl -X POST https://your-domain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "device": {
      "uuid": "test-device-001",
      "platform": "android",
      "name": "Test Device"
    }
  }'

# Get groups (after login, use the access_token)
curl https://your-domain.com/api/v1/sync \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Send message
curl -X POST https://your-domain.com/api/v1/groups/1/messages \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"body": "Hello from the mobile app!"}'
```

---

## Checklist for Implementation

- [ ] Set up Flutter project with required dependencies
- [ ] Implement secure token storage
- [ ] Create API client with interceptors
- [ ] Implement login/logout flow
- [ ] Create data models from JSON
- [ ] Build groups list screen
- [ ] Build chat screen with messages
- [ ] Implement message sending
- [ ] Build polls list and voting
- [ ] Build poll creation
- [ ] Build events list and RSVP
- [ ] Build event creation
- [ ] Implement push notifications
- [ ] Implement sync service
- [ ] Add offline support (optional)
- [ ] Error handling and loading states
- [ ] Pull-to-refresh on all lists
- [ ] Infinite scroll pagination

---

## Support

For API issues, check the server logs at:
- `temp/log/` directory on the server

For questions about the API implementation, refer to the source files:
- `app/Presentation/Api/V1/` - All API presenters
- `app/Model/Repository/` - Data access layer
- `app/Model/Service/` - Business logic
