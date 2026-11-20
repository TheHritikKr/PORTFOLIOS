package com.skfashion.app.data

import androidx.compose.ui.graphics.Color

/** A sellable item. imageUrl is optional: when null the UI draws an emoji tile; set it to show a real photo via Coil. */
data class Product(val id: Int, val name: String, val section: String, val price: Int, val mrp: Int, val rating: Double,
                   val reviews: Int, val emoji: String, val tint: Color, val sizes: List<String>, val desc: String,
                   val imageUrl: String? = null) { val off get() = 100 - price * 100 / mrp }
data class CartLine(val productId: Int, val size: String, val qty: Int)
data class Order(val id: String, val dateMs: Long, val total: Int, val pay: String, val city: String, val summary: String, val status: String)
data class User(val id: String, val name: String, val email: String)
data class Totals(val sub: Int, val disc: Int, val ship: Int) { val total get() = sub - disc + ship }

const val FREE_SHIPPING_FROM = 999
val SECTIONS = listOf("All", "Men", "Women", "Kids", "GenZ", "Baby", "Ethnic")
val COUPONS = mapOf("SK10" to 10, "SKNEW" to 15)   // code -> percent off
val CITIES = mapOf("Delhi" to (28.61 to 77.21), "Mumbai" to (19.08 to 72.88), "Bengaluru" to (12.97 to 77.59), "Kolkata" to (22.57 to 88.36),
    "Chennai" to (13.08 to 80.27), "Hyderabad" to (17.39 to 78.48), "Patna" to (25.59 to 85.14), "Lucknow" to (26.85 to 80.95))

private val ADULT = "S M L XL XXL".split(" "); private val KIDS = "2-3Y 4-5Y 6-7Y 8-9Y".split(" "); private val BABY = "0-3M 3-6M 6-12M".split(" ")
/** Sample catalogue so the app runs immediately. Replace with your API later (see README). */
val CATALOG = listOf(
    Product(1, "Oxford Cotton Shirt", "Men", 1299, 2199, 4.5, 3120, "👔", Color(0xFF8FB8E8), ADULT, "Breathable cotton oxford, tailored fit."),
    Product(2, "Slim Fit Chinos", "Men", 1499, 2499, 4.3, 2210, "👖", Color(0xFFD8C3A5), ADULT, "Stretch chinos for office or weekend."),
    Product(3, "Floral Wrap Dress", "Women", 1899, 3299, 4.6, 1870, "👗", Color(0xFFF2A7C3), ADULT, "Flowy wrap dress with a waist tie."),
    Product(4, "Cozy Knit Hoodie", "Women", 1599, 2699, 4.7, 1760, "🧥", Color(0xFFE6D5BF), ADULT, "Brushed-fleece hoodie, kangaroo pocket."),
    Product(5, "Dino Print Tee", "Kids", 499, 899, 4.5, 1450, "👕", Color(0xFF52BDB4), KIDS, "Playful print on skin-friendly cotton."),
    Product(6, "Party Twirl Dress", "Kids", 1199, 1999, 4.6, 640, "👗", Color(0xFFB196EC), KIDS, "Twirl-ready dress with soft lining."),
    Product(7, "Oversized Street Hoodie", "GenZ", 1799, 2999, 4.6, 4120, "🧥", Color(0xFF55556A), ADULT, "Heavy oversized hoodie, dropped shoulder."),
    Product(8, "Baggy Cargo Pants", "GenZ", 1999, 3299, 4.4, 2870, "👖", Color(0xFF91A362), ADULT, "Relaxed cargos with utility pockets."),
    Product(9, "Soft Cotton Onesie", "Baby", 399, 699, 4.8, 2380, "🍼", Color(0xFFB0D8F8), BABY, "Gentle organic cotton, snap closure."),
    Product(10, "Snuggle Sleepsuit", "Baby", 549, 899, 4.7, 1540, "🧸", Color(0xFFF9B0CC), BABY, "Cosy sleepsuit with fold-over mittens."),
    Product(11, "Embroidered Cotton Kurta", "Ethnic", 1599, 2699, 4.6, 1760, "👘", Color(0xFF52BDB4), ADULT, "Straight kurta with neckline embroidery."),
    Product(12, "Festive Anarkali Kurta", "Ethnic", 2299, 3999, 4.7, 830, "👘", Color(0xFFEC6E82), ADULT, "Flared festive kurta with trim detail."))
fun product(id: Int) = CATALOG.first { it.id == id }
