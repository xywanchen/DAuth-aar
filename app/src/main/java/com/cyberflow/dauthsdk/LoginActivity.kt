package com.cyberflow.dauthsdk

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cyberflow.dauth.databinding.ActivityLoginLayoutBinding
import com.cyberflow.dauthsdk.login.DAuthSDK
import com.cyberflow.dauthsdk.login.google.GoogleLoginManager
import com.cyberflow.dauthsdk.login.callback.BaseHttpCallback
import com.cyberflow.dauthsdk.login.callback.OnActivityResultListener
import com.cyberflow.dauthsdk.login.model.LoginRes
import com.cyberflow.dauthsdk.login.twitter.TwitterLoginManager
import com.cyberflow.dauthsdk.login.utils.DAuthLogger
import com.cyberflow.dauthsdk.login.view.WalletWebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"
private const val TWITTER_REQUEST_CODE = 140
private const val GOOGLE_REQUEST_CODE = 9001
private const val GOOGLE = "GOOGLE"
private const val TWITTER = "TWITTER"
private const val FACEBOOK = "FACEBOOK"

class LoginActivity : AppCompatActivity(), OnActivityResultListener {
    var loginBinding: ActivityLoginLayoutBinding?  = null
    private val binding: ActivityLoginLayoutBinding get() = loginBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginBinding = ActivityLoginLayoutBinding.inflate(LayoutInflater.from(this))

        setContentView(binding.root)

        binding.btnDauthLogin.setOnClickListener {
            val account = binding.edtAccount.text.toString()
            val password = binding.edtPassword.text.toString()
            lifecycleScope.launch {
                val code = DAuthSDK.instance.login(account,password)
                DAuthLogger.d("login return code == $code")
            }


        }

        binding.tvForgetPwd.setOnClickListener {
            ResetPasswordActivity.launch(this)
        }

        binding.tvRegister.setOnClickListener {
            RegisterActivity.launch(this)
        }
        initView()
    }

    private fun initView() {
        binding.ivGoogle.setOnClickListener {
            DAuthSDK.instance.loginWithType(GOOGLE, this)
        }

        binding.ivTwitter.setOnClickListener {
            DAuthSDK.instance.loginWithType(TWITTER, this)
        }

        binding.ivWallet.setOnClickListener {
            WalletWebViewActivity.launch(this, false)
        }

        binding.tvSendCode.setOnClickListener {
            val account = binding.edtAccount.text.toString()
            lifecycleScope.launch {
                val isSend = DAuthSDK.instance.sendEmailVerifyCode(account)
                if (isSend) {
                    DAuthLogger.d("验证码发送成功")
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            TWITTER_REQUEST_CODE -> {
                TwitterLoginManager.instance.onActivityResult(requestCode, resultCode, data)
            }
            GOOGLE_REQUEST_CODE -> {
                GoogleLoginManager.instance.onActivityResult(data)
            }
        }
    }


}