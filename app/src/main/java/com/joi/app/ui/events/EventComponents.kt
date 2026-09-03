package com.joi.app.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joi.app.util.formatMoney
import com.joi.domain.model.EventPayment
import com.joi.domain.model.Event
import com.joi.app.util.formatEventDate

/**
 * The one-line summary of a balance, used identically on a member's own event card and on every
 * row of the moderator's payment sheet — "Paid 200 of 500 · 300 left", with a bar behind it.
 */
@Composable
fun PaymentProgress(
    price: Double,
    paidAmount: Double,
    remainingAmount: Double,
    fullyPaid: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // A free event has nothing to divide by — treat it as complete rather than showing an
        // empty bar nobody can ever fill.
        val fraction = if (price <= 0.0) 1f else (paidAmount / price).coerceIn(0.0, 1.0).toFloat()
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            color = if (fullyPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = when {
                price <= 0.0 -> "Free event"
                fullyPaid -> "✅ Paid in full — ${formatMoney(paidAmount)}"
                else -> "Paid ${formatMoney(paidAmount)} of ${formatMoney(price)} · ${formatMoney(remainingAmount)} left"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (fullyPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The when/where/how-much line under an event's name. */
@Composable
fun EventFactsRow(event: Event, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val time = event.eventTime
        Text(
            text = formatEventDate(event.eventDate) + if (!time.isNullOrBlank()) " · $time" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val location = event.location
        if (!location.isNullOrBlank()) {
            Text(
                "📍 $location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One installment as it appears in a ledger list: what was paid, when, and any note. */
@Composable
fun PaymentRow(payment: EventPayment, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                // A negative entry is a refund or a correction — showing the sign makes that
                // readable at a glance instead of looking like a smaller payment.
                text = if (payment.amount < 0) "− ${formatMoney(-payment.amount)}" else formatMoney(payment.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = if (payment.amount < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            val note = payment.note
            Text(
                text = payment.createdAt.take(10) + if (!note.isNullOrBlank()) " · $note" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}
