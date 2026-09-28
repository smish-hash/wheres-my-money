package com.smish.wheresmymoney.ui.navigation

sealed class Screen(val route: String) {
    object OnboardingName : Screen("onboarding_name")
    object OnboardingSync : Screen("onboarding_sync")
    object Home : Screen("home")
    object Categories : Screen("categories")
    object Settings : Screen("settings")

    object AddExpense : Screen("add_expense?expenseId={expenseId}&categoryId={categoryId}") {
        fun createRoute(expenseId: Long? = null, categoryId: Long? = null) =
            "add_expense?expenseId=${expenseId ?: -1}&categoryId=${categoryId ?: -1}"
    }

    object CategoryHistory : Screen("category_history/{categoryId}/{yearMonth}") {
        fun createRoute(categoryId: Long, yearMonth: String) = "category_history/$categoryId/$yearMonth"
    }
}
