package com.infras.dauth.repository

import com.infras.dauth.manager.AccountManager
import com.infras.dauth.util.LogUtil
import com.infras.dauthsdk.api.entity.DAuthResult
import com.infras.dauthsdk.api.entity.LoginResultData
import com.infras.dauthsdk.login.model.BindPhoneParam
import com.infras.dauthsdk.login.network.BaseResponse

class SignInRepository constructor() {

    companion object {
        private const val TAG = "SignInRepository"
    }

    private val sdk get() = AccountManager.sdk

    suspend fun signIn(signInBlock: suspend () -> LoginResultData?): Boolean {
        val result = signInBlock.invoke()
        return handleResult(result)
    }

    private suspend fun handleResult(loginResultData: LoginResultData?): Boolean {
        return when (loginResultData) {
            is LoginResultData.Success -> {
                if (loginResultData.needCreateWallet) {
                    val createResult = sdk.createWallet(false)
                    if (createResult is DAuthResult.Success) {
                        LogUtil.d(TAG, "创建的aa钱包地址：${createResult.data.address}")
                        true
                    } else {
                        false
                    }
                } else {
                    val idToken = loginResultData.accessToken
                    // 处理登录成功逻辑
                    LogUtil.d(TAG, "登录成功，返回的ID令牌：$idToken")
                    true
                }
            }

            is LoginResultData.Failure -> {
                val failureCode = loginResultData.code
                // 处理登录失败逻辑
                LogUtil.d(TAG, "登录失败，返回的errorCode：$failureCode")
                false
            }

            else -> {
                LogUtil.e(TAG, "用户取消授权")
                false
            }
        }
    }

    suspend fun sendEmailVerifyCode(account: String): Boolean {
        return sdk.sendEmailVerifyCode(account)?.isSuccess() ?: false
    }

    suspend fun sendPhoneVerifyCode(phone: String, areaCode: String): Boolean {
        return sdk.sendPhoneVerifyCode(phone, areaCode)?.isSuccess() ?: false
    }

    suspend fun bindEmail(email: String, verifyCode: String): BaseResponse? {
        return sdk.bindEmail(email, verifyCode)
    }

    suspend fun bindPhone(phone: String, areaCode: String, verifyCode: String): BaseResponse? {
        return sdk.bindPhone(
            BindPhoneParam(
                phone = phone,
                phone_area_code = areaCode,
                verify_code = verifyCode
            )
        )
    }
}