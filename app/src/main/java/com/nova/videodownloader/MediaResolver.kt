package com.nova.videodownloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI

data class MediaCandidate(val url:String,val type:String)

class MediaResolver {
    private val client=OkHttpClient.Builder().followRedirects(true).build()

    fun resolve(input:String):List<MediaCandidate>{
        val url=input.trim()
        if(!url.startsWith("http://")&&!url.startsWith("https://")) return emptyList()
        if(isDirect(url)) return listOf(MediaCandidate(url,kind(url)))
        val req=Request.Builder().url(url).header("User-Agent",USER_AGENT).build()
        client.newCall(req).execute().use { response ->
            if(!response.isSuccessful) return emptyList()
            val html=response.body?.string().orEmpty()
            val base=response.request.url.toString()
            val found=LinkedHashSet<String>()
            val patterns=listOf(
                Regex("""(?i)<meta[^>]+property=["']og:video(?::url)?["'][^>]+content=["']([^"']+)["']"""),
                Regex("""(?i)<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:video(?::url)?["']"""),
                Regex("""(?i)<video[^>]+src=["']([^"']+)["']"""),
                Regex("""(?i)<source[^>]+src=["']([^"']+)["']"""),
                Regex("""(?i)["'](https?[^"'\\s]+\.(?:mp4|webm|mov|m4v|mkv)(?:\?[^"'\\s]*)?)["']"""),
                Regex("""(?i)["'](https?[^"'\\s]+\.m3u8(?:\?[^"'\\s]*)?)["']""")
            )
            patterns.forEach { pattern ->
                pattern.findAll(html).forEach { match ->
                    runCatching { URI(base).resolve(match.groupValues[1]).toString() }.getOrNull()?.let { found.add(it) }
                }
            }
            return found.map { MediaCandidate(it,kind(it)) }
        }
    }

    private fun isDirect(url:String):Boolean{
        val path=runCatching { URI(url).path.lowercase() }.getOrDefault("")
        return listOf(".mp4",".webm",".mov",".m4v",".mkv",".mp3",".m4a",".aac",".wav",".flac",".m3u8",".mpd").any { path.endsWith(it) }
    }

    private fun kind(url:String):String{
        val path=runCatching { URI(url).path.lowercase() }.getOrDefault("")
        return when {
            path.endsWith(".m3u8")->"HLS stream"
            path.endsWith(".mpd")->"DASH stream"
            listOf(".mp3",".m4a",".aac",".wav",".flac").any { path.endsWith(it) }->"Audio"
            else->"Video"
        }
    }

    companion object { const val USER_AGENT="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36" }
}
