package com.cyberflow.dauthsdk.login

import android.app.Activity
import android.content.Context
import com.cyberflow.dauthsdk.login.api.bean.SdkConfig
import com.cyberflow.dauthsdk.login.constant.LoginType
import com.cyberflow.dauthsdk.login.google.GoogleLoginManager
import com.cyberflow.dauthsdk.login.callback.BaseHttpCallback
import com.cyberflow.dauthsdk.login.callback.ResetPwdCallback
import com.cyberflow.dauthsdk.login.model.*
import com.cyberflow.dauthsdk.login.network.RequestApi
import com.cyberflow.dauthsdk.login.utils.JwtChallengeCode
import com.cyberflow.dauthsdk.login.utils.ThreadPoolUtils
import com.cyberflow.dauthsdk.login.twitter.TwitterLoginManager
import com.cyberflow.dauthsdk.login.utils.DAuthLogger
import com.cyberflow.dauthsdk.login.utils.SignUtils
import com.cyberflow.dauthsdk.wallet.api.IWalletApi
import com.cyberflow.dauthsdk.wallet.impl.DAuthWallet
import com.cyberflow.dauthsdk.wallet.impl.WalletHolder
import com.twitter.sdk.android.core.*
import kotlinx.coroutines.*
import java.math.BigInteger

private const val ACCOUNT_TYPE_OF_OWN = "70"  //自定义账号
private const val ACCOUNT_TYPE_OF_EMAIL = 10  //邮箱
private const val USER_TYPE = "user_type"
private const val PHONE = "phone"
private const val VERIFY_CODE = "verify_code"
private const val ACCOUNT = "account"

class DAuthSDK private constructor(): IWalletApi by WalletHolder.walletApi {

    companion object {
        val instance by lazy {
            DAuthSDK()
        }
    }

    private var _context: Context? = null
    internal val context get() = _context ?: throw RuntimeException("please call initSDK() first")
    private var _config: SdkConfig? = null
    private val config get() = _config ?: throw RuntimeException("please call initSDK() first")

    /**
     * 所有后续SDK的方法都应在SDK初始化成功之后调用
     */
    fun initSDK(context: Context, config: SdkConfig) {
        val appContext = context.applicationContext
        this._context = appContext
        this._config = config
        //Twitter初始化
        Twitter.initialize(appContext)
        initWallet(appContext)
        DAuthLogger.i("init sdk")
    }

    fun login(account: String, passWord: String, callback: BaseHttpCallback<LoginRes>) {
        val map = HashMap<String, String>()
        map[USER_TYPE] = ACCOUNT_TYPE_OF_OWN
        map["account"] = account
        map["password"] = passWord
        val sign = SignUtils.sign(map)
        val loginParam = LoginParam(
            ACCOUNT_TYPE_OF_OWN.toInt(),
            account = account,
            password = passWord,
            sign = sign
        )
        ThreadPoolUtils.execute {
            val loginRes = RequestApi().login(loginParam)

            if(loginRes?.iRet == 0) {
                val didToken = loginRes.data.did_token
                //DAuth 授权接口测试获取临时code
                val codeVerifier = JwtChallengeCode().generateCodeVerifier()
                val codeChallenge = JwtChallengeCode().generateCodeChallenge(codeVerifier)
                DAuthLogger.d("codeVerify == $codeVerifier")
                val code = loginAuth(codeChallenge)
                //DAuth 授权接口测试获取token
                val tokenAuthenticationRes = getDAuthToken(codeVerifier,code,didToken)
                DAuthLogger.e("sdk授权登录返回：$tokenAuthenticationRes.")
                DAuthLogger.e("登录成功 didToken == $didToken,loginRes== ${loginRes.data}")
            } else {
                DAuthLogger.e("登录失败：${loginRes?.sMsg}")
            }

        }

    }

    /**
     * @param codeChallengeCode sha256算法生成的一个code
     * @return 服务端返回临时code
     */

    fun loginAuth(codeChallengeCode: String) :String = runBlocking{
        val map = HashMap<String,String>()
        var code = ""
        map["user_type"] = "10"
        map["code_challenge"] = codeChallengeCode
        map["code_challenge_method"] = "SHA-256"
        val sign = SignUtils.sign(map)
//        ThreadPoolUtils.execute {
        withContext(Dispatchers.IO) {
            val body = AuthorizeParam(10,codeChallengeCode,"SHA-256",sign)
            val authorizeParam = RequestApi().ownAuthorize(body)
            code = authorizeParam?.data?.code.orEmpty()  //获取临时code
            DAuthLogger.e("ownAuthorize 临时code： $code ")
        }
        return@runBlocking code
//        }
    }

    private fun getDAuthToken(codeVerifier: String, code: String, didToken: String?) {
        val map = HashMap<String,String>()
        map["code_verifier"] = codeVerifier
        map["code"] = code
        val sign = SignUtils.sign(map)
        ThreadPoolUtils.execute {
            val body = TokenAuthenticationParam(codeVerifier,code,sign)
            RequestApi().ownOauth2Token(body, didToken)
        }
    }

