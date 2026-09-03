package com.joi.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Money is deliberately shown without a currency symbol — the backend stores a bare number and the
 * app is used in a single currency that everyone in the room already knows. Whole amounts lose the
 * trailing ".00" so a 500 event doesn't read as "500.00".
 */
fun formatMoney(amount: Double): String =
    if (amount == Math.floor(amount) && !amount.isInfinite()) {
        String.format(Locale.getDefault(), "%,.0f", amount)
    } else {
        String.format(Locale.getDefault(), "%,.2f", amount)
    }

private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())

/** "2026-07-01" -> "Wed 1 Jul 2026". Falls back to the raw string if the backend ever sends
 * something unparseable, so a display bug can never blank out an event. */
fun formatEventDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate).format(DAY_MONTH_YEAR) }.getOrDefault(isoDate)

/** Today in the same YYYY-MM-DD shape the backend uses, for defaulting a new event's date. */
fun todayIsoDate(): String = LocalDate.now().toString()
