package com.infras.dauthsdk.login.impl

import com.infras.dauthsdk.api.entity.LoginResultData
import com.infras.dauthsdk.api.entity.ResponseCode
import com.infras.dauthsdk.login.model.AuthorizeToken2Param
import com.infras.dauthsdk.login.network.RequestApi
import com.infras.dauthsdk.login.utils.DAuthLogger
import com.infras.dauthsdk.login.utils.JwtDecoder
import com.infras.dauthsdk.wallet.impl.manager.Managers
import com.infras.dauthsdk.wallet.impl.manager.WalletManager


class ThirdPlatformLogin private constructor() {

    private val requestApi get() = Managers.requestApi

    companion object {
        val instance by lazy {
            ThirdPlatformLogin()
        }
    }

    suspend fun thirdPlatFormLogin(body: AuthorizeToken2Param) : LoginResultData {
        var loginResultData: LoginResultData? = null
        val authorizeToken2Res = requestApi.authorizeExchangedToken(body)
        if (authorizeToken2Res?.ret == ResponseCode.RESPONSE_CORRECT_CODE) {
            val didToken = authorizeToken2Res.data?.didToken.orEmpty()
            DAuthLogger.d("第三方登录后的didToken:  $didToken")
            val googleUserInfo = JwtDecoder().decoded(didToken)
            val accessToken = authorizeToken2Res.data?.accessToken.orEmpty()
            DAuthLogger.d("第三方登录后获取的accessToken：$accessToken")
            val refreshToken = authorizeToken2Res.data?.refreshToken.orEmpty()
            val authId = googleUserInfo.sub.orEmpty()
            val expireTime = authorizeToken2Res.data?.expireIn
            val userType = body.user_type
            Managers.loginPrefs.putLoginInfo(
                accessToken,
                authId,
                userId = null,
                refreshToken,
                expireTime,
                userType,
                didToken
            )

            val needCreateWallet = Managers.walletManager.getState() != WalletManager.STATE_OK

            // 该邮箱绑定过钱包
            loginResultData = LoginResultData.Success(
                accessToken = accessToken,
                openId = authId,
                needCreateWallet = needCreateWallet
            )
            DAuthLogger.d("第三方账号已绑定钱包，直接进入主页")
        } else {
            val loginResCode = authorizeToken2Res?.ret
            loginResultData = LoginResultData.Failure(loginResCode)
            DAuthLogger.e("第三方认证登录失败 errCode == $loginResCode")
        }
        return loginResultData
    }

}