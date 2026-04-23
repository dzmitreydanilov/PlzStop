# Export to Google Sheets — Implementation Plan

This plan delivers two sequential phases: platform authentication (Google + Apple Sign-In) followed by a Cloud Function that builds and shares a Google Spreadsheet, notifying the user via FCM deep link. Phase 2 depends on Phase 1 being complete for authenticated users.

---

## Prerequisites

- Firebase project already targets `europe-west1`; Cloud Functions Gen 2 must be enabled in the Firebase console
- `ITokensStorage` and `IUserStorage` interfaces exist and are Koin-bound; implementations are already in place
- `ExpenseDao.getExpensesInRange` returns a `List<ExpenseEntity>` — the export uses the suspend variant (not the Flow one)
- The `FirebaseCallableFunctions` interface (`call(functionName, data)`) is already implemented for both Android (`AndroidFirebaseCallableFunctions`) and iOS (`IosFirebaseCallableFunctions`); the export Cloud Function will be invoked through it
- Google OAuth Client ID must be provisioned in Google Cloud Console for **both** Android (SHA-1 fingerprint registered) **and** iOS (bundle ID registered) — two separate client IDs
- A **Server (Web) Client ID** is also required for backend token verification — this is a third client ID, distinct from the platform-specific ones
- Apple Developer account with Sign in with Apple capability enabled
- Python 3.11+ runtime available in Firebase Functions (Gen 2)
- **iOS: APNs auth key must be uploaded to Firebase Console** — required for FCM push notifications on iOS
- **Android 13+**: `POST_NOTIFICATIONS` permission must be declared in manifest
- **iOS**: `UNUserNotificationCenter.requestAuthorization` must be called before FCM can deliver visible notifications

---

## Resolved Design Decisions

1. **No refresh token stored.** The app requests a fresh Google access token from the platform SDK at export time (silent re-auth, no UI). Only the connection status (email + linked flag) is persisted — not the token itself.
2. **Two-step Google flow on Android.** Step 1: Credential Manager (`GetGoogleIdOption`) for identity (ID token). Step 2: Google Identity Services `AuthorizationRequest` for scoped access token (`spreadsheets` scope). These are separate API calls.
3. **Export bottom sheet is the primary result path.** The FCM notification is a bonus for users who background the app. The bottom sheet always shows the result inline.
4. **Apple Sign-In users see the export button** — tapping it triggers inline "Connect Google Account" flow. After connecting, export proceeds automatically (no second tap).
5. **`userEmail` is never sent from the client** in the export payload. The Cloud Function extracts email from the validated Google access token via tokeninfo.
6. **Firebase Auth is used for user identity** — `signInWithCredential` with Google/Apple credentials. This ensures `context.auth` is populated in Cloud Functions automatically.
7. **Conditional gzip compression.** Payload < 100KB → send raw JSON (typical monthly export). Payload >= 100KB → gzip + base64 (multi-month, yearly). A `compressed: true/false` flag in the payload tells the Cloud Function which format to expect. Uses `kotlinx-io` `GzipSink`/`GzipSource` in `commonMain` — no `expect/actual` needed.
8. **Auth strategy per platform:**
   - **Android:** Credential Manager for Google Sign-In → Firebase Auth
   - **iOS:** Apple Sign-In (native `AuthenticationServices`) + Google Sign-In SDK (webview-based OAuth) → Firebase Auth
   - **Export:** Separate call to platform Google SDK for a fresh scoped access token (not stored, requested each time)
9. **Fire-and-forget export.** User taps Export → app enqueues background work → bottom sheet shows "Export started" → dismisses. The Cloud Function sends FCM push notification when done. No spinner, no waiting. Android uses WorkManager, iOS uses `BGProcessingTask`/`Task {}`.
10. **Push notification is the only result delivery.** Since export runs in the background, the app does not wait for the Cloud Function response. The notification with deep link is how the user gets the spreadsheet URL.

---

## Open Questions

1. Should the app support exporting a custom date range, or always the current calendar month?
2. Should multi-currency expenses show both the original amount/currency and the converted amount, or only the converted amount?
3. Should the spreadsheet URL be opened in `CustomTabsIntent` / `SFSafariViewController` or always in the system browser?

---

## Phase 1 — Authentication: Sign In with Google & Apple

**Goal:** Users can authenticate with Google (both platforms) or Apple (iOS only) via Firebase Auth. A Google account connection is tracked separately for Sheets export in Phase 2.

### Step 1 — Dependencies | Effort: Low

**`gradle/libs.versions.toml` additions:**

```toml
[versions]
google-signin-ios = "8.0.0"   # Declared for SPM — not in Gradle

[libraries]
# Android — already declared, just need to be wired into build.gradle
# androidx-credentials, androidx-credentials-play-services, google-identity,
# androidx-credentials-play-services-auth are already in libs.versions.toml
```

Android Gradle (`composeApp/build.gradle.kts`) — add to `androidMain` dependencies:
```kotlin
implementation(libs.androidx.credentials)
implementation(libs.androidx.credentials.play.services)
implementation(libs.google.identity)
implementation(libs.androidx.credentials.play.services.auth)
```

iOS — add via SPM in `iosApp/iosApp.xcodeproj`:
- `GoogleSignIn-iOS` SDK: `https://github.com/google/GoogleSignIn-iOS` tag `8.0.0`
- `AuthenticationServices` is a system framework — no SPM entry needed

**iOS `Info.plist` additions (required):**
- `GIDClientID` — iOS OAuth client ID from Google Cloud Console
- `GIDServerClientID` — Server (Web) client ID for backend verification
- Add reversed client ID as a URL scheme (e.g., `com.googleusercontent.apps.123456`) — required for Google Sign-In redirect

### Step 2 — Common Auth Models (`commonMain`) | Effort: Low

