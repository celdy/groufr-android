# Groufr Android App - Gap Analysis

**Date:** 2026-01-17
**Compared against:** `android-functional-spec.md` and `26-01-17-api.md`

---

## Summary

| Category | Implemented | Partially | Not Started |
|----------|-------------|-----------|-------------|
| Authentication | 5 | 0 | 3 |
| Groups | 2 | 1 | 2 |
| Events | 5 | 2 | 4 |
| Chat | 2 | 0 | 1 |
| Polls | 3 | 1 | 1 |
| Expenses | 0 | 0 | 5 |
| Notifications | 4 | 1 | 3 |
| Profile | 1 | 0 | 4 |
| Navigation | 1 | 0 | 1 |
| Offline | 0 | 0 | 3 |
| Deep Links | 0 | 0 | 3 |

**Overall Progress: ~45% of MVP features implemented**

---

## Detailed Analysis

### 1. Authentication

| Feature | Status | Priority | Notes |
|---------|--------|----------|-------|
| Login screen | DONE | - | Email/password fields, login button |
| Token storage | DONE | - | Uses TokenStore with EncryptedSharedPreferences |
| Session refresh | DONE | - | AuthInterceptor handles token refresh |
| Logout | DONE | - | Clears tokens and redirects to login |
| Persistent session (Remember me) | DONE | HIGH | Mobile app always requests long-lived refresh tokens via `remember_me=true` |
| Forgot password flow | NOT STARTED | LOW | No ForgotPasswordActivity |
| Password reset (deep link) | NOT STARTED | LOW | No deep link handling |
| Two-factor authentication (PIN) | NOT STARTED | LOW | Optional 6-digit PIN for extra security, future implementation |

**Priority:** Authentication core is complete. Forgot/reset password and 2FA are low priority for future

---

### 2. Groups

| Feature | Status | Notes |
|---------|--------|-------|
| Groups list | DONE | MainActivity shows list of groups |
| Group detail (chat) | DONE | GroupDetailActivity with messages |
| Group members list | NOT STARTED | No GroupMembersActivity |
| Invite member | NOT STARTED | No invite flow |
| Group settings | PARTIAL | Basic navigation exists, no actual settings |

**Priority:** MEDIUM - Members list would be useful

---

### 3. Events

| Feature | Status | Notes |
|---------|--------|-------|
| Events list (per group) | DONE | EventsActivity |
| Event detail | DONE | EventDetailActivity with tabs |
| Event join | DONE | API call implemented |
| Event decline | DONE | API call implemented |
| Event chat | DONE | Chat tab in EventDetailActivity |
| Create event | DONE | EventCreateActivity |
| Event "maybe" status | NOT STARTED | No API endpoint called, no UI |
| Edit event | NOT STARTED | No edit functionality |
| Event status change | PARTIAL | Dialog exists but doesn't call API |
| Guest invitations | NOT STARTED | No invite guest flow |
| Min/max participants display | NOT STARTED | Fields not shown in UI |
| My Events (global) | NOT STARTED | No cross-group events view |

**Priority:** HIGH - "Maybe" status is important, edit event useful

---

### 4. Chat

| Feature | Status | Notes |
|---------|--------|-------|
| Group chat | DONE | Send/receive in GroupDetailActivity |
| Event chat | DONE | Send/receive in EventDetailActivity |
| System messages | PARTIAL | MessageAdapter handles some types |
| Reply to message | NOT STARTED | No reply functionality |
| Unread divider | DONE | Implemented in event chat |

**Priority:** LOW - Core chat works

---

### 5. Polls

| Feature | Status | Notes |
|---------|--------|-------|
| Group polls list | DONE | PollsActivity |
| Poll detail | DONE | PollDetailActivity |
| Vote/clear vote | DONE | API calls implemented |
| Create poll | DONE | PollCreateActivity |
| Event-level polls | NOT STARTED | Only group polls, no event polls |
| Poll deadline display | PARTIAL | Field exists in model but not always shown |

**Priority:** MEDIUM - Event polls would be useful

---

### 6. Expenses (NOT IMPLEMENTED)

| Feature | Status | Notes |
|---------|--------|-------|
| Expenses list | NOT STARTED | No ExpensesActivity |
| Expense detail | NOT STARTED | No ExpenseDetailActivity |
| Add expense | NOT STARTED | No AddExpenseActivity |
| Edit/delete expense | NOT STARTED | - |
| Balances view | NOT STARTED | - |

**Priority:** HIGH - Core feature for group events

