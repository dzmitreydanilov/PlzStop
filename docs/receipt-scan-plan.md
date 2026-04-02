# Receipt Scan Feature — Implementation Plan

## Context

The Add Expense screen needs a "Scan Receipt" button that launches a document scanner, sends the image to a cloud AI (Gemini 2.0 Flash via Firebase Cloud Function), and pre-fills the expense form with extracted data. The cloud function is protected by Firebase App Check so only the real app can call it. User's expense categories are sent to the AI so it returns a matching `categoryId`.

---

## Step 1: Firebase Cloud Function (Node.js/TypeScript)

### Setup
- `firebase init functions` in project root (or separate `functions/` directory)
- Runtime: Node.js 20, TypeScript
- Dependencies: `@google-cloud/vertexai`, `firebase-functions`, `firebase-admin`

### File: `functions/src/index.ts`

**Function: `analyzeReceipt`** — Firebase callable with App Check enforcement

```typescript
export const analyzeReceipt = onCall(
  {
    enforceAppCheck: true,
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request) => { ... }
);
```

**Request data:**
```json
{
  "imageBase64": "...",
  "categories": [{ "id": 1, "name": "Groceries" }, ...]
}
```

**Gemini prompt strategy:**
- Model: `gemini-2.0-flash`
- **Use `responseMimeType: "application/json"`** to force structured JSON output (no markdown wrapping)
- System prompt instructs to extract: merchantName, totalAmount (decimal), currency (ISO 4217), date (YYYY-MM-DD), categoryId (from user's list)
- If field is uncertain → return null
- **Fallback parsing:** If JSON parsing fails despite JSON mode, strip ` ```json ``` ` markers and retry before returning `IMAGE_UNREADABLE`

**Response contract:**
```json
{
  "status": "success" | "partial" | "unreadable",
  "data": {
    "merchantName": "string | null",
    "totalAmount": "number | null (decimal, e.g. 42.99)",
    "currency": "string | null (ISO 4217)",
    "date": "string | null (YYYY-MM-DD)",
    "categoryId": "number | null"
  },
  "message": "string | null (human-readable explanation)"
}
```

**Error responses:**
| Code | When |
|------|------|
| `IMAGE_UNREADABLE` | Gemini can't parse the image |
| `IMAGE_TOO_LARGE` | Base64 payload > 5MB |
| `INVALID_REQUEST` | Missing imageBase64 or categories |
| `QUOTA_EXCEEDED` | Gemini/function quota hit |
| `INTERNAL` | Unexpected server error |

**Validation in function:**
- Reject if `imageBase64` is missing or empty
- Reject if `categories` is not an array or empty
- Reject if decoded image > 5MB
- Wrap Gemini call in try/catch, return `INTERNAL` on failure
- Parse Gemini JSON response, return `IMAGE_UNREADABLE` if parsing fails

### Deploy
```bash
firebase deploy --only functions
```

---

## Step 2: Firebase App Check Setup

### Firebase Console
- Enable App Check for the project
- Register Android app with Play Integrity provider
- Register iOS app with App Attest provider
- Enforce App Check on the `analyzeReceipt` function

### Android — `androidApp/`
Initialize in `Application.onCreate()`:
```kotlin
Firebase.appCheck.installAppCheckProviderFactory(
    PlayIntegrityAppCheckProviderFactory.getInstance()
)
```

**For debug builds:** Use `DebugAppCheckProviderFactory` instead.

### iOS — `iosApp/`
Initialize in `AppDelegate`:
```swift
let providerFactory = AppCheckProviderFactory()  // App Attest
AppCheck.setAppCheckProviderFactory(providerFactory)
```

### Dependencies (see Step 3)

---

## Step 3: Add Dependencies

### `gradle/libs.versions.toml`
```toml
[versions]
mlkit-document-scanner = "16.0.0-beta1"

[libraries]
mlkit-document-scanner = { module = "com.google.android.gms:play-services-mlkit-document-scanner", version.ref = "mlkit-document-scanner" }
```

### `androidApp/build.gradle.kts` — add:
```kotlin
implementation("com.google.firebase:firebase-functions")
implementation("com.google.firebase:firebase-appcheck-playintegrity")
```

### `composeApp/build.gradle.kts`

**androidMain dependencies — add:**
```kotlin
implementation("com.google.firebase:firebase-functions-ktx")
implementation("com.google.firebase:firebase-appcheck-ktx")
implementation("com.google.firebase:firebase-appcheck-playintegrity")
implementation(libs.mlkit.document.scanner)
```

**swiftPMDependencies — update products list:**
```kotlin
swiftPackage(
    url = "https://github.com/firebase/firebase-ios-sdk.git",
    version = "11.12.0",
    products = listOf(
        "FirebaseCore",
        "FirebaseRemoteConfig",
        "FirebaseFunctions",    // NEW
        "FirebaseAppCheck",     // NEW
    ),
)
```

**cinterop — add new def for FirebaseFunctions:**
```
// src/nativeInterop/cinterop/FirebaseFunctions.def
language = Objective-C
package = cocoapods.FirebaseFunctions
headers = FIRFunctions.h FIRHTTPSCallable.h FIRError.h
headerFilter = FIR**
```

Register in build.gradle.kts alongside the existing RemoteConfig cinterop.

---

## Step 4: Document Scanner — `expect/actual`

### Common interface
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/scanner/DocumentScanner.kt`
```kotlin
expect class DocumentScanner {
    suspend fun scan(): Result<ByteArray>  // JPEG bytes, compressed
}
```

### Scanner UX — What the User Sees

Both platforms provide a **native full-screen scanner experience** (not our UI):

**Android (ML Kit Document Scanner):**
```
┌──────────────────────────────┐
│  ← Back                     │
│                              │
│   ┌────────────────────┐    │
│   │                    │    │  Camera viewfinder
│   │   ┌──────────┐    │    │
│   │   │ Receipt  │    │    │  Blue overlay highlights
│   │   │ content  │    │    │  detected document edges
│   │   │   ...    │    │    │
│   │   └──────────┘    │    │
│   │                    │    │
│   └────────────────────┘    │
│                              │
│         [  📷  ]             │  Capture button
│   Gallery              ✓    │  Can also import from gallery
└──────────────────────────────┘
         ↓ after capture
┌──────────────────────────────┐
│  Crop & Rotate screen        │
│  ┌──────────────────────┐   │  User can adjust corners
│  │  ●───────────────●   │   │  if auto-detection was off
│  │  │   Receipt     │   │   │
│  │  │   content     │   │   │
│  │  │     ...       │   │   │
│  │  ●───────────────●   │   │
│  └──────────────────────┘   │
│              [ Done ]        │
└──────────────────────────────┘
```

**iOS (VisionKit):**
```
┌──────────────────────────────┐
│  Cancel              Auto ●  │  Auto-capture toggle
│                              │
│   ┌────────────────────┐    │
│   │                    │    │  Camera viewfinder
│   │   ┌──────────┐    │    │
│   │   │ Receipt  │    │    │  Yellow/white overlay on
│   │   │ content  │    │    │  detected document edges
│   │   │   ...    │    │    │
│   │   └──────────┘    │    │
│   │                    │    │
│   └────────────────────┘    │
│                              │
│  Auto-captures when stable   │  Or manual shutter button
│         [  📷  ]             │
└──────────────────────────────┘
         ↓ after capture
┌──────────────────────────────┐
│  Retake              Keep    │  Review screen
│  ┌──────────────────────┐   │  Perspective-corrected
│  │                      │   │  and cropped result
│  │   Receipt content    │   │
│  │   ...                │   │
│  │                      │   │
│  └──────────────────────┘   │
│              [ Save ]        │
└──────────────────────────────┘
```

**Key UX points:**
- Both scanners auto-detect document edges in real-time
- Both allow manual corner adjustment after capture
- Both return a **cropped, perspective-corrected** image — flat and rectangular
- User sees a "review" step before the image is returned to our app
- Scanner handles its own loading/processing states
- Our "Analyzing receipt..." overlay appears **only after** the scanner dismisses

### Preventing Non-Receipt Images

The document scanner prevents non-document captures (it needs to detect edges), but it **cannot distinguish a receipt from a random document**. A user could scan a book page, a letter, etc.

**Approach: Server-side validation in Gemini prompt.**

Add to the system prompt in the Cloud Function:
```
IMPORTANT: First determine if this image is a receipt, invoice, or bill.

Receipts vary significantly by country and format. Accept ALL of the following:
- Printed thermal receipts (US, EU — typical register tape)
- Handwritten receipts (common in Asia, Middle East, small vendors)
- Formal invoices / tax invoices (EU VAT invoices, Japanese 領収書)
- Restaurant bills with tips or service charges
- Digital/screenshot receipts (e-commerce order confirmations, email receipts)
- Delivery receipts (food delivery, courier services)
- Fuel/gas station receipts
- Market/bazaar handwritten notes with amounts

Do NOT accept:
- Photos with no financial information (landscapes, selfies, random objects)
- ID cards, passports, driver's licenses
- Book pages, articles, non-financial documents
- Screenshots of apps that are not receipts/invoices

If the image is NOT a financial document, return:
{ "status": "unreadable", "message": "This doesn't appear to be a receipt." }

If the image IS a financial document but some fields are unreadable or missing,
still extract what you can and return status: "partial".
```

This is cheaper and more reliable than any client-side image classification. Gemini already "sees" the image — adding one check to the prompt costs zero extra tokens.

**What happens for different images:**
| Image type | Gemini returns | User sees |
|------------|---------------|-----------|
| Clear printed receipt (any country) | `status: "success"` | Form pre-filled |
| Handwritten receipt (amounts readable) | `status: "success"` or `"partial"` | Form pre-filled (maybe no merchant) |
| Formal tax invoice (EU/Asia) | `status: "success"` | Form pre-filled |
| Digital/screenshot receipt | `status: "success"` | Form pre-filled |
| Blurry/damaged receipt | `status: "partial"` or `"unreadable"` | Partial fill or error message |
| Non-receipt document | `status: "unreadable"` | "This doesn't appear to be a receipt." |
| Random photo (landscape, selfie) | `status: "unreadable"` | "This doesn't appear to be a receipt." |
| Blank page | `status: "unreadable"` | "Couldn't read this receipt." |

### Android actual
**File:** `composeApp/src/androidMain/kotlin/com/please/stop/app/features/addexpense/scanner/DocumentScanner.android.kt`

- Uses ML Kit Document Scanner API (`GmsDocumentScannerOptions`)
- Scanner mode: `FULL` (camera + import from gallery)
- Max pages: 1
- Result format: JPEG
- Compress output to max 1024px wide, quality 80 (~200-500KB)
- Launches via `ActivityResultLauncher` — needs Activity reference from Koin

### iOS actual
**File:** `composeApp/src/iosMain/kotlin/com/please/stop/app/features/addexpense/scanner/DocumentScanner.ios.kt`

- Uses `VNDocumentCameraViewController` (VisionKit framework)
- Present via `UIApplication.shared.keyWindow.rootViewController`
- Delegate receives `VNDocumentCameraScan`, take page 0
- Convert `UIImage` → JPEG data, compress to ~1024px wide, quality 0.8
- Bridge to coroutines via `suspendCancellableCoroutine`

---

## Step 5: Firebase Callable Wrapper — `expect/actual`

### Common interface
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/data/remote/FirebaseCallableFunctions.kt`
```kotlin
expect class FirebaseCallableFunctions {
    suspend fun call(functionName: String, data: Map<String, Any?>): Result<Map<String, Any?>>
}
```

### Android actual
**File:** `composeApp/src/androidMain/kotlin/com/please/stop/app/features/addexpense/data/remote/FirebaseCallableFunctions.android.kt`
- `Firebase.functions("europe-west1").getHttpsCallable(name).call(data).await()`
- App Check token attached automatically

### iOS actual
**File:** `composeApp/src/iosMain/kotlin/com/please/stop/app/features/addexpense/data/remote/FirebaseCallableFunctions.ios.kt`
- `FIRFunctions.functionsForRegion("europe-west1").HTTPSCallableWithName(name).callWithObject(data) { result, error -> ... }`
- App Check token attached automatically
- Bridge callback to coroutines via `suspendCancellableCoroutine`

---

## Step 6: Data Layer (commonMain)

### Domain model
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/domain/model/ReceiptData.kt`
```kotlin
data class ReceiptData(
    val merchantName: String?,
    val totalAmountMinorUnits: Long?,
    val currency: String?,       // ISO 4217 from receipt (may differ from user's currency)
    val date: String?,           // YYYY-MM-DD
    val categoryId: Long?,
    val isPartial: Boolean,      // true when status == "partial"
    val message: String?,        // human-readable message from function
)
```

**Note:** `lineItems` are not included in MVP. The function response may contain them but we ignore them. The form only has a single amount + title, not per-item breakdown.

### Repository interface
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/domain/repository/ReceiptRepository.kt`
```kotlin
interface ReceiptRepository {
    suspend fun analyzeReceipt(
        imageBytes: ByteArray,
        categories: List<ExpenseCategory>,
        decimalPlaces: Int,
    ): Result<ReceiptData>
}
```

### Repository implementation
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/data/repository/ReceiptRepositoryImpl.kt`

- Encodes `imageBytes` to base64 string
- Maps `List<ExpenseCategory>` → `List<Map<String, Any?>>` with `id` and `name`
- Calls `FirebaseCallableFunctions.call("analyzeReceipt", data)`
- Parses response map → `ReceiptData`
- Converts `totalAmount` decimal → `totalAmountMinorUnits` using `decimalPlaces`
- Maps `status: "unreadable"` → `Result.failure(ReceiptUnreadableException)`
- Maps `status: "partial"` → `Result.success(ReceiptData(isPartial = true, ...))`
- Maps error codes to appropriate exceptions

### Custom exceptions
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/data/remote/ReceiptAnalysisException.kt`
```kotlin
sealed class ReceiptAnalysisException(message: String) : Exception(message) {
    class Unreadable(message: String) : ReceiptAnalysisException(message)
    class ServiceUnavailable(message: String) : ReceiptAnalysisException(message)
}
```

---

## Step 7: Use Case (commonMain)

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/domain/usecase/AnalyzeReceiptUseCase.kt`

```kotlin
class AnalyzeReceiptUseCase(
    private val receiptRepository: ReceiptRepository,
    private val addExpenseRepository: AddExpenseRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(imageBytes: ByteArray): DomainResult =
        withContext(ioDispatcher) {
            val formData = addExpenseRepository.getFormData()  // new method needed
            val result = receiptRepository.analyzeReceipt(
                imageBytes = imageBytes,
                categories = formData.categories,
                decimalPlaces = formData.decimalPlaces,
            )
            result.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it.toReceiptErrorType()) },
            )
        }

    sealed interface Result : DomainResult {
        data class Success(val data: ReceiptData) : Result
        data class Failure(val receiptError: ReceiptError) : Result
    }
}
```

### AddExpenseRepository change
Add to `AddExpenseRepository` interface:
```kotlin
suspend fun getFormData(): AddExpenseFormData
```
Implement in `AddExpenseRepositoryImpl` — extract the mapping logic from `observeFormData()` into a reusable suspend function.

---

## Step 8: Presentation Layer

### State changes
**File:** `AddExpenseState.kt` — add to `Content`:
```kotlin
val isAnalyzingReceipt: Boolean = false,
val receiptError: ReceiptError? = null,
```

**New file:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/presentation/ReceiptError.kt`
```kotlin
enum class ReceiptError {
    UNREADABLE,
    NO_NETWORK,
    SERVICE_UNAVAILABLE,
}
```

