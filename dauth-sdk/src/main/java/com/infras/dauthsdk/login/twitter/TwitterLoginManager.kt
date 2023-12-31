package com.infras.dauthsdk.login.twitter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.infras.dauthsdk.api.SdkConfig
import com.infras.dauthsdk.api.entity.LoginResultData
import com.infras.dauthsdk.login.impl.ThirdPlatformLogin
import com.infras.dauthsdk.login.model.AuthorizeToken2Param
import com.infras.dauthsdk.login.model.DAuthUser
import com.infras.dauthsdk.login.utils.DAuthLogger
import com.infras.dauthsdk.mpc.util.MoshiUtil
import com.google.gson.Gson
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.DefaultLogger
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterConfig
import com.twitter.sdk.android.core.TwitterCore
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import com.twitter.sdk.android.core.internal.CommonUtils
import com.twitter.sdk.android.core.models.User
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TYPE_OF_TWITTER = 110

class TwitterLoginManager private constructor() {

    private var callback: Callback<TwitterSession>? = null
    private val twitterAuthClient by lazy { TwitterAuthClient() }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            TwitterLoginManager()
        }
    }

    fun initTwitterSDK(context: Context, sdkConfig: SdkConfig) {
        val config = TwitterConfig.Builder(context)
            .logger(DefaultLogger(Log.DEBUG))
            .twitterAuthConfig(TwitterAuthConfig(sdkConfig.twitterConsumerKey, sdkConfig.twitterConsumerSecret))
            .debug(true)
            .build()
        Twitter.initialize(config)
    }

    fun twitterLoginAuth(activity: Activity, callback: Callback<TwitterSession>?) {
        this.callback = callback
        checkCallback(callback)
        twitterAuthClient.authorize(activity, callback)
    }

    private fun checkCallback(callback: Callback<*>?) {
        if (callback == null) {
            CommonUtils.logOrThrowIllegalStateException(
                TwitterCore.TAG,
                "Callback must not be null, did you call setCallback?"
            )
        }
    }

    private suspend fun twitterAuth(requestCode: Int, resultCode: Int, data: Intent?): String? =
        suspendCancellableCoroutine {
            val userData = DAuthUser()
            twitterAuthClient.onActivityResult(requestCode, resultCode, data)
            val twitterApiClient = TwitterCore.getInstance().apiClient
            val call = twitterApiClient.accountService.verifyCredentials(false, true, true)
            call.enqueue(object : Callback<User>() {
                override fun success(result: Result<User>?) {
                    val twitterUserInfo = result?.data
                    userData.email = twitterUserInfo?.email
                    userData.openid = twitterUserInfo?.idStr
                    userData.head_img_url = twitterUserInfo?.profileImageUrl
                    userData.nickname = twitterUserInfo?.screenName
                    val twitterUser = MoshiUtil.toJson(userData)
                    DAuthLogger.d("twitter verifyCredentials success, twitterUser=${twitterUser}")
                    it.resume(twitterUser)
                }

                override fun failure(exception: TwitterException?) {
                    DAuthLogger.d("twitter verifyCredentials failure:${exception?.stackTraceToString()}")
                    it.resume(null)
                }
            })
    }

    suspend fun twitterAuthLogin(requestCode: Int, resultCode: Int, data: Intent?): LoginResultData? {
        var loginResultData : LoginResultData? = null
        var twitterUser :String? = null
        try {
            twitterUser = twitterAuth(requestCode, resultCode, data)
        }catch (e: Exception) {
            DAuthLogger.e("suspendCancellableCoroutine exception:$e")
        }
        val body = AuthorizeToken2Param(
            access_token = null,
            refresh_token = null,
            user_type = TYPE_OF_TWITTER,
            commonHeader = null,
            id_token = null,
            user_data = twitterUser
        )
        loginResultData = ThirdPlatformLogin.instance.thirdPlatFormLogin(body)
        return loginResultData
    }

}