**New files:**

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/models/data/GoogleAccountLink.kt`
```kotlin
data class GoogleAccountLink(val email: String, val isConnected: Boolean)
```

> Note: No access token stored. The app requests a fresh token from the platform SDK at export time.

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/models/data/AppleAuthCredential.kt`
- `data class AppleAuthCredential(val identityToken: String, val authorizationCode: String, val email: String?, val fullName: String?)`

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/IGoogleAccountStorage.kt`
```kotlin
interface IGoogleAccountStorage {
    suspend fun write(link: GoogleAccountLink)
    suspend fun read(): GoogleAccountLink?
    suspend fun delete()
}
```

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/auth/domain/SocialAuthProvider.kt`
```kotlin
sealed interface SocialAuthResult {
    data class GoogleSuccess(val idToken: String, val email: String) : SocialAuthResult
    data class AppleSuccess(val credential: AppleAuthCredential) : SocialAuthResult
    data class Failure(val cause: Throwable) : SocialAuthResult
}

interface SocialAuthProvider {
    /** Authenticate with Google — returns ID token for Firebase Auth */
    suspend fun signInWithGoogle(): SocialAuthResult

    /** Authenticate with Apple — returns identity token for Firebase Auth */
    suspend fun signInWithApple(): SocialAuthResult  // throws UnsupportedOperationException on Android

    /**
     * Request a fresh Google access token with the specified scopes.
     * Uses silent re-auth (no UI) if the user previously signed in with Google.
     * Called at export time — token is NOT persisted.
     */
    suspend fun getGoogleAccessToken(scopes: List<String>): String
}
```

### Step 3 — Platform Implementations | Effort: High

**Android** (`composeApp/src/androidMain/`)

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/auth/data/AndroidSocialAuthProvider.kt`
- Inject `ActivityProvider` (Android-only class, NOT `expect/actual`)
- `signInWithGoogle()`:
  - **Step 1 — Identity:** Use `CredentialManager.getCredential(GetGoogleIdOption(serverClientId = SERVER_CLIENT_ID))` → extract `GoogleIdTokenCredential` → return `GoogleSuccess(idToken, email)`
  - This ID token is used for Firebase Auth sign-in
- `signInWithApple()`: throws `UnsupportedOperationException`
- `getGoogleAccessToken(scopes)`:
  - **Step 2 — Authorization:** Use Google Identity Services `AuthorizationRequest.builder().setRequestedScopes(scopes).build()` via `Identity.getAuthorizationClient(activity).authorize(request)` → return `authorizationResult.accessToken`
  - This is a **separate** API call from identity — Credential Manager cannot request OAuth scopes

> **Critical:** `GetGoogleIdOption` returns an ID token only. You CANNOT get a scoped access token from Credential Manager. The `AuthorizationRequest` from Google Identity Services is required as a second step for Sheets access.

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/auth/data/ActivityProvider.kt`
- Plain Android-only class (NOT `expect/actual`) wrapping `ComponentActivity` reference
- Updated from `MainActivity.onCreate`
- Injected via `PlatformModule.android.kt`

**iOS** (`composeApp/src/iosMain/`)

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/auth/data/IosSocialAuthBridge.kt`
```kotlin
@ObjCName("IosSocialAuthBridge", exact = true)
interface IosSocialAuthBridge {
    fun signInWithGoogle(
        onSuccess: (idToken: String, email: String) -> Unit,
        onError: (String) -> Unit,
    )
    fun signInWithApple(
        onSuccess: (identityToken: String, authCode: String, email: String?, firstName: String?, lastName: String?) -> Unit,
        onError: (String) -> Unit,
    )
    fun getGoogleAccessToken(
        scopes: List<String>,
        onSuccess: (accessToken: String) -> Unit,
        onError: (String) -> Unit,
    )
}
```

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/auth/data/IosSocialAuthProvider.kt`
- Inject `IosSocialAuthBridge`; delegate all methods via `suspendCancellableCoroutine`

**Swift side** (`iosApp/`)

`iosApp/IosSocialAuthBridgeImpl.swift`
- `signInWithGoogle`: call `GIDSignIn.sharedInstance.signIn(withPresenting:)` → return `user.idToken?.tokenString` and `user.profile?.email`
- `signInWithApple`: use `ASAuthorizationAppleIDProvider` via `ASAuthorizationController`
  - **Presentation context:** Use `UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene` (NOT deprecated `UIApplication.keyWindow`)
- `getGoogleAccessToken(scopes)`: call `GIDSignIn.sharedInstance.currentUser?.addScopes(scopes, presenting:)` then `currentUser?.refreshTokensIfNeeded()` → return `accessToken.tokenString`
  - If `currentUser` is nil (user signed in with Apple), this triggers a Google sign-in flow

Register `IosSocialAuthBridgeImpl` when initialising Koin in `MainViewController.kt` / `IosKoinHelper.kt`.