### Event changes
**File:** `AddExpenseEvent.kt` — add:
```kotlin
data class ReceiptScanned(val imageBytes: ByteArray) : AddExpenseEvent
data object DismissReceiptError : AddExpenseEvent
```

Note: The scanner launch itself is triggered from Compose UI, not via an event. The event fires after scanner returns bytes.

### StateHolder changes
**File:** `AddExpenseStateHolder.kt`

Constructor — add `analyzeReceiptUseCase: AnalyzeReceiptUseCase`

`resolveEventResult()` — add:
```kotlin
is AddExpenseEvent.ReceiptScanned -> handleReceiptScanned(event.imageBytes)
is AddExpenseEvent.DismissReceiptError -> flowOf(updateContent { copy(receiptError = null) })
```

New method:
```kotlin
private fun handleReceiptScanned(imageBytes: ByteArray): Flow<DomainResult> = flow {
    emit(updateContent { copy(isAnalyzingReceipt = true, receiptError = null) })
    val result = analyzeReceiptUseCase(imageBytes)
    emit(result)
}
```

`getStateByResult()` — add:
```kotlin
is AnalyzeReceiptUseCase.Result.Success -> {
    val data = result.data
    (previous as? Content)?.copy(
        isAnalyzingReceipt = false,
        title = data.merchantName ?: content.title,
        amountInput = data.totalAmountMinorUnits?.let {
            formatMinorUnitsToInput(it, content.decimalPlaces)
        } ?: content.amountInput,
        selectedCategoryId = data.categoryId ?: content.selectedCategoryId,
        dateEpochMillis = data.date?.toEpochMillis() ?: content.dateEpochMillis,
    ) ?: previous
}
is AnalyzeReceiptUseCase.Result.Failure -> {
    (previous as? Content)?.copy(
        isAnalyzingReceipt = false,
        receiptError = result.receiptError,
    ) ?: previous
}
```

