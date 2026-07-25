# Export to Spreadsheet or CSV — Implementation Plan

> **Superseded OAuth design:** The token decisions in this WIP plan—especially Resolved Decisions 1, 2, and 8 and all
> steps that return or send a client Google access token—are replaced by
> [Export Feature Technical Specification](../features/export.md) and OpenSpec change
> `define-export-token-lifecycle`. Current clients send a one-time server authorization code only when linking; the
> backend stores an encrypted refresh token and mints access tokens inside `exportToSheets`. The CSV, workbook layout,
> formatting, date-range, and sharing notes below remain useful unless contradicted by the technical specification.

This plan delivers two sequential phases: platform authentication (Google + Apple Sign-In) followed by export options for either a Google Spreadsheet or a local CSV file shared with any app. Google Sheets export depends on Phase 1 being complete for authenticated users; CSV sharing only depends on local expense data.

---

## Prerequisites

- Firebase project already targets `europe-west1`; Cloud Functions Gen 2 must be enabled in the Firebase console
- `ITokensStorage` interface exists (for future custom backend bearer auth) but has **no implementation and is not Koin-bound** — out of scope for this plan; auth/export uses Firebase Auth SDK directly
- `ExpenseDao.getExpensesInRange` returns a `List<ExpenseEntity>` — the export uses the suspend variant (not the Flow one)
- The `FirebaseCallableFunctions` interface (`call(functionName, data)`) is already implemented for both Android (`AndroidFirebaseCallableFunctions`) and iOS (`IosFirebaseCallableFunctions`); the export Cloud Function will be invoked through it
- Google OAuth Client ID must be provisioned in Google Cloud Console for **both** Android (SHA-1 fingerprint registered) **and** iOS (bundle ID registered) — two separate client IDs
- A **Server (Web) Client ID** is also required for backend token verification — this is a third client ID, distinct from the platform-specific ones
- Apple Developer account with Sign in with Apple capability enabled
- Existing TypeScript Firebase Functions project available for Gen 2 exports
- **iOS: APNs auth key must be uploaded to Firebase Console** — required for FCM push notifications on iOS
- **Android 13+**: `POST_NOTIFICATIONS` permission must be declared in manifest
- **iOS**: `UNUserNotificationCenter.requestAuthorization` must be called before FCM can deliver visible notifications

---

## Resolved Design Decisions

1. **No refresh token stored.** The app requests a fresh Google access token from the platform SDK at export time (silent re-auth, no UI). Only the connection status (email + linked flag) is persisted — not the token itself.
2. **Two-step Google flow on Android.** Step 1: Credential Manager (`GetGoogleIdOption`) for identity (ID token). Step 2: Google Identity Services `AuthorizationRequest` for scoped access token (`spreadsheets` scope). These are separate API calls.
3. **Export bottom sheet confirms enqueue only.** The bottom sheet shows "Export started" and auto-dismisses. The worker persists the spreadsheet URL locally when the callable returns; FCM is a best-effort notification path.
4. **Apple Sign-In users see the export button** — tapping it triggers inline "Connect Google Account" flow. After connecting, export proceeds automatically (no second tap).
5. **`userEmail` is never sent from the client** in the export payload. The callable verifies `request.auth` from Firebase Auth. Email is not needed for the current export flow because the spreadsheet is created with the user's Google access token.
6. **Firebase Auth is used for user identity** — `signInWithCredential` with Google/Apple credentials. This ensures callable `request.auth` is populated in Cloud Functions automatically. Firebase Auth calls live behind platform `FirebaseAuthProvider` implementations (Android/iOS), not in `commonMain`. This follows the existing bridge pattern used by `FirebaseCallableFunctions`.
7. **Conditional gzip compression.** Payload < 100KB → send raw JSON (typical monthly export). Payload >= 100KB → gzip + base64 (multi-month, yearly). A `compressed: true/false` flag in the payload tells the Cloud Function which format to expect. Uses `kotlinx-io` `GzipSink`/`GzipSource` in `commonMain` — no `expect/actual` needed.
8. **Auth strategy per platform:**
   - **Android:** Credential Manager for Google Sign-In → Firebase Auth
   - **iOS:** Apple Sign-In (native `AuthenticationServices`) + Google Sign-In SDK (webview-based OAuth) → Firebase Auth
   - **Export:** Separate call to platform Google SDK for a fresh scoped access token (not stored, requested each time)
9. **Background export with dual result delivery.** User taps Export → app enqueues background work → bottom sheet shows "Export started" → dismisses. The background worker calls the Cloud Function, **waits for the response**, and **updates local DB** with the export result (spreadsheet URL, status, timestamp). Android uses WorkManager; iOS uses the existing bridge to Swift and should use background `URLSession` for resilience.
10. **FCM push as optional notification channel.** The Cloud Function sends an FCM push notification with the spreadsheet URL when a token is available. The worker response remains the source of truth and updates local DB with the URL, so export is not blocked if notifications are denied or FCM token retrieval fails.
11. **Multi-currency expenses export converted values only.** The spreadsheet shows the normalized converted amount/currency used by the app for totals and analysis, not both original and converted values.
12. **Open spreadsheet in in-app browser surfaces.** Android uses `CustomTabsIntent`; iOS uses `SFSafariViewController` instead of forcing the system browser.
13. **CSV sharing is local and app-agnostic.** CSV export generates a local `.csv` file from the same converted expense rows and opens the platform share sheet so the user can send it to any compatible app. It does not require Google auth, FCM, Cloud Functions, or `export_history`.

---

## Open Questions

1. ~~Should the app support exporting a custom date range, or always the current calendar month?~~ **Resolved:** Multi-month export supported. User picks a date range. Tab layout choice (`single_tab` / `separate_tabs`) controls whether months go on one sheet or separate sheets.
2. ~~Should multi-currency expenses show both the original amount/currency and the converted amount, or only the converted amount?~~ **Resolved:** Keep converted values only.
3. ~~Should the spreadsheet URL be opened in `CustomTabsIntent` / `SFSafariViewController` or always in the system browser?~~ **Resolved:** Use `CustomTabsIntent` on Android and `SFSafariViewController` on iOS.

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

**`gradle/libs.versions.toml` — new entries needed:**
```toml
[versions]
security-crypto = "1.1.0-alpha06"
kotlinx-io = "0.6.0"

[libraries]
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
kotlinx-io-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-io-core", version.ref = "kotlinx-io" }
```

> `workmanager` and `work-runtime-ktx` are already declared in `libs.versions.toml` but not wired into any `build.gradle.kts`.
> OAuth deps (`androidx-credentials`, `google-identity`, etc.) are already declared and wired via `libs.bundles.googleOauth`.

Android Gradle (`composeApp/build.gradle.kts`) — add to `androidMain` dependencies:
```kotlin
// OAuth deps already wired via libs.bundles.googleOauth — no change needed
implementation(libs.androidx.security.crypto)
implementation(libs.androidx.work.runtime.ktx)
// Firebase Auth + Messaging (not yet in BOM imports)
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-messaging-ktx")
```

`commonMain` dependencies — add:
```kotlin
implementation(libs.kotlinx.io.core)
```

iOS — add via SPM in `iosApp/iosApp.xcodeproj`:
- `GoogleSignIn-iOS` SDK: `https://github.com/google/GoogleSignIn-iOS` tag `8.0.0`
- `FirebaseAuth` SPM package (from existing Firebase SPM dependency)
- `FirebaseMessaging` SPM package (from existing Firebase SPM dependency)
- `AuthenticationServices` is a system framework — no SPM entry needed

**iOS `Info.plist` additions (required):**
- `GIDClientID` — iOS OAuth client ID from Google Cloud Console
- `GIDServerClientID` — Server (Web) client ID for backend verification
- Add reversed client ID as a URL scheme (e.g., `com.googleusercontent.apps.123456`) — required for Google Sign-In redirect

### Step 2 — Common Auth Models & Google Auth Interfaces (`commonMain`) | Effort: Low

> **Existing code:** Google auth interfaces already exist in `features/aauth/google/`. They use a **Composable-driven** pattern where sign-in is triggered from UI context (Activity available naturally). This is the correct approach — it eliminates the need for `ActivityProvider` hacks.

**Required fixes to existing code:**

1. **Rename `features/aauth/` → `features/auth/`** — the `aauth` package is a typo; Android impl is already in `features/auth/`
2. **Fix `com.dog.care` imports** — these are copy-pasted from another project:
   - `GoogleAuthUiProviderImpl.kt`: replace `com.dog.care.logger.logDebug` with project's logging utility (`com.please.stop.app.core.logger.logDebug` — already imported on a separate line)
   - Delete `GetGooglePlayServicesAvailableUseCase.kt` and `IGetGooglePlayServiceAvailableUseCase.kt` — unused, don't follow project MVI/UseCase patterns. Rewrite from scratch if Google Play Services check is needed (see Step 3)