### Step 4 — `IGoogleAccountStorage` Implementation | Effort: Low

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/auth/data/GoogleAccountStorageImpl.kt`
- Back with platform secure storage:
  - **Android:** `EncryptedSharedPreferences` (from `security-crypto`) — NOT plain DataStore
  - **iOS:** Keychain via `expect/actual` wrapper
- Stores only `GoogleAccountLink(email, isConnected)` — no tokens

> **Security note:** The original plan used plain DataStore. Since this tracks account linkage (and future implementations may cache tokens), use encrypted storage from the start.

### Step 5 — Auth Feature Module | Effort: Medium

Directory: `composeApp/src/commonMain/kotlin/com/please/stop/app/features/auth/`

**Data layer**

`features/auth/data/repository/AuthRepositoryImpl.kt`
- Inject `SocialAuthProvider`, `IGoogleAccountStorage`, `ITokensStorage`, `FirebaseAuth`
- `suspend fun signInWithGoogle(): kotlin.Result<Unit>`:
  1. Call `socialAuthProvider.signInWithGoogle()` → get ID token
  2. Call `FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken))` → Firebase user
  3. Write `GoogleAccountLink(email, isConnected = true)` to `IGoogleAccountStorage`
  4. Write `AuthToken` from Firebase user to `ITokensStorage`
- `suspend fun signInWithApple(): kotlin.Result<Unit>`:
  1. Call `socialAuthProvider.signInWithApple()` → get identity token + auth code
  2. Call `FirebaseAuth.signInWithCredential(OAuthProvider.getCredential("apple.com", idToken, authCode))`
  3. Write `AuthToken` from Firebase user to `ITokensStorage`
  4. **Store email on backend on first sign-in** — Apple only returns email on first authorization
- `fun observeIsAuthenticated(): Flow<Boolean>`

`features/auth/domain/repository/AuthRepository.kt` — interface

**Domain layer**

`features/auth/domain/usecase/SignInWithGoogleUseCase.kt`
```kotlin
class SignInWithGoogleUseCase(
    private val repository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface Result : com.please.stop.app.core.models.domain.Result {
        data object Success : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
    suspend operator fun invoke(): Result = withContext(ioDispatcher) {
        repository.signInWithGoogle().fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }
}
```

`features/auth/domain/usecase/SignInWithAppleUseCase.kt` — identical shape for Apple

`features/auth/domain/usecase/ObserveAuthStateUseCase.kt`
- Returns `Flow<Boolean>` from `repository.observeIsAuthenticated()`

**Presentation layer**

`features/auth/presentation/AuthState.kt`
```kotlin
@Stable
sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Error(val errorType: ErrorType) : AuthState
}
```

`features/auth/presentation/AuthEvent.kt`
```kotlin
sealed interface AuthEvent {
    data object SignInWithGoogle : AuthEvent
    data object SignInWithApple : AuthEvent
    data object DismissError : AuthEvent
}
```

`features/auth/presentation/AuthStateHolder.kt`
- Extends `StateHolder<AuthState, AuthEvent>`
- `resolveEventResult`: maps events → use cases
- `getNavigationByResult`: Success → `router.replaceStack(MainBottomTabs.Home)`
- `getStateByResult`: Loading → Idle/Error

`features/auth/presentation/ui/AuthScreen.kt`
- Two buttons: "Continue with Google" (both platforms) and "Continue with Apple" (iOS-only, hidden via `Platform.isIos` check)
- Wrap in `ScreenOverlayContainer` for error handling per MVI pattern

### Step 6 — Navigation Route | Effort: Low

`composeApp/src/commonMain/kotlin/com/please/stop/app/navigation/routes/AuthRoute.kt`
```kotlin
@Serializable
data object AuthRoute : NavKey
```

Register in `RegisteredRoutes.kt`. Add entry in root navigation host — when not authenticated, push `AuthRoute` before `OnboardingRoute`.

### Step 7 — DI Module | Effort: Low

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/auth/di/AuthModule.kt`
```kotlin
val authModule = module {
    single<IGoogleAccountStorage> { GoogleAccountStorageImpl(/* platform-injected encrypted storage */) }
    single<AuthRepository> {
        AuthRepositoryImpl(
            socialAuthProvider = get(),
            googleAccountStorage = get(),
            tokensStorage = get(),
            firebaseAuth = get(),
        )
    }
    factory { SignInWithGoogleUseCase(repository = get(), ioDispatcher = get(named(IO.name))) }
    factory { SignInWithAppleUseCase(repository = get(), ioDispatcher = get(named(IO.name))) }
    factory { ObserveAuthStateUseCase(repository = get()) }
    viewModel { AuthStateHolder(signInWithGoogleUseCase = get(), signInWithAppleUseCase = get()) }
}
```

Platform modules:
- `PlatformModule.android.kt`: `single<SocialAuthProvider> { AndroidSocialAuthProvider(activityProvider = get()) }`
- `PlatformModule.ios.kt`: `single<SocialAuthProvider> { IosSocialAuthProvider(bridge = get()) }`

Register `authModule` in `AppModule.kt`.

### Step 8 — "Connect Google Account" Flow (for Apple users) | Effort: Medium

Apple Sign-In users need a Google account linked to create Sheets.

`features/auth/domain/usecase/ConnectGoogleAccountUseCase.kt`
- Calls `SocialAuthProvider.signInWithGoogle()` — triggers Google Sign-In UI
- Writes `GoogleAccountLink(email, isConnected = true)` to `IGoogleAccountStorage`
- Does NOT re-authenticate with Firebase — the user remains signed in with Apple
- Returns `Result.Success` or `Result.Failure`

Surfaced inline in the export flow (see Phase 2, Step 2 — `NeedsGoogleAccount` state). After successful connection, export proceeds automatically.

### Step 9 — Logout Cleanup | Effort: Low

`features/auth/domain/usecase/LogoutUseCase.kt`
- Calls atomically:
  1. `ITokensStorage.deleteAuthToken()`
  2. `IGoogleAccountStorage.delete()`
  3. `BearerTokenClearer.clear()`
  4. `FirebaseAuth.signOut()`
- Prevents stale Google account data surviving across user sessions

### Backend: Firebase Cloud Functions for Auth Verification | Effort: Medium

> **Decision:** Use Firebase Authentication SDK (`signInWithCredential`) on the client side. This means `context.auth` is automatically populated in Cloud Functions — no custom JWT verification needed. The backend functions below are only needed if you want additional server-side user record management.

Two Gen 2 HTTPS callable functions in `europe-west1`:

`functions/verify_google_token/main.py`
- Receive `{ idToken: string }` from app
- Verify with `google-auth` library (`id_token.verify_oauth2_token`)
- Look up / create user record in Firestore; return user profile data

