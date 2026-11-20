package com.skfashion.app.data

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.skfashion.app.security.*
import org.json.JSONArray
import org.json.JSONObject

/** Holds all app state (cart, wishlist, orders, session). Data is saved encrypted and kept separate per user. */
class ShopViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SecureStore(app)
    private val auth = AuthRepository(store)
    var user by mutableStateOf(auth.current()); private set
    var city by mutableStateOf(store.get("city") ?: "Select location"); private set
    var coupon by mutableStateOf<String?>(null); private set
    var seenIntro by mutableStateOf(store.get("seen") == "1"); private set
    val cart = mutableStateListOf<CartLine>(); val wish = mutableStateListOf<Int>(); val orders = mutableStateListOf<Order>()
    private val ns get() = user?.id ?: "guest"
    init { load() }

    private fun load() {
        cart.clear(); wish.clear(); orders.clear()
        store.get("cart_$ns")?.split(",")?.filter { it.isNotBlank() }?.forEach { val p = it.split(":"); cart.add(CartLine(p[0].toInt(), p[1], p[2].toInt())) }
        store.get("wish_$ns")?.split(",")?.filter { it.isNotBlank() }?.forEach { wish.add(it.toInt()) }
        JSONArray(store.get("orders_$ns") ?: "[]").let { a -> for (i in 0 until a.length()) a.getJSONObject(i).run {
            orders.add(Order(getString("id"), getLong("t"), getInt("total"), getString("pay"), getString("city"), getString("sum"), getString("st"))) } }
    }
    private fun save() {
        store.put("cart_$ns", cart.joinToString(",") { "${it.productId}:${it.size}:${it.qty}" }); store.put("wish_$ns", wish.joinToString(","))
        store.put("orders_$ns", JSONArray().apply { orders.forEach { put(JSONObject().put("id", it.id).put("t", it.dateMs).put("total", it.total)
            .put("pay", it.pay).put("city", it.city).put("sum", it.summary).put("st", it.status)) } }.toString())
    }
    fun finishIntro() { store.put("seen", "1"); seenIntro = true }

    // ---- cart, wishlist, coupon
    fun add(id: Int, size: String, qty: Int) { val i = cart.indexOfFirst { it.productId == id && it.size == size }
        if (i >= 0) cart[i] = cart[i].copy(qty = (cart[i].qty + qty).coerceAtMost(10)) else cart.add(CartLine(id, size, qty)); save() }
    fun changeQty(line: CartLine, d: Int) { val i = cart.indexOf(line); if (i < 0) return
        if (line.qty + d < 1) cart.removeAt(i) else cart[i] = line.copy(qty = (line.qty + d).coerceAtMost(10)); save() }
    fun toggleWish(id: Int) { if (!wish.remove(id)) wish.add(id); save() }
    fun applyCoupon(code: String): Boolean { val c = code.trim().uppercase(); coupon = if (COUPONS.containsKey(c)) c else null; return coupon != null }
    val totals get() = cart.sumOf { product(it.productId).price * it.qty }.let { s ->
        val d = s * (COUPONS[coupon] ?: 0) / 100; Totals(s, d, if (s == 0 || s - d >= FREE_SHIPPING_FROM) 0 else 49) }

    // ---- orders. Returns an error message, or null on success (order id is in lastOrderId).
    var lastOrderId by mutableStateOf("")
    fun placeOrder(name: String, phone: String, addr: String, cityIn: String, pin: String, pay: String): String? {
        if (user == null) return "Sign in to place your order."
        if (cart.isEmpty()) return "Your bag is empty."
        if (Validators.clean(name, 60).length < 2 || Validators.clean(addr, 120).length < 6 || Validators.clean(cityIn, 40).length < 2) return "Enter your name, full address and city."
        if (!Validators.phone(phone)) return "Enter a valid 10-digit mobile number."
        if (!Validators.pincode(pin)) return "Enter a valid 6-digit pincode."
        val t = totals; val id = "SKF" + System.currentTimeMillis().toString().takeLast(8)
        orders.add(0, Order(id, System.currentTimeMillis(), t.total, pay, Validators.clean(cityIn, 40),
            cart.joinToString("; ") { "${product(it.productId).name} (${it.size}) x${it.qty}" }, "Confirmed"))
        cart.clear(); coupon = null; lastOrderId = id; save(); return null
    }
    fun cancel(o: Order) { val i = orders.indexOf(o); if (i >= 0 && o.status == "Confirmed") { orders[i] = o.copy(status = "Cancelled"); save() } }

    // ---- accounts. Each returns an error message or null.
    fun signUp(n: String, e: String, p: CharArray) = after(auth.register(n, e, p))
    fun signIn(e: String, p: CharArray) = after(auth.login(e, p))
    private fun after(r: AuthRepository.Result): String? = when (r) {
        is AuthRepository.Result.Err -> r.msg
        is AuthRepository.Result.Ok -> { val guest = cart.toList(); user = r.user; load(); guest.forEach { add(it.productId, it.size, it.qty) }; null } }
    fun signOut() { auth.logout(); user = null; coupon = null; load() }
    fun deleteAccount(p: CharArray): String? = when (val r = auth.delete(p)) { is AuthRepository.Result.Err -> r.msg; else -> { user = null; load(); null } }

    // ---- location: nearest known city from the last network fix (needs coarse-location permission)
    fun setCity(c: String) { city = c; store.put("city", c) }
    @SuppressLint("MissingPermission")
    fun detectCity(ctx: Context) { runCatching {
        val l = (ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager).getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: return
        CITIES.minByOrNull { (_, c) -> Math.hypot(c.first - l.latitude, c.second - l.longitude) }?.key?.let { setCity(it) } } }
}