3. **Create `GoogleAuthCredentials`** — referenced by Android impl but doesn't exist yet:

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/auth/google/GoogleAuthCredentials.kt`
```kotlin
data class GoogleAuthCredentials(val webClientId: String)
```

**Existing files (after rename to `features/auth/google/`):**

| File | Status | Description |
|------|--------|-------------|
| `GoogleAuthProvider.kt` | EXISTS | Interface: `@Composable getUiProvider()`, `suspend signOut()` |
| `GoogleAuthUiProvider.kt` | EXISTS | Interface: `suspend signIn(filterByAuthorizedAccounts, isAutoSelectEnabled, scopes): GoogleUser?` |
| `GoogleUser.kt` | EXISTS | `data class GoogleUser(val idToken: String, val accessToken: String? = null)` |
| `GoogleButtonUiContainer.kt` | EXISTS | Composable wrapper — triggers sign-in on click, delivers `GoogleUser` via callback |
| `UiContainerScope.kt` | EXISTS | DSL interface for button container |

> **Key insight:** `GoogleAuthUiProvider.signIn(scopes)` returns both `idToken` (for Firebase Auth) and `accessToken` (for Sheets API) in a single `GoogleUser`. The `scopes` parameter controls whether an access token is requested via Google Identity Services `AuthorizationRequest`.

**New files:**

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/models/data/GoogleAccountLink.kt`
```kotlin
data class GoogleAccountLink(val email: String, val isConnected: Boolean)
```

> Note: No access token stored. The app requests a fresh token from the platform SDK at export time.

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/IGoogleAccountStorage.kt`
```kotlin
interface IGoogleAccountStorage {
    suspend fun write(link: GoogleAccountLink)
    suspend fun read(): GoogleAccountLink?
    suspend fun delete()
}
```

> **No `SocialAuthProvider` interface.** The plan originally proposed a pure-suspend `SocialAuthProvider` in the domain layer. The existing Composable-based `GoogleAuthProvider` replaces it for Google. Apple Sign-In uses a similar Composable pattern on iOS (see Step 3). Sign-in is triggered from the UI layer and the result (`GoogleUser`) flows through events to the StateHolder → UseCase → Repository.

### Step 3 — Platform Implementations | Effort: High

**Android** (`composeApp/src/androidMain/`)

**Existing files (after fix):**

| File | Status | Description |
|------|--------|-------------|
| `GoogleAuthProviderImpl.kt` | EXISTS | Implements `GoogleAuthProvider`; uses `CredentialManager`, `rememberLauncherForActivityResult` for scope consent |
| `GoogleAuthUiProviderImpl.kt` | EXISTS (needs fix) | Implements `GoogleAuthUiProvider`; handles Credential Manager + Google Identity Services `AuthorizationRequest` for scoped tokens |
| `HashedNonce.kt` | EXISTS | SHA-256 nonce for Credential Manager requests |

**Required fixes:**
- `GoogleAuthUiProviderImpl.kt`: remove duplicate `import com.dog.care.logger.logDebug` (line 12) — the correct import `com.please.stop.app.core.logger.logDebug` is already present (line 20)
- Delete `domain/GetGooglePlayServicesAvailableUseCase.kt` and `domain/IGetGooglePlayServiceAvailableUseCase.kt` — unused, don't follow project patterns

**No `ActivityProvider` needed.** The Composable-based `GoogleAuthProviderImpl.getUiProvider()` uses `LocalContext.current` for Activity context and `rememberLauncherForActivityResult` for the authorization intent — Activity context is naturally available.

> **Critical:** `GetGoogleIdOption` returns an ID token only. You CANNOT get a scoped access token from Credential Manager. The `GoogleAuthUiProviderImpl.fetchAccessTokenWithScopes()` uses Google Identity Services `AuthorizationRequest` as a second step — this is already implemented correctly.

**iOS** (`composeApp/src/iosMain/`)

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/auth/google/IosGoogleAuthProvider.kt`
- Implements `GoogleAuthProvider` (same interface as Android)
- `getUiProvider()`: returns `IosGoogleAuthUiProvider` that delegates to `IosSocialAuthBridge`
- `signOut()`: delegates to bridge

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/auth/google/IosSocialAuthBridge.kt`
```kotlin
@ObjCName("IosSocialAuthBridge", exact = true)
interface IosSocialAuthBridge {
    /** Google Sign-In via GIDSignIn SDK (webview) — returns idToken and optional accessToken */
    fun signInWithGoogle(
        scopes: List<String>,
        onSuccess: (idToken: String, accessToken: String?) -> Unit,
        onError: (String) -> Unit,
    )
    /** Apple Sign-In via ASAuthorization — returns identityToken and optional email */
    fun signInWithApple(
        onSuccess: (identityToken: String, nonce: String, email: String?) -> Unit,
        onError: (String) -> Unit,
    )
    fun getGoogleAccessToken(
        scopes: List<String>,
        onSuccess: (accessToken: String) -> Unit,
        onError: (String) -> Unit,
    )
    fun signOut(onComplete: () -> Unit)
}
```

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/auth/google/IosGoogleAuthUiProvider.kt`
- Implements `GoogleAuthUiProvider`; delegates `signIn()` to `IosSocialAuthBridge.signInWithGoogle()` via `suspendCancellableCoroutine`
- Returns `GoogleUser(idToken, accessToken)`

**Apple Sign-In (iOS only):**

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/auth/apple/AppleAuthProvider.kt`
```kotlin
interface AppleAuthProvider {
    /** Returns identity token + nonce for Firebase Auth. Null = user cancelled. */
    suspend fun signIn(): AppleUser?
    suspend fun signOut()
}

data class AppleUser(val identityToken: String, val nonce: String, val email: String?)
```

`composeApp/src/iosMain/kotlin/com/please/stop/app/features/auth/apple/IosAppleAuthProvider.kt`
- Implements `AppleAuthProvider`; delegates to `IosSocialAuthBridge.signInWithApple()` via `suspendCancellableCoroutine`

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/auth/apple/NoOpAppleAuthProvider.kt`
- Implements `AppleAuthProvider`; `signIn()` throws `UnsupportedOperationException`

**Swift side** (`iosApp/`)

`iosApp/IosSocialAuthBridgeImpl.swift`
- `signInWithGoogle(scopes)`:
  1. Call `GIDSignIn.sharedInstance.signIn(withPresenting:)` → get `GIDGoogleUser`
  2. If scopes requested, call `currentUser.addScopes(scopes, presenting:)`
  3. Return `(user.idToken.tokenString, user.accessToken.tokenString)` via `onSuccess`
- `signInWithApple`:
  1. Use `ASAuthorizationAppleIDProvider` via `ASAuthorizationController`
  2. **Presentation context:** Use `UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene` (NOT deprecated `UIApplication.keyWindow`)
  3. Return `(identityToken, nonce, email)` via `onSuccess`
- `getGoogleAccessToken(scopes)`: call `GIDSignIn.sharedInstance.currentUser?.addScopes(scopes, presenting:)` then `currentUser?.refreshTokensIfNeeded()` → return `accessToken.tokenString`
  - If `currentUser` is nil (user signed in with Apple), this triggers a Google sign-in flow
- `signOut`: calls `GIDSignIn.sharedInstance.signOut()`

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

> **Auth flow (Composable-driven):**
> ```
> UI (GoogleButtonUiContainer / AppleButtonUiContainer)
>   → user taps → GoogleAuthUiProvider.signIn() / AppleAuthProvider.signIn()
>   → GoogleUser(idToken, accessToken?) / AppleUser(identityToken, nonce, email?)
>   → AuthEvent.GoogleSignInCompleted(googleUser) / AuthEvent.AppleSignInCompleted(appleUser)
>   → StateHolder → UseCase → Repository
>   → Firebase Auth signInWithCredential(idToken) → persist tokens
> ```
> The UI layer triggers sign-in (has Activity context). The result flows down through MVI events. Firebase Auth calls happen in the **repository**, which uses a platform `FirebaseAuthProvider` bridge (same pattern as `FirebaseCallableFunctions`).

**Data layer**

`features/auth/data/FirebaseAuthProvider.kt` (commonMain interface)
```kotlin
interface FirebaseAuthProvider {
    /** Sign in with Google ID token → Firebase Auth. Returns Firebase UID. */
    suspend fun signInWithGoogleCredential(idToken: String): kotlin.Result<String>
    /** Sign in with Apple identity token → Firebase Auth. Returns Firebase UID. */
    suspend fun signInWithAppleCredential(identityToken: String, nonce: String): kotlin.Result<String>
    /** Delete the current Firebase Auth user. Returns [DeleteAccountResult]. */
    suspend fun deleteAccount(): DeleteAccountResult
    /** Re-authenticate with Google before sensitive operations (delete). */
    suspend fun reauthenticateWithGoogle(idToken: String): kotlin.Result<Unit>
    /** Re-authenticate with Apple before sensitive operations (delete). */
    suspend fun reauthenticateWithApple(identityToken: String, nonce: String): kotlin.Result<Unit>
    /** Sign out from Firebase Auth. */
    suspend fun signOut()
    /** Observe whether a Firebase user is currently signed in. */
    fun observeIsAuthenticated(): Flow<Boolean>
}

sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data object NeedsReauthentication : DeleteAccountResult
    data class Failure(val error: Throwable) : DeleteAccountResult
}
```

