package com.cyberflow.dauthsdk.mpc

import androidx.annotation.Keep
import com.cyberflow.dauthsdk.api.DAuthSDK
import com.cyberflow.dauthsdk.login.utils.DAuthLogger
import com.cyberflow.dauthsdk.login.utils.LoginPrefs
import com.cyberflow.dauthsdk.mpc.ext.runSpending
import com.cyberflow.dauthsdk.wallet.util.cleanHexPrefix
import com.cyberflow.dauthsdk.wallet.util.prependHexPrefix
import com.cyberflow.dauthsdk.wallet.util.sha3
import com.cyberflow.dauthsdk.wallet.util.sha3String
import com.cyberflow.dauthsdk.wallet.util.toHexString
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import kotlin.random.Random

private const val TAG = "DAuthJniInvoker"

private fun ByteArray.printable(): String {
    val sb = StringBuilder()
    for (b in this) {
        sb.append(String.format("%02X ", b))
    }
    return sb.toString()
}

/**
 * 使用kotlin进行调用，方便使用内联函数
 */
object DAuthJniInvoker {
    private const val THRESHOLD = 2
    private const val PARTIES = 3
    private val jni by lazy { DAuthJni.getInstance() }
    private val keystore get() = MpcKeyStore

    fun initialize(){

        Thread {
            DAuthLogger.d(">>> thread", TAG)
            initializeInner()
            DAuthLogger.d("<<< thread", TAG)
        }.let {
            it.name = "DAuthJniInvoker"
            it.start()
        }
    }

    private fun initializeInner() {
        jni.init()

        val keyInSp = MpcKeyStore.getAllKeys()/*.let { emptyList<String>() }*/
        val keys = if (keyInSp.isEmpty()) {
            runSpending(TAG, "generateSignKeys") {
                generateSignKeys().also {
                    keystore.setAllKeys(it.toList())
                }
            }
        } else {
            keyInSp.toTypedArray()
        }
        /*runSpending(TAG, "refreshKeys") {
            jni.refreshKeys(MpcKeyIds.getKeyIds(), keys)
        }*/

        val msg = genRandomMsg()
        val msgHash = genRandomMsg().sha3String().cleanHexPrefix()

        /*runSpending(TAG, "localSign") {
            val account = generateEoaAddress(msg, keys)
            DAuthLogger.d("localSign account=$account", TAG)
        }*/

        /*runSpending("remoteSignMsg") {
            DAuthLogger.d("remoteSignMsg msgHash=$msgHash", TAG)
            val byteArrayList = ArrayList<JniOutBuffer>()
            jni.remoteSignMsg(
                msgHash,
                keys[0],
                MpcKeyIds.getLocalId(),
                MpcKeyIds.getRemoteIdsToSign(),
                byteArrayList
            )
            val returnedBytes = if (byteArrayList.isNotEmpty()){
                byteArrayList.first().bytes
            } else{
                null
            }
            DAuthLogger.d("outBuffer[${returnedBytes?.size}]=${returnedBytes?.printable()}", TAG)
        }*/

        // 模拟多轮签名
        val u1 = CoSignerUser("coSigner1", msgHash, keys[0], MpcKeyIds.getLocalId(), MpcKeyIds.getRemoteIdsToSign())
        val u2 = CoSignerUser("coSigner2", msgHash, keys[1], MpcKeyIds.getRemoteIdsToSign(), MpcKeyIds.getLocalId())

        val out1: ByteArray = u1.startRemoveSign()
        val out2: ByteArray = u2.startRemoveSign()

        var temp1 : Pair<Boolean, ByteArray>? = null
        var temp2 : Pair<Boolean, ByteArray>? = null

        var result1 : Pair<Boolean, ByteArray> = false to out1
        var result2 : Pair<Boolean, ByteArray> = false to out2

        var index = 1

        while (true) {
            DAuthLogger.v("poll ${index++}", TAG)

            if (!result1.first){
                temp2 = u2.signRound(result1.second)
            }
            if (!result2.first){
                temp1 = u1.signRound(result2.second)
            }

            result2 = temp2!!
            result1 = temp1!!

            // 都完成就退出
            if (result2.first && result1.first){
                break
            }
        }

        val r1 = String(result1.second)
        val r2 = String(result2.second)
        DAuthLogger.d("\ncoSignResult1=${r1}\ncoSignResult2=${r2}", TAG)
        DAuthLogger.d("equals=${r1==r2}", TAG)
    }

