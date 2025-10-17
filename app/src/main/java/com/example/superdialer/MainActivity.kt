package com.example.superdialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.superdialer.ui.theme.SuperDialerTheme
import com.example.superdialer.ui.theme.DialerScreen
import com.example.superdialer.ui.theme.ProfileWithHistoryScreen

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SuperDialerTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "dialer"
                ) {
                    composable("dialer") {
                        DialerScreen(
                            navController = navController,
                            onProfileClick = { number ->
                                navController.navigate("profileWithHistory/$number")
                            }
                        )
                    }

                    composable("profileWithHistory/{number}") { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        ProfileWithHistoryScreen(
                            number = number,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