`functions/verify_apple_token/main.py`
- Receive `{ identityToken: string, authorizationCode: string, email: string?, fullName: string? }`
- Verify Apple identity token (JWT RS256, Apple's public keys)
- **Store email and fullName on first sign-in** — Apple only provides these once
- Return user profile data

---

## Phase 2 — Export to Google Sheets via Cloud Function + FCM

**Goal:** User taps "Export to Sheets" in the Analytics screen. The app requests a fresh Google access token, collects expenses for the current month, and sends them to a Firebase Cloud Function. The function creates a Google Spreadsheet and sends an FCM notification with the spreadsheet URL.

**Depends on:** Phase 1 complete — user is authenticated, and Google account is connected (for Apple users).

### Step 1 — Cloud Function: `exportToSheets` | Effort: High

Location: `functions/export_to_sheets/main.py` (Firebase Gen 2, `europe-west1`)

**Configuration:**
- Timeout: **300 seconds** (default 60s is too tight for Sheets API calls)
- Memory: 256MB (default is sufficient)
- Authentication: Firebase Auth required (`context.auth` must be non-null)

**Trigger:** HTTPS Callable (authenticated)

**Input payload:**

Small payload (< 100KB, typical monthly export):
```json
{
  "googleAccessToken": "ya29...",
  "fcmToken": "device-fcm-token",
  "month": "2026-04",
  "currencySymbol": "€",
  "decimalPlaces": 2,
  "compressed": false,
  "expenses": [
    {
      "date": "2026-04-03",
      "title": "Coffee",
      "category": "Food",
      "subcategory": "Cafe",
      "amount": "3.50",
      "notes": "With colleagues",
      "originalAmount": "4.00",
      "originalCurrency": "USD"
    }
  ]
}
```

Large payload (>= 100KB, multi-month/yearly export):
```json
{
  "googleAccessToken": "ya29...",
  "fcmToken": "device-fcm-token",
  "month": "2026-01 to 2026-12",
  "currencySymbol": "€",
  "decimalPlaces": 2,
  "compressed": true,
  "expenses": "H4sIAAAAAAAAA6tWKkktLlGyUlAqS8wpTtVRSs7..."
}
```

> When `compressed: true`, `expenses` is a base64-encoded gzip string. When `compressed: false`, `expenses` is the raw JSON array.

> **Security:** `userEmail` is NOT in the payload. The function extracts email from the validated Google access token via `https://oauth2.googleapis.com/tokeninfo?access_token=...`. This prevents a malicious client from sharing spreadsheets with arbitrary emails.

**Python dependencies** (`functions/export_to_sheets/requirements.txt`):
```
gspread==6.1.2
google-auth==2.29.0
firebase-admin==6.5.0
```

**Spreadsheet structure (sheet name: "April 2026"):**

| Row | A | B | C | D | E | F |
|-----|---|---|---|---|---|---|
| 1 | Date | Title | Category | Subcategory | Amount (€) | Notes |
| 2..N | expense rows (dates as Sheets date serial numbers) | | | | | |
| N+1 | | | | | **Total** | `=SUM(E2:EN)` |

Styling:
- Header row: bold, background `#4285F4`, white text
- Amount column: number format matching `decimalPlaces`
- Date column: number format `yyyy-mm-dd` (using Sheets date serial numbers for proper sorting)
- Total row: bold
- Freeze first row (`freeze_rows=1`)
- Column widths: A=100, B=200, C=150, D=150, E=120, F=250

> **Data formatting:** Dates must be written as Sheets date serial numbers (not ISO strings) to enable proper sorting/filtering. Use `(date - epoch).days + 25569` to convert.

**Steps inside the function:**
1. Validate `context.auth` is non-null (Firebase Auth)
2. **Parse expenses (conditional decompression):**
   ```python
   import gzip, base64, json

   if data.get("compressed", False):
       raw = gzip.decompress(base64.b64decode(data["expenses"]))
       expenses = json.loads(raw)
   else:
       expenses = data["expenses"]
   ```
3. Validate `googleAccessToken` by calling Google's tokeninfo endpoint → extract `email`
4. Build `gspread.Client` using `google.oauth2.credentials.Credentials(token=googleAccessToken)` — acting on behalf of the user, not a service account
5. Create spreadsheet titled `"PlzStop Export – {month}"`
6. **Batch write** headers + all expense rows + total formula in a single `worksheet.update(values, 'A1')` call
7. **Batch format** all styling in a single `worksheet.batch_format(formats)` call
8. Share spreadsheet with validated `email` as owner
9. If `fcmToken` is provided (non-null), send FCM notification:
   ```python
   spreadsheet_url_encoded = urllib.parse.quote(spreadsheet_url, safe='')
   message = messaging.Message(
       token=fcm_token,
       notification=messaging.Notification(
           title="Your export is ready",
           body="Tap to open your Google Spreadsheet",
       ),
       data={
           "deepLink": f"plzstop://open?url={spreadsheet_url_encoded}",
           "spreadsheetUrl": spreadsheet_url,
       },
       apns=messaging.APNSConfig(
           payload=messaging.APNSPayload(
               aps=messaging.Aps(sound="default"),
           ),
       ),
   )
   messaging.send(message)
   ```
10. Return `{ "spreadsheetUrl": spreadsheet_url }`

> **FCM notification is required** in this architecture. If `fcmToken` is null (notification permission denied), the function still creates the spreadsheet but the user has no way to receive the URL. The app must request notification permission **before** allowing export, and block export if denied with a message explaining why notifications are needed.

**Error handling in the function:**
- Gzip/base64 decompression failure → return error code `INVALID_PAYLOAD` with message
- Google token invalid/expired → return error code `INVALID_TOKEN` with message
- Sheets API quota exceeded (60 req/min/user) → return error code `QUOTA_EXCEEDED` with retry-after hint
- `gspread` 401 during write → return error code `TOKEN_EXPIRED` (access token expired mid-execution — unlikely with batch ops but possible)

### Step 2 — Background Worker (Platform) | Effort: High

The export runs in the background — user taps Export, sees confirmation, and dismisses. Push notification delivers the result.

**Common interface** (`commonMain`):

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/export/domain/ExportWorkerScheduler.kt`
```kotlin
interface ExportWorkerScheduler {
    /**
     * Enqueue export work. The worker will:
     * 1. Get fresh Google access token
     * 2. Query expenses for the date range
     * 3. Build payload (conditionally compressed)
     * 4. Call Cloud Function
     * Push notification with result is sent by the Cloud Function.
     */
    fun enqueue(startDateMillis: Long, endDateMillis: Long)
}
```

**Android** — WorkManager (already in dependencies):

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/export/data/ExportWorker.kt`
```kotlin
class ExportWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val startDate = inputData.getLong("startDate", 0)
        val endDate = inputData.getLong("endDate", 0)

        // 1. Get fresh Google access token (silent, no UI)
        val accessToken = socialAuthProvider.getGoogleAccessToken(
            scopes = listOf("https://www.googleapis.com/auth/spreadsheets")
        )

        // 2. Query expenses, categories, user profile from Room
        // 3. Build payload, conditionally compress
        // 4. Call Cloud Function via FirebaseCallableFunctions
        // Cloud Function sends push notification — worker is done

        return Result.success()
    }
}
```

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/export/data/AndroidExportWorkerScheduler.kt`
```kotlin
class AndroidExportWorkerScheduler(
    private val context: Context,
) : ExportWorkerScheduler {

    override fun enqueue(startDateMillis: Long, endDateMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(workDataOf(
                "startDate" to startDateMillis,
                "endDate" to endDateMillis,
            ))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("export_sheets", ExistingWorkPolicy.KEEP, request)
    }
}
```

> `ExistingWorkPolicy.KEEP` prevents duplicate exports if user taps twice.

**iOS** — `Task {}` with background URLSession:

`iosApp/ExportWorkerImpl.swift`
- Swift implementation that calls the KMP export repository
- Uses `URLSession` background configuration for network resilience
- Called from KMP via bridge pattern (same as `IosSocialAuthBridge`)

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/export/data/IosExportWorkerScheduler.kt`
- Bridge to Swift side via `@ObjCName` interface
- Uses `suspendCancellableCoroutine` pattern

> **iOS limitation:** `BGProcessingTask` requires scheduling in advance and may not run immediately. For a user-triggered action, use a `Task {}` coroutine that survives brief backgrounding (up to ~30s). For longer operations, use a background `URLSession` that continues even after app suspension.

**Data layer** (`commonMain`):

`features/export/data/repository/ExportRepositoryImpl.kt`
- Inject `IGoogleAccountStorage`, `ExpenseDao`, `CategoryDao`, `SubcategoryDao`, `UserProfileDao`, `ExportWorkerScheduler`, `INotificationPermission`, `IFcmTokenProvider`

Key implementation details:

1. **Pre-flight checks (run synchronously before enqueuing worker):**
   ```kotlin
   suspend fun validateAndEnqueueExport(startDate: Instant, endDate: Instant): ExportValidationResult {
       // Check Google account is linked
       val googleAccount = googleAccountStorage.read()
           ?: return ExportValidationResult.GoogleAccountNotLinked

       // Check notification permission (required for result delivery)
       if (!notificationPermission.isGranted()) {
           val granted = notificationPermission.request()
           if (!granted) return ExportValidationResult.NotificationPermissionDenied
       }

       // Check expenses exist in range
       val expenses = expenseDao.getExpensesInRange(startDate.toEpochMilliseconds(), endDate.toEpochMilliseconds())
       if (expenses.isEmpty()) return ExportValidationResult.NoExpenses

       // All checks pass — enqueue background work
       exportWorkerScheduler.enqueue(startDate.toEpochMilliseconds(), endDate.toEpochMilliseconds())
       return ExportValidationResult.Enqueued(expenseCount = expenses.size)
   }
   ```

2. **Category/subcategory name mapping — avoid N+1 queries (inside worker):**
   ```kotlin
   val allCategories = categoryDao.getAllIncludingArchived()  // single query
   val allSubcategories = subcategoryDao.getAllIncludingArchived()  // single query
   val categoryMap = allCategories.associateBy { it.id }
   val subcategoryMap = allSubcategories.associateBy { it.id }
   ```
   > **Edge case:** Archived categories/subcategories must be included — expenses may reference them.

3. **Amount conversion from minor units:**
   ```kotlin
   val displayAmount = expense.amountMinorUnits.toBigDecimal()
       .movePointLeft(userProfile.decimalPlaces)
       .toPlainString()
   ```

4. **Date range uses device timezone:**
   ```kotlin
   val tz = TimeZone.currentSystemDefault()
   val startOfMonth = LocalDate(year, month, 1).atStartOfDayIn(tz)
   val endOfMonth = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).atStartOfDayIn(tz)
   ```

5. **Conditional compression (inside worker):**
   ```kotlin
   val expensesJson = Json.encodeToString(expensesList)
   val jsonBytes = expensesJson.encodeToByteArray()

   val (payloadExpenses, isCompressed) = if (jsonBytes.size >= 100_000) {
       // Use kotlinx-io GzipSink — works in commonMain, no expect/actual
       val compressed = Buffer().use { buffer ->
           GzipSink(buffer).use { gzip -> gzip.write(jsonBytes) }
           buffer.readByteArray()
       }
       compressed.encodeBase64() to true
   } else {
       expensesJson to false
   }
   ```

6. **Nullable subcategory handling (iOS safety):**
   ```kotlin
   val subcategoryName = subcategoryMap[expense.subcategoryId]?.name ?: ""
   ```

`features/export/domain/repository/ExportRepository.kt` — interface

**Domain layer**

`features/export/domain/usecase/ExportToSheetsUseCase.kt`
```kotlin
class ExportToSheetsUseCase(
    private val repository: ExportRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface Result : com.please.stop.app.core.models.domain.Result {
        data class Enqueued(val expenseCount: Int) : Result
        data object GoogleAccountNotLinked : Result
        data object NotificationPermissionDenied : Result
        data object NoExpenses : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }

    suspend operator fun invoke(): Result = withContext(ioDispatcher) {
        // Delegates to repository.validateAndEnqueueExport()
    }
}
```

> Note: `Success(spreadsheetUrl)` is removed — the URL is delivered via push notification, not via use case return value.

**Presentation layer**

`features/export/presentation/ExportState.kt`
```kotlin
@Stable
sealed interface ExportState {
    data object Idle : ExportState
    data class Confirm(val expenseCount: Int, val month: String) : ExportState
    data object Enqueued : ExportState
    data object NeedsGoogleAccount : ExportState
    data object NeedsNotificationPermission : ExportState
    data object NoExpenses : ExportState
    data class Error(val errorType: ErrorType) : ExportState
}
```

> **Simplified states:** No `Exporting` spinner or `Success` with URL. User sees `Confirm` → `Enqueued` ("Your export is being prepared. You'll receive a notification when it's ready.") → bottom sheet auto-dismisses after a short delay.

`features/export/presentation/ExportEvent.kt`
```kotlin
sealed interface ExportEvent {
    data object ExportTapped : ExportEvent
    data object ConfirmExport : ExportEvent
    data object ConnectGoogleAccountTapped : ExportEvent
    data object EnableNotificationsTapped : ExportEvent
    data object DismissError : ExportEvent
    data object Dismiss : ExportEvent
}
```

`features/export/presentation/ExportStateHolder.kt`
- Extends `StateHolder<ExportState, ExportEvent>`
- **Guard against double-tap:** `resolveEventResult(ExportTapped)` returns `emptyFlow()` when current state is `Enqueued`
- `resolveEventResult(ExportTapped)` → count expenses + show `Confirm` state
- `resolveEventResult(ConfirmExport)` → call `exportToSheetsUseCase()` → map to state
- `getStateByResult`:
  - `Enqueued(count)` → `ExportState.Enqueued` (auto-dismiss after 3s)
  - `GoogleAccountNotLinked` → `NeedsGoogleAccount`
  - `NotificationPermissionDenied` → `NeedsNotificationPermission`
  - `NoExpenses` → `NoExpenses`
- `getNavigationByResult`: `ConnectGoogleAccountTapped` → triggers `ConnectGoogleAccountUseCase` inline, then auto-retries export on success

`features/export/presentation/ui/ExportBottomSheet.kt`
- A `ModalBottomSheet` (Material3) composable
- States:
  - `Idle` → export button + explanation text
  - `Confirm` → "Export {count} expenses from {month}?" + Export/Cancel buttons
  - `Enqueued` → checkmark + "Your export is being prepared. You'll receive a notification when it's ready." Auto-dismiss after 3s
  - `NeedsGoogleAccount` → "Connect Google Account" button + explanation
  - `NeedsNotificationPermission` → "Enable notifications to receive your export" + "Enable" button
  - `NoExpenses` → "Nothing to export for this period" message
  - `Error` → error message + retry button

### Step 3 — Notification Permission | Effort: Low

**Hard requirement for export.** Since the export runs in the background and the result is delivered via push notification, notification permission must be granted. If denied, export is blocked with a message explaining why.

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/INotificationPermission.kt`
```kotlin
interface INotificationPermission {
    suspend fun isGranted(): Boolean
    suspend fun request(): Boolean
}
```

- **Android:** Check/request `POST_NOTIFICATIONS` (API 33+). Below API 33, always returns `true`.
- **iOS:** `UNUserNotificationCenter.requestAuthorization(options: [.alert, .sound])`

Called in `ExportRepositoryImpl.validateAndEnqueueExport()` before enqueuing the worker. If denied, returns `NotificationPermissionDenied` and the UI shows `NeedsNotificationPermission` state with an "Enable" button.

### Step 4 — FCM Token Retrieval | Effort: Low

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/IFcmTokenProvider.kt`
```kotlin
interface IFcmTokenProvider {
    suspend fun getToken(): String?
}
```

`composeApp/src/androidMain/.../FcmTokenProvider.android.kt` — `FirebaseMessaging.getInstance().token.await()`

`composeApp/src/iosMain/.../IosFcmTokenProvider.kt` — bridge to Swift `Messaging.messaging().token(completion:)`
> **iOS timing:** `Messaging.messaging().token` may return nil if called before APNs registration. Use the callback variant which waits for registration.

### Step 5 — Deep Link Handling | Effort: Medium

**Deep link scheme:** `plzstop://open?url=<percent-encoded-spreadsheet-url>`

> **URL encoding is critical.** Google Sheets URLs contain `?`, `=`, `#`. The Cloud Function must use `urllib.parse.quote(url, safe='')`. The app must decode with `URLDecoder.decode(url, "UTF-8")` (Android) or `removingPercentEncoding` (iOS).

> **Security note:** The `plzstop://` custom scheme is NOT verified — any app can register it. This is acceptable because: (a) the spreadsheet URL is also returned inline via the callable function, (b) the URL only opens a public Google Sheets link, no sensitive action is taken. If stronger security is needed later, switch to Android App Links / iOS Universal Links with domain verification.

**`DeepLinkResolver.kt` update:**
```kotlin
"open" -> {
    val url = data.queryParams["url"]?.decodeUrl() ?: return null
    DeepLinkResult.OpenExternalUrl(url)
}
```

Add `data class OpenExternalUrl(val url: String) : DeepLinkResult`.

**Cold start handling:** When the app is launched via deep link, Koin/DB/nav graph may not be ready. Queue the pending deep link URI and process it after the root navigation host is composed:
```kotlin
// In RootContent.kt or equivalent
var pendingDeepLink by remember { mutableStateOf<Uri?>(null) }
LaunchedEffect(navHostReady, pendingDeepLink) {
    if (navHostReady && pendingDeepLink != null) {
        deepLinkHandler.handle(pendingDeepLink!!)
        pendingDeepLink = null
    }
}
```

**Android `AndroidManifest.xml`** — add intent filter:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="plzstop" android:host="open" />
</intent-filter>
```
> Removed `android:autoVerify="true"` — auto-verification only works with `https://` App Links, not custom schemes.

**iOS `Info.plist`** — add `CFBundleURLSchemes` entry for `plzstop`; handle in `AppDelegate` / SwiftUI `onOpenURL`.

**`UrlOpener.kt`** — `expect fun openUrl(url: String)`:
- Android: `CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))`
- iOS: `UIApplication.sharedApplication.openURL(NSURL(string = url)!!)`

### Step 6 — Export Button in Analytics Screen | Effort: Low

Modify `features/analytics/presentation/AnalyticsEvent.kt`:
```kotlin
data object ExportTapped : AnalyticsEvent
data object DismissExportSheet : AnalyticsEvent
```

`AnalyticsScreen.kt`:
- Add FAB or top-bar action for export
- When tapped, show `ExportBottomSheet` with its own `ExportStateHolder` scoped via `koinViewModel()`

### Step 7 — DI Module | Effort: Low

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/export/di/ExportModule.kt`
```kotlin
val exportModule = module {
    single<ExportRepository> {
        ExportRepositoryImpl(
            googleAccountStorage = get(),
            expenseDao = get<AppDatabase>().expenseDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            subcategoryDao = get<AppDatabase>().subcategoryDao(),
            userProfileDao = get<AppDatabase>().userProfileDao(),
            fcmTokenProvider = get(),
            notificationPermission = get(),
            exportWorkerScheduler = get(),
        )
    }
    factory { ExportToSheetsUseCase(repository = get(), ioDispatcher = get(named(IO.name))) }
    viewModel { ExportStateHolder(exportToSheetsUseCase = get(), connectGoogleAccountUseCase = get()) }
}
```

Platform modules:
- `PlatformModule.android.kt`:
  - `single<IFcmTokenProvider> { AndroidFcmTokenProvider() }`
  - `single<INotificationPermission> { AndroidNotificationPermission(context = get()) }`
  - `single<ExportWorkerScheduler> { AndroidExportWorkerScheduler(context = get()) }`
- `PlatformModule.ios.kt`:
  - `single<IFcmTokenProvider> { IosFcmTokenProvider(bridge = get()) }`
  - `single<INotificationPermission> { IosNotificationPermission() }`
  - `single<ExportWorkerScheduler> { IosExportWorkerScheduler(bridge = get()) }`

Register `exportModule` in `AppModule.kt`.

---

## Edge Cases Checklist

| # | Edge Case | Handling |
|---|-----------|----------|
| 1 | **Empty date range** — 0 expenses | Pre-flight check in `validateAndEnqueueExport()` returns `NoExpenses`; no worker enqueued |
| 2 | **Double-tap export** | WorkManager `ExistingWorkPolicy.KEEP` ignores duplicate enqueue. StateHolder ignores event when `Enqueued` |
| 3 | **Archived categories** | DAO queries for export use `getAllIncludingArchived()` |
| 4 | **Null subcategory** | Replaced with `""` before serialization (iOS `FirebaseCallableFunctions` safety) |
| 5 | **Multi-currency expenses** | Include `originalAmount` + `originalCurrency` columns (if decided yes in Open Questions) |
| 6 | **Deep link URL encoding** | Python: `urllib.parse.quote(url, safe='')`. App: decode before opening |
| 7 | **Timezone for date range boundaries** | Use `TimeZone.currentSystemDefault()` for start/end calculation |
| 8 | **Cold start from deep link** | Queue deep link, process after nav graph composition |
| 9 | **Notification permission denied** | Export blocked — UI shows `NeedsNotificationPermission` with "Enable" button. Notifications are required for result delivery |
| 10 | **Google token expired mid-function** | Unlikely with batch ops (<30s), but function returns `TOKEN_EXPIRED` error → push notification with error message |
| 11 | **Apple first sign-in email** | Email stored on backend during first auth — subsequent logins use subject ID |
| 12 | **Network failure mid-export** | WorkManager retries automatically (Android). Orphan spreadsheet may exist — title includes date range for discoverability |
| 13 | **Sheets API quota** | Batch ops keep it to ~5 API calls. Function returns `QUOTA_EXCEEDED` → push notification with error |
| 14 | **Stale Google session on logout** | `LogoutUseCase` clears `IGoogleAccountStorage` + `ITokensStorage` + Firebase Auth |
| 15 | **iOS FCM token nil before APNs registration** | Use callback variant of `Messaging.messaging().token(completion:)` |
| 16 | **`amountMinorUnits` conversion** | `BigDecimal.movePointLeft(decimalPlaces)` — handles JPY (0 places), EUR (2), BHD (3) |
| 17 | **Payload size** | < 100KB raw JSON (most months). >= 100KB → gzip+base64 (multi-month/yearly). `compressed` flag in payload |
| 18 | **Auto-retry after Google connect** | Apple user connects Google → `ExportStateHolder` auto-triggers export, no second tap |
| 19 | **Worker fails silently** | Cloud Function sends error notification via FCM if export fails (e.g., "Export failed — please try again") |
| 20 | **App killed during background export** | Android: WorkManager survives process death. iOS: background URLSession continues independently |
| 21 | **Google access token in worker** | Fresh token requested inside worker — but on Android, `getGoogleAccessToken()` requires Activity context. Use `AuthorizationClient` with application context or store a short-lived token before enqueuing |

---

## File Index

### New files — `commonMain`

| Path | Description |
|------|-------------|
| `core/models/data/GoogleAccountLink.kt` | Google account connection model (email + linked flag) |
| `core/models/data/AppleAuthCredential.kt` | Apple credential model |
| `core/IGoogleAccountStorage.kt` | Interface for Google account link persistence |
| `core/IFcmTokenProvider.kt` | FCM token interface |
| `core/INotificationPermission.kt` | Notification permission check/request interface |
| `features/auth/domain/SocialAuthProvider.kt` | Common auth interface + `getGoogleAccessToken()` |
| `features/auth/domain/repository/AuthRepository.kt` | Auth repository interface |
| `features/auth/domain/usecase/SignInWithGoogleUseCase.kt` | Google sign-in use case |
| `features/auth/domain/usecase/SignInWithAppleUseCase.kt` | Apple sign-in use case |
| `features/auth/domain/usecase/ObserveAuthStateUseCase.kt` | Auth state observer |
| `features/auth/domain/usecase/ConnectGoogleAccountUseCase.kt` | Link Google account for Apple users |
| `features/auth/domain/usecase/LogoutUseCase.kt` | Clears all auth state atomically |
| `features/auth/data/repository/AuthRepositoryImpl.kt` | Auth repository implementation |
| `features/auth/data/GoogleAccountStorageImpl.kt` | Encrypted storage for Google account link |
| `features/auth/presentation/AuthState.kt` | Auth UI state |
| `features/auth/presentation/AuthEvent.kt` | Auth events |
| `features/auth/presentation/AuthStateHolder.kt` | Auth state holder |
| `features/auth/presentation/ui/AuthScreen.kt` | Login screen composable |
| `features/auth/di/AuthModule.kt` | Koin auth module |
| `features/export/domain/repository/ExportRepository.kt` | Export repository interface |
| `features/export/domain/usecase/ExportToSheetsUseCase.kt` | Export use case |
| `features/export/data/repository/ExportRepositoryImpl.kt` | Export repository implementation |
| `features/export/domain/ExportWorkerScheduler.kt` | Common interface for platform background worker |
| `features/export/presentation/ExportState.kt` | Export UI state (Confirm, Enqueued, NeedsNotificationPermission) |
| `features/export/presentation/ExportEvent.kt` | Export events (with ConfirmExport) |
| `features/export/presentation/ExportStateHolder.kt` | Export state holder (double-tap guard) |
| `features/export/presentation/ui/ExportBottomSheet.kt` | Export bottom sheet composable |
| `features/export/di/ExportModule.kt` | Koin export module |
| `navigation/routes/AuthRoute.kt` | Auth navigation route |
| `navigation/deeplink/DeepLinkResult.kt` | Add `OpenExternalUrl` variant |
| `navigation/deeplink/DeepLinkResolver.kt` | Handle `open` deep link path |
| `utils/UrlOpener.kt` | `expect fun openUrl(url: String)` |

### New files — `androidMain`

| Path | Description |
|------|-------------|
| `features/auth/data/AndroidSocialAuthProvider.kt` | Credential Manager (identity) + Google Identity Services (authorization) |
| `features/auth/data/ActivityProvider.kt` | Activity context — Android-only class, NOT expect/actual |
| `features/auth/data/AndroidFcmTokenProvider.kt` | Firebase Messaging token |
| `features/auth/data/AndroidNotificationPermission.kt` | POST_NOTIFICATIONS permission |
| `features/export/data/ExportWorker.kt` | WorkManager `CoroutineWorker` for background export |
| `features/export/data/AndroidExportWorkerScheduler.kt` | WorkManager enqueue logic |
| `utils/UrlOpener.android.kt` | Custom Tabs URL opener |

### New files — `iosMain`

| Path | Description |
|------|-------------|
| `features/auth/data/IosSocialAuthBridge.kt` | ObjC-visible bridge interface (includes `getGoogleAccessToken`) |
| `features/auth/data/IosSocialAuthProvider.kt` | KMP side of social auth |
| `features/auth/data/IosFcmTokenProvider.kt` | KMP side of FCM token retrieval |
| `features/auth/data/IosNotificationPermission.kt` | UNUserNotificationCenter permission |
| `features/export/data/IosExportWorkerScheduler.kt` | Bridge to Swift export worker |
| `utils/UrlOpener.ios.kt` | UIApplication URL opener |

### New files — `iosApp` (Swift)

| Path | Description |
|------|-------------|
| `iosApp/IosSocialAuthBridgeImpl.swift` | GIDSignIn + ASAuthorization + scoped token retrieval |
| `iosApp/IosFcmTokenBridgeImpl.swift` | Firebase Messaging token retrieval |
| `iosApp/ExportWorkerImpl.swift` | Background export using URLSession background configuration |

### New files — `functions/` (Python)

| Path | Description |
|------|-------------|
| `functions/export_to_sheets/main.py` | Cloud Function: build sheet + send FCM |
| `functions/export_to_sheets/requirements.txt` | gspread, google-auth, firebase-admin |
| `functions/verify_google_token/main.py` | Verify Google ID token, manage user record |
| `functions/verify_apple_token/main.py` | Verify Apple identity token, store email on first auth |

### Modified files

| Path | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add `kotlinx-io` for gzip (conditional compression in `commonMain`) |
| `composeApp/build.gradle.kts` | Wire `androidMain` credentials + google-identity deps |
| `di/AppModule.kt` | Include `authModule`, `exportModule` |
| `di/PlatformModule.android.kt` | Bind `SocialAuthProvider`, `IFcmTokenProvider`, `INotificationPermission`, `ExportWorkerScheduler` |
| `di/PlatformModule.ios.kt` | Bind `SocialAuthProvider`, `IFcmTokenProvider`, `INotificationPermission`, `ExportWorkerScheduler` |
| `navigation/routes/RegisteredRoutes.kt` | Register `AuthRoute` |
| `navigation/RootContent.kt` | Handle `OpenExternalUrl` deep link + cold start queuing |
| `features/analytics/presentation/AnalyticsEvent.kt` | Add `ExportTapped`, `DismissExportSheet` |
| `features/analytics/presentation/AnalyticsScreen.kt` | Add export button + `ExportBottomSheet` |
| `androidApp/src/main/AndroidManifest.xml` | Add `plzstop://` intent filter + `POST_NOTIFICATIONS` permission |
| `iosApp/Info.plist` | Add `CFBundleURLSchemes`, `GIDClientID`, `GIDServerClientID`, reversed client ID URL scheme |
| `core/db/dao/CategoryDao.kt` | Add `getAllIncludingArchived()` query |
| `core/db/dao/SubcategoryDao.kt` | Add `getAllIncludingArchived()` query |

---

## Implementation Order

1. Phase 1, Steps 1–4: deps + models + platform implementations + encrypted storage
2. Phase 1, Step 5: auth feature module (data → domain → presentation)
3. Phase 1, Steps 6–7: navigation route + DI wiring
4. Phase 1, Step 8: connect-google-account flow
5. Phase 1, Step 9: logout cleanup
6. Phase 1, Backend: `verifyGoogleToken` + `verifyAppleToken` Cloud Functions
7. Phase 2, Steps 3–4: notification permission + FCM token provider
8. Phase 2, Step 1: `exportToSheets` Cloud Function
9. Phase 2, Step 2: background worker (Android WorkManager + iOS bridge)
10. Phase 2, Step 2 continued: export feature module (data → domain → presentation)
11. Phase 2, Step 5: deep link handling (`plzstop://open`)
12. Phase 2, Steps 6–7: export button in Analytics + DI wiring

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| **Android two-step auth** — Credential Manager gives ID token only, not scoped access token | Explicit second step via `Identity.getAuthorizationClient().authorize()` with `spreadsheets` scope |
| **Google token scope missing** | Request `https://www.googleapis.com/auth/spreadsheets` explicitly in both `AndroidSocialAuthProvider` and `IosSocialAuthBridgeImpl` |
| **Apple Sign-In email only on first auth** | Store email on backend during first sign-in; subsequent logins use subject identifier to look up stored email |
| **FCM token null / notification denied** | Export blocked at pre-flight check. Notification permission is required since push is the only result delivery mechanism |
| **Google access token in background worker (Android)** | `AuthorizationClient` may need Activity context. Options: (a) get token before enqueuing worker and pass as input data, (b) use application-context `AuthorizationClient` if supported. Token is short-lived (~60min) so option (a) is safe if worker runs promptly |
| **iOS `keyWindow` deprecated** | Use `UIApplication.shared.connectedScenes` to get active `UIWindowScene` |
| **Custom URL scheme not verified** | Accept risk — spreadsheet URL is also returned inline. Deep link is convenience, not sole delivery path |
| **Orphan spreadsheet on network failure** | Titled with date range for discoverability. WorkManager retries on Android. Cloud Function sends error push on failure |
| **Worker process death (Android)** | WorkManager persists work across process death — export resumes automatically |
| **iOS background time limit** | Use background `URLSession` for the Cloud Function call — continues even after app suspension. `Task {}` alone only gets ~30s |
| **Stale Google token on logout** | `LogoutUseCase` atomically clears all auth storage |
| **iOS FCM token timing** | Use `Messaging.messaging().token(completion:)` callback variant, not synchronous property |
| **Google account link stored insecurely** | Use `EncryptedSharedPreferences` (Android) / Keychain (iOS), not plain DataStore |
| **`IosFirebaseCallableFunctions` null handling** | Replace null subcategory with `""` before serialization to avoid cast crash in iOS bridge |
| **Sheets API quota** | Use batch operations exclusively (~5 API calls total). Return `QUOTA_EXCEEDED` error if hit |
| **Cloud Function timeout** | Set to 300s. Batch ops should complete in <30s, but large months have buffer |
