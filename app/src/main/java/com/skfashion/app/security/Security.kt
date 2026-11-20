package com.skfashion.app.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.skfashion.app.data.User
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Input validation. Every user-typed value goes through here before it is stored or used. */
object Validators {
    private val email = Regex("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,}$")
    fun email(s: String) = email.matches(s)
    fun password(p: CharArray) = p.size in 8..64 && p.any { it.isLetter() } && p.any { it.isDigit() }
    fun phone(s: String) = Regex("^[6-9]\\d{9}$").matches(s)
    fun pincode(s: String) = Regex("^\\d{6}$").matches(s)
    /** Trims, caps length and strips control characters. */
    fun clean(s: String, max: Int) = s.trim().filter { !it.isISOControl() }.take(max)
}

/** Salted PBKDF2-HMAC-SHA256 (210k rounds). Format: pbkdf2$rounds$salt$hash. Never stores the plain password. */
object PasswordHasher {
    private const val ROUNDS = 210_000
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun derive(pw: CharArray, salt: ByteArray, rounds: Int) =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pw, salt, rounds, 256)).encoded
    fun token(bytes: Int) = b64(ByteArray(bytes).also { SecureRandom().nextBytes(it) }).replace('|', '_')
    fun hash(pw: CharArray): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "pbkdf2\$$ROUNDS\$${b64(salt)}\$${b64(derive(pw, salt, ROUNDS))}"
    }
    fun verify(pw: CharArray, stored: String): Boolean {
        val p = stored.split("$"); if (p.size != 4) return false
        val actual = derive(pw, Base64.decode(p[2], Base64.NO_WRAP), p[1].toInt())
        return MessageDigest.isEqual(actual, Base64.decode(p[3], Base64.NO_WRAP)) // constant-time compare
    }
}

/** AES-256 encrypted key/value storage, keys protected by the Android Keystore. All app data lives here. */
class SecureStore(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(ctx, "sk_secure",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    fun get(k: String): String? = prefs.getString(k, null)
    fun put(k: String, v: String?) = prefs.edit().apply { if (v == null) remove(k) else putString(k, v) }.apply()
}

/** Accounts, sessions, lockout and account deletion. Swap the storage for your backend API when you have one. */
class AuthRepository(private val store: SecureStore) {
    sealed interface Result { data class Ok(val user: User) : Result; data class Err(val msg: String) : Result }
    private val ttl = 7 * 24 * 3600 * 1000L                 // session lasts 7 days, then sign-in is required again
    private val dummy by lazy { PasswordHasher.hash("dummy-password-1".toCharArray()) } // equalises timing for unknown emails
    private fun users() = JSONArray(store.get("users") ?: "[]")
    private fun find(email: String): JSONObject? = users().let { a -> (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.getString("email") == email } }
    private fun start(u: JSONObject): Result {
        store.put("session", "${PasswordHasher.token(32)}|${u.getString("id")}|${System.currentTimeMillis() + ttl}")
        return Result.Ok(User(u.getString("id"), u.getString("name"), u.getString("email")))
    }
    fun register(name: String, email: String, pw: CharArray): Result {
        val n = Validators.clean(name, 60); val e = email.trim().lowercase()
        if (n.length < 2) return Result.Err("Enter your full name.")
        if (!Validators.email(e)) return Result.Err("Enter a valid email address.")
        if (!Validators.password(pw)) return Result.Err("Use 8–64 characters with at least one letter and one number.")
        if (find(e) != null) return Result.Err("This email already has an account. Sign in instead.")
        val u = JSONObject().put("id", PasswordHasher.token(12)).put("name", n).put("email", e).put("hash", PasswordHasher.hash(pw))
        store.put("users", users().put(u).toString()); return start(u)
    }
    fun login(email: String, pw: CharArray): Result {
        val e = email.trim().lowercase(); val left = lockedMs(e)
        if (left > 0) return Result.Err("Too many attempts. Try again in ${left / 60000 + 1} min.")
        val u = find(e); val ok = PasswordHasher.verify(pw, u?.getString("hash") ?: dummy) && u != null
        if (!ok) { fail(e); return Result.Err("Email or password is incorrect.") }
        store.put("fail_$e", null); return start(u!!)
    }
    fun current(): User? {
        val p = store.get("session")?.split("|") ?: return null
        if (p.size != 3 || p[2].toLong() < System.currentTimeMillis()) { logout(); return null }
        val u = users().let { a -> (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.getString("id") == p[1] } } ?: return null
        return User(u.getString("id"), u.getString("name"), u.getString("email"))
    }
    fun logout() = store.put("session", null)
    /** Re-checks the password, then erases the account and every piece of data tied to it. */
    fun delete(pw: CharArray): Result {
        val me = current() ?: return Result.Err("You are not signed in.")
        val u = find(me.email) ?: return Result.Err("Account not found.")
        if (!PasswordHasher.verify(pw, u.getString("hash"))) return Result.Err("Incorrect password. Account not deleted.")
        val a = users(); val keep = JSONArray(); for (i in 0 until a.length()) if (a.getJSONObject(i).getString("id") != me.id) keep.put(a.getJSONObject(i))
        store.put("users", keep.toString()); listOf("cart_", "wish_", "orders_").forEach { store.put(it + me.id, null) }; logout(); return Result.Ok(me)
    }
    private fun lockedMs(e: String) = (store.get("fail_$e")?.split("|")?.getOrNull(1)?.toLong() ?: 0L) - System.currentTimeMillis()
    private fun fail(e: String) { val c = (store.get("fail_$e")?.split("|")?.get(0)?.toInt() ?: 0) + 1   // 5 failures = 5 minute lock
        store.put("fail_$e", if (c >= 5) "0|${System.currentTimeMillis() + 5 * 60000}" else "$c|0") }
}