### UI changes
**File:** `AddExpenseScreen.kt`

- Add "Scan Receipt" button with camera icon at top of form (next to amount input area)
- Button launches `DocumentScanner.scan()` via coroutine scope
- On success → dispatch `ReceiptScanned(bytes)` event
- On failure → show scanner-level error (camera permission denied, etc.)
- Show loading overlay with "Analyzing receipt..." when `isAnalyzingReceipt == true`
- Show snackbar/dialog for `receiptError` with user-friendly messages:
  - `UNREADABLE` → "Couldn't read this receipt. Try again or enter manually."
  - `NO_NETWORK` → "Receipt scan requires an internet connection."
  - `SERVICE_UNAVAILABLE` → "Service temporarily unavailable. Enter manually."

---

## Step 9: DI Wiring

**File:** `AddExpenseModule.kt` — add:

```kotlin
single { FirebaseCallableFunctions() }

single<ReceiptRepository> {
    ReceiptRepositoryImpl(
        callableFunctions = get(),
        ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
    )
}

factory {
    AnalyzeReceiptUseCase(
        receiptRepository = get(),
        addExpenseRepository = get(),
        ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
    )
}
```

Update `AddExpenseStateHolder` viewModel definition to inject `analyzeReceiptUseCase`.

`DocumentScanner` is provided platform-specifically:
- Android: needs `ComponentActivity` from platform module
- iOS: no constructor dependencies

