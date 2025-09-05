package com.elad.zmanim

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("About Zmanim & Methods") },
        text = {
            Column(Modifier.fillMaxWidth().padding(PaddingValues(top = 4.dp))) {
                Text(
                    "- Times are calculated with the KosherJava Zmanim library.\n" +
                            "- Sha’ah zmanit (halachic hour) can follow GRA (sunrise→sunset) or MGA (alos→tzeit).\n" +
                            "- Dawn / nightfall (alos / tzeit) can be degree-based (e.g., 11.5°, 16.1°, 18°) or fixed minutes (e.g., 72 min).\n" +
                            "- Visible vs sea-level sunrise/sunset may differ based on location elevation.\n" +
                            "- Candle-lighting offset is city/minhag dependent (e.g., 18, 30, 40 min before sunset).\n" +
                            "- Shabbat card (parasha, candle-lighting, havdalah) uses Hebcal data for the selected city.\n" +
                            "\nTo match a specific luach exactly, set the Board preset and offsets to that luach’s method (including degrees/minutes and city elevation).",
                    textAlign = TextAlign.Start
                )
            }
        }
    )
}
