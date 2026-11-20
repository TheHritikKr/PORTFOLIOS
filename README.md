# SK Fashion – Android app (Kotlin + Jetpack Compose)

An original clothing-store app: Men, Women, Kids, GenZ, Baby and Ethnic. It is **not** a copy of any other company's app,
code or branding. The feature list follows what a modern fashion shopping app needs.

> **Status:** written but **not compiled or run** in the environment that produced it (no Android SDK there).
> Open it in Android Studio; if Gradle or a dependency version complains, update the version number and sync.

## 1. What the app does
**Roles:** Guest (browse, search, bag, wishlist) and Customer (all of that + checkout, orders, profile).

| Screen | What you can do |
|---|---|
| Intro | Brand splash and "Get started" (shown once) |
| Sign in / Sign up | Create account, sign in, or continue as guest; sign-in is requested only at checkout |
| Home | Left category rail, product grid, search, wishlist, profile, floating Bag button, auto-detected delivery city |
| Search | Live results, empty state and "no results" state |
| Product | Size selection (required), quantity, wishlist, pincode delivery estimate, Add to bag, Buy now |
| Bag | Quantity +/−/remove, free-delivery progress (₹999), promo codes `SK10` (10%) and `SKNEW` (15%), totals |
| Checkout | Validated address form, payment choice (simulated), place order |
| Order done / Orders | Confirmation, order list with status, cancel while "Confirmed" |
| Wishlist | Saved items |
| Profile | Sign out, **delete account** (password required) |

States covered: loading (Home), empty (bag, wishlist, orders, search), error (form and sign-in messages), success (order placed, toasts via screens).

## 2. UI, colours and layout
Kept: rounded cards, pill buttons, bold product titles, price with struck-through MRP and % off, size chips, quantity steppers, status colours.
**New layout:** no bottom tab bar. Home uses a **vertical category rail on the left**, actions (search, wishlist, profile) sit in the top bar, and the Bag is a **floating button**.

| Token | Light | Dark |
|---|---|---|
| Primary | `#4F46E5` | `#8B83FF` |
| Secondary | `#0EA5A4` | `#2DD4BF` |
| Accent | `#F59E0B` | `#FBBF24` |
| Background | `#F7F7FB` | `#0E0F1A` |
| Surface | `#FFFFFF` | `#181A2B` |
| Text | `#14142B` | `#F1F1F8` |
| Success / Warning / Error / Info | `#16A34A` / `#F59E0B` / `#DC2626` / `#2563EB` | `#16A34A` / `#FBBF24` / `#F87171` / `#2563EB` |

## 3. Security
| Control | Where | How |
|---|---|---|
| Password storage | `Security.kt` `PasswordHasher` | Random salt + PBKDF2-HMAC-SHA256, 210,000 rounds; constant-time compare |
| Encrypted storage | `SecureStore` | EncryptedSharedPreferences (AES-256-GCM, Android Keystore keys) for accounts, sessions, cart, wishlist, orders |
| Sessions | `AuthRepository` | Random 256-bit token, 7-day expiry, checked on every launch |
| Brute force | `AuthRepository.login` | 5 failed attempts lock that email for 5 minutes; dummy hash for unknown emails keeps timing equal |
| Account deletion | `AuthRepository.delete` | Password re-check, then removes the account and its cart, wishlist and orders |
| Input validation | `Validators` | Email, password strength, 10-digit mobile, 6-digit pincode, length caps, control characters stripped |
| App hardening | Manifest + `MainActivity` | `allowBackup=false`, backup/extraction rules exclude data, HTTPS only, only the launcher activity exported, `FLAG_SECURE` blocks screenshots |
| Permissions | Manifest | Only coarse location (optional, asked at runtime) and internet (for product photos) |

## 4. Get an installable APK
This zip is **source code**, so it is small. An APK is built from it.
- **Android Studio:** open the folder, wait for sync, then *Build > Build Bundle(s) / APK(s) > Build APK(s)*. The file is `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone and install (allow "install unknown apps").
- **No Android Studio:** put this folder on GitHub and run the *Build APK* workflow (Actions tab). Download the APK from the run's Artifacts.
- A Play Store release needs a signed release build with your own keystore.

## 5. Run it
1. Install Android Studio (Koala or newer) and open this folder. Let Gradle sync (it creates the wrapper).
2. Run on an emulator or phone (Android 8.0 / API 26+). Sample products are built in, so it works immediately.
3. Test flow: Get started → Sign up → add a product (pick a size) → Bag → apply `SK10` → Checkout → Orders.

**Files:** `data/Models.kt` (products, sample catalogue) · `data/ShopViewModel.kt` (state and logic) · `security/Security.kt` · `ui/Theme.kt` · `ui/Screens.kt` · `MainActivity.kt` (navigation).

## 6. Assumptions and what is NOT included (nothing silently dropped)
- **No backend.** Accounts, orders and the catalogue are local sample data. Real security needs a server: move hashing, sessions and orders there and keep the same repository functions as the app-side interface. Add certificate pinning (OkHttp `CertificatePinner`) when you have a domain.
- **Payments are simulated.** Add a gateway SDK and verify payment server-side before marking an order paid.
- **Photos:** products show emoji tiles. Set `imageUrl` on a product and Coil loads it.
- **Not built:** OTP / social login, push notifications, returns flow, address book, reviews submission, virtual try-on, biometric unlock, unit and UI tests, launcher icon and a Gradle wrapper.
- Location uses the last known network fix mapped to the nearest of 8 built-in cities; it cannot give a street address.
- Passwords are never logged or stored in plain text, but a rooted device could still read data from RAM while the app runs.