> Platform implementations catch `FirebaseAuthRecentLoginRequiredException` (Android) / equivalent (iOS) internally and return `NeedsReauthentication`. This keeps platform-specific exceptions out of `commonMain`.
```

Platform implementations:
- **Android:** `AndroidFirebaseAuthProvider` — calls `FirebaseAuth.getInstance().signInWithCredential()` directly
- **iOS:** `IosFirebaseAuthProvider` — delegates to Swift bridge via `suspendCancellableCoroutine`

> Firebase Auth calls live in platform implementations of `FirebaseAuthProvider`, NOT in the repository. Common layer only sees the interface. This matches the `FirebaseCallableFunctions` pattern.

`features/auth/data/repository/AuthRepositoryImpl.kt`
- Inject `FirebaseAuthProvider`, `IGoogleAccountStorage`
- `suspend fun signInWithGoogle(googleUser: GoogleUser): kotlin.Result<Unit>`:
  1. Call `firebaseAuthProvider.signInWithGoogleCredential(googleUser.idToken)` → get Firebase UID
  2. Write `GoogleAccountLink(email, isConnected = true)` to `IGoogleAccountStorage`
- `suspend fun signInWithApple(appleUser: AppleUser): kotlin.Result<Unit>`:
  1. Call `firebaseAuthProvider.signInWithAppleCredential(appleUser.identityToken, appleUser.nonce)` → get Firebase UID
  2. **Store email on backend on first sign-in** — Apple only returns email on first authorization
- `fun observeIsAuthenticated(): Flow<Boolean>` — delegates to `firebaseAuthProvider.observeIsAuthenticated()`

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
    suspend operator fun invoke(googleUser: GoogleUser): Result = withContext(ioDispatcher) {
        repository.signInWithGoogle(googleUser).fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }
}
```

`features/auth/domain/usecase/SignInWithAppleUseCase.kt` — identical shape, takes `AppleUser`

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
    /** Delivered by GoogleButtonUiContainer callback after platform sign-in completes */
    data class GoogleSignInCompleted(val googleUser: GoogleUser) : AuthEvent
    data object GoogleSignInCancelled : AuthEvent
    /** Delivered by AppleButtonUiContainer callback (iOS only) */
    data class AppleSignInCompleted(val appleUser: AppleUser) : AuthEvent
    data object AppleSignInCancelled : AuthEvent
    data object DismissError : AuthEvent
}
```

`features/auth/presentation/AuthStateHolder.kt`
- Extends `StateHolder<AuthState, AuthEvent>`
- `resolveEventResult`:
  - `GoogleSignInCompleted(user)` → `signInWithGoogleUseCase(user)` → Result
  - `AppleSignInCompleted(user)` → `signInWithAppleUseCase(user)` → Result
  - `*Cancelled` → emit `Idle`
- `getNavigationByResult`: Success → `router.replaceStack(MainBottomTabs.Home)`
- `getStateByResult`: Loading → Idle/Error

`features/auth/presentation/ui/AuthScreen.kt`
- Google: wraps "Continue with Google" button in `GoogleButtonUiContainer(onGoogleSignInResult = { user -> if (user != null) processEvent(GoogleSignInCompleted(user)) else processEvent(GoogleSignInCancelled) })`
- Apple (iOS only, hidden via `platform == Platform.IOS` check): similar `AppleButtonUiContainer`
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
            firebaseAuthProvider = get(),
            googleAccountStorage = get(),
        )
    }
    factory { SignInWithGoogleUseCase(repository = get(), ioDispatcher = get(named(DispatchersQualifiers.IO.name))) }
    factory { SignInWithAppleUseCase(repository = get(), ioDispatcher = get(named(DispatchersQualifiers.IO.name))) }
    factory { ObserveAuthStateUseCase(repository = get()) }
    factory {
        ConnectGoogleAccountUseCase(
            googleAccountStorage = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    factory {
        LogoutUseCase(
            googleAccountStorage = get(),
            bearerTokenClearer = get(),
            firebaseAuthProvider = get(),
            googleAuthProvider = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    factory {
        DeleteAccountUseCase(
            authRepository = get(),
            firebaseAuthProvider = get(),
            googleAccountStorage = get(),
            bearerTokenClearer = get(),
            googleAuthProvider = get(),
            appDatabase = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    viewModel { AuthStateHolder(signInWithGoogleUseCase = get(), signInWithAppleUseCase = get()) }
}
```

Platform modules:
- `PlatformModule.android.kt`:
  - `single<GoogleAuthProvider> { GoogleAuthProviderImpl(credentials = get(), credentialManager = get()) }`
  - `single { GoogleAuthCredentials(webClientId = "YOUR_WEB_CLIENT_ID") }`
  - `single { CredentialManager.create(androidContext()) }`
  - `single<FirebaseAuthProvider> { AndroidFirebaseAuthProvider() }`
  - `single<AppleAuthProvider> { NoOpAppleAuthProvider() }`
- `PlatformModule.ios.kt`:
  - `single<GoogleAuthProvider> { IosGoogleAuthProvider(bridge = get()) }`
  - `single<FirebaseAuthProvider> { IosFirebaseAuthProvider(bridge = get()) }`
  - `single<AppleAuthProvider> { IosAppleAuthProvider(bridge = get()) }`

Register `authModule` in `AppModule.kt`.

### Step 8 — "Connect Google Account" Flow (for Apple users) | Effort: Medium

Apple Sign-In users need a Google account linked to create Sheets.

`features/auth/domain/usecase/ConnectGoogleAccountUseCase.kt`
- Takes `GoogleUser` (from `GoogleButtonUiContainer` callback in export bottom sheet)
- Writes `GoogleAccountLink(email, isConnected = true)` to `IGoogleAccountStorage`
- Does NOT re-authenticate with Firebase — the user remains signed in with Apple
- Returns `Result.Success` or `Result.Failure`

Surfaced inline in the export flow: `ExportBottomSheet` shows `NeedsGoogleAccount` state → renders `GoogleButtonUiContainer` → user taps → `GoogleUser` → `ConnectGoogleAccountUseCase` → export proceeds automatically.

### Step 9 — Logout Cleanup | Effort: Low

`features/auth/domain/usecase/LogoutUseCase.kt`
- Calls atomically:
  1. `IGoogleAccountStorage.delete()`
  2. `BearerTokenClearer.clear()`
  3. `FirebaseAuthProvider.signOut()`
  4. `GoogleAuthProvider.signOut()` — clears Credential Manager state (Android) / GIDSignIn state (iOS)
- Prevents stale Google account data surviving across user sessions

### Step 10 — Delete Account | Effort: Medium

> **Required by App Store & Play Store.** Both stores require apps with account creation to provide account deletion.

> `deleteAccount()`, `reauthenticateWithGoogle()`, and `reauthenticateWithApple()` are already declared in `FirebaseAuthProvider` (see Step 5). Platform implementations catch `FirebaseAuthRecentLoginRequiredException` (Android) / equivalent (iOS) internally and return the typed `DeleteAccountResult`.

`features/auth/domain/usecase/DeleteAccountUseCase.kt`
```kotlin
class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAccountStorage: IGoogleAccountStorage,
    private val bearerTokenClearer: BearerTokenClearer,
    private val googleAuthProvider: GoogleAuthProvider,
    private val appDatabase: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface Result : com.please.stop.app.core.models.domain.Result {
        data object Success : Result
        data object NeedsReauthentication : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }

    suspend operator fun invoke(): Result = withContext(ioDispatcher) {
        when (val deleteResult = firebaseAuthProvider.deleteAccount()) {
            is DeleteAccountResult.Success -> {
                // Clean up all local data
                googleAccountStorage.delete()
                bearerTokenClearer.clear()
                googleAuthProvider.signOut()
                appDatabase.clearAllTables()
                Result.Success
            }
            is DeleteAccountResult.NeedsReauthentication -> Result.NeedsReauthentication
            is DeleteAccountResult.Failure -> Result.Failure(deleteResult.error.toErrorType())
        }
    }
}
```

> **Re-authentication flow:** Firebase requires recent auth for account deletion. If `NeedsReauthentication` is returned, the UI shows the sign-in button again (Google or Apple depending on provider). After re-auth, retry delete.

**Presentation:** Add to Settings screen:
- "Delete Account" button (destructive style)
- Confirmation dialog: "This will permanently delete your account and all data. This cannot be undone."
- On confirm → `DeleteAccountUseCase()` → if `NeedsReauthentication` → show "Please sign in again to confirm" + `GoogleButtonUiContainer` / Apple button → re-auth → retry delete
- On success → navigate to `AuthRoute`

**Cloud Function (optional):** `functions/src/deleteUserData.ts`
- Triggered by Firebase Auth `onDelete` event (background trigger, not callable)
- Deletes user's Firestore records, shared spreadsheets, etc.
- Ensures server-side cleanup even if client-side cleanup fails

### Backend: Optional User-Profile Cloud Functions | Effort: Medium

> **Decision:** Use Firebase Authentication SDK (`signInWithCredential`) on the client side. This means callable `request.auth` is automatically populated in Cloud Functions — no custom JWT verification needed. The backend functions below are only needed if you want additional server-side user record management.

Optional Gen 2 helpers in the existing TypeScript Functions project:

`functions/src/userProfile.ts`
- Callable can receive `{ provider: "apple" | "google", email: string?, fullName: string? }` from an already authenticated Firebase user
- Stores email/fullName on first Apple sign-in when provided — Apple only provides these once
- Does not verify Google/Apple identity tokens itself; Firebase Auth is the identity authority

---

## Phase 2 — Export to Spreadsheet or CSV

**Goal:** User taps "Export" in the Analytics screen, picks an export destination, and exports the selected date range:
- **Google Sheets:** App requests a fresh Google access token, sends expenses to a Firebase Cloud Function, receives a spreadsheet URL, persists it locally, and sends an FCM notification when possible.
- **CSV:** App generates a local CSV file and opens the platform share sheet so the user can share it with any compatible app.

**Depends on:** Phase 1 complete only for Google Sheets export. CSV export can run without Google auth because it uses local data and the platform share sheet.

### Step 0 — Export Destination Choice | Effort: Low

Add a destination selector to the export UI:
```kotlin
enum class ExportDestination { GOOGLE_SHEETS, CSV }
```

