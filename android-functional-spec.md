# Groufr Android App - Functional Specification

**Version:** 1.0
**Date:** 2026-01-17

---

## 1. Overview

### 1.1 App Purpose
Groufr is a privacy-first mobile application for groups of friends to:
- Organize events together
- Communicate via group and event chats
- Vote on decisions through polls
- Split costs fairly with expense tracking

### 1.2 Core Principles
- **Privacy-first**: No ads, no tracking, no selling user data
- **Offline-capable**: Core features work offline with sync when connected
- **Real-time**: Push notifications and live updates for chat/events
- **Simple**: Clean UI focused on the essential features

### 1.3 Target Platforms
- Android 8.0 (API 26) and higher
- Kotlin with Jetpack Compose (recommended)
- Material Design 3

---

## 2. User Roles & Access Control

### 2.1 Group Roles
| Role | Permissions |
|------|-------------|
| **Owner** | Full control, can delete group, transfer ownership |
| **Admin** | Manage members, create/edit events, manage polls |
| **Member** | Participate in chat, events, polls; create events |

### 2.2 Event Roles
| Role | Permissions |
|------|-------------|
| **Owner** | Full control of event, can cancel |
| **Admin** | Edit event, manage participants, manage expenses |
| **Participant** | Join/decline, chat, add expenses, vote on polls |
| **Guest** | Non-member invited to specific event only; can chat, vote, add expenses |

### 2.3 Access Scope
- **Group scope**: Only group members can access group chat, group polls
- **Event scope**: All event participants (members + guests) can access event chat, event polls, event expenses

---

## 3. Authentication

### 3.1 Login Screen
**Fields:**
- Email (required, validated)
- Password (required)
- "Remember me" checkbox
- "Forgot password?" link

**Behavior:**
- On success: Store access token + refresh token securely (EncryptedSharedPreferences)
- On failure: Show error message, log attempt
- Rate limiting: Show message if too many failed attempts

**Navigation:**
- Success → Groups List
- Forgot password → Password Reset screen

### 3.2 Password Reset Flow

#### 3.2.1 Request Reset Screen
**Fields:**
- Email (required)