    fun localSignMsg(msgHash: String, keys: Array<String>): SignResult? {
        val keyIds = MpcKeyIds.getKeyIds()
        DAuthLogger.d("localSignMsg: ${keyIds.contentToString()}", TAG)
        val signedResultJson = jni.localSignMsg(
            msgHash.cleanHexPrefix(),
            keyIds.take(2).toTypedArray(),
            keys.take(2).toTypedArray()
        )
        DAuthLogger.d("localSignMsg result=$signedResultJson", TAG)
        return signedResultJson.toSignResult()
    }

    fun getWalletAddress(msgHash: ByteArray, sd: SignatureData): String {
        val signedPublicKey = Sign.signedMessageHashToKey(msgHash, sd)
        return Keys.getAddress(signedPublicKey).prependHexPrefix()
    }

    fun generateEoaAddress(msg: ByteArray, keys: Array<String>): String? {
        val msgHash = msg.sha3()
        val msgHashHex = msgHash.toHexString()
        val signResult = localSignMsg(msgHashHex, keys)
        if (signResult == null) {
            DAuthLogger.e("signResult null", TAG)
            return null
        }
        val sd = signResult.toSignatureData()
        return getWalletAddress(msgHash, sd)
    }

    /**
     * 生成随机消息。为了执行[DAuthJni.localSignMsg]
     */
    fun genRandomMsg(): String{
        val seed = System.currentTimeMillis()
        val random = Random(seed)
        val long = random.nextLong()
        return long.toString()
    }

    fun String.toSignResult() = try {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(SignResult::class.java)
        adapter.fromJson(this)
    } catch (e: Exception) {
        DAuthLogger.e(e.stackTraceToString(), TAG)
        null
    }

    fun generateSignKeys(): Array<String> {
        // 3签2
        val authId = LoginPrefs(DAuthSDK.impl.context).getAuthId()
        if (authId.isEmpty()) {
            DAuthLogger.e("auth id empty", TAG)
            return emptyArray()
        }

        val ids = MpcKeyIds.getKeyIds()
        val r = jni.generateSignKeys(THRESHOLD, PARTIES, ids)
        DAuthLogger.d("----generateSignKeys----", TAG)
        /*r.forEachIndexed { index, s ->
            DAuthLogger.d("key$index) len=${s.length}\n$s", TAG)
        }*/
        return r
    }
}

@Keep
class SignResult(
    private val rh: String,
    private val sh: String,
    private val r: String,
    private val s: String,
    private val v: Int
) {
    fun toSignatureData(): SignatureData {
        val r = rh.hexStringToByteArray()
        val s = sh.hexStringToByteArray()
        // MPC.so返回的结果为0或者1
        // 但是以太坊的合法值范围是[27, 34]
        // 参考方法signedMessageHashToKey
        val vByte = (v + 27).toByte()
        return SignatureData(vByte, r, s)
    }

    private fun String.hexStringToByteArray(): ByteArray {
        val hexChars = this.toCharArray()
        val result = ByteArray(this.length / 2)
        for (i in 0 until this.length step 2) {
            val firstNibble = Character.digit(hexChars[i], 16)
            val secondNibble = Character.digit(hexChars[i + 1], 16)
            val byteValue = firstNibble shl 4 or secondNibble
            result[i / 2] = byteValue.toByte()
        }
        return result
    }

    override fun toString(): String {
        return "SignResult(rh='$rh', sh='$sh', r='$r', s='$s', v=$v)"
    }
}