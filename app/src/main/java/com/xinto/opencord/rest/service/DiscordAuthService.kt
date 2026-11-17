package com.xinto.opencord.rest.service

import com.xinto.opencord.BuildConfig
import com.xinto.opencord.rest.body.LoginBody
import com.xinto.opencord.rest.body.TwoFactorBody
import com.xinto.opencord.rest.models.ApiExperiments
import com.xinto.opencord.rest.models.login.ApiLogin
import com.xinto.opencord.rest.models.login.ApiLoginError
import com.xinto.opencord.rest.models.login.LoginException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HttpHeaders.XFingerprint: String
    get() = "X-Fingerprint"

private val HttpHeaders.XCaptchaRqtoken: String
    get() = "X-Captcha-Rqtoken"

interface DiscordAuthService {
    suspend fun getFingerprint(): Pair<String, List<Cookie>>

    suspend fun login(
        body: LoginBody,
        existingCookies: List<Cookie>?,
        xFingerprint: String,
        xCaptchaRqtoken: String? = null,
    ): ApiLogin

    suspend fun verifyTwoFactor(
        body: TwoFactorBody,
        existingCookies: List<Cookie>?,
        xFingerprint: String,
        xCaptchaRqtoken: String? = null,
    ): ApiLogin
}

class DiscordAuthServiceImpl(
    private val client: HttpClient
) : DiscordAuthService {
    override suspend fun getFingerprint(): Pair<String, List<Cookie>> {
        return withContext(Dispatchers.IO) {
            val response = client.get(getExperimentsUrl())
            val data = response.body<ApiExperiments>()
            val fixedCookies = response.setCookie().map { cookie ->
                if (cookie.domain !== null) {
                    cookie
                } else cookie.copy(
                    domain = BASE_DOMAIN,
                )
            }

            data.fingerprint to fixedCookies
        }
    }

    override suspend fun login(
        body: LoginBody,
        existingCookies: List<Cookie>?,
        xFingerprint: String,
        xCaptchaRqtoken: String?,
    ): ApiLogin {
        return withContext(Dispatchers.IO) {
            val response = client.post(getLoginUrl()) {
                setBody(body)
                setCookies(existingCookies)
                header(HttpHeaders.XFingerprint, xFingerprint)
                if (xCaptchaRqtoken != null) {
                    header(HttpHeaders.XCaptchaRqtoken, xCaptchaRqtoken)
                }
            }
            response.bodyOrThrowLoginError()
        }
    }

    override suspend fun verifyTwoFactor(
        body: TwoFactorBody,
        existingCookies: List<Cookie>?,
        xFingerprint: String,
        xCaptchaRqtoken: String?,
    ): ApiLogin {
        return withContext(Dispatchers.IO) {
            val response = client.post(getTwoFactorUrl()) {
                setBody(body)
                setCookies(existingCookies)
                header(HttpHeaders.XFingerprint, xFingerprint)
                if (xCaptchaRqtoken != null) {
                    header(HttpHeaders.XCaptchaRqtoken, xCaptchaRqtoken)
                }
            }
            response.bodyOrThrowLoginError()
        }
    }

    private suspend fun HttpResponse.bodyOrThrowLoginError(): ApiLogin {
        if (status.isSuccess()) return body()
        throw LoginException(body<ApiLoginError>())
    }

    private fun HttpRequestBuilder.setCookies(cookies: List<Cookie>?) {
        if (!cookies.isNullOrEmpty()) {
            val stringCookies = cookies.joinToString("; ") {
                renderCookieHeader(it)
            }
            header(HttpHeaders.Cookie, stringCookies)
        }
    }

    private companion object {
        const val BASE = BuildConfig.URL_API
        val BASE_DOMAIN = Url(BASE).host

        fun getLoginUrl(): String {
            return "$BASE/auth/login"
        }

        fun getTwoFactorUrl(): String {
            return "$BASE/auth/mfa/totp"
        }

        fun getExperimentsUrl(): String {
            return "$BASE/experiments"
        }
    }
}