---

## Step 10: Image Compression — `expect/actual`

Compression happens **inside `DocumentScanner`** before returning bytes. The rest of the app always receives a pre-compressed image (~200-500KB). This reduces upload time, Firebase function payload, and Gemini token cost.

**Target:** max 1024px on the longest side, JPEG quality 80%, result ~200-500KB.

### Common interface
**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/addexpense/scanner/ImageCompressor.kt`
```kotlin
expect fun compressImage(imageBytes: ByteArray, maxWidthPx: Int = 1024, quality: Int = 80): ByteArray
```

### Android actual
**File:** `composeApp/src/androidMain/kotlin/com/please/stop/app/features/addexpense/scanner/ImageCompressor.android.kt`
```kotlin
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

actual fun compressImage(imageBytes: ByteArray, maxWidthPx: Int, quality: Int): ByteArray {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    val originalWidth = options.outWidth
    val originalHeight = options.outHeight

    // Calculate sample size for initial downscaling (power of 2, fast)
    options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxWidthPx)
    options.inJustDecodeBounds = false

    val sampledBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        ?: return imageBytes

    // Fine-scale to exact target if still too large
    val scaledBitmap = if (sampledBitmap.width > maxWidthPx || sampledBitmap.height > maxWidthPx) {
        val scale = maxWidthPx.toFloat() / maxOf(sampledBitmap.width, sampledBitmap.height)
        val targetWidth = (sampledBitmap.width * scale).toInt()
        val targetHeight = (sampledBitmap.height * scale).toInt()
        Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true).also {
            if (it != sampledBitmap) sampledBitmap.recycle()
        }
    } else {
        sampledBitmap
    }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    scaledBitmap.recycle()

    return outputStream.toByteArray()
}

