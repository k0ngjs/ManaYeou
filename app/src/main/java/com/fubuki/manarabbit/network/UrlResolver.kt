package com.fubuki.manarabbit.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URL

/**
 * 텔레그램 채널(https://t.me/s/newtoki9)에서 최신 manatoki 주소를 가져옵니다.
 * 성공 시 "https://manatokiXXX.net/" 형태의 URL을 반환, 실패 시 null.
 */
suspend fun fetchUrlFromTelegram(): String? {
    return withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://t.me/s/newtoki9")
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = resolveClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext null
            resp.close()
            // 가장 마지막에 등장하는 manatoki URL을 최신으로 간주
            val matches = Regex("""https://manatoki(\d+)\.net""").findAll(body).toList()
            matches.lastOrNull()?.value?.let { "$it/" }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * 기본 URL에서 현재 접속 가능한 번호를 탐색해 반환합니다. (텔레그램 실패 시 폴백용)
 *
 * 탐색 순서:
 *  ① 저장된 번호 / URL 내 번호에서 ±50 범위 먼저 시도
 *  ② 전체 범위 460~1000 병렬 배치 스캔, 이후 100~459
 */
suspend fun resolveAutoUrl(baseUrl: String, lastNumber: Int): Pair<String, Int>? {
    return withContext(Dispatchers.IO) {
        try {
            val clean = baseUrl.trimEnd('/')
            val parsed = URL(clean)
            val host = parsed.host
            val scheme = parsed.protocol

            // 호스트 파싱: "manatoki469.net" → prefix="manatoki", num=469, suffix=".net"
            val hostMatch = Regex("""^([a-zA-Z0-9\-]+?)(\d*)(\.[a-zA-Z.]+)$""")
                .find(host) ?: return@withContext null
            val prefix = hostMatch.groupValues[1]
            val numInUrl = hostMatch.groupValues[2].toIntOrNull()
            val domainSuffix = hostMatch.groupValues[3]

            suspend fun tryNumber(n: Int): Boolean {
                if (n <= 0) return false
                return try {
                    val req = Request.Builder()
                        .url("$scheme://$prefix$n$domainSuffix/")
                        .header("User-Agent", USER_AGENT)
                        .build()
                    val resp = resolveClient.newCall(req).execute()
                    val ok = resp.isSuccessful // 200..299
                    resp.close()
                    ok
                } catch (_: Exception) {
                    false
                }
            }

            // ① 알려진 번호가 있으면 근처 ±50 먼저 시도 (빠른 경로)
            val baseNum = numInUrl ?: lastNumber.takeIf { it > 0 }
            if (baseNum != null) {
                val candidates = buildList {
                    if (lastNumber > 0) add(lastNumber)
                    add(baseNum)
                    for (i in 1..50) add(baseNum + i)
                    for (i in 1..20) { val n = baseNum - i; if (n > 0) add(n) }
                }.distinct()
                for (n in candidates) {
                    if (tryNumber(n)) {
                        return@withContext Pair("$scheme://$prefix$n$domainSuffix", n)
                    }
                }
            }

            // ② 전체 범위 병렬 배치 스캔 (460부터 시작, 이후 100~459 탐색)
            val scanRanges = (460..1000 step 30).toList() + (100..459 step 30).toList()
            for (batchStart in scanRanges) {
                val batchEnd = if (batchStart >= 460) minOf(batchStart + 30, 1001)
                               else minOf(batchStart + 30, 460)
                val found = coroutineScope {
                    (batchStart until batchEnd).map { n ->
                        async { if (tryNumber(n)) n else null }
                    }.awaitAll().firstOrNull { it != null }
                }
                if (found != null) {
                    return@withContext Pair("$scheme://$prefix$found$domainSuffix", found)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
