package com.xinto.opencord.di

import android.content.Context
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

typealias HCaptchaFactory = (siteKey: String, rqData: String?) -> HCaptcha

val hcaptchaModule = module {
    fun provideHCaptcha(activity: Context, siteKey: String, rqData: String?): HCaptcha {
        val config = HCaptchaConfig.builder()
            .siteKey(siteKey)
            .host("discord.com")
            .rqdata(rqData)
            .resetOnTimeout(true)
            .tokenExpiration(1000000L)
            .build()
        return HCaptcha.getClient(activity).setup(config)
    }

    factory<HCaptchaFactory> {
        { siteKey, rqData -> provideHCaptcha(androidContext(), siteKey, rqData) }
    }
}
