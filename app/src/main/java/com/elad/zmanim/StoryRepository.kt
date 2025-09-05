package com.elad.zmanim

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class ShabbatStory(
    val title: String,
    val body: String,
    val source: String // "local:<key>" or "remote:<url>"
)

object StoryRepository {
    private val client by lazy { OkHttpClient() }

    suspend fun fetchWeeklyStory(
        context: Context,
        tz: ZoneId,
        useIsoWeek: Boolean = true,
        remoteUrl: String? = null // "https://example.com/stories/{key}.txt"
    ): ShabbatStory? = withContext(Dispatchers.IO) {
        val shabbat = nextShabbat(LocalDate.now(tz))
        val key = if (useIsoWeek) isoWeekKey(shabbat) else dateKey(shabbat)

        readAsset(context, "stories/$key.txt")?.let { txt ->
            return@withContext parseStory(txt, source = "local:$key")
        }

        if (!remoteUrl.isNullOrBlank() && remoteUrl.contains("{key}")) {
            val url = remoteUrl.replace("{key}", key)
            download(url)?.let { txt ->
                return@withContext parseStory(txt, source = "remote:$url")
            }
        }

        null
    }

    private fun isoWeekKey(date: LocalDate): String {
        val wf = WeekFields.of(Locale.getDefault())
        val week = date.get(wf.weekOfWeekBasedYear())
        val year = date.get(wf.weekBasedYear())
        return "%04d-W%02d".format(year, week)
    }

    private fun dateKey(date: LocalDate): String = date.format(DateTimeFormatter.ISO_DATE)

    private fun readAsset(context: Context, path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            null
        } catch (t: Throwable) {
            DebugLog.e("readAsset failed: $path", t); null
        }
    }

    private fun download(url: String): String? {
        return try {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        } catch (t: Throwable) {
            DebugLog.e("download story failed: $url", t); null
        }
    }

    private fun parseStory(raw: String, source: String): ShabbatStory {
        val lines = raw.lines()
        val first = lines.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val body = lines.dropWhile { it.isBlank() }
            .drop(1)
            .joinToString("\n")
            .trim()
        return ShabbatStory(
            title = if (first.isBlank()) "סיפור לשבת" else first,
            body = if (body.isBlank()) raw.trim() else body,
            source = source
        )
    }
}
