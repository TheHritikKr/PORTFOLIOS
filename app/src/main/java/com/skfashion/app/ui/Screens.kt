@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.skfashion.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.skfashion.app.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private fun rs(n: Int) = "₹" + "%,d".format(Locale("en", "IN"), n)

// ───────────── shared building blocks ─────────────
@Composable fun Art(p: Product, m: Modifier = Modifier) = Box(m.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(p.tint.copy(.35f), p.tint))), Alignment.Center) {
    if (p.imageUrl != null) AsyncImage(p.imageUrl, p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Text(p.emoji, fontSize = 56.sp) }

@Composable fun Bar(title: String, nav: NavController, actions: @Composable RowScope.() -> Unit = {}) = TopAppBar(
    title = { Text(title, fontWeight = FontWeight.Bold) }, actions = actions,
    navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } })

@Composable fun Empty(icon: String, title: String, sub: String) = Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
    Text(icon, fontSize = 48.sp); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable fun Tile(p: Product, vm: ShopViewModel, nav: NavController) = Column(Modifier.clickable { nav.navigate("pd/${p.id}") }) {
    Box { Art(p, Modifier.fillMaxWidth().aspectRatio(1f))
        if (p.off >= 40) Text("${p.off}% OFF", Modifier.padding(8.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.tertiary).padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.Black)
        IconButton({ vm.toggleWish(p.id) }, Modifier.align(Alignment.TopEnd)) { Icon(if (p.id in vm.wish) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Wishlist", tint = MaterialTheme.colorScheme.primary) } }
    Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 6.dp)); Text("★ ${p.rating} (${p.reviews})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(verticalAlignment = Alignment.CenterVertically) { Text(rs(p.price), fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(6.dp))
        Text(rs(p.mrp), fontSize = 12.sp, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable fun Grid(list: List<Product>, vm: ShopViewModel, nav: NavController, m: Modifier = Modifier) = LazyVerticalGrid(GridCells.Fixed(2), m, contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 96.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { items(list) { Tile(it, vm, nav) } }

@Composable fun Field(v: String, on: (String) -> Unit, label: String, secret: Boolean = false, kb: KeyboardType = KeyboardType.Text) = OutlinedTextField(v, on, Modifier.fillMaxWidth(), label = { Text(label) },
    singleLine = true, visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else kb))
@Composable fun Err(s: String?) { if (s != null) Text(s, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp)) }

// ───────────── onboarding + auth ─────────────
@Composable fun Intro(vm: ShopViewModel, nav: NavController) = Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background))).padding(28.dp), Arrangement.Bottom) {
    Text("SK", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White)
    Text("Wear your story.\nEvery age, every size.", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp, color = androidx.compose.ui.graphics.Color.White)
    Text("Men, women, kids, GenZ, baby and ethnic wear delivered to your door.", Modifier.padding(vertical = 12.dp), color = androidx.compose.ui.graphics.Color.White.copy(.85f))
    Button({ vm.finishIntro(); nav.navigate("auth") { popUpTo("intro") { inclusive = true } } }, Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White, contentColor = androidx.compose.ui.graphics.Color.Black)) { Text("Get started", fontWeight = FontWeight.Bold) } }

@Composable fun Auth(vm: ShopViewModel, nav: NavController, next: String?) { var up by remember { mutableStateOf(false) }; var n by remember { mutableStateOf("") }; var e by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    fun done() { if (next != null) { nav.popBackStack(); nav.navigate(next) } else nav.navigate("home") { popUpTo(0) { inclusive = true } } }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(24.dp)); Text(if (up) "Create your account" else "Welcome back", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (up) "Save your wishlist and track orders." else "Sign in to checkout and track orders.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf("Sign in", "Sign up").forEachIndexed { i, t -> SegmentedButton((i == 1) == up, { up = i == 1; err = null }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(t) } } }
        if (up) Field(n, { n = it }, "Full name"); Field(e, { e = it }, "Email", kb = KeyboardType.Email); Field(p, { p = it }, "Password (8+ with a letter and number)", true); Err(err)
        Button({ err = if (up) vm.signUp(n, e, p.toCharArray()) else vm.signIn(e, p.toCharArray()); if (err == null) done() }, Modifier.fillMaxWidth().height(52.dp)) { Text(if (up) "Create account" else "Sign in") }
        TextButton({ done() }, Modifier.fillMaxWidth()) { Text("Continue as guest") } } }

