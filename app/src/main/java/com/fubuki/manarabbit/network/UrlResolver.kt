package com.fubuki.manarabbit.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URL

/**
 * 기본 URL에서 현재 접속 가능한 번호를 탐색해 반환합니다.
 *
 * 탐색 순서:
 *  ① 저장된 번호 / URL 내 번호에서 ±50 범위 먼저 시도
 *  ② 기본 도메인(숫자 없음)이 번호 있는 URL로 리다이렉트하면 그 번호 사용
 *  ③ 전체 범위 100~1000 병렬 배치 스캔
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

            // ② 기본 도메인(숫자 없음) 자체가 리다이렉트로 번호를 알려주는지 확인
            try {
                val baseReq = Request.Builder()
                    .url("$scheme://$prefix$domainSuffix/")
                    .header("User-Agent", USER_AGENT)
                    .build()
                val resp = resolveClient.newCall(baseReq).execute()
                val finalHost = resp.request.url.host  // 리다이렉트 후 최종 URL
                resp.close()
                val redirectMatch = Regex("""^([a-zA-Z0-9\-]+?)(\d+)(\.[a-zA-Z.]+)$""")
                    .find(finalHost)
                val redirectN = redirectMatch?.groupValues?.get(2)?.toIntOrNull()
                if (redirectN != null) {
                    return@withContext Pair("$scheme://$prefix$redirectN$domainSuffix", redirectN)
                }
            } catch (_: Exception) { }

            // ③ 전체 범위 병렬 배치 스캔 (100~1000)
            for (batchStart in 100..1000 step 30) {
                val found = coroutineScope {
                    (batchStart until minOf(batchStart + 30, 1001)).map { n ->
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