**Behavior:**
- Submit sends request to API
- Always show success message (security: don't reveal if email exists)
- Navigate to Login with success message

#### 3.2.2 Reset Password Screen (Deep Link)
**Entry:** Deep link from email: `groufr://reset-password?token=xxx`

**Fields:**
- New password (required, min 8 chars)
- Confirm password (required, must match)

**Behavior:**
- Validate token on screen load
- If invalid/expired: Show error, link to request new reset
- On success: Navigate to Login with success message

### 3.3 Session Management
- Access token expires in 1 hour
- Refresh token expires in 30 days
- Auto-refresh access token when expired
- On refresh failure: Logout, navigate to Login

### 3.4 Logout
- Revoke current token via API
- Clear local storage
- Navigate to Login

---

## 4. Main Navigation

### 4.1 Bottom Navigation Bar
| Tab | Icon | Destination |
|-----|------|-------------|
| Groups | people | Groups List |
| Events | calendar | My Events (across all groups) |
| Notifications | bell | Notifications List |
| Profile | person | User Profile |

### 4.2 Navigation Structure
```
App
├── Login
├── Password Reset
├── Main (Bottom Nav)
│   ├── Groups List
│   │   └── Group Detail
│   │       ├── Group Chat
│   │       ├── Group Members
│   │       ├── Group Events
│   │       ├── Group Polls
│   │       └── Group Settings
│   ├── My Events
│   │   └── Event Detail
│   │       ├── Event Chat
│   │       ├── Event Participants
│   │       ├── Event Polls
│   │       ├── Event Expenses
│   │       └── Event Settings
│   ├── Notifications
│   └── Profile
│       ├── Edit Profile
│       ├── Change Password
│       ├── Notification Settings
│       └── About/Support
└── Invitation Accept (Deep Link)
```

---

## 5. Groups

### 5.1 Groups List Screen
**Display:**
- List of groups user is member of
- Each item shows:
  - Group name
  - Group description (truncated)
  - User's role badge (Owner/Admin/Member)
  - Unread notification count badge
  - Member count

**Actions:**
- Tap group → Group Detail
- Pull to refresh

**Empty State:**
- Message: "You're not in any groups yet"
- Info about joining via invitation

### 5.2 Group Detail Screen
**Header:**
- Group name
- Group description
- Member count
- User's role

**Tabs or Sections:**
1. **Chat** - Group chat (default)
2. **Events** - Group events list
3. **Polls** - Group-level polls
4. **Members** - Member list

**Actions (based on role):**
- Invite member (Admin+)
- Edit group (Admin+)
- Leave group
- Group settings

### 5.3 Group Members Screen
**Display:**
- List of members with:
  - Avatar/initials
  - Name
  - Role badge
  - Joined date

**Actions (Admin+):**
- Change member role
- Remove member

### 5.4 Invite Member Flow
**Entry:** Button in Group Detail (Admin+)

**Fields:**
- Email address

**Behavior:**
- Send invitation via API
- Show success/error message
- Invitation email sent by backend

---

## 6. Events

### 6.1 My Events Screen (Global)
**Display:**
- Combined list of events from all groups
- Segmented/tabs: Upcoming | Past
- Each item shows:
  - Event title
  - Group name
  - Date/time
  - Status badge (Offered/Preparing/Closed/Cancelled)
  - Your status (Joined/Maybe/Declined/Invited)
  - Participant count

**Actions:**
- Tap event → Event Detail
- Pull to refresh
- Filter by status

### 6.2 Group Events Screen
**Display:**
- Events for specific group only
- Same layout as My Events

**Actions:**
- Create new event (Member+)
- Filter by status

### 6.3 Event Detail Screen
**Header:**
- Event title
- Group name (tappable → Group Detail)
- Status badge
- Date/time
- Location (if any)
- Description

**Quick Stats:**
- Participants: X joined, Y maybe, Z declined
- Min/Max participants (if set)

**Your Status Section:**
- Current status displayed
- Action buttons: Join | Maybe | Decline

**Tabs:**
1. **Chat** - Event chat
2. **Participants** - Participant list
3. **Polls** - Event-level polls
4. **Expenses** - Expense tracking

**Actions (based on role):**
- Edit event (Owner/Admin)
- Invite guest (Owner/Admin)
- Cancel event (Owner)

### 6.4 Event States
| State | Description | User Actions |
|-------|-------------|--------------|
| **Offered** | Event proposed, collecting interest | Join/Maybe/Decline |
| **Preparing** | Event confirmed, preparation phase | Same + expense tracking active |
| **Closed** | Event finished | View only, expense settlement |
| **Cancelled** | Event cancelled | View only |

### 6.5 Create/Edit Event Screen
**Fields:**
- Title (required)
- Description
- Start date/time (required)
- End date/time
- Join deadline
- Min participants
- Max participants
- Currency (for expenses)

**Validation:**
- End must be after start
- Deadline must be before start

### 6.6 Event Participants Screen
**Display:**
- Grouped by status: Joined | Maybe | Declined | Invited
- Each item shows:
  - Avatar/initials
  - Name
  - Role badge (Owner/Admin/Participant/Guest)
  - Status

**Guest Display:**
- Guest badge
- Guest name/email if not registered user

**Actions (Owner/Admin):**
- Change participant role
- Remove participant
- Invite guest

### 6.7 Invite Guest Flow
**Entry:** Button in Participants screen (Owner/Admin)

**Fields:**
- Email address
- Name (optional, for display before they register)

**Behavior:**
- Creates event invitation
- Guest receives email with link
- Guest can accept without being group member

---

## 7. Chat

### 7.1 Group Chat
**Access:** Group members only

**Display:**
- Messages in chronological order (newest at bottom)
- Message bubbles with:
  - Sender name/avatar
  - Message text
  - Timestamp
  - Read indicator (optional)
- System messages styled differently:
  - User joined
  - Event created
  - Poll created

**Actions:**
- Send text message
- Reply to message (optional, Phase 2)
- Scroll to load older messages
- Pull down to refresh/sync

**Unread Indicator:**
- Show unread count badge
- "New messages" divider
- Mark as read when scrolled to

### 7.2 Event Chat
**Access:** Event participants (joined/maybe) + guests

**Display:** Same as group chat

**Distinction:**
- Separate notification counts
- Separate read status tracking
- Guests can participate (unlike group chat)

### 7.3 Message Types
| Type | Display |
|------|---------|
| `text` | Normal message bubble |
| `system` | Centered, muted text |
| `user_joined` | "[User] joined the group" with link to user |
| `event_created` | "[User] created event [Title]" with link to event |
| `poll_created` | "[User] created poll [Question]" with link to poll |

### 7.4 Real-time Updates
- Use polling initially (every 10-30 seconds when chat visible)
- Push notifications for new messages when app backgrounded
- Future: WebSocket for true real-time

---

## 8. Polls

### 8.1 Polls List Screen
**Display:**
- List of polls (group-level or event-level depending on context)
- Each item shows:
  - Question
  - Creator name
  - Status (Open/Closed)
  - Deadline (if set)
  - "You voted" indicator
  - Total votes count

**Filters:**
- All | Open | Closed

### 8.2 Poll Detail Screen
**Display:**
- Question
- Description (if any)
- Creator + creation date
- Deadline (if set, with countdown if soon)
- Status

**Options List:**
- Each option shows:
  - Label
  - Vote count
  - Percentage bar
  - Checkmark if you voted for it
  - Voter names (optional, Phase 2)

**Actions (if Open):**
- Vote (single or multi-select based on poll type)
- Change vote
- Remove vote

### 8.3 Create Poll Screen
**Fields:**
- Question (required)
- Description
- Options (min 2, add more dynamically)
- Multi-select toggle
- Deadline (optional date/time)

**Context:**
- Group poll: Created from Group Detail
- Event poll: Created from Event Detail

### 8.4 Voting Behavior
- **Single-select**: Tap option to vote, tap again to change
- **Multi-select**: Tap to toggle options, submit button
- Immediate feedback on vote counts

---

## 9. Expenses

### 9.1 Event Expenses Screen
**Access:** Event participants

**Display:**
- Summary card:
  - Total expenses
  - Your balance (positive = owed to you, negative = you owe)
- Expenses list:
  - Each expense shows:
    - Label
    - Amount
    - Payer name
    - Date
    - Share count

**Actions:**
- Add expense
- Tap expense → Expense Detail

### 9.2 Expense Detail Screen
**Display:**
- Label
- Amount
- Currency
- Payer
- Created by + date
- Shares breakdown:
  - Each participant's share amount

**Actions (Creator/Admin):**
- Edit expense
- Delete expense

### 9.3 Add/Edit Expense Screen
**Fields:**
- Label (required)
- Amount (required, numeric)
- Payer (dropdown of participants)
- Shares:
  - Split equally toggle
  - Or: Custom amounts per participant

**Split Options:**
1. **Equal split**: Total / number of selected participants
2. **Custom split**: Manual entry per participant

**Validation:**
- Shares must sum to total amount

### 9.4 Balances View
**Display:**
- List of participants with their balance
- Positive: They are owed money (green)
- Negative: They owe money (red)
- Suggested settlements (who pays whom)

---

## 10. Notifications

### 10.1 Notifications List Screen
**Display:**
- Chronological list of notifications
- Each item shows:
  - Icon based on type
  - Actor name/avatar
  - Description text
  - Timestamp
  - Unread indicator (dot/bold)
  - Group/event context

**Grouping (optional):**
- Group by date (Today, Yesterday, This Week, etc.)

**Actions:**
- Tap notification → Navigate to relevant screen
- Mark all as read
- Pull to refresh

### 10.2 Notification Types & Navigation

| Type | Text Template | Navigation |
|------|---------------|------------|
| `new_message` (group) | "[User] sent a message in [Group]" | Group Chat |
| `new_message` (event) | "[User] sent a message in [Event]" | Event Chat |
| `event_created` | "[User] created event [Title]" | Event Detail |
| `event_updated` | "[User] updated event [Title]" | Event Detail |
| `participant_status_changed` | "[User] joined [Event]" | Event Participants |
| `poll_created` | "[User] created a poll in [Group/Event]" | Poll Detail |
| `poll_closed` | "Poll [Question] has been closed" | Poll Detail |
| `user_joined` | "[User] joined [Group]" | Group Members |
| `invitation_received` | "[User] invited you to join [Group]" | Invitation Accept |
| `event_invitation_received` | "[User] invited you to [Event]" | Event Detail |

### 10.3 Push Notifications
**Registration:**
- Register FCM token on login
- Update token if changed
- Remove token on logout

**Payload Structure:**
```json
{
  "notification": {
    "title": "New message in Friends",
    "body": "John: Hey everyone!"
  },
  "data": {
    "type": "new_message",
    "group_id": "1",
    "chat_id": "1",
    "event_id": null
  }
}
```

**Behavior:**
- Tap notification → Open app to relevant screen
- Badge count on app icon
- Group notifications by conversation (Android notification channels)

### 10.4 Notification Badges
**Locations:**
- App icon (total unread count)
- Bottom nav: Notifications tab
- Groups list: Per-group badge
- Group detail: Per-section badges (chat, events, polls)
- Events list: Per-event badge

---

## 11. User Profile

### 11.1 Profile Screen
**Display:**
- Avatar (initials-based or uploaded)
- Name
- Email
- Locale
- Timezone
- Member since

**Actions:**
- Edit profile
- Change password
- Notification settings
- Support/Help
- About
- Logout

### 11.2 Edit Profile Screen
**Fields:**
- Name (required)
- Locale (dropdown)
- Timezone (dropdown/searchable)

### 11.3 Change Password Screen
**Fields:**
- Current password (required)
- New password (required, min 8 chars)
- Confirm new password (required)

### 11.4 Notification Settings Screen
**Global Settings:**
- Push notifications enabled/disabled
- Email notifications enabled/disabled
- Email digest frequency (Daily/Weekly/Never)

**Per-Event-Type Settings:**
- New messages
- Events
- Polls
- Members

**Per-Group Overrides (optional, Phase 2):**
- Override global settings for specific groups

---

## 12. Invitations (Deep Links)

### 12.1 Group Invitation
**Entry:** Deep link `groufr://invitation?token=xxx`

**Flow:**
1. Check if logged in
2. If not logged in:
   - Check if email has account → Show login form
   - No account → Show registration form
3. If logged in:
   - Check if email matches → Show "Join Group" button
   - Email mismatch → Show "Wrong account" message
4. On join: Add to group, navigate to Group Detail

### 12.2 Event Guest Invitation
**Entry:** Deep link `groufr://event-invitation?token=xxx`

**Flow:**
1. Similar to group invitation
2. On accept: Add as event guest, navigate to Event Detail
3. Guest has access to event only, not full group

---

## 13. Offline Support

### 13.1 Cached Data
- Groups list
- Recent messages (last 50 per chat)
- Events list
- Polls and votes
- User profile

### 13.2 Offline Capabilities
| Feature | Offline Behavior |
|---------|-----------------|
| View groups | Cached data |
| View messages | Cached data |
| Send message | Queue, sync when online |
| View events | Cached data |
| Join/decline event | Queue, sync when online |
| Vote on poll | Queue, sync when online |
| View expenses | Cached data |
| Add expense | Queue, sync when online |

### 13.3 Sync Strategy
- On app foreground: Full sync
- Periodic background sync (WorkManager)
- On connectivity restored: Process queued actions

### 13.4 Conflict Resolution
- Server wins for most data
- Messages: Merge by timestamp
- Votes: Last write wins

---

## 14. Error Handling

### 14.1 Network Errors
- Show toast/snackbar with retry option
- Cache last successful data
- Show "offline" indicator in app bar

### 14.2 API Errors
| Error | User Message |
|-------|-------------|
| 401 Unauthorized | Session expired, redirect to login |
| 403 Forbidden | "You don't have permission to do this" |
| 404 Not Found | "Content not found or has been deleted" |
| 422 Validation | Show field-specific errors |
| 429 Rate Limited | "Too many requests, please wait" |
| 500 Server Error | "Something went wrong, please try again" |

### 14.3 Form Validation
- Inline validation as user types
- Disable submit until valid
- Show all errors on submit attempt

---

## 15. UI/UX Guidelines

### 15.1 Design System
- Material Design 3
- Dynamic color (Android 12+)
- Light/Dark theme support
- Consistent spacing (8dp grid)

### 15.2 Loading States
- Skeleton screens for lists
- Circular progress for actions
- Pull-to-refresh on all lists

### 15.3 Empty States
- Friendly illustration
- Clear message
- Action button where applicable

### 15.4 Accessibility
- Content descriptions for images
- Minimum touch targets (48dp)
- Sufficient color contrast
- Screen reader support

---

## 16. Technical Requirements

### 16.1 Architecture
- MVVM with Clean Architecture
- Kotlin Coroutines + Flow
- Hilt for dependency injection
- Room for local database
- Retrofit for API calls

### 16.2 Security
- Encrypted token storage (EncryptedSharedPreferences)
- Certificate pinning (optional)
- No sensitive data in logs
- Biometric authentication (optional, Phase 2)

### 16.3 Performance
- Lazy loading for lists (Paging 3)
- Image caching (Coil/Glide)
- Minimize main thread work
- Battery-efficient background sync

### 16.4 Testing
- Unit tests for ViewModels and UseCases
- Integration tests for Repository layer
- UI tests for critical flows

---

## 17. Development Phases

### Phase 1: MVP
- Authentication (login, logout, password reset)
- Groups list and detail
- Group chat (send/receive messages)
- Events list and detail
- Event join/decline
- Basic notifications
- Push notifications

### Phase 2: Full Features
- Event chat
- Polls (create, vote)
- Expenses (CRUD, balances)
- User profile editing
- Notification preferences
- Offline support
- Guest invitations

### Phase 3: Polish
- Real-time chat (WebSocket)
- File attachments in chat
- Rich notifications (actions)
- Widgets
- Deep link improvements

---

## 18. API Reference

See `26-01-17-api.md` for complete API documentation.

Key endpoints:
- `POST /api/v1/auth/login` - Authentication
- `GET /api/v1/groups` - Groups list
- `GET /api/v1/groups/{id}/messages` - Group chat
- `GET /api/v1/events/{id}` - Event detail
- `GET /api/v1/notifications` - Notifications list
- `PUT /api/v1/device/push-token` - Register push token