// ───────────── home: NEW LAYOUT = left category rail + product grid + floating bag button ─────────────
@Composable fun Home(vm: ShopViewModel, nav: NavController) { var sec by remember { mutableStateOf("All") }; var loading by remember { mutableStateOf(true) }; var pick by remember { mutableStateOf(false) }; val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) vm.detectCity(ctx) }
    LaunchedEffect(Unit) { delay(500); loading = false; if (vm.city == "Select location") perm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
    val list = CATALOG.filter { sec == "All" || it.section == sec }
    Scaffold(topBar = { TopAppBar(title = { Column { Text("SK Fashion", fontWeight = FontWeight.ExtraBold); Text("📍 ${vm.city}", fontSize = 12.sp, modifier = Modifier.clickable { pick = true }) } },
        actions = { IconButton({ nav.navigate("search") }) { Icon(Icons.Default.Search, "Search") }; IconButton({ nav.navigate("wish") }) { BadgedBox({ if (vm.wish.isNotEmpty()) Badge { Text("${vm.wish.size}") } }) { Icon(Icons.Default.FavoriteBorder, "Wishlist") } }
            IconButton({ nav.navigate("profile") }) { Icon(Icons.Default.Person, "Profile") } }) },
        floatingActionButton = { ExtendedFloatingActionButton({ nav.navigate("cart") }, icon = { Icon(Icons.Default.ShoppingBag, null) }, text = { Text("Bag (${vm.cart.sumOf { it.qty }})") }) }) { pad ->
        Row(Modifier.padding(pad)) {
            LazyColumn(Modifier.width(78.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant), contentPadding = PaddingValues(vertical = 8.dp)) { items(SECTIONS) { s ->
                Text(s, Modifier.fillMaxWidth().clickable { sec = s }.background(if (s == sec) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent).padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (s == sec) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) } }
            when { loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                list.isEmpty() -> Empty("👗", "Nothing here yet", "New styles are coming soon.")
                else -> Grid(list, vm, nav) } } }
    if (pick) AlertDialog({ pick = false }, confirmButton = { TextButton({ pick = false }) { Text("Close") } }, title = { Text("Delivery location") },
        text = { Column { TextButton({ pick = false; perm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Text("📍 Use my current location") }
            CITIES.keys.forEach { c -> TextButton({ vm.setCity(c); pick = false }) { Text(c) } } } }) }

@Composable fun Search(vm: ShopViewModel, nav: NavController) { var q by remember { mutableStateOf("") }; val r = if (q.isBlank()) emptyList() else CATALOG.filter { (it.name + it.section).contains(q.trim(), true) }
    Scaffold(topBar = { Bar("Search", nav) }) { pad -> Column(Modifier.padding(pad).padding(12.dp)) { Field(q, { q = it.take(60) }, "Search shirts, kurtas, hoodies")
        when { q.isBlank() -> Empty("🔍", "Find your style", "Try “hoodie”, “kurta” or “dress”."); r.isEmpty() -> Empty("😕", "No results", "Check the spelling or try another word."); else -> Grid(r, vm, nav) } } } }

// ───────────── product, cart, checkout ─────────────
@Composable fun Detail(vm: ShopViewModel, nav: NavController, id: Int) { val p = product(id); var size by remember { mutableStateOf("") }; var q by remember { mutableIntStateOf(1) }; var err by remember { mutableStateOf<String?>(null) }; var pin by remember { mutableStateOf("") }; var eta by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { Bar("", nav) { IconButton({ vm.toggleWish(id) }) { Icon(if (id in vm.wish) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Wishlist", tint = MaterialTheme.colorScheme.primary) } } }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(10.dp)) {
            Art(p, Modifier.fillMaxWidth().height(300.dp)); Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("★ ${p.rating} · ${p.reviews} reviews", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) { Text(rs(p.price), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(8.dp)); Text(rs(p.mrp), textDecoration = TextDecoration.LineThrough); Spacer(Modifier.width(8.dp)); Text("${p.off}% off", color = Status.success, fontWeight = FontWeight.Bold) }
            Text(p.desc); Text("Select size", fontWeight = FontWeight.Bold); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { p.sizes.forEach { s -> FilterChip(size == s, { size = s; err = null }, { Text(s) }) } }; Err(err)
            Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ q = (q - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, "Less") }; Text("$q", fontWeight = FontWeight.Bold); IconButton({ q = (q + 1).coerceAtMost(10) }) { Icon(Icons.Default.Add, "More") } }
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, Modifier.weight(1f), label = { Text("Pincode") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextButton({ eta = if (pin.length == 6) "Delivery by " + SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(System.currentTimeMillis() + 4 * 86400000L)) else "Enter a valid 6-digit pincode." }) { Text("Check") } }; eta?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton({ if (size.isEmpty()) err = "Please select a size." else { vm.add(id, size, q); nav.navigate("cart") } }, Modifier.weight(1f).height(52.dp)) { Text("Add to bag") }
                Button({ if (size.isEmpty()) err = "Please select a size." else { vm.add(id, size, q); nav.navigate(if (vm.user == null) "auth?next=checkout" else "checkout") } }, Modifier.weight(1f).height(52.dp)) { Text("Buy now") } } } } }