**API Endpoints needed:**
- `GET /api/v1/events/{eventId}/expenses`
- `POST /api/v1/events/{eventId}/expenses`
- `PUT /api/v1/expenses/{id}`
- `DELETE /api/v1/expenses/{id}`

---

### 7. Notifications

| Feature | Status | Notes |
|---------|--------|-------|
| Notifications list | DONE | NotificationsActivity |
| Mark as read (single) | DONE | On tap |
| Mark all as read | DONE | Menu action |
| Filter unread only | DONE | Toggle in menu |
| Notification count badge | DONE | MainViewModel provides count |
| Push notifications (FCM) | NOT STARTED | No FCM integration |
| Event chat notification nav | PARTIAL | Goes to group chat, not event chat |
| Background sync | PARTIAL | NotificationSyncWorker exists but may not be scheduled |
| Invitation notifications | NOT STARTED | No handling for invitation_received |

**Priority:** HIGH - Push notifications important for engagement

---

### 8. Profile

| Feature | Status | Notes |
|---------|--------|-------|
| Profile screen | DONE | Shows name, logout button |
| Edit profile | NOT STARTED | No edit fields |
| Change password | NOT STARTED | No ChangePasswordActivity |
| Notification settings | NOT STARTED | No settings UI |
| About/Support | NOT STARTED | No about screen |

**Priority:** MEDIUM - Edit profile and change password are useful

**API Endpoints needed:**
- `GET /api/v1/user/profile`
- `PUT /api/v1/user/profile`
- `PUT /api/v1/user/password`
- `GET/PUT /api/v1/user/notification-preferences`

---

### 9. Navigation

| Feature | Status | Notes |
|---------|--------|-------|
| Toolbar navigation | DONE | Action bar with menu items |
| Bottom navigation bar | NOT STARTED | Spec calls for bottom nav with Groups/Events/Notifications/Profile |

**Priority:** LOW - Current navigation works, bottom nav is UX improvement

---

### 10. Deep Links

| Feature | Status | Notes |
|---------|--------|-------|
| Group invitation deep link | NOT STARTED | `groufr://invitation?token=xxx` |
| Event invitation deep link | NOT STARTED | `groufr://event-invitation?token=xxx` |
| Password reset deep link | NOT STARTED | `groufr://reset-password?token=xxx` |

**Priority:** MEDIUM - Needed for invitation flow

---

### 11. Offline Support

| Feature | Status | Notes |
|---------|--------|-------|
| Local database (Room) | NOT STARTED | No Room entities |
| Offline message queue | NOT STARTED | Messages only sent online |
| Cached data display | NOT STARTED | No cache-first strategy |

**Priority:** LOW for MVP, HIGH for production

---

## Recommended Implementation Order

### Phase 1: Critical Missing Features
1. **Password reset flow** - Essential for user recovery
2. **Push notifications (FCM)** - Critical for engagement
3. **Expenses module** - Core feature for events
4. **Event "maybe" status** - Complete the event participation flow

### Phase 2: Important Features
5. **Edit profile** - Users need to update their info
6. **Change password** - Security feature
7. **Event edit** - Event creators need this
8. **Group members list** - Useful for group management
9. **Notification navigation fix** - Event messages should open event chat

### Phase 3: Enhanced UX
10. **Deep link handling** - For invitations
11. **Bottom navigation** - Better navigation pattern
12. **Event-level polls** - Complete polls feature
13. **Guest invitations** - Allow non-members in events

### Phase 4: Production Ready
14. **Offline support** - Room database, caching
15. **Background sync** - Keep data fresh
16. **Notification settings** - User preferences

---

## Files to Create

### New Activities
- `ForgotPasswordActivity.kt`
- `ResetPasswordActivity.kt`
- `ChangePasswordActivity.kt`
- `EditProfileActivity.kt`
- `GroupMembersActivity.kt`
- `ExpensesActivity.kt` (or tab in EventDetail)
- `ExpenseDetailActivity.kt`
- `AddExpenseActivity.kt`
- `NotificationSettingsActivity.kt`

### New ViewModels
- `ForgotPasswordViewModel.kt`
- `ResetPasswordViewModel.kt`
- `ChangePasswordViewModel.kt`
- `EditProfileViewModel.kt`
- `GroupMembersViewModel.kt`
- `ExpensesViewModel.kt`

### New Models
- `data/expenses/ExpenseModels.kt`
- `data/expenses/ExpensesRepository.kt`
- `data/profile/ProfileModels.kt`
- `data/profile/ProfileRepository.kt`

