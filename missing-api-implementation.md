# Missing API Implementations

Endpoints documented in `api.md` but not yet implemented in the Android app.

---

## Authentication

### POST /api/v1/auth/logout

Logout and revoke the current access token.

**Headers:** `Authorization: Bearer <token>`

**Response:** `204 No Content`

---

### POST /api/v1/auth/logout-all

Logout from all devices (revoke all tokens).

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "revoked_count": 3
}
```

---

### POST /api/v1/auth/forgot-password

Request a password reset email.

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "If an account with that email exists, we've sent a password reset link."
}
```

**Rate Limiting:** 3 requests per IP per 15 minutes.

---

### POST /api/v1/auth/reset-password

Reset password using the token from email.

**Request Body:**
```json
{
  "token": "reset-token-from-email",
  "password": "new-password",
  "password_confirmation": "new-password"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password has been reset successfully."
}
```

---

## User Profile

### PUT /api/v1/user/profile

Update current user's profile.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "John Doe",
  "locale": "cs_CZ",
  "timezone": "Europe/Prague"
}
```

**Response:** Updated user profile object.

---

### PUT /api/v1/user/password

Change current user's password.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "current_password": "old-password",
  "password": "new-password",
  "password_confirmation": "new-password"
}
```

**Response:**
```json
{
  "success": true
}
```

---

## Notification Preferences

### GET /api/v1/user/notification-preferences

Get user's notification preferences.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "global": {
    "new_message": {
      "email_enabled": true,
      "push_enabled": true,
      "in_app_enabled": true,
      "digest": "daily"
    },
    "event_created": {"..."},
    "event_updated": {"..."},
    "participant_status_changed": {"..."},
    "poll_created": {"..."},
    "poll_closed": {"..."},
    "user_joined": {"..."},
    "invitation_received": {"..."},
    "event_invitation_received": {"..."}
  },
  "by_group": {
    "1": {
      "new_message": {
        "email_enabled": false,
        "push_enabled": true,
        "in_app_enabled": true,
        "digest": "never"
      }
    }
  }
}
```

---

### PUT /api/v1/user/notification-preferences

Update notification preferences.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "event_type": "new_message",
  "group_id": null,
  "email_enabled": true,
  "push_enabled": true,
  "in_app_enabled": true,
  "digest": "daily|weekly|none"
}
```

**Response:** Updated preferences object.

---

## Groups

### GET /api/v1/groups/{groupId}/digest

Get the user's email digest frequency for a group.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "digest_frequency": "daily"
}
```

---

### PUT /api/v1/groups/{groupId}/digest

Set the user's email digest frequency for a group.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "digest_frequency": "daily|weekly|never"
}
```

**Response:**
```json
{
  "digest_frequency": "weekly"
}
```

---

## Invitations

### GET /api/v1/invitations

List pending group invitations for the authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "invitations": [
    {
      "id": 1,
      "group": {
        "id": 1,
        "slug": "friends",
        "name": "Friends"
      },
      "inviter": {
        "id": 2,
        "name": "Jane Doe"
      },
      "expires_at": "2026-01-09T10:00:00+00:00",
      "created_at": "2026-01-02T10:00:00+00:00"
    }
  ]
}
```

---

### GET /api/v1/invitations/{token}

Get invitation details by token. Does not require authentication.

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "status": "pending|accepted|expired|cancelled|reported",
  "is_valid": true,
  "group": {
    "id": 1,
    "slug": "friends",
    "name": "Friends"
  },
  "expires_at": "2026-01-09T10:00:00+00:00",
  "created_at": "2026-01-02T10:00:00+00:00"
}
```

---

## Events

### GET /api/v1/events/{eventId}/participants

Get event participants.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "participants": [
    {
      "user": {
        "id": 1,
        "name": "John Doe"
      },
      "status": "joined|maybe|declined|invited",
      "role": "owner|admin|participant|guest",
      "guest_name": null,
      "guest_email": null,
      "joined_at": "2026-01-02T10:00:00+00:00"
    }
  ],
  "counts": {
    "joined": 5,
    "maybe": 2,
    "declined": 2,
    "invited": 3
  }
}
```

---

### POST /api/v1/events/{eventId}/invite-guest

Invite a guest (non-member) to an event.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "email": "guest@example.com",
  "name": "Guest Name"
}
```

**Response:**
```json
{
  "success": true,
  "invitation_id": 123
}
```

**Access Control:** Only event owner/admin can invite guests.

---

## Polls

### PUT /api/v1/polls/{id}/status

Change poll status (open/close).

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "status": "open|closed"
}
```

**Response:** Updated poll object.

**Access Control:** Poll creator, group owner, group admin, event owner (if event poll), event admin (if event poll).

---

## Reports

### GET /api/v1/reports/{id}

Get report details.

**Headers:** `Authorization: Bearer <token>`

**Response:** Report object.
