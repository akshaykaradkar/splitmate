package com.splitmate.app.guide.sync

import com.splitmate.app.data.guide.CoordinatePrecision
import com.splitmate.app.data.guide.LatLng
import com.splitmate.app.data.guide.PlanManifest
import com.splitmate.app.data.guide.StayPin
import com.splitmate.app.data.guide.sync.GuideHttpResponse
import com.splitmate.app.data.guide.sync.GuideHttpTransport
import java.io.File
import java.io.IOException

/** Shared helpers for the v2.3.4 persistence/sync tests (pure JVM, no network). */
internal object GuideSyncFixtures {

    fun manifest(
        destQid: String? = "Q1066187",
        wvRev: Long? = 4567890L,
        stay: StayPin? = null,
        pinned: Map<String, Long> = emptyMap(),
        hidden: Map<String, Long> = emptyMap(),
        days: Map<String, Int> = emptyMap(),
        updatedAt: Long = 1_727_600_000_000L,
        by: String? = "9876500001"
    ) = PlanManifest(
        version = 1,
        destinationQid = destQid,
        wikivoyageRevisionId = wvRev,
        stay = stay,
        pinned = pinned,
        hidden = hidden,
        days = days,
        updatedAtEpochMs = updatedAt,
        updatedByMemberKey = by
    )

    fun stay(
        lat: Double = 15.335,
        lng: Double = 76.46,
        label: String = "Our stay",
        by: String? = "9876500001",
        at: Long = 1_727_600_000_000L,
        precision: CoordinatePrecision = CoordinatePrecision.EXACT
    ) = StayPin(LatLng(lat, lng), label, precision, by, at)

    /** Loads a test resource from the classpath, falling back to the source tree. */
    fun resource(path: String): String {
        val url = GuideSyncFixtures::class.java.classLoader?.getResource(path)
        if (url != null) return url.readText(Charsets.UTF_8)
        val candidates = listOf(
            File("src/test/resources/$path"),
            File("app/src/test/resources/$path")
        )
        val file = candidates.firstOrNull { it.exists() } ?: error("fixture not found: $path")
        return file.readText(Charsets.UTF_8)
    }
}

/** Recording fake transport. Each call is answered by [handler]; requests are recorded. */
internal class FakeGuideHttpTransport(
    var handler: (method: String, url: String, body: String?) -> GuideHttpResponse
) : GuideHttpTransport {
    data class Request(val method: String, val url: String, val body: String?, val headers: Map<String, String>)

    val requests = mutableListOf<Request>()

    override suspend fun get(url: String, headers: Map<String, String>): GuideHttpResponse {
        requests += Request("GET", url, null, headers)
        return handler("GET", url, null)
    }

    override suspend fun post(url: String, body: String, headers: Map<String, String>): GuideHttpResponse {
        requests += Request("POST", url, body, headers)
        return handler("POST", url, body)
    }

    companion object {
        fun failing() = FakeGuideHttpTransport { _, _, _ -> throw IOException("offline") }
        fun ok(body: String) = FakeGuideHttpTransport { _, _, _ -> GuideHttpResponse(200, body) }
    }
}
