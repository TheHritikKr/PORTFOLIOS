package com.skfashion.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.skfashion.app.data.ShopViewModel
import com.skfashion.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) // blocks screenshots / recent-apps previews
        setContent { SkTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val vm: ShopViewModel = viewModel(); val nav = rememberNavController()
            NavHost(nav, if (vm.seenIntro) "home" else "intro") {
                composable("intro") { Intro(vm, nav) }
                composable("auth?next={next}", listOf(navArgument("next") { type = NavType.StringType; nullable = true; defaultValue = null })) { Auth(vm, nav, it.arguments?.getString("next")) }
                composable("home") { Home(vm, nav) }; composable("search") { Search(vm, nav) }
                composable("pd/{id}", listOf(navArgument("id") { type = NavType.IntType })) { Detail(vm, nav, it.arguments!!.getInt("id")) }
                composable("cart") { Cart(vm, nav) }; composable("checkout") { Checkout(vm, nav) }; composable("done") { Done(vm, nav) }
                composable("orders") { Orders(vm, nav) }; composable("wish") { Wish(vm, nav) }; composable("profile") { Profile(vm, nav) }
            } } } }
    }
}
