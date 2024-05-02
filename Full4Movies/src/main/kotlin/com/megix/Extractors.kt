package com.megix

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.helper.AesHelper.cryptoAESHandler
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper

class Vectorx : Chillx() {
    override val name = "Vectorx"
    override val mainUrl = "https://vectorx.top"
}

class Boltx : Chillx() {
    override val name = "Boltx"
    override val mainUrl = "https://boltx.stream"
}

class Bestx : Chillx() {
    override val name = "Bestx"
    override val mainUrl = "https://bestx.top"
}


open class Chillx : ExtractorApi() {
    override val name = "Chillx"
    override val mainUrl = "https://chillx.top"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val master = Regex("""JScript[\w+]?\s*=\s*'([^']+)""").find(
            app.get(
                url,
                referer = url,
            ).text
        )?.groupValues?.get(1)
        val key = app.get("https://raw.githubusercontent.com/rushi-chavan/multi-keys/keys/keys.json").parsedSafe<Keys>()?.key?.get(0) ?: throw ErrorLoadingException("Unable to get key")
        val decrypt = cryptoAESHandler(master ?: "",key.toByteArray(), false)?.replace("\\", "") ?: throw ErrorLoadingException("failed to decrypt")
        val source = Regex(""""?file"?:\s*"([^"]+)""").find(decrypt)?.groupValues?.get(1)
        val subtitles = Regex("""subtitle"?:\s*"([^"]+)""").find(decrypt)?.groupValues?.get(1)
        val subtitlePattern = """\[(.*?)](https?://[^\s,]+)""".toRegex()
        val matches = subtitlePattern.findAll(subtitles ?: "")
        val languageUrlPairs = matches.map { matchResult ->
            val (language, url) = matchResult.destructured
            decodeUnicodeEscape(language) to url
        }.toList()

        languageUrlPairs.forEach{ (name, file) ->
            subtitleCallback.invoke(
                SubtitleFile(
                    name,
                    file
                )
            )
        }
        // required
        val headers = mapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            "Origin" to mainUrl,
        )

        M3u8Helper.generateM3u8(
            name,
            source ?: return,
            "$mainUrl/",
            headers = headers
        ).forEach(callback)
    }

    private fun decodeUnicodeEscape(input: String): String {
        val regex = Regex("u([0-9a-fA-F]{4})")
        return regex.replace(input) {
            it.groupValues[1].toInt(16).toChar().toString()
        }
    }

    data class Keys(
        @JsonProperty("chillx") val key: List<String>
    )
}