Behavior:
- `GOOGLE_SHEETS` keeps the existing Google access token + background worker + Cloud Function path.
- `CSV` skips Google account checks, scoped-token consent, FCM token retrieval, and Cloud Function calls.
- Both destinations use the same date range, category/subcategory mapping, converted-only amount values, and tab layout where applicable.
- For CSV, `SINGLE_TAB` produces one CSV file. `SEPARATE_TABS` is not representable in one CSV, so either hide the tab-layout control for CSV or force `SINGLE_TAB`.

### Step 1 — Cloud Function: `exportToSheets` | Effort: High

Location: `functions/src/exportToSheets.ts` (Firebase Gen 2, `europe-west1`), exported from the existing TypeScript `functions/src/index.ts`

**Configuration:**
- Timeout: **300 seconds** (default 60s is too tight for Sheets API calls)
- Memory: 256MB (default is sufficient)
- Authentication: Firebase Auth required (`request.auth` must be non-null)

**Trigger:** HTTPS Callable (authenticated)

**Input payload:**

Small payload (< 100KB, typical monthly export):
```json
{
  "googleAccessToken": "ya29...",
  "fcmToken": "device-fcm-token",
  "dateRangeLabel": "April 2026",
  "tabLayout": "single_tab",
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
      "notes": "With colleagues"
    }
  ]
}
```

Large payload (>= 100KB, multi-month/yearly export):
```json
{
  "googleAccessToken": "ya29...",
  "fcmToken": "device-fcm-token",
  "dateRangeLabel": "2026-01 to 2026-12",
  "tabLayout": "separate_tabs",
  "currencySymbol": "€",
  "decimalPlaces": 2,
  "compressed": true,
  "expenses": "H4sIAAAAAAAAA6tWKkktLlGyUlAqS8wpTtVRSs7..."
}
```

> When `compressed: true`, `expenses` is a base64-encoded gzip string. When `compressed: false`, `expenses` is the raw JSON array.

**`tabLayout` options:**
- `"single_tab"` — All expenses on one sheet, sorted by date. A **Category Summary** section at the bottom shows totals per category across the full range.
- `"separate_tabs"` — One sheet per month (e.g., "April 2026", "May 2026"). Each sheet has that month's expenses + a Category Summary section. A union of ALL categories across all months is used for every summary — if a category has no expenses in that month, the amount cell is left empty. This ensures consistent row layout across tabs for easy comparison.

> **Security:** `userEmail` is NOT in the payload. The function reads email from `request.auth.token.email` (Firebase Auth, server-verified) only if a future sharing/email feature needs it. The export itself creates the spreadsheet with the user's Google access token, so the file is already owned by the Google account that granted Sheets/Drive access.

**Node dependencies** (`functions/package.json`):
```json
{
  "dependencies": {
    "googleapis": "^144.0.0"
  }
}
```

> `firebase-admin` already exists in the Functions project. Use Node's built-in `zlib` for gzip decompression.

**Spreadsheet structure:**

**Expense rows (same for both layouts):**

| Row | A | B | C | D | E | F |
|-----|---|---|---|---|---|---|
| 1 | Date | Title | Category | Subcategory | Amount (€) | Notes |
| 2..N | expense rows (dates as Sheets date serial numbers) | | | | | |
| N+1 | | | | | **Total** | `=SUM(E2:EN)` |

**Category Summary section (appended after total row, separated by 1 empty row):**

| Row | A | B |
|-----|---|---|
| N+3 | **Category Summary** | |
| N+4 | Category | Amount (€) |
| N+5 | Food | `=SUMIF(C2:CN,"Food",E2:EN)` |
| N+6 | Transport | `=SUMIF(C2:CN,"Transport",E2:EN)` |
| ... | ... | ... |
| Last | Entertainment | _(empty — no expenses this month)_ |

> **Category union for `separate_tabs`:** The Cloud Function first collects ALL unique categories across the entire export range. Each monthly tab's Category Summary uses this full union — categories with no expenses in that month have an empty amount cell. This ensures the same row = same category across all tabs, enabling side-by-side comparison.

> **Category order for `single_tab`:** Same union, sorted alphabetically. Each category appears once with `SUMIF` over the full range.

**Layout behavior:**

| | `single_tab` | `separate_tabs` |
|---|---|---|
| **Sheet count** | 1 sheet (named by date range, e.g., "Jan–Dec 2026") | 1 sheet per month (e.g., "January 2026", "February 2026", ...) |
| **Expense rows** | All expenses sorted by date, all months together | Only that month's expenses per sheet |
| **Total formula** | Sum of all expenses in the range | Sum per month |
| **Category Summary** | Union of all categories, totals across full range | Union of all categories, totals per month — empty cell if category absent that month |

Styling:
- Header row: bold, background `#4285F4`, white text
- Amount column: number format matching `decimalPlaces`
- Date column: number format `yyyy-mm-dd` (using Sheets date serial numbers for proper sorting)
- Total row: bold
- Category Summary header: bold, background `#E8EAF6`
- Freeze first row (`freeze_rows=1`)
- Column widths: A=100, B=200, C=150, D=150, E=120, F=250

> **Data formatting:** Dates must be written as Sheets date serial numbers (not ISO strings) to enable proper sorting/filtering. Use `(date - epoch).days + 25569` to convert.

**Steps inside the function:**
1. Validate `request.auth` is non-null (Firebase Auth)
2. **Parse expenses (conditional decompression):**
   ```ts
   const expenses = data.compressed
     ? JSON.parse(gunzipSync(Buffer.from(data.expenses, "base64")).toString("utf8"))
     : data.expenses;
   ```
3. Build a `google.auth.OAuth2` client and set `{ access_token: googleAccessToken }` — acting on behalf of the user, not a service account. Token must have `spreadsheets` + `drive.file` scopes
4. **Collect category union** — gather all unique category names across all expenses (sorted alphabetically). This is the canonical category list used for every Category Summary section.
5. Create spreadsheet titled `"PlzStop Export - {dateRangeLabel}"` using `google.sheets({ version: "v4", auth })`
6. **Branch on `tabLayout`:**

   **`single_tab`:**
   - One worksheet (named by date range)
   - Batch write: headers → all expense rows sorted by date → total formula → empty row → Category Summary (full union, `SUMIF` formulas)
   - Batch format all styling

   **`separate_tabs`:**
   - Group expenses by month (based on `date` field)
   - For each month, create a worksheet named "{Month Year}" (e.g., "April 2026")
   - Per worksheet: batch write headers → that month's expenses → total formula → empty row → Category Summary (full category union — empty cell for categories absent that month, `SUMIF` formulas for present ones)
   - Batch format each worksheet
   - Delete the default "Sheet1" if it was auto-created

7. Build `spreadsheetUrl` from the created spreadsheet ID. Do not transfer ownership; the spreadsheet is created with the user's Google access token and belongs to that Google account.
8. If `fcmToken` is provided (non-null), send FCM notification:
   ```ts
   const spreadsheetUrlEncoded = encodeURIComponent(spreadsheetUrl);
   await messaging().send({
     token: fcmToken,
     notification: {
       title: "Your export is ready",
       body: "Tap to open your Google Spreadsheet",
     },
     data: {
       deepLink: `plzstop://open?url=${spreadsheetUrlEncoded}`,
       spreadsheetUrl,
     },
     apns: {
       payload: {
         aps: { sound: "default" },
       },
     },
   });
   ```
9. Return `{ "spreadsheetUrl": spreadsheetUrl }` — the background worker receives this response and persists the result to local DB

> FCM is optional in this architecture. If `fcmToken` is null or notification permission is denied, the function still creates the spreadsheet and returns the URL to the worker, which persists it in `export_history`.

**Error handling in the function:**
- Gzip/base64 decompression failure → return error code `INVALID_PAYLOAD` with message
- Google API 403 on create/write → return error code `INVALID_TOKEN` (access token invalid or missing required scopes: `spreadsheets`, `drive.file`)
- Sheets API quota exceeded (60 req/min/user) → return error code `QUOTA_EXCEEDED` with retry-after hint
- Google API 401 during write → return error code `TOKEN_EXPIRED` (access token expired mid-execution — unlikely with batch ops but possible)

### Step 1a — CSV Generation + Share Sheet | Effort: Medium

CSV export is local-only and does not enqueue background work.

**CSV structure:**

| Column | Value |
|--------|-------|
| Date | Local date in `yyyy-mm-dd` |
| Title | Expense title |
| Category | Category name, including archived categories |
| Subcategory | Subcategory name or empty string |
| Amount | Converted amount as a decimal string using the user's `decimalPlaces` |
| Notes | Expense notes or empty string |

Rules:
- Encode as UTF-8.
- Use RFC 4180 escaping: wrap fields containing comma, quote, CR, or LF in quotes; double embedded quotes.
- Use `\r\n` line endings for maximum spreadsheet compatibility.
- File name: `plzstop-export-{startDate}-to-{endDate}.csv`.
- Export converted amount only; do not include original amount/currency or conversion-rate columns.

**Common CSV builder** (`commonMain`):
```kotlin
class CsvExportBuilder {
    fun build(rows: List<ExportExpenseRow>): String
}
```

`ExportExpenseRow` should be shared by Google Sheets and CSV payload builders so both paths stay aligned.

**Platform share boundary** (`commonMain`):
```kotlin
interface DocumentSharer {
    suspend fun shareCsv(fileName: String, content: String): kotlin.Result<Unit>
}
```

Platform implementations:
- Android: write the CSV to `cacheDir/exports/`, expose it through `FileProvider`, and launch `Intent.ACTION_SEND` with MIME type `text/csv` and `FLAG_GRANT_READ_URI_PERMISSION`.
- iOS: write the CSV to `NSTemporaryDirectory()` and present `UIActivityViewController` with the file URL.

> The share-sheet result should be treated as "launched", not "delivered". Platforms do not reliably report whether the receiving app actually saved or sent the file.

### Step 2 — Background Worker (Platform) | Effort: High

The Google Sheets export runs in the background — user taps Export, sees confirmation, and dismisses. The worker calls the Cloud Function, waits for the result, and updates local DB. The Cloud Function also sends an FCM push as an optional notification channel when a token is available. CSV export does not use this worker.

**Export history entity** (`commonMain`):

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/entity/ExportHistoryEntity.kt`
```kotlin
@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val status: ExportStatus,
    val spreadsheetUrl: String? = null,
    val errorMessage: String? = null,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)

enum class ExportStatus { PENDING, SUCCESS, FAILED }
```

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/dao/ExportHistoryDao.kt`
```kotlin
@Dao
interface ExportHistoryDao {
    @Insert
    suspend fun insert(entity: ExportHistoryEntity): Long