private fun calculateInSampleSize(width: Int, height: Int, maxPx: Int): Int {
    var inSampleSize = 1
    val longestSide = maxOf(width, height)
    if (longestSide > maxPx) {
        val halfLongest = longestSide / 2
        while (halfLongest / inSampleSize >= maxPx) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
```

### iOS actual
**File:** `composeApp/src/iosMain/kotlin/com/please/stop/app/features/addexpense/scanner/ImageCompressor.ios.kt`
```kotlin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual fun compressImage(imageBytes: ByteArray, maxWidthPx: Int, quality: Int): ByteArray {
    val nsData = imageBytes.toNSData()
    val image = UIImage.imageWithData(nsData) ?: return imageBytes

    val originalWidth = image.size.width
    val originalHeight = image.size.height
    val longestSide = maxOf(originalWidth, originalHeight)

    // Calculate target size preserving aspect ratio
    val scale = if (longestSide > maxWidthPx.toDouble()) {
        maxWidthPx.toDouble() / longestSide
    } else {
        1.0
    }
    val targetWidth = originalWidth * scale
    val targetHeight = originalHeight * scale

    // Resize
    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(targetWidth, targetHeight),
        true,  // opaque — no alpha channel needed for JPEG
        1.0,   // scale factor 1.0 — we already calculated pixel dimensions
    )
    image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (resizedImage == null) return imageBytes

    // Compress to JPEG
    val qualityFloat = quality.toDouble() / 100.0
    val jpegData = UIImageJPEGRepresentation(resizedImage, qualityFloat)
        ?: return imageBytes

    return jpegData.toByteArray()
}

// Helper: ByteArray → NSData
private fun ByteArray.toNSData(): NSData {
    return NSData.create(bytes = this.refTo(0), length = this.size.toULong())
}

// Helper: NSData → ByteArray
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(this.length.toInt())
    this.getBytes(bytes.refTo(0), this.length)
    return bytes
}
```

### Size comparison

| Input | After compression |
|-------|------------------|
| 3MB (3024x4032 raw photo) | ~300KB (1024x1365) |
| 5MB (4000x6000 hi-res) | ~400KB (683x1024) |
| 1MB (1200x1600 mid-res) | ~250KB (768x1024) |

### Where it's called

Inside each platform's `DocumentScanner.scan()`, as the last step before returning:

```kotlin
// Inside DocumentScanner.scan()
val rawBytes = ... // from ML Kit or VisionKit
val compressed = compressImage(rawBytes, maxWidthPx = 1024, quality = 80)
return Result.success(compressed)
```

---

## Step 11: Camera Permission (iOS only)

ML Kit Document Scanner on Android **handles camera permission internally** — no extra work needed.

iOS VisionKit requires camera permission **before** presenting the scanner.

**File:** `composeApp/src/iosMain/kotlin/com/please/stop/app/features/addexpense/scanner/DocumentScanner.ios.kt`

Use moko-permissions (already in the project) to request camera access before launching `VNDocumentCameraViewController`:

```kotlin
// Inside DocumentScanner.scan() on iOS
val permissionState = permissionsController.getPermissionState(Permission.CAMERA)
if (permissionState != PermissionState.Granted) {
    permissionsController.providePermission(Permission.CAMERA)
}
// then launch VNDocumentCameraViewController
```

If denied → return `Result.failure(CameraPermissionDeniedException)` → UI shows "Camera access required. Enable in Settings."

**Info.plist** — ensure `NSCameraUsageDescription` is set (likely already present).

---

## Step 12: Client-Side Timeout

Firebase callable has no built-in client timeout. Wrap the call with `withTimeout`:

**In `ReceiptRepositoryImpl`:**
```kotlin
import kotlinx.coroutines.withTimeout

