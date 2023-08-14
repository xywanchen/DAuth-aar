package com.infras.dauthsdk.wallet.impl.manager.task

import com.infras.dauthsdk.api.entity.CreateWalletData
import com.infras.dauthsdk.api.entity.DAuthResult
import com.infras.dauthsdk.api.entity.DAuthResult.Companion.SDK_ERROR_AA_ADDRESS_INVALID
import com.infras.dauthsdk.api.entity.DAuthResult.Companion.SDK_ERROR_CANNOT_GENERATE_ADDRESS
import com.infras.dauthsdk.api.entity.ResponseCode
import com.infras.dauthsdk.api.entity.traceResult
import com.infras.dauthsdk.api.entity.transformError
import com.infras.dauthsdk.login.model.BindWalletParam
import com.infras.dauthsdk.login.model.GetParticipantsRes
import com.infras.dauthsdk.login.model.LogReportParam
import com.infras.dauthsdk.login.network.MpcServiceConst.MpcSecretAlreadyBoundError
import com.infras.dauthsdk.login.network.RequestApiMpc
import com.infras.dauthsdk.login.utils.DAuthLogger
import com.infras.dauthsdk.login.utils.LoginPrefs
import com.infras.dauthsdk.mpc.DAuthJniInvoker
import com.infras.dauthsdk.mpc.MpcKeyIds
import com.infras.dauthsdk.mpc.ext.ElapsedContext
import com.infras.dauthsdk.mpc.util.MergeResultUtil
import com.infras.dauthsdk.wallet.ext.digest
import com.infras.dauthsdk.wallet.impl.manager.Managers
import com.infras.dauthsdk.wallet.impl.manager.WalletManager
import com.infras.dauthsdk.wallet.util.AssertUtil
import com.infras.dauthsdk.wallet.util.ThreadUtil

private const val TAG = "CreateWalletTask"