@Composable fun Cart(vm: ShopViewModel, nav: NavController) { var code by remember { mutableStateOf("") }; var msg by remember { mutableStateOf<String?>(null) }; val t = vm.totals
    Scaffold(topBar = { Bar("Bag (${vm.cart.sumOf { it.qty }})", nav) }) { pad -> if (vm.cart.isEmpty()) Empty("🛍️", "Your bag is empty", "Pick something you love.") else
        LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(if (t.ship > 0) "Add ${rs(FREE_SHIPPING_FROM - (t.sub - t.disc))} more for free delivery" else "✔ You get free delivery", color = Status.success, fontWeight = FontWeight.Bold) }
            items(vm.cart.toList()) { l -> val p = product(l.productId); Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Art(p, Modifier.size(84.dp)); Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("Size ${l.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(rs(p.price), fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ vm.changeQty(l, -1) }) { Icon(if (l.qty == 1) Icons.Default.Delete else Icons.Default.Remove, "Less") }; Text("${l.qty}"); IconButton({ vm.changeQty(l, 1) }) { Icon(Icons.Default.Add, "More") } } } } }
            item { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(code, { code = it.take(12) }, Modifier.weight(1f), label = { Text("Promo code (try SK10)") }, singleLine = true); TextButton({ msg = if (vm.applyCoupon(code)) "Promo applied" else "Invalid promo code" }) { Text("Apply") } }; Err(msg.takeIf { it == "Invalid promo code" }) }
            item { Column { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Subtotal"); Text(rs(t.sub)) }; Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Discount"); Text("−" + rs(t.disc)) }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Delivery"); Text(if (t.ship == 0) "Free" else rs(t.ship)) }; Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Text(rs(t.total), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) } } }
            item { Button({ nav.navigate(if (vm.user == null) "auth?next=checkout" else "checkout") }, Modifier.fillMaxWidth().height(54.dp)) { Text("Checkout · ${rs(t.total)}") } } } } }

@Composable fun Checkout(vm: ShopViewModel, nav: NavController) { var n by remember { mutableStateOf(vm.user?.name ?: "") }; var ph by remember { mutableStateOf("") }; var a by remember { mutableStateOf("") }; var c by remember { mutableStateOf(vm.city.takeIf { it in CITIES } ?: "") }; var pin by remember { mutableStateOf("") }; var pay by remember { mutableStateOf("UPI") }; var err by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { Bar("Checkout", nav) }) { pad -> Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
        Text("Delivery address", fontWeight = FontWeight.Bold); Field(n, { n = it }, "Full name"); Field(ph, { ph = it.filter(Char::isDigit).take(10) }, "Mobile number", kb = KeyboardType.Phone); Field(a, { a = it }, "House no., street, area"); Field(c, { c = it }, "City"); Field(pin, { pin = it.filter(Char::isDigit).take(6) }, "Pincode", kb = KeyboardType.Number)
        Text("Payment", fontWeight = FontWeight.Bold); listOf("UPI", "Credit / Debit card", "Net banking", "Cash on delivery").forEach { m -> Row(Modifier.fillMaxWidth().clickable { pay = m }, verticalAlignment = Alignment.CenterVertically) { RadioButton(pay == m, { pay = m }); Text(m) } }
        Text("Payments are simulated in this demo. Plug in a gateway SDK (e.g. Razorpay) and verify payment on your server.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Err(err)
        Button({ err = vm.placeOrder(n, ph, a, c, pin, pay); if (err == null) nav.navigate("done") { popUpTo("home") } }, Modifier.fillMaxWidth().height(54.dp)) { Text("Place order · ${rs(vm.totals.total)}") } } } }

