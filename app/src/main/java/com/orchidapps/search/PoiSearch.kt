package com.orchidapps.search

import com.orchidapps.data.Poi
import com.orchidapps.data.PoiCategory
import com.orchidapps.location.Geo
import java.text.Normalizer
import kotlin.math.min

data class PoiSearchResult(
    val poi: Poi,
    /** Jarak dari posisi pengguna; null bila posisi belum diketahui. */
    val distanceMeters: Double?,
    /** Kualitas kecocokan: 0 = sama persis, makin besar makin longgar. */
    val matchScore: Int,
) {
    val walkingMinutes: Int? get() = distanceMeters?.let(Geo::walkingMinutes)
}

/**
 * Pencarian POI (PB10, F05): mencocokkan nama, nama lain (alias), dan kategori; tidak peka huruf
 * besar/kecil, menerima kata yang tidak lengkap, dan toleran terhadap salah ketik ringan.
 */
object PoiSearch {

    fun search(
        pois: List<Poi>,
        query: String,
        category: PoiCategory? = null,
        userLat: Double? = null,
        userLng: Double? = null,
    ): List<PoiSearchResult> {
        val queryTokens = tokens(query)
        return pois
            .filter { category == null || it.category == category }
            .mapNotNull { poi ->
                val score = if (queryTokens.isEmpty()) 0 else matchScore(poi, query, queryTokens) ?: return@mapNotNull null
                val distance = if (userLat != null && userLng != null) {
                    Geo.distanceMeters(userLat, userLng, poi.lat, poi.lng)
                } else {
                    null
                }
                PoiSearchResult(poi, distance, score)
            }
            // Diurutkan dari yang terdekat; tanpa posisi, dari yang paling cocok.
            .sortedWith(
                compareBy<PoiSearchResult> { it.distanceMeters ?: Double.MAX_VALUE }
                    .thenBy { it.matchScore }
                    .thenBy { it.poi.id }
            )
    }

    private fun matchScore(poi: Poi, query: String, queryTokens: List<String>): Int? {
        val phrases = (listOf(poi.name, poi.id, poi.category.label) + poi.aliases).map(::normalize)
        val phrase = normalize(query)
        if (phrases.any { it == phrase }) return 0
        if (phrases.any { it.startsWith(phrase) }) return 1

        val haystack = phrases.flatMap { it.split(' ') }.filter { it.isNotEmpty() }.toSet()
        var worst = 0
        for (token in queryTokens) {
            val best = haystack.minOfOrNull { tokenScore(token, it) } ?: return null
            if (best == NO_MATCH) return null
            worst = maxOf(worst, best)
        }
        return worst
    }

    private fun tokenScore(query: String, word: String): Int = when {
        word == query -> 0
        word.startsWith(query) -> 1
        query.length >= 3 && word.contains(query) -> 2
        isTypo(query, word) -> 3
        else -> NO_MATCH
    }

    /** Salah ketik ringan: 1 huruf untuk kata 4-7 huruf, 2 huruf untuk kata yang lebih panjang. */
    private fun isTypo(query: String, word: String): Boolean {
        val allowed = when {
            query.length < 4 -> 0
            query.length < 8 -> 1
            else -> 2
        }
        if (allowed == 0) return false
        // Juga cocokkan dengan awal kata, untuk kata yang sedang diketik ("aspsia" → "aspasia").
        val candidates = listOf(word, word.take(query.length))
        return candidates.any { editDistance(query, it) <= allowed }
    }

    /** Jarak Damerau-Levenshtein (termasuk pertukaran dua huruf bersebelahan). */
    internal fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }

    internal fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace(NON_ALPHANUMERIC, " ")
            .trim()
            .replace(SPACES, " ")

    private fun tokens(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }

    private const val NO_MATCH = Int.MAX_VALUE
    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
    private val SPACES = Regex(" +")
}