### New Layouts
- `activity_forgot_password.xml`
- `activity_reset_password.xml`
- `activity_change_password.xml`
- `activity_edit_profile.xml`
- `activity_group_members.xml`
- `activity_expenses.xml` (or fragment)
- `activity_expense_detail.xml`
- `activity_add_expense.xml`
- `item_expense.xml`
- `item_group_member.xml`

### API Service Additions
```kotlin
// Password reset
@POST("auth/forgot-password")
suspend fun forgotPassword(@Body request: ForgotPasswordRequest): GenericResponse

@POST("auth/reset-password")
suspend fun resetPassword(@Body request: ResetPasswordRequest): GenericResponse

// Profile
@GET("user/profile")
suspend fun getProfile(): UserProfileDto

@PUT("user/profile")
suspend fun updateProfile(@Body request: UpdateProfileRequest): UserProfileDto

@PUT("user/password")
suspend fun changePassword(@Body request: ChangePasswordRequest): GenericResponse

// Group members
@GET("groups/{groupId}/members")
suspend fun getGroupMembers(@Path("groupId") groupId: Long): GroupMembersResponse

// Expenses
@GET("events/{eventId}/expenses")
suspend fun getEventExpenses(@Path("eventId") eventId: Long): ExpensesResponse

@POST("events/{eventId}/expenses")
suspend fun createExpense(@Path("eventId") eventId: Long, @Body request: CreateExpenseRequest): ExpenseDto

@PUT("expenses/{id}")
suspend fun updateExpense(@Path("id") expenseId: Long, @Body request: UpdateExpenseRequest): ExpenseDto

@DELETE("expenses/{id}")
suspend fun deleteExpense(@Path("id") expenseId: Long)

// Event actions
@POST("events/{eventId}/maybe")
suspend fun maybeEvent(@Path("eventId") eventId: Long): EventActionResponse

@PUT("events/{id}")
suspend fun updateEvent(@Path("id") eventId: Long, @Body request: UpdateEventRequest): EventDetailDto

// Push token
@PUT("device/push-token")
suspend fun registerPushToken(@Body request: PushTokenRequest): GenericResponse
```

---

## Current Project Structure

```
app/src/main/java/com/celdy/groufr/
├── data/
│   ├── auth/           # AuthModels, AuthRepository
│   ├── device/         # DeviceInfoProvider
│   ├── events/         # EventModels, EventsRepository
│   ├── groups/         # GroupDto, GroupsRepository
│   ├── messages/       # MessageModels, MessagesRepository
│   ├── network/        # ApiService, AuthInterceptor, NetworkModule
│   ├── notifications/  # NotificationModels, NotificationsRepository, SyncManager
│   ├── polls/          # PollModels, PollsRepository
│   ├── storage/        # TokenStore
│   └── sync/           # SyncModels
├── di/
│   └── NetworkModule.kt
├── ui/
│   ├── common/         # SvgLoader, ChatDateFormatter
│   ├── eventcreate/    # EventCreateActivity/ViewModel
│   ├── eventdetail/    # EventDetailActivity/ViewModel, Adapters
│   ├── events/         # EventsActivity/ViewModel, Adapter
│   ├── groupdetail/    # GroupDetailActivity/ViewModel, MessageAdapter
│   ├── launcher/       # LauncherActivity
│   ├── login/          # LoginActivity/ViewModel
│   ├── main/           # MainActivity/ViewModel, GroupAdapter
│   ├── notifications/  # NotificationsActivity/ViewModel, Adapter
│   ├── pollcreate/     # PollCreateActivity/ViewModel
│   ├── polldetail/     # PollDetailActivity/ViewModel, Adapter
│   ├── polls/          # PollsActivity/ViewModel, Adapter
│   └── profile/        # ProfileActivity
└── GroufrApplication.kt
```

---

## Technical Debt

1. **No unit tests** - Only example tests exist
2. **No offline support** - No Room database
3. **Hardcoded strings** - Some UI text not in strings.xml
4. **No error handling standardization** - Each screen handles errors differently
5. **No loading state abstraction** - Repeated Loading/Content/Error pattern
6. **Missing ProGuard rules** - May need obfuscation rules for release

---

## Notes

- The app uses **Hilt** for dependency injection
- Uses **View Binding** (not Data Binding or Compose)
- Uses **Retrofit** for API calls
- Uses **Coroutines** for async operations
- Architecture: **MVVM** with LiveData
- No bottom navigation - uses toolbar menu instead