    /**
     * @param type 第三方账号类型 GOOGLE TWITTER FACEBOOK
     * @param activity
     */
    fun loginWithType(type: String, activity: Activity) {
        when (type) {
            LoginType.GOOGLE -> {
                GoogleLoginManager.instance.googleSignInAuth(activity)
            }
            LoginType.TWITTER -> {
                TwitterLoginManager.instance.twitterLoginAuth(
                    activity, object : Callback<TwitterSession>() {
                        override fun success(result: Result<TwitterSession>?) {
                            val token = result?.data?.authToken
                            val userId = result?.data?.userId

                            DAuthLogger.d("twitter login success")
                        }

                        override fun failure(exception: TwitterException?) {
                            DAuthLogger.e("twitter login failed:$exception")
                        }

                    }
                )
            }
        }
    }

    /**
     * @param account 自有账号（字母和数字组合）
     * @param passWord 密码
     * @param confirmPwd 确认密码
     */

    fun createDAuthAccount(account: String, passWord: String, confirmPwd: String) : Boolean {
        var isSuccess  = false
        val map = HashMap<String,String?>()
        var userType = ACCOUNT_TYPE_OF_OWN
        map[ACCOUNT] = account
        map[USER_TYPE] = userType
        map["uuid"] = "123456"
        map["is_login"] = "1"
        map["sex"] = "0"
        map["password"] = passWord
        map["confirm_password"] = confirmPwd
        val sign = SignUtils.sign(map)
        val createAccountParam = CreateAccountParam(userType, "123456", sign, 1,
            passWord, confirm_password = confirmPwd, sex = 0, account = account)
        val feature = ThreadPoolUtils.submit {
            val createAccountRes = RequestApi().createAccount(createAccountParam)
            if(createAccountRes?.iRet == 0) {
                isSuccess = true
            }

        }
        val f = feature.get()
        return isSuccess
    }


    /**
     * @param account 手机号或邮箱
     * @param verifyCode 验证码
     * @param type  10(邮箱) 60(手机)
     */
    fun loginByMobileOrEmail(account: String, verifyCode: String, type: Int) {
        val map = HashMap<String, String>()
        map[USER_TYPE] = type.toString()
        map[VERIFY_CODE] = verifyCode
        if(type == 10) {
            map[ACCOUNT] = account
            val sign = SignUtils.sign(map)
            val loginParam = LoginParam(type, sign, account = account, verify_code = verifyCode)
            ThreadPoolUtils.execute {
                RequestApi().login(loginParam)
            }
        } else {
            map[PHONE] = account
            val sign = SignUtils.sign(map)
            val loginParam = LoginParam(type, sign, phone = account, verify_code = verifyCode)
            ThreadPoolUtils.execute {
                RequestApi().login(loginParam)
            }
        }
    }


    fun logout(openUid: String) {
        val map = HashMap<String,String>()
        map["openudid"] = openUid
        val sign = SignUtils.sign(map)
        val requestBody = LogoutParam(openUid, sign)
        ThreadPoolUtils.execute {
            RequestApi().logout(requestBody)
        }
    }

    fun setRecoverPassword(callback: ResetPwdCallback) {
        callback.success()
    }

    /**
     * @param phone 手机号
     * @param areaCode  区号
     */
    fun sendPhoneVerifyCode(phone: String, areaCode: String) {
        val map = HashMap<String, String>()
        map["phone"] = phone
        map["phone_area_code"] = areaCode
        val sign = SignUtils.sign(map)
        val body = SendPhoneVerifyCodeParam(openudid = null, phone, areaCode,sign)
        ThreadPoolUtils.execute {
            RequestApi().sendPhoneVerifyCode(body)
        }
    }

    /**
     * @param email 邮箱
     */
    fun sendEmailVerifyCode(email: String): Boolean {
        var isSend = false
        val map = HashMap<String, String>()
        map[ACCOUNT] = email
        val sign = SignUtils.sign(map)
        val body = SendEmailVerifyCodeParam(email, sign)
        val feature = ThreadPoolUtils.submit {
            val response = RequestApi().sendEmailVerifyCode(body)
            if(response?.iRet == 0) {
                isSend = true
                DAuthLogger.d("发送邮箱验证码成功")
            } else {
                DAuthLogger.e("发送邮箱验证码失败：${response?.sMsg}")
            }
        }
        val f = feature.get()
        return isSend
    }

    /**
     * @param bindParams 对象
     *  包含 openudid(用户id)
     *  phone(手机号)
     *  phone_area_code(区号)
     *  verify_code(验证码)
     */
    fun bindPhone(bindParams: BindPhoneParam) {
        ThreadPoolUtils.execute {
            RequestApi().bindPhone(bindParams)
        }
    }

    /**
     * @param email 邮箱
     * @param verifyCode 邮箱验证码
     */
    fun bindEmail(email: String, verifyCode: String) {

    }
}