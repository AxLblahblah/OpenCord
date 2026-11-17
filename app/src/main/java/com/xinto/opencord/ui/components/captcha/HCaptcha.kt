package com.xinto.opencord.ui.components.captcha

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaError
import com.xinto.opencord.di.HCaptchaFactory
import org.koin.compose.koinInject

@Composable
fun HCaptcha(
    captchaSiteKey: String,
    captchaRqData: String?,
    onSuccess: (token: String) -> Unit,
    onFailure: (HCaptchaError, code: Int) -> Unit,
) {
    val captchaFactory: HCaptchaFactory = koinInject()
    val captcha = remember(captchaFactory, captchaSiteKey, captchaRqData) {
        captchaFactory(captchaSiteKey, captchaRqData)
    }
    DisposableEffect(captcha) {
        captcha.verifyWithHCaptcha()
            .addOnSuccessListener {
                onSuccess(it.tokenResult)
            }
            .addOnFailureListener {
                onFailure(it.hCaptchaError, it.statusCode)
            }
        onDispose {}
    }
}
