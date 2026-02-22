package com.celdy.groufr.ui.common

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    fun format(amountCents: Long, currencyCode: String): String {
        val amount = amountCents / 100.0
        return try {
            val currency = Currency.getInstance(currencyCode)
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
            formatter.currency = currency
            formatter.format(amount)
        } catch (_: Exception) {
            "%.2f %s".format(amount, currencyCode)
        }
    }
}
