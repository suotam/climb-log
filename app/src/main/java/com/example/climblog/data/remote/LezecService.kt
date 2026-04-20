package com.example.climblog.data.remote

import android.util.Log
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.RouteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LezecService"

data class BulkAscentParams(
    val lezecId: Int,
    val dateMillis: Long,
    val style: AscentStyle,
    val grade: String,
    val attempts: Int,
    val note: String,
    val routeType: RouteType,
)

@Singleton
class LezecService @Inject constructor() {

    suspend fun loginAndLogAscent(
        uid: String,
        password: String,
        lezecId: Int,
        dateMillis: Long,
        style: AscentStyle,
        grade: String,
        attempts: Int,
        note: String,
        routeType: RouteType
    ): Boolean = withContext(Dispatchers.IO) {
        val client = login(uid, password) ?: return@withContext false
        postAscent(client, lezecId, dateMillis, style, grade, attempts, note, routeType)
    }

    /** Login jednou, pak zapiš každý přelez zvlášť. Vrátí seznam výsledků ve stejném pořadí. */
    suspend fun loginAndLogAscents(
        uid: String,
        password: String,
        ascents: List<BulkAscentParams>
    ): List<Boolean> = withContext(Dispatchers.IO) {
        if (ascents.isEmpty()) return@withContext emptyList()
        val client = login(uid, password) ?: return@withContext List(ascents.size) { false }
        ascents.map { a ->
            postAscent(client, a.lezecId, a.dateMillis, a.style, a.grade, a.attempts, a.note, a.routeType)
        }
    }

    private fun login(uid: String, password: String): OkHttpClient? {
        val cookieJar = InMemoryCookieJar()
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .build()

        // 1. GET homepage — server nastaví počáteční session cookies
        val initRequest = Request.Builder()
            .url("https://www.lezec.cz/index.php")
            .get()
            .addHeader("User-Agent", "Mozilla/5.0 (Android)")
            .build()
        client.newCall(initRequest).execute().use { response ->
            Log.d(TAG, "Init GET: ${response.code}, cookies: ${cookieJar.cookieNames()}")
        }

        // 2. POST login
        val loginParams = buildWindows1250Form(
            "login" to "2",
            "uid" to uid,
            "hes" to password,
            "x" to "15",
            "y" to "5"
        )
        val loginRequest = Request.Builder()
            .url("https://www.lezec.cz/login.php")
            .post(loginParams.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .addHeader("Origin", "https://www.lezec.cz")
            .addHeader("Referer", "https://www.lezec.cz/index.php")
            .addHeader("User-Agent", "Mozilla/5.0 (Android)")
            .build()
        client.newCall(loginRequest).execute().use { response ->
            val body = response.body?.string()?.take(300) ?: ""
            Log.d(TAG, "Login response: ${response.code}, cookies: ${cookieJar.cookieNames()}")
            Log.d(TAG, "Login body: $body")
            if (!response.isSuccessful && response.code != 302) {
                Log.e(TAG, "Login failed with code ${response.code}")
                return null
            }
        }

        if (cookieJar.isEmpty()) {
            Log.e(TAG, "Login OK but no cookies — špatné heslo nebo blokování")
            return null
        }

        return client
    }

    private fun postAscent(
        client: OkHttpClient,
        lezecId: Int,
        dateMillis: Long,
        style: AscentStyle,
        grade: String,
        attempts: Int,
        note: String,
        routeType: RouteType
    ): Boolean {
        val stl = style.toLezecCode() ?: return false
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val ascentParams = buildWindows1250Form(
            "denik" to "2",
            "kei" to lezecId.toString(),
            "kat" to routeType.toLezecKat(),
            "dtd" to cal.get(Calendar.DAY_OF_MONTH).toString(),
            "dtm" to (cal.get(Calendar.MONTH) + 1).toString(),
            "dty" to cal.get(Calendar.YEAR).toString(),
            "stl" to stl,
            "hid" to "on",
            "svd" to "",
            "kls" to grade,
            "pok" to attempts.toString(),
            "poz" to note
        )
        val ascentRequest = Request.Builder()
            .url("https://www.lezec.cz/denik.php")
            .post(ascentParams.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .addHeader("Referer", "https://www.lezec.cz/cesta.php?key=$lezecId")
            .addHeader("Origin", "https://www.lezec.cz")
            .addHeader("User-Agent", "Mozilla/5.0 (Android)")
            .build()

        client.newCall(ascentRequest).execute().use { response ->
            val body = response.body?.string()?.take(600) ?: ""
            Log.d(TAG, "Denik[$lezecId] response: ${response.code}")
            Log.d(TAG, "Denik[$lezecId] body preview: $body")
            val isLoginPage = body.contains("login.php") || body.contains("name=\"hes\"")
            val success = (response.isSuccessful || response.code == 302) && !isLoginPage
            if (!success) Log.e(TAG, "Denik.php[$lezecId] failed — pravděpodobně neautentizováno")
            return success
        }
    }
}

/** Sestaví URL-encoded formulář v kódování Windows-1250 (jak očekává lezec.cz). */
private fun buildWindows1250Form(vararg pairs: Pair<String, String>): String =
    pairs.joinToString("&") { (k, v) ->
        URLEncoder.encode(k, "windows-1250") + "=" + URLEncoder.encode(v, "windows-1250")
    }

private class InMemoryCookieJar : CookieJar {
    private val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.removeAll { existing -> cookies.any { it.name == existing.name } }
        store.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = store.toList()

    fun isEmpty(): Boolean = store.isEmpty()
    fun cookieNames(): List<String> = store.map { it.name }
}

private fun AscentStyle.toLezecCode(): String? = when (this) {
    AscentStyle.ONSIGHT  -> "OS"
    AscentStyle.FLASH    -> "flash"
    AscentStyle.REDPOINT -> "RP"
    AscentStyle.TOPROPE  -> "TH"
    AscentStyle.ATTEMPT  -> "PP"
    AscentStyle.PROJECT  -> null
}

private fun RouteType.toLezecKat(): String = when (this) {
    RouteType.BOULDER -> "bouldering"
    else              -> "skalní"
}
