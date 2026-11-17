package com.xinto.opencord.rest.models.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiLoginError(
    val message: String? = null,
    val code: Int? = null,
    val errors: ApiLoginErrorFields? = null,
) {
    val isEmailVerificationRequired: Boolean
        get() = errors?.login?.errors?.any { it.code == "ACCOUNT_LOGIN_VERIFICATION_EMAIL" } == true
}

@Serializable
data class ApiLoginErrorFields(
    @SerialName("login")
    val login: ApiLoginErrorField? = null,
)

@Serializable
data class ApiLoginErrorField(
    @SerialName("_errors")
    val errors: List<ApiLoginErrorDetail> = emptyList(),
)

@Serializable
data class ApiLoginErrorDetail(
    val code: String? = null,
    val message: String? = null,
)

class LoginException(
    val error: ApiLoginError,
) : Exception(error.message)