@Composable fun Done(vm: ShopViewModel, nav: NavController) = Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
    Box(Modifier.size(88.dp).clip(CircleShape).background(Status.success), Alignment.Center) { Icon(Icons.Default.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(44.dp)) }
    Text("Order placed!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp)); Text("Order ${vm.lastOrderId} is confirmed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button({ nav.navigate("orders") { popUpTo("home") } }, Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Track my order") }; TextButton({ nav.navigate("home") { popUpTo(0) } }) { Text("Continue shopping") } }

// ───────────── orders, wishlist, profile ─────────────
@Composable fun Orders(vm: ShopViewModel, nav: NavController) = Scaffold(topBar = { Bar("My orders", nav) }) { pad -> if (vm.orders.isEmpty()) Empty("📦", "No orders yet", "Your orders will appear here.") else
    LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(vm.orders.toList()) { o -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(o.id, fontWeight = FontWeight.Bold); Text(o.status, fontWeight = FontWeight.ExtraBold, color = if (o.status == "Cancelled") MaterialTheme.colorScheme.error else Status.success) }
        Text("${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(o.dateMs))} · ${rs(o.total)} · ${o.pay} · ${o.city}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(o.summary)
        if (o.status == "Confirmed") { LinearProgressIndicator({ .25f }, Modifier.fillMaxWidth()); Text("Confirmed → Packed → Shipped → Delivered", fontSize = 11.sp) }
        if (o.status == "Confirmed") OutlinedButton({ vm.cancel(o) }) { Text("Cancel order") } } } } }

@Composable fun Wish(vm: ShopViewModel, nav: NavController) = Scaffold(topBar = { Bar("Saved items", nav) }) { pad -> if (vm.wish.isEmpty()) Empty("♡", "Nothing saved yet", "Tap the heart on any style to keep it here.") else Grid(vm.wish.map { product(it) }, vm, nav, Modifier.padding(pad)) }

@Composable fun Profile(vm: ShopViewModel, nav: NavController) { var del by remember { mutableStateOf(false) }; var pw by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }; val u = vm.user
    Scaffold(topBar = { Bar("Profile", nav) }) { pad -> Column(Modifier.padding(pad).padding(16.dp), Arrangement.spacedBy(10.dp)) {
        if (u == null) { Text("Hi, guest", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Sign in to track orders and sync your wishlist."); Button({ nav.navigate("auth") }, Modifier.fillMaxWidth()) { Text("Sign in or create account") } }
        else { Text(u.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text(u.email, color = MaterialTheme.colorScheme.onSurfaceVariant); Button({ nav.navigate("orders") }, Modifier.fillMaxWidth()) { Text("My orders") }
            OutlinedButton({ vm.signOut(); nav.navigate("home") { popUpTo(0) } }, Modifier.fillMaxWidth()) { Text("Sign out") }
            Button({ del = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete account") } } } }
    if (del) AlertDialog({ del = false }, title = { Text("Delete your account?") }, text = { Column { Text("This permanently erases your account, wishlist and orders from this device."); Field(pw, { pw = it }, "Enter password to confirm", true); Err(err) } },
        confirmButton = { TextButton({ err = vm.deleteAccount(pw.toCharArray()); if (err == null) { del = false; nav.navigate("home") { popUpTo(0) } } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ del = false }) { Text("Cancel") } }) }
