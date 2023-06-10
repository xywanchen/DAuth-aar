package com.cyberflow.dauthsdk.wallet.impl

import android.content.Context
import com.cyberflow.dauthsdk.api.entity.CreateWalletResult
import com.cyberflow.dauthsdk.api.entity.EstimateGasResult
import com.cyberflow.dauthsdk.api.entity.GetAddressResult
import com.cyberflow.dauthsdk.api.entity.GetBalanceResult
import com.cyberflow.dauthsdk.api.entity.SendTransactionResult
import com.cyberflow.dauthsdk.login.utils.DAuthLogger
import com.cyberflow.dauthsdk.api.IWalletApi
import kotlinx.coroutines.*
import java.math.BigInteger

/**
 * 苟建的账号: 0xbDA5747bFD65F08deb54cb465eB87D40e51B197E (10000 ETH)
 * Private Key: 0x689af8efa8c651a91ad287602527f3af2fe9f6501a7ac4b061667b5a93e037fd
 * 有钱的账号：0x23618e81E3f5cdF7f54C3d65f7FBc0aBf5B21E8f
 */
class DAuthWallet internal constructor() : IWalletApi {

    override fun initWallet(context: Context) {
    }

    override suspend fun createWallet(passcode: String?): CreateWalletResult {
        return CreateWalletResult.CreateWalletFailure
    }

    override suspend fun queryWalletAddress(): GetAddressResult {
        val address = Web3Manager.invokeGetAcc().orEmpty()
        return if (address.isEmpty()) {
            GetAddressResult.Failure
        } else {
            GetAddressResult.Success(address)
        }
    }

    override suspend fun queryWalletBalance(): GetBalanceResult {
        val addressResult = queryWalletAddress()
        if (addressResult !is GetAddressResult.Success){
            return GetBalanceResult.Failure
        }
        val address = addressResult.address
        return Web3Manager.getBalance(address).takeIf {
            address.isNotEmpty()
        }?.let { GetBalanceResult.Success(it) } ?: GetBalanceResult.Failure
    }

    override suspend fun estimateGas(toUserId: String, amount: BigInteger): EstimateGasResult {
        return EstimateGasResult.Failure
    }

    override suspend fun sendTransaction(toAddress: String, amount: BigInteger): SendTransactionResult {
        runBlocking {
            val address = queryWalletAddress()
            val queryAddress = toAddress
            DAuthLogger.d("sendTransaction: $queryAddress")
            if (queryAddress.isNotEmpty()) {
                val tx = Web3Manager.invokeTestTemp(
                    queryAddress,
                    amount
                )
                DAuthLogger.d("tx: $tx")
            }
        }
        return SendTransactionResult.CannotFetchTransactionCount
    }

}