internal class CreateWalletTask(
    private val context: ElapsedContext,
    private val loginPrefs: LoginPrefs,
    private val participants: List<GetParticipantsRes.Participant>,
) {
    private val mpcApi: RequestApiMpc = Managers.requestApiMpc
    private val keystore get() = Managers.mpcKeyStore
    private val walletPrefsV2 get() = Managers.walletPrefsV2
    private val requestApi = Managers.requestApi

    suspend fun execute(): DAuthResult<CreateWalletData> {
        ThreadUtil.assertInMainThread(false)
        val state = context.runSpending("getWalletState") { walletPrefsV2.getWalletState() }
        AssertUtil.assert(state < WalletManager.STATE_OK)

        val allKeys = context.runSpending("getAllKeys") { keystore.getAllKeys() }
        val keySize = allKeys.size
        val keys = when (keySize) {
            MpcKeyIds.getKeyIds().size -> {
                // 使用未提交的密钥
                DAuthLogger.e("keys to be submit", TAG)
                allKeys
            }
            else -> {
                // 创建三片
                val generated = context.runSpending("generate keys") {
                    val preGenerateKeyManager = Managers.preGenerateKeyManager
                    val popped = preGenerateKeyManager.popKeys()
                    if (popped.size == MpcKeyIds.getKeyIds().size) {
                        DAuthLogger.d("use pre-generated", TAG)
                        popped
                    } else {
                        DAuthLogger.d("generate now", TAG)
                        DAuthJniInvoker.generateSignKeys().toList()
                    }
                }

                context.runSpending("save keys") {
                    DAuthLogger.d("save keys:${generated.map { it.length }}", TAG)
                    keystore.setAllKeys(generated)
                    walletPrefsV2.setWalletState(WalletManager.STATE_KEY_GENERATED)
                }

                generated
            }
        }.toTypedArray()

        // 秘钥求和
        val mergeResult = context.runSpending("merge keys") {
            MergeResultUtil.encodeKey(keys)
        }
        if (mergeResult.isNullOrEmpty()) {
            DAuthLogger.e("mergeResult is empty", TAG)
            return DAuthResult.SdkError(DAuthResult.SDK_ERROR_MERGE_RESULT)
        }

        val mergeResultLen = mergeResult.length
        DAuthLogger.d("key merge len=$mergeResultLen", TAG)
        if (mergeResultLen == 0) {
            return DAuthResult.SdkError(DAuthResult.SDK_ERROR_MERGE_RESULT)
        }

        // 生成账号
        val signerAddress = context.runSpending("generateEoaAddress") {
            val msg = DAuthJniInvoker.genRandomMsg()
            DAuthJniInvoker.generateEoaAddress(msg.toByteArray(), keys).also {
                DAuthLogger.i("generateEoaAddress $it", TAG)
            }
        } ?: return DAuthResult.SdkError(SDK_ERROR_CANNOT_GENERATE_ADDRESS)
        val aaAddressResult = context.runSpending("generateAAAddress") {
            // 根据EOA地址获取AA地址
            val web3m = Managers.web3m
            web3m.getAaAddressByEoaAddress(signerAddress)
                .also { it.traceResult(TAG, "generateAAAddress") }
        }
        if (aaAddressResult !is DAuthResult.Success) {
            return aaAddressResult.transformError()
        }
        val aaAddress = aaAddressResult.data
        if (aaAddress.length <= 2) {
            DAuthLogger.e("aa address too short")
            return DAuthResult.SdkError(SDK_ERROR_AA_ADDRESS_INVALID)
        }

        val r = submitWalletAddressAndKeys(
            keys,
            signerAddress,
            aaAddress,
            context,
            mergeResult,
        )

        context.traceElapsedList()
        return r
    }

    private suspend fun submitWalletAddressAndKeys(
        keys: Array<String>,
        eoaAddress: String,
        aaAddress: String,
        context: ElapsedContext,
        mergeResult: String,
    ): DAuthResult<CreateWalletData> {
        // 绑定钱包地址
        val accessToken = loginPrefs.getAccessToken()
        val authId = loginPrefs.getAuthId()
        DAuthLogger.d("auth id=${authId}")
        val dauthKey = keys[MpcKeyIds.KEY_INDEX_DAUTH_SERVER]
        val bindWalletParam = BindWalletParam(
            accessToken, authId, aaAddress, BindWalletParam.WALLET_TYPE_AA,
            dauthKey,
            mergeResult
        )
        val bindResponse = context.runSpending("bindWallet") {
            requestApi.bindWallet(bindWalletParam)
        }
        if (bindResponse == null) {
            DAuthLogger.e("bind network error", TAG)
            return DAuthResult.SdkError(DAuthResult.SDK_ERROR_BIND_WALLET)
        }
        if (!bindResponse.isSuccess()) {
            DAuthLogger.e("bind error:${bindResponse.info}", TAG)
            return DAuthResult.SdkError(DAuthResult.SDK_ERROR_BIND_WALLET)
        }

        // 把密钥分发给远端
        DAuthLogger.d("dispatch keys", TAG)

        participants.filter {
            it.id > MpcKeyIds.KEY_INDEX_LOCAL && it.id <= MpcKeyIds.KEY_INDEX_APP_SERVER
        }.forEach { each ->
            val keyId = each.id
            DAuthLogger.d("set key $keyId", TAG)
            val participantDAuth = participants[keyId]
            val setKeyUrl = participantDAuth.set_key_url
            val key = keys[keyId]
            val finalMergeResult = mergeResult.takeIf { keyId == MpcKeyIds.KEY_INDEX_DAUTH_SERVER }
            val p = context.runSpending("dispatch keys $keyId") {
                mpcApi.setKey(setKeyUrl, key, finalMergeResult)
            }
            DAuthLogger.d("set key $keyId ${key.digest()} ret=${p?.ret}", TAG)
            if (p == null) {
                DAuthLogger.e("set key $keyId network error", TAG)
                return DAuthResult.SdkError(DAuthResult.SDK_ERROR_SET_KEY)
            }
            when (p.ret) {
                ResponseCode.RESPONSE_CORRECT_CODE -> {
                    DAuthLogger.e("success", TAG)
                }

                MpcSecretAlreadyBoundError -> {
                    DAuthLogger.e("already bound", TAG)
                }

                else -> {
                    DAuthLogger.e("set key $keyId error:${p.info}", TAG)
                    return DAuthResult.SdkError(DAuthResult.SDK_ERROR_SET_KEY)
                }
            }
        }

        DAuthLogger.d("release temp keys", TAG)
        context.runSpending("releaseTempKeys") {
            keystore.releaseTempKeys()
        }

        val r = context.runSpending("save address") {
            if (walletPrefsV2.setAddresses(eoaAddress, aaAddress)) {
                DAuthResult.Success(CreateWalletData(aaAddress))
            } else {
                DAuthLogger.e("sp error", TAG)
                DAuthResult.SdkError(DAuthResult.SDK_ERROR_UNKNOWN)
            }
        }
        DAuthLogger.d("submit result=$r", TAG)
        return r
    }


}