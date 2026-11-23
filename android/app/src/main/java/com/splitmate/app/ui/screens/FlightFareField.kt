package com.splitmate.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.splitmate.app.ui.formatIndianRupeesFromCents

/**
 * Total fare for a flight ticket. The fare read from the PDF can be missing or wrong; whatever is
 * typed here (above zero) is used instead. Empty means "use the fare from the ticket".
 */
@Composable
internal fun FlightFareField(
    value: String,
    onValueChange: (String) -> Unit,
    parsedFarePaise: Long,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val fareMissing = parsedFarePaise <= 0L
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text("Total fare") },
        prefix = { Text("\u20B9 ") },
        placeholder = {
            Text(if (fareMissing) "Enter the fare" else formatIndianRupeesFromCents(parsedFarePaise, currencySymbol = ""))
        },
        isError = fareMissing && value.isBlank(),
        supportingText = {
            Text(
                text = if (fareMissing) {
                    "The fare was not found on this ticket. Enter it to continue."
                } else {
                    "Read from the ticket. Type a different amount if it is wrong."
                },
                style = MaterialTheme.typography.bodySmall
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .testTag("FlightFareField")
    )
}
