package com.fubuki.manarabbit.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URL

/**
 * Tries to find a working numbered URL given a base URL.
 * e.g., "https://manatoki.net/" or "https://manatoki469.net/" → "https://manatoki470.net/"
 *
 * Returns Pair(resolvedUrl, workingNumber) or null if nothing found.
 */
suspend fun resolveAutoUrl(baseUrl: String, lastNumber: Int): Pair<String, Int>? {
    return withContext(Dispatchers.IO) {
        try {
            val clean = baseUrl.trimEnd('/')
            val parsed = URL(clean)
            val host = parsed.host
            val scheme = parsed.protocol

            // Split host into (prefix, number, domainSuffix)
            // e.g. "manatoki469.net" → ("manatoki", 469, ".net")
            val hostMatch = Regex("""^([a-zA-Z0-9\-]+?)(\d*)(\.[a-zA-Z.]+)$""")
                .find(host) ?: return@withContext null
            val prefix = hostMatch.groupValues[1]
            val numInUrl = hostMatch.groupValues[2].toIntOrNull()
            val domainSuffix = hostMatch.groupValues[3]

            suspend fun tryNumber(n: Int): Boolean {
                return try {
                    val req = Request.Builder()
                        .url("$scheme://$prefix$n$domainSuffix/")
                        .header("User-Agent", USER_AGENT)
                        .build()
                    val resp = resolveClient.newCall(req).execute()
                    val ok = resp.code in 200..403
                    resp.close()
                    ok
                } catch (_: Exception) {
                    false
                }
            }

            val baseNum = numInUrl ?: lastNumber.takeIf { it > 0 }

            if (baseNum != null) {
                // Try stored number first, then current URL number, then +1..+15
                val candidates = buildList {
                    if (lastNumber > 0) add(lastNumber)
                    add(baseNum)
                    for (i in 1..15) add(baseNum + i)
                }.distinct()
                for (n in candidates) {
                    if (tryNumber(n)) {
                        return@withContext Pair("$scheme://$prefix$n$domainSuffix", n)
                    }
                }
            } else {
                // No number in URL and no stored number — scan 300..600 in batches of 20
                for (batchStart in 300..600 step 20) {
                    val found = coroutineScope {
                        (batchStart until minOf(batchStart + 20, 601)).map { n ->
                            async { if (tryNumber(n)) n else null }
                        }.awaitAll().firstOrNull { it != null }
                    }
                    if (found != null) {
                        return@withContext Pair("$scheme://$prefix$found$domainSuffix", found)
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
