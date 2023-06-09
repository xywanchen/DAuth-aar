package com.cyberflow.dauthsdk


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cyberflow.dauth.databinding.ActivityMainLayoutBinding
import com.cyberflow.dauthsdk.login.DAuthSDK
import com.cyberflow.dauthsdk.login.utils.DAuthLogger

import java.math.BigInteger


class MainActivity : AppCompatActivity() {
    private val testAddress = "0x7F0466E1e43A6d00d27C2111B0aa9BA0c52ACA5D"
    companion object {
        fun launch(context : Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    private var mainBinding: ActivityMainLayoutBinding?  = null
    private val binding: ActivityMainLayoutBinding get() = mainBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainLayoutBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
        //testWeb3j()
    }

    private fun initView() {
        binding.btnQueryBalance.setOnClickListener {
            val balance = DAuthSDK.instance.queryWalletBalance()
            Toast.makeText(this,"钱包余额：$balance",Toast.LENGTH_LONG).show()
        }
        binding.btnGas.setOnClickListener {
            val gas = DAuthSDK.instance.estimateGas(testAddress, BigInteger("100"))
            Toast.makeText(this,"gas费预估：$gas",Toast.LENGTH_LONG).show()
        }
        binding.btnSendTransaction.setOnClickListener {
           val result = DAuthSDK.instance.sendTransaction(testAddress, BigInteger("100"))
            Toast.makeText(this,"转账结果：${result.toString()}",Toast.LENGTH_LONG).show()
        }
    }

    private fun testWeb3j() {
        val sdk = DAuthSDK.instance
        val address = sdk.queryWalletAddress()
        DAuthLogger.d("address=$address")
        val balance = sdk.queryWalletBalance()
        DAuthLogger.d("balance=$balance")
        val to = "0x386F221660f58157aa05C107dDae69295316d82D"
        val amount = BigInteger("10")
        val estimateGas = sdk.estimateGas(to, amount)
        DAuthLogger.d("estimateGas=$estimateGas")
        val gasUsed = sdk.sendTransaction(to, amount)
        DAuthLogger.d("gasUsed=$gasUsed")
    }

    override fun onDestroy() {
        super.onDestroy()
        DAuthLogger.d("MainActivity onDestroy")
    }
}