    @Query("UPDATE export_history SET status = :status, spreadsheetUrl = :url, completedAtEpochMillis = :completedAt WHERE id = :id")
    suspend fun updateResult(id: Long, status: ExportStatus, url: String?, completedAt: Long)

    @Query("UPDATE export_history SET status = :status, errorMessage = :error, completedAtEpochMillis = :completedAt WHERE id = :id")
    suspend fun updateError(id: Long, status: ExportStatus, error: String?, completedAt: Long)

    @Query("SELECT * FROM export_history ORDER BY createdAtEpochMillis DESC LIMIT 1")
    fun observeLatest(): Flow<ExportHistoryEntity?>
}
```

> Add `ExportHistoryEntity` to `@Database(entities = [...])` in `AppDatabase.kt` and add `abstract fun exportHistoryDao(): ExportHistoryDao`.

**Common interface** (`commonMain`):

`composeApp/src/commonMain/kotlin/com/please/stop/app/features/export/domain/ExportWorkerScheduler.kt`
```kotlin
interface ExportWorkerScheduler {
    /**
     * Enqueue export work. The Google access token is obtained BEFORE enqueuing
     * (requires Activity context on Android) and passed as input data.
     * The worker then:
     * 1. Query expenses for the date range
     * 2. Build payload (conditionally compressed)
     * 3. Call Cloud Function with the pre-fetched token — WAIT for response
     * 4. Update local DB (export_history) with result
     * Cloud Function also sends FCM push notification as a parallel channel.
     */
    fun enqueue(exportId: Long, googleAccessToken: String, tabLayout: String, startDateMillis: Long, endDateMillis: Long)
}
```

**Android** — WorkManager (declared in `libs.versions.toml`, wire into `build.gradle.kts`):

> **DI in WorkManager:** `CoroutineWorker` doesn't support constructor injection. A custom `KoinWorkerFactory` must be registered in `PleaseStopApplication.onCreate()` via `WorkManager.Configuration.Builder().setWorkerFactory(factory).build()`. See below.

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/export/data/KoinWorkerFactory.kt`
```kotlin
class KoinWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            ExportWorker::class.java.name -> ExportWorker(
                context = appContext,
                params = workerParameters,
                expenseDao = KoinJavaComponent.get(AppDatabase::class.java).expenseDao(),
                categoryDao = KoinJavaComponent.get(AppDatabase::class.java).categoryDao(),
                subcategoryDao = KoinJavaComponent.get(AppDatabase::class.java).subcategoryDao(),
                userProfileDao = KoinJavaComponent.get(AppDatabase::class.java).userProfileDao(),
                callableFunctions = KoinJavaComponent.get(FirebaseCallableFunctions::class.java),
                fcmTokenProvider = KoinJavaComponent.get(IFcmTokenProvider::class.java),
                exportHistoryDao = KoinJavaComponent.get(AppDatabase::class.java).exportHistoryDao(),
            )
            else -> null
        }
    }
}
```