suspend fun analyzeReceipt(...): Result<ReceiptData> = runCatching {
    withTimeout(30_000L) { // 30 seconds — covers upload + Gemini processing
        callableFunctions.call("analyzeReceipt", data)
    }
    // ... parse response
}
```

If timeout → `kotlinx.coroutines.TimeoutCancellationException` → caught by `runCatching` → mapped to `ReceiptError.SERVICE_UNAVAILABLE` in use case.

---

## Step 13: Currency Mismatch Handling

The receipt currency (from Gemini) may differ from the user's app currency.

**MVP approach:** Ignore currency mismatch — just fill the amount as-is. The user reviews all fields before saving anyway.

**Data flow:**
- Gemini returns `currency: "EUR"` and `totalAmount: 42.99`
- `ReceiptRepositoryImpl` converts `42.99` → `4299` minor units using the user's `decimalPlaces`
- If user's currency is USD but receipt is EUR, the amount `42.99` is still filled — user sees it and can adjust

**Future enhancement (not MVP):** Show a warning badge on the amount field: "Receipt is in EUR, your currency is USD."

---

## Step 14: Base64 Encoding (commonMain)

Use Kotlin's built-in `kotlin.io.encoding.Base64` (available since Kotlin 1.8, stable in 2.x):

**In `ReceiptRepositoryImpl`:**
```kotlin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
val imageBase64 = Base64.encode(imageBytes)
```

No extra dependency needed — this is Kotlin stdlib, works on all KMP targets.

---

## Implementation Order

| # | What | Can test with |
|---|------|--------------|
| 1 | Firebase Cloud Function (with JSON mode, timeout, memory config) + deploy | `curl` / Firebase console test |
| 2 | Dependencies (gradle + SPM + cinterop) | Project builds |
| 3 | App Check init (Android + iOS, debug provider for dev) | Firebase console shows attestations |
| 4 | `FirebaseCallableFunctions` expect/actual | Call test function from app |
| 5 | `DocumentScanner` expect/actual + `ImageCompressor` + iOS camera permission | Launch scanner, get bytes |
| 6 | `ReceiptData` + `ReceiptRepository` (with base64, timeout, currency handling) + exceptions | Unit test with mock callable |
| 7 | `AnalyzeReceiptUseCase` + `AddExpenseRepository.getFormData()` | Unit test |
| 8 | DI wiring | App compiles and resolves all deps |
| 9 | Presentation (state, events, StateHolder) | Scan receipt → form pre-filled |
| 10 | UI (button, loading, error messages) | Full E2E flow |

## Verification

1. **Cloud Function:** Deploy, call with curl + sample base64 image, verify JSON response
2. **App Check:** Call function without App Check → 401. Call from app → success
3. **Scanner:** Launch on both platforms, get cropped document image
4. **E2E:** Scan receipt → loading state → form pre-filled with merchant, amount, date, category
5. **Error cases:** Blurry image → "Couldn't read" message. Airplane mode → "No internet" message
6. **Partial:** Receipt with missing date → other fields filled, date left as-is
