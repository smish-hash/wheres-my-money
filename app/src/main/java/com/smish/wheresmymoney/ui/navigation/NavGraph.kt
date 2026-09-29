package com.smish.wheresmymoney.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.addexpense.AddExpenseScreen
import com.smish.wheresmymoney.ui.categories.CategoriesScreen
import com.smish.wheresmymoney.ui.categoryhistory.CategoryHistoryScreen
import com.smish.wheresmymoney.ui.home.HomeScreen
import com.smish.wheresmymoney.ui.onboarding.OnboardingNameScreen
import com.smish.wheresmymoney.ui.onboarding.OnboardingSyncScreen
import com.smish.wheresmymoney.ui.settings.SettingsScreen
import java.time.YearMonth

@Composable
fun NavGraph(
    container: AppContainer,
    startWithOnboarding: Boolean,
    intent: Intent? = null,
    onIntentConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(intent, startWithOnboarding) {
        if (!startWithOnboarding && intent?.getStringExtra("navigate_to") == "add_expense") {
            navController.navigate(Screen.AddExpense.createRoute())
            onIntentConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (startWithOnboarding) Screen.OnboardingName.route else Screen.Home.route
    ) {
        composable(Screen.OnboardingName.route) {
            OnboardingNameScreen(
                container = container,
                onContinue = { navController.navigate(Screen.OnboardingSync.route) }
            )
        }

        composable(Screen.OnboardingSync.route) {
            OnboardingSyncScreen(
                container = container,
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.OnboardingName.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                container = container,
                onAddExpense = { categoryId ->
                    navController.navigate(Screen.AddExpense.createRoute(categoryId = categoryId))
                },
                onManageCategories = { navController.navigate(Screen.Categories.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onCategoryClick = { catId, ym ->
                    navController.navigate(Screen.CategoryHistory.createRoute(catId, ym.toString()))
                }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(container = container, onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(container = container, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")?.takeIf { it != -1L }
            val categoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            AddExpenseScreen(
                container = container,
                expenseId = expenseId,
                initialCategoryId = categoryId,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CategoryHistory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("yearMonth") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
            val yearMonth = YearMonth.parse(backStackEntry.arguments?.getString("yearMonth"))
            CategoryHistoryScreen(
                container = container,
                categoryId = categoryId,
                yearMonth = yearMonth,
                onBack = { navController.popBackStack() },
                onEditExpense = { expenseId ->
                    navController.navigate(Screen.AddExpense.createRoute(expenseId = expenseId))
                },
                onAddExpense = {
                    navController.navigate(Screen.AddExpense.createRoute(categoryId = categoryId))
                }
            )
        }
    }
}