> Register in `PleaseStopApplication.onCreate()`:
> ```kotlin
> WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(KoinWorkerFactory()).build())
> ```
> Also add `android:name=".PleaseStopApplication"` with `tools:replace="android:name"` if not already set, and set `default_process_initializer` provider to `false` in manifest to disable default WorkManager initializer.

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/export/data/ExportWorker.kt`
```kotlin
class ExportWorker(
    context: Context,
    params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val userProfileDao: UserProfileDao,
    private val callableFunctions: FirebaseCallableFunctions,
    private val fcmTokenProvider: IFcmTokenProvider,
    private val exportHistoryDao: ExportHistoryDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val accessToken = inputData.getString("accessToken")
            ?: return Result.failure()
        val startDate = inputData.getLong("startDate", 0)
        val endDate = inputData.getLong("endDate", 0)
        val exportId = inputData.getLong("exportId", 0)

        // 1. Query expenses, categories, user profile from Room
        // 2. Build payload, conditionally compress
        // 3. Call Cloud Function via FirebaseCallableFunctions — WAIT for response
        // 4. On success: update export_history with spreadsheetUrl + SUCCESS status
        // 5. On failure: update export_history with error + FAILED status
        // Cloud Function ALSO sends FCM push — independent of worker result

        return try {
            val response = callableFunctions.call("exportToSheets", payload)
            response.fold(
                onSuccess = { data ->
                    val url = data["spreadsheetUrl"] as? String
                    exportHistoryDao.updateResult(
                        id = exportId,
                        status = ExportStatus.SUCCESS,
                        url = url,
                        completedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                    Result.success()
                },
                onFailure = { error ->
                    exportHistoryDao.updateError(
                        id = exportId,
                        status = ExportStatus.FAILED,
                        error = error.message,
                        completedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                    Result.failure()
                },
            )
        } catch (e: Exception) {
            exportHistoryDao.updateError(
                id = exportId,
                status = ExportStatus.FAILED,
                error = e.message,
                completedAt = Clock.System.now().toEpochMilliseconds(),
            )
            Result.failure()
        }
    }
}
```

`composeApp/src/androidMain/kotlin/com/please/stop/app/features/export/data/AndroidExportWorkerScheduler.kt`
```kotlin
class AndroidExportWorkerScheduler(
    private val context: Context,
) : ExportWorkerScheduler {

    override fun enqueue(exportId: Long, googleAccessToken: String, tabLayout: String, startDateMillis: Long, endDateMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(workDataOf(
                "exportId" to exportId,
                "accessToken" to googleAccessToken,
                "tabLayout" to tabLayout,
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
- Inject `IGoogleAccountStorage`, `ExpenseDao`, `CategoryDao`, `SubcategoryDao`, `UserProfileDao`, `ExportHistoryDao`, `ExportWorkerScheduler`, `INotificationPermission`, `IFcmTokenProvider`, `DocumentSharer`, `CsvExportBuilder`

> **No `SocialAuthProvider` / `GoogleAuthProvider` injection.** The Google access token is obtained in the **UI layer** via `GoogleButtonUiContainer(scopes = listOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))` and passed down through the export event. This is required because `GoogleAuthUiProvider` needs Composable context (Activity) to request scoped tokens.

Key implementation details:

1. **Pre-flight checks (run synchronously before enqueuing worker):**
   ```kotlin
   suspend fun validateAndEnqueueExport(
       destination: ExportDestination,
       googleAccessToken: String?,
       tabLayout: TabLayout,
       startDate: Instant,
       endDate: Instant,
   ): ExportValidationResult {
      // Notification permission is optional. Request it from the UI layer before export
      // when possible, but do not block export if permission is denied.

       // Check expenses exist in range
       val expenses = expenseDao.getExpensesInRange(startDate.toEpochMilliseconds(), endDate.toEpochMilliseconds())
       if (expenses.isEmpty()) return ExportValidationResult.NoExpenses

       if (destination == ExportDestination.CSV) {
           val rows = buildExportRows(expenses)
           val csv = csvExportBuilder.build(rows)
           return documentSharer.shareCsv(
               fileName = buildCsvFileName(startDate, endDate),
               content = csv,
           ).fold(
               onSuccess = { ExportValidationResult.CsvShareLaunched(expenseCount = expenses.size) },
               onFailure = { ExportValidationResult.Failure(it) },
           )
       }

       val accessToken = googleAccessToken ?: return ExportValidationResult.GoogleAccountNotLinked

       // Create PENDING record in local DB before enqueuing worker
       val exportId = exportHistoryDao.insert(
           ExportHistoryEntity(
               startDateEpochMillis = startDate.toEpochMilliseconds(),
               endDateEpochMillis = endDate.toEpochMilliseconds(),
               status = ExportStatus.PENDING,
               createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
           )
       )

       // Enqueue background work — worker will update this record on completion
       exportWorkerScheduler.enqueue(exportId, accessToken, tabLayout.name.lowercase(), startDate.toEpochMilliseconds(), endDate.toEpochMilliseconds())
       return ExportValidationResult.Enqueued(expenseCount = expenses.size)
   }
   ```
   > **Token obtained in UI layer.** The `ExportBottomSheet` wraps the export confirm button in `GoogleButtonUiContainer(scopes = listOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))`. When the user taps, `GoogleAuthUiProvider` requests a scoped access token (Composable context has Activity). The `spreadsheets` scope covers data writes/formatting; `drive.file` covers spreadsheet creation and sharing (scoped to files created by this app only). The token is passed via `ExportEvent.ConfirmExport(accessToken)` → use case → repository → worker input data. Token is short-lived (~60min) but WorkManager with `NetworkType.CONNECTED` runs promptly.

2. **Category/subcategory name mapping — avoid N+1 queries (inside worker):**
   ```kotlin
   val allCategories = categoryDao.observeAllIncludingArchived().first()  // single query
   val allSubcategories = subcategoryDao.observeAllIncludingArchived().first()  // single query
   val categoryMap = allCategories.associateBy { it.id }
   val subcategoryMap = allSubcategories.associateBy { it.id }
   ```
   > Uses existing `observeAllIncludingArchived()` Flow DAO methods with `.first()` — same pattern as `AnalyticsRepositoryImpl`. No new DAO queries needed.
   > **Edge case:** Archived categories/subcategories must be included — expenses may reference them.

3. **Amount conversion from minor units:**
   ```kotlin
   val displayAmount = expense.amountMinorUnits.toBigDecimal()
       .movePointLeft(userProfile.decimalPlaces)
       .toPlainString()
   ```
   > Export converted values only. Do not include `originalAmount`, `originalCurrency`, or conversion-rate columns in the Sheets payload.

4. **Date range uses device timezone:**
   ```kotlin
   val tz = TimeZone.currentSystemDefault()
   val startInstant = selectedStartDate.atStartOfDayIn(tz)
   val endInstant = selectedEndDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
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

`features/export/domain/usecase/ExportDataUseCase.kt`
```kotlin
class ExportDataUseCase(
    private val repository: ExportRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface Result : com.please.stop.app.core.models.domain.Result {
        data class Enqueued(val expenseCount: Int) : Result
        data class CsvShareLaunched(val expenseCount: Int) : Result
        data object GoogleAccountNotLinked : Result
        data object NoExpenses : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }

    suspend operator fun invoke(
        destination: ExportDestination,
        googleAccessToken: String?,
        tabLayout: TabLayout,
        startDate: Instant,
        endDate: Instant,
    ): Result = withContext(ioDispatcher) {
        repository.validateAndEnqueueExport(destination, googleAccessToken, tabLayout, startDate, endDate).toResult()
    }
}
```

> Note: `Success(spreadsheetUrl)` is removed from the foreground use case return. The worker persists the URL from the callable response in `export_history`; push notification is best effort only.

**Presentation layer**

`features/export/presentation/ExportState.kt`
```kotlin
@Stable
sealed interface ExportState {
    data object Idle : ExportState
    data class Confirm(
        val expenseCount: Int,
        val destination: ExportDestination = ExportDestination.GOOGLE_SHEETS,
        val tabLayout: TabLayout = TabLayout.SINGLE_TAB,
    ) : ExportState
    data object Enqueued : ExportState
    data object CsvShareLaunched : ExportState
    data object NeedsGoogleAccount : ExportState
    data object NoExpenses : ExportState
    data class Error(val errorType: ErrorType) : ExportState
}
```

> **Simplified states:** No `Exporting` spinner or `Success` with URL. User sees `Confirm` → `Enqueued` ("Your export is being prepared. You'll receive a notification when it's ready, if notifications are enabled.") → bottom sheet auto-dismisses after a short delay.

`features/export/presentation/ExportEvent.kt`
```kotlin
sealed interface ExportEvent {
    data object ExportTapped : ExportEvent
    data class DestinationSelected(val destination: ExportDestination) : ExportEvent
    data class TabLayoutSelected(val tabLayout: TabLayout) : ExportEvent
    /** Carries the Google access token obtained from GoogleButtonUiContainer in the UI layer */
    data class ConfirmExport(val googleAccessToken: String) : ExportEvent
    data object ShareCsvTapped : ExportEvent
    data class GoogleAccountConnected(val googleUser: GoogleUser) : ExportEvent
    data object DismissError : ExportEvent
    data object Dismiss : ExportEvent
}

enum class ExportDestination { GOOGLE_SHEETS, CSV }
enum class TabLayout { SINGLE_TAB, SEPARATE_TABS }
```

`features/export/presentation/ExportStateHolder.kt`
- Extends `StateHolder<ExportState, ExportEvent>`
- **Guard against double-tap:** `resolveEventResult(ExportTapped)` returns `emptyFlow()` when current state is `Enqueued`
- `resolveEventResult(ExportTapped)` → count expenses + show `Confirm` state. Google account linkage is checked only when the user chooses Google Sheets.
- `resolveEventResult(ConfirmExport(accessToken))` → call `exportDataUseCase(destination = GOOGLE_SHEETS, googleAccessToken = accessToken)` → map to state
- `resolveEventResult(ShareCsvTapped)` → call `exportDataUseCase(destination = CSV, googleAccessToken = null)` → map to state
- `resolveEventResult(GoogleAccountConnected(googleUser))` → call `connectGoogleAccountUseCase(googleUser)` → auto-retry export
- `getStateByResult`:
  - `Enqueued(count)` → `ExportState.Enqueued` (auto-dismiss after 3s)
  - `CsvShareLaunched(count)` → `ExportState.CsvShareLaunched` (show "Share sheet opened" / auto-dismiss after 1s)
  - `GoogleAccountNotLinked` → `NeedsGoogleAccount` (renders `GoogleButtonUiContainer`)
  - `NoExpenses` → `NoExpenses`

`features/export/presentation/ui/ExportBottomSheet.kt`
- A `ModalBottomSheet` (Material3) composable
- States:
  - `Idle` → export button + explanation text
  - `Confirm` → "Export {count} expenses from selected range?" + **Destination toggle** ("Google Sheets" / "CSV") + **Tab layout toggle** only when Google Sheets is selected + action button / Cancel button. Default destination: `GOOGLE_SHEETS`.
  - Google Sheets action button is wrapped in `GoogleButtonUiContainer(scopes = listOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))` to obtain access token on tap.
  - CSV action button dispatches `ShareCsvTapped` and opens the platform share sheet. It does not render `GoogleButtonUiContainer`.
  - `Enqueued` → checkmark + "Your export is being prepared. You'll receive a notification when it's ready, if notifications are enabled." Auto-dismiss after 3s
  - `CsvShareLaunched` → checkmark + "Share sheet opened." Auto-dismiss after 1s
  - `NeedsGoogleAccount` → "Connect Google Account" button + explanation
  - `NoExpenses` → "Nothing to export for this period" message
  - `Error` → error message + retry button

### Step 3 — Notification Permission | Effort: Low

**Optional enhancement for export.** Since the worker response updates local DB with the spreadsheet URL, notification permission is not required for export. Request permission from the UI when the user opts into notifications or before export if the UX asks, but denial should not block the worker.

`composeApp/src/commonMain/kotlin/com/please/stop/app/core/INotificationPermission.kt`
```kotlin
interface INotificationPermission {
    suspend fun isGranted(): Boolean
    suspend fun request(): Boolean
}
```

- **Android:** Check `POST_NOTIFICATIONS` (API 33+). Below API 33, always returns `true`. Runtime permission requests must be triggered from Activity/Compose UI, not from `ExportRepositoryImpl`.
- **iOS:** `UNUserNotificationCenter.requestAuthorization(options: [.alert, .sound])`

`ExportRepositoryImpl.validateAndEnqueueExport()` may read permission state to decide whether to fetch/send an FCM token, but it must still enqueue export if permission is denied.

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

> **URL encoding is critical.** Google Sheets URLs contain `?`, `=`, `#`. The Cloud Function must use `encodeURIComponent(url)`. The app must decode with `URLDecoder.decode(url, "UTF-8")` (Android) or `removingPercentEncoding` (iOS).

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

**`UrlOpener.kt`** — interface bound via Koin (NOT expect/actual, matching project's bridge pattern):
```kotlin
interface UrlOpener {
    fun open(url: String)
}
```
- Android: `CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))` — bound in `PlatformModule.android.kt`
- iOS: present `SFSafariViewController(url = NSURL(string = url)!!)` from the active view controller — bound in `PlatformModule.ios.kt`

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
            exportHistoryDao = get<AppDatabase>().exportHistoryDao(),
            fcmTokenProvider = get(),
            notificationPermission = get(),
            exportWorkerScheduler = get(),
            documentSharer = get(),
            csvExportBuilder = get(),
        )
    }
    factory { CsvExportBuilder() }
    factory { ExportDataUseCase(repository = get(), ioDispatcher = get(named(DispatchersQualifiers.IO.name))) }
    viewModel { ExportStateHolder(exportDataUseCase = get(), connectGoogleAccountUseCase = get()) }
}
```

Platform modules:
- `PlatformModule.android.kt`:
  - `single<IFcmTokenProvider> { AndroidFcmTokenProvider() }`
  - `single<INotificationPermission> { AndroidNotificationPermission(context = get()) }`
  - `single<ExportWorkerScheduler> { AndroidExportWorkerScheduler(context = get()) }`
  - `single<UrlOpener> { AndroidUrlOpener(context = get()) }`
  - `single<DocumentSharer> { AndroidDocumentSharer(context = get()) }`
- `PlatformModule.ios.kt`:
  - `single<IFcmTokenProvider> { IosFcmTokenProvider(bridge = get()) }`
  - `single<INotificationPermission> { IosNotificationPermission() }`
  - `single<ExportWorkerScheduler> { IosExportWorkerScheduler(bridge = get()) }`
  - `single<UrlOpener> { IosUrlOpener() }`
  - `single<DocumentSharer> { IosDocumentSharer(bridge = get()) }`

Register `exportModule` in `AppModule.kt`.

---

## Edge Cases Checklist

| # | Edge Case | Handling |
|---|-----------|----------|
| 1 | **Empty date range** — 0 expenses | Pre-flight check in `validateAndEnqueueExport()` returns `NoExpenses`; no worker enqueued |
| 2 | **Double-tap export** | WorkManager `ExistingWorkPolicy.KEEP` ignores duplicate enqueue. StateHolder ignores event when `Enqueued` |
| 3 | **Archived categories** | Uses existing `observeAllIncludingArchived().first()` — no new DAO queries needed |
| 4 | **Null subcategory** | Replaced with `""` before serialization (iOS `FirebaseCallableFunctions` safety) |
| 5 | **Multi-currency expenses** | Export converted amount only. Do not include original amount/currency columns |
| 6 | **Deep link URL encoding** | Cloud Function: `encodeURIComponent(url)`. App: decode before opening |
| 7 | **Timezone for date range boundaries** | Use `TimeZone.currentSystemDefault()` for start/end calculation |
| 8 | **Cold start from deep link** | Queue deep link, process after nav graph composition |
| 9 | **Notification permission denied** | Export proceeds. No FCM push is sent, but worker response persists `spreadsheetUrl` in local DB |
| 10 | **Google token expired mid-function** | Unlikely with batch ops (<30s), but function returns `TOKEN_EXPIRED` error. Error push is best effort when an FCM token is available |
| 11 | **Apple first sign-in email** | Email stored on backend during first auth — subsequent logins use subject ID |
| 12 | **Network failure mid-export** | WorkManager retries automatically (Android). Orphan spreadsheet may exist — title includes date range for discoverability |
| 13 | **Sheets API quota** | Batch ops keep it to ~5 API calls. Function returns `QUOTA_EXCEEDED`; error push is best effort when an FCM token is available |
| 14 | **Stale Google session on logout** | `LogoutUseCase` clears `IGoogleAccountStorage` + `BearerTokenClearer` + Firebase Auth + Google Sign-In |
| 15 | **iOS FCM token nil before APNs registration** | Use callback variant of `Messaging.messaging().token(completion:)` |
| 16 | **`amountMinorUnits` conversion** | `BigDecimal.movePointLeft(decimalPlaces)` — handles JPY (0 places), EUR (2), BHD (3) |
| 17 | **Payload size** | < 100KB raw JSON (most months). >= 100KB → gzip+base64 (multi-month/yearly). `compressed` flag in payload |
| 18 | **Auto-retry after Google connect** | Apple user connects Google → `ExportStateHolder` auto-triggers export, no second tap |
| 19 | **Worker fails silently** | Worker updates `export_history` with `FAILED`. Error push is best effort only when an FCM token is available |
| 20 | **App killed during background export** | Android: WorkManager survives process death. iOS: background URLSession continues independently |
| 21 | **Google access token in worker** | **Resolved:** Token is obtained before enqueuing the worker (requires Activity context on Android). Passed as WorkManager input data. Token lasts ~60min; WorkManager with `NetworkType.CONNECTED` runs promptly |
| 22 | **Delete account requires re-auth** | Firebase requires recent authentication for `user.delete()`. If stale session, show re-auth flow (Google/Apple button) then retry |
| 23 | **Delete account server-side cleanup** | Firebase Auth `onDelete` trigger cleans Firestore records. Client clears local DB via `appDatabase.clearAllTables()` |
| 24 | **Delete account during export** | If WorkManager export is in-flight when account is deleted, Cloud Function will fail (invalid auth) — orphan spreadsheet may exist but user won't be notified |
| 25 | **CSV share cancelled** | Treat share sheet launch as success; platforms do not reliably report final delivery |
| 26 | **No compatible CSV app** | Platform share sheet shows no targets or returns failure; map to export error/snackbar |
| 27 | **CSV + separate tabs** | Hide tab-layout control for CSV or force `SINGLE_TAB`; one CSV file cannot represent workbook tabs |

---

## File Index

### Existing files — `commonMain` (rename `aauth/` → `auth/`)

| Path | Status | Description |
|------|--------|-------------|
| `features/auth/google/GoogleAuthProvider.kt` | EXISTS (fix package) | Interface: `@Composable getUiProvider()`, `suspend signOut()` |
| `features/auth/google/GoogleAuthUiProvider.kt` | EXISTS (fix package) | Interface: `suspend signIn(filterByAuthorizedAccounts, isAutoSelectEnabled, scopes): GoogleUser?` |
| `features/auth/google/GoogleUser.kt` | EXISTS (fix package) | `data class GoogleUser(val idToken: String, val accessToken: String? = null)` |
| `features/auth/google/GoogleButtonUiContainer.kt` | EXISTS (fix package) | Composable wrapper — triggers sign-in, delivers `GoogleUser` via callback |
| `features/auth/google/UiContainerScope.kt` | EXISTS (fix package) | DSL interface for button container |

### New files — `commonMain`

| Path | Description |
|------|-------------|
| `features/auth/google/GoogleAuthCredentials.kt` | `data class GoogleAuthCredentials(val webClientId: String)` |
| `features/auth/apple/AppleAuthProvider.kt` | Interface: `suspend signIn(): AppleUser?`, `suspend signOut()` |
| `features/auth/apple/AppleUser.kt` | `data class AppleUser(val identityToken: String, val nonce: String, val email: String?)` |
| `features/auth/data/FirebaseAuthProvider.kt` | Interface for Firebase Auth calls (platform-implemented) |
| `core/models/data/GoogleAccountLink.kt` | Google account connection model (email + linked flag) |
| `core/IGoogleAccountStorage.kt` | Interface for Google account link persistence |
| `core/db/entity/ExportHistoryEntity.kt` | Export history Room entity + `ExportStatus` enum |
| `core/db/dao/ExportHistoryDao.kt` | DAO for export history (insert, updateResult, updateError, observeLatest) |
| `core/IFcmTokenProvider.kt` | FCM token interface |
| `core/INotificationPermission.kt` | Notification permission check/request interface |
| `core/DocumentSharer.kt` | Platform share-sheet interface for CSV files |
| `features/auth/domain/repository/AuthRepository.kt` | Auth repository interface |
| `features/auth/domain/usecase/SignInWithGoogleUseCase.kt` | Google sign-in use case (takes `GoogleUser`) |
| `features/auth/domain/usecase/SignInWithAppleUseCase.kt` | Apple sign-in use case (takes `AppleUser`) |
| `features/auth/domain/usecase/ObserveAuthStateUseCase.kt` | Auth state observer |
| `features/auth/domain/usecase/ConnectGoogleAccountUseCase.kt` | Link Google account for Apple users |
| `features/auth/domain/usecase/LogoutUseCase.kt` | Clears all auth state atomically |
| `features/auth/domain/usecase/DeleteAccountUseCase.kt` | Deletes Firebase Auth account + all local data |
| `features/auth/data/repository/AuthRepositoryImpl.kt` | Auth repository implementation |
| `features/auth/data/GoogleAccountStorageImpl.kt` | Encrypted storage for Google account link |
| `features/auth/presentation/AuthState.kt` | Auth UI state |
| `features/auth/presentation/AuthEvent.kt` | Auth events (carries `GoogleUser`/`AppleUser` from UI) |
| `features/auth/presentation/AuthStateHolder.kt` | Auth state holder |
| `features/auth/presentation/ui/AuthScreen.kt` | Login screen composable |
| `features/auth/di/AuthModule.kt` | Koin auth module |
| `features/export/domain/repository/ExportRepository.kt` | Export repository interface |
| `features/export/domain/usecase/ExportDataUseCase.kt` | Export use case for Google Sheets and CSV |
| `features/export/domain/model/ExportDestination.kt` | Export destination enum (`GOOGLE_SHEETS`, `CSV`) |
| `features/export/domain/model/ExportExpenseRow.kt` | Shared normalized export row for Sheets and CSV |
| `features/export/data/CsvExportBuilder.kt` | RFC 4180 CSV builder |
| `features/export/data/repository/ExportRepositoryImpl.kt` | Export repository implementation |
| `features/export/domain/ExportWorkerScheduler.kt` | Common interface for platform background worker |
| `features/export/presentation/ExportState.kt` | Export UI state (Confirm, Enqueued, CsvShareLaunched, NeedsGoogleAccount, NoExpenses) |
| `features/export/presentation/ExportEvent.kt` | Export events (`ConfirmExport` carries access token from UI) |
| `features/export/presentation/ExportStateHolder.kt` | Export state holder (double-tap guard) |
| `features/export/presentation/ui/ExportBottomSheet.kt` | Export bottom sheet composable |
| `features/export/di/ExportModule.kt` | Koin export module |
| `navigation/routes/AuthRoute.kt` | Auth navigation route |
| `navigation/deeplink/DeepLinkResult.kt` | Add `OpenExternalUrl` variant |
| `navigation/deeplink/DeepLinkResolver.kt` | Handle `open` deep link path |
| `utils/UrlOpener.kt` | Interface for platform URL opening (Koin-bound, not expect/actual) |

### Existing files — `androidMain` (fix imports)

| Path | Status | Description |
|------|--------|-------------|
| `features/auth/GoogleAuthProviderImpl.kt` | EXISTS (fix imports) | Implements `GoogleAuthProvider` with `CredentialManager` |
| `features/auth/GoogleAuthUiProviderImpl.kt` | EXISTS (fix `com.dog.care` imports) | Credential Manager + Google Identity Services authorization |
| `features/auth/HashedNonce.kt` | EXISTS | SHA-256 nonce utility |

> **Delete:** `features/auth/domain/GetGooglePlayServicesAvailableUseCase.kt` and `IGetGooglePlayServiceAvailableUseCase.kt` — unused, don't follow project patterns. Rewrite if needed.

### New files — `androidMain`

| Path | Description |
|------|-------------|
| `features/auth/data/AndroidFirebaseAuthProvider.kt` | Firebase Auth `signInWithCredential()` + `signOut()` |
| `features/auth/apple/NoOpAppleAuthProvider.kt` | Throws `UnsupportedOperationException` on Android |
| `features/auth/data/AndroidFcmTokenProvider.kt` | Firebase Messaging token |
| `features/auth/data/AndroidNotificationPermission.kt` | POST_NOTIFICATIONS permission |
| `features/export/data/ExportWorker.kt` | WorkManager `CoroutineWorker` for background export |
| `features/export/data/AndroidExportWorkerScheduler.kt` | WorkManager enqueue logic |
| `features/export/data/KoinWorkerFactory.kt` | Custom WorkerFactory for DI in WorkManager workers |
| `utils/AndroidUrlOpener.kt` | Custom Tabs URL opener |
| `utils/AndroidDocumentSharer.kt` | Writes CSV to cache + shares via `Intent.ACTION_SEND` / `FileProvider` |

### New files — `iosMain`

| Path | Description |
|------|-------------|
| `features/auth/google/IosGoogleAuthProvider.kt` | Implements `GoogleAuthProvider` via bridge |
| `features/auth/google/IosGoogleAuthUiProvider.kt` | Implements `GoogleAuthUiProvider` via bridge |
| `features/auth/google/IosSocialAuthBridge.kt` | ObjC-visible bridge interface (Google sign-in + scoped tokens) |
| `features/auth/apple/IosAppleAuthProvider.kt` | Implements `AppleAuthProvider` via bridge |
| `features/auth/data/IosFirebaseAuthProvider.kt` | Firebase Auth via Swift bridge |
| `features/auth/data/IosFcmTokenProvider.kt` | KMP side of FCM token retrieval |
| `features/auth/data/IosNotificationPermission.kt` | UNUserNotificationCenter permission |
| `features/export/data/IosExportWorkerScheduler.kt` | Bridge to Swift export worker |
| `utils/IosUrlOpener.kt` | `SFSafariViewController` URL opener |
| `utils/IosDocumentSharer.kt` | Writes CSV to temp directory + presents `UIActivityViewController` |

### New files — `iosApp` (Swift)

| Path | Description |
|------|-------------|
| `iosApp/IosSocialAuthBridgeImpl.swift` | GIDSignIn + ASAuthorization + scoped token retrieval |
| `iosApp/IosFirebaseAuthBridgeImpl.swift` | Firebase Auth `signIn(with:)` + `signOut()` |
| `iosApp/IosFcmTokenBridgeImpl.swift` | Firebase Messaging token retrieval |
| `iosApp/ExportWorkerImpl.swift` | Background export using URLSession background configuration |
| `iosApp/IosDocumentSharerImpl.swift` | Presents `UIActivityViewController` for CSV sharing |

### New files — `functions/` (TypeScript)

| Path | Description |
|------|-------------|
| `functions/src/exportToSheets.ts` | Cloud Function: build sheet + optional FCM |
| `functions/src/index.ts` | Export `exportToSheets` from the existing TypeScript Functions entry point |
| `functions/src/userProfile.ts` | Optional user-profile helpers for first Apple sign-in email storage |

### Modified files

| Path | Change |
|------|--------|
| `features/aauth/` → `features/auth/` | **Rename package** (typo fix) — all 5 files |
| `gradle/libs.versions.toml` | Add `kotlinx-io`, `security-crypto` |
| `composeApp/build.gradle.kts` | Wire `androidMain` security-crypto + work-runtime + firebase-auth + firebase-messaging; `commonMain` kotlinx-io-core |
| `core/db/AppDatabase.kt` | Add `ExportHistoryEntity` to `@Database(entities)`, add `exportHistoryDao()` |
| `di/AppModule.kt` | Include `authModule`, `exportModule` |
| `di/PlatformModule.android.kt` | Bind `GoogleAuthProvider`, `FirebaseAuthProvider`, `AppleAuthProvider`, `IFcmTokenProvider`, `INotificationPermission`, `ExportWorkerScheduler`, `UrlOpener`, `DocumentSharer` |
| `di/PlatformModule.ios.kt` | Bind `GoogleAuthProvider`, `FirebaseAuthProvider`, `AppleAuthProvider`, `IFcmTokenProvider`, `INotificationPermission`, `ExportWorkerScheduler`, `UrlOpener`, `DocumentSharer` |
| `navigation/routes/RegisteredRoutes.kt` | Register `AuthRoute` |
| `navigation/RootContent.kt` | Handle `OpenExternalUrl` deep link + cold start queuing |
| `features/analytics/presentation/AnalyticsEvent.kt` | Add `ExportTapped`, `DismissExportSheet` |
| `features/analytics/presentation/AnalyticsScreen.kt` | Add export button + `ExportBottomSheet` |
| `androidApp/src/main/AndroidManifest.xml` | Add `plzstop://` intent filter + `POST_NOTIFICATIONS` permission + `FileProvider` for CSV sharing |
| `iosApp/Info.plist` | Add `CFBundleURLSchemes`, `GIDClientID`, `GIDServerClientID`, reversed client ID URL scheme |
| `androidApp/src/main/kotlin/.../PleaseStopApplication.kt` | Register `KoinWorkerFactory` for WorkManager |

---

## Implementation Order

1. Phase 1, Steps 1–2: deps + fix existing code (`aauth` → `auth`, `com.dog.care` imports, add `GoogleAuthCredentials`)
2. Phase 1, Step 3: platform implementations (Android fixes + iOS Google/Apple + `FirebaseAuthProvider`)
3. Phase 1, Steps 4–5: encrypted storage + auth feature module (data → domain → presentation)
4. Phase 1, Steps 6–7: navigation route + DI wiring
5. Phase 1, Step 8: connect-google-account flow
6. Phase 1, Step 9: logout cleanup
7. Phase 1, Step 10: delete account
8. Phase 1, Backend: optional user-profile storage for first Apple sign-in email + optional `deleteUserData` cleanup trigger
9. Phase 2, Step 0 + Step 1a: destination selector + CSV row model/builder + `DocumentSharer` platform implementations
10. Phase 2, Steps 3–4: notification permission + FCM token provider
11. Phase 2, Step 1: `exportToSheets` Cloud Function
12. Phase 2, Step 2: background worker (Android WorkManager + iOS bridge)
13. Phase 2, Step 2 continued: export feature module (data → domain → presentation)
14. Phase 2, Step 5: deep link handling (`plzstop://open`)
15. Phase 2, Steps 6–7: export button in Analytics + DI wiring

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| **Android two-step auth** — Credential Manager gives ID token only, not scoped access token | Explicit second step via `Identity.getAuthorizationClient().authorize()` with `spreadsheets` scope |
| **Google token scope missing** | Request `spreadsheets` + `drive.file` scopes explicitly in `GoogleButtonUiContainer(scopes=...)` and `IosSocialAuthBridgeImpl`. `drive.file` needed for create + share; scoped to app-created files only |
| **Apple Sign-In email only on first auth** | Store email on backend during first sign-in; subsequent logins use subject identifier to look up stored email |
| **FCM token null / notification denied** | Export continues. Worker response persists the URL locally; push notification is best effort |
| **Google access token in background worker (Android)** | **Resolved:** Token obtained before enqueuing, passed as WorkManager input data. `AuthorizationClient` requires Activity context — cannot run inside worker. Token is short-lived (~60min) but worker runs promptly with `NetworkType.CONNECTED` constraint |
| **iOS `keyWindow` deprecated** | Use `UIApplication.shared.connectedScenes` to get active `UIWindowScene` |
| **Custom URL scheme not verified** | Accept risk — spreadsheet URL is also persisted from the worker response. Deep link is convenience, not sole delivery path |
| **Orphan spreadsheet on network failure** | Titled with date range for discoverability. WorkManager retries on Android. Error push is best effort only when an FCM token is available |
| **Worker process death (Android)** | WorkManager persists work across process death — export resumes automatically |
| **iOS background time limit** | Use background `URLSession` for the Cloud Function call — continues even after app suspension. `Task {}` alone only gets ~30s |
| **Stale Google token on logout** | `LogoutUseCase` atomically clears `IGoogleAccountStorage` + `BearerTokenClearer` + calls `FirebaseAuthProvider.signOut()` + `GoogleAuthProvider.signOut()` |
| **iOS FCM token timing** | Use `Messaging.messaging().token(completion:)` callback variant, not synchronous property |
| **Google account link stored insecurely** | Use `EncryptedSharedPreferences` (Android) / Keychain (iOS), not plain DataStore |
| **`IosFirebaseCallableFunctions` null handling** | Replace null subcategory with `""` before serialization to avoid cast crash in iOS bridge |
| **Sheets API quota** | Use batch operations exclusively (~5 API calls total). Return `QUOTA_EXCEEDED` error if hit |
| **Cloud Function timeout** | Set to 300s. Batch ops should complete in <30s, but large months have buffer |
| **CSV escaping bugs** | Centralize CSV generation in `CsvExportBuilder` and unit-test commas, quotes, newlines, empty fields, and UTF-8 text |
| **Android file URI exposure** | Use `FileProvider` content URIs and `FLAG_GRANT_READ_URI_PERMISSION`; never share `file://` URIs |
| **iOS share sheet lifecycle** | Present `UIActivityViewController` from the active view controller and treat completion as best effort |
