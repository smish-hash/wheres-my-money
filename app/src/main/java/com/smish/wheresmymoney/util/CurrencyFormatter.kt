package com.smish.wheresmymoney.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun format(amount: Double): String = "₹${formatter.format(amount)}"
}
