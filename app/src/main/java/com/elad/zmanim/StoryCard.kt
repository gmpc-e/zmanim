package com.elad.zmanim

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.ZoneId

@Composable
fun StoryCard(tz: ZoneId, remoteUrlTemplate: String? = null) {
    val ctx = LocalContext.current
    var story by remember { mutableStateOf<ShabbatStory?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(tz, remoteUrlTemplate) {
        loading = true
        story = StoryRepository.fetchWeeklyStory(
            context = ctx,
            tz = tz,
            useIsoWeek = true,
            remoteUrl = remoteUrlTemplate
        )
        loading = false
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "סיפור לשבת",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            when {
                loading -> Text("…טוען סיפור", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                story == null -> Text("לא נמצא סיפור לשבוע זה", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                else -> {
                    Text(
                        story!!.title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        story!!.body,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "מקור: ${story!!.source}",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
