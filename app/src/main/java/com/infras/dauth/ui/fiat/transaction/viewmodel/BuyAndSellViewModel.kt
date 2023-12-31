package com.infras.dauth.ui.fiat.transaction.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.infras.dauth.app.BaseViewModel
import com.infras.dauth.entity.BuyAndSellPageEntity
import com.infras.dauth.entity.KycBundledState
import com.infras.dauth.manager.AppManagers
import com.infras.dauth.manager.StorageDir
import com.infras.dauth.repository.FiatTxRepository
import com.infras.dauth.ui.fiat.transaction.util.CurrencyCalcUtil
import com.infras.dauth.util.MoshiUtil
import com.infras.dauthsdk.login.model.CurrencyPriceParam
import com.infras.dauthsdk.login.model.CurrencyPriceRes
import com.infras.dauthsdk.login.model.DigitalCurrencyListRes
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode


class BuyAndSellViewModel : BaseViewModel() {

    private var _kycBundledState = MutableLiveData<KycBundledState>()
    val kycBundledState: LiveData<KycBundledState> = _kycBundledState

    private var tokenInfoList: List<BuyAndSellPageEntity.TokenInfo> = mutableListOf()
    private var fiatList: List<DigitalCurrencyListRes.FiatInfo> = mutableListOf()
    private var priceInfo: List<CurrencyPriceRes.PriceInfo> = mutableListOf()

    private val _pageEntity = mutableStateOf(generatePageEntity())
    val pageEntity: State<BuyAndSellPageEntity> = _pageEntity

    private val repo = FiatTxRepository()

    fun fetchAccountDetail() = viewModelScope.launch {
        val r = repo.accountDetail()
        if (r != null && r.isSuccess()) {
            r.data?.let {
                val newValue = KycBundledState(
                    kycState = it.detail?.state,
                    isBound = it.is_phone_bound == 1 || it.is_email_bound == 1
                )
                _kycBundledState.value = newValue
            }
        }
    }


    fun fetchCurrencyList() = viewModelScope.launch {
        val r = showLoading { repo.currencyList() }
        if (r != null && r.isSuccess()) {
            val data = r.data ?: return@launch

            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                CurrencyCalcUtil.setCurrencyList(data)
            }

            val cryptoList = data.cryptoList ?: listOf()
            tokenInfoList = cryptoList.map {
                BuyAndSellPageEntity.TokenInfo(
                    name = it.cryptoCode.orEmpty(),
                    issuer = it.cryptoIssuer.orEmpty(),
                    avatarUrl = it.cryptoIcon.orEmpty(),
                    changeRange = "",
                    "",
                    it
                )
            }
            fiatList = data.fiatList ?: listOf()
            _pageEntity.value = generatePageEntity()

            fetchPrice(fiatList, cryptoList)
        }
    }

    private fun generatePageEntity(changeFiatIndexTo: Int? = null): BuyAndSellPageEntity {
        val dstFiatIndex: Int?
        val newTokenInfoList: List<BuyAndSellPageEntity.TokenInfo>
        if (fiatList.isEmpty()) {
            dstFiatIndex = null
            newTokenInfoList = tokenInfoList
        } else {
            dstFiatIndex = changeFiatIndexTo ?: 0
            val dstFiatCode = fiatList[dstFiatIndex].fiatCode
            val fiatSymbol = fiatList[dstFiatIndex].fiatSymbol
            val cryptoPriceMap = priceInfo.asSequence().filter {
                it.fiatCode == dstFiatCode
            }.associateBy { it.cryptoCode.orEmpty() }.toMap()

            val m = tokenInfoList.map { tokenInfo ->
                val priceFound = cryptoPriceMap[tokenInfo.crypto.cryptoCode]
                if (priceFound != null) {
                    val price = priceFound.price.orEmpty()
                    val cryptoPrecision = tokenInfo.crypto.cryptoPrecision
                    val scaled = kotlin.runCatching {
                        val decimal =
                            BigDecimal(price).setScale(cryptoPrecision, RoundingMode.HALF_UP)
                        "$fiatSymbol $decimal"
                    }.getOrNull() ?: ""
                    tokenInfo.copy(price = scaled)
                } else {
                    tokenInfo
                }
            }
            newTokenInfoList = mutableListOf<BuyAndSellPageEntity.TokenInfo>().also { it.addAll(m) }
        }

        val list = listOf(
            BuyAndSellPageEntity.TokenInfoOfTag(
                tag = BuyAndSellPageEntity.TagsEntity(
                    title = "All",
                    onClick = {}
                ),
                tokenInfoList = newTokenInfoList
            ),
            BuyAndSellPageEntity.TokenInfoOfTag(
                tag = BuyAndSellPageEntity.TagsEntity(
                    title = "Recent",
                    onClick = {}
                ),
                tokenInfoList = listOf()
            ),
        )

        return BuyAndSellPageEntity(
            list,
            fiatList,
            dstFiatIndex,
        )
    }

    fun updateSelect(index: Int) {
        val newValue = generatePageEntity(index)
        _pageEntity.value = newValue
    }

    private suspend fun fetchPrice(
        fiatList: List<DigitalCurrencyListRes.FiatInfo>,
        cryptoList: List<DigitalCurrencyListRes.CryptoInfo>
    ) {
        val r = showLoading {
            repo.currencyPrice(
                CurrencyPriceParam(
                    fiat_list = fiatList.map { it.fiatCode }.toJson(),
                    crypto_list = cryptoList.map { it.cryptoCode }.toJson()
                )
            )
        }
        if (r != null && r.isSuccess()) {
            val list = r.data?.list.orEmpty()
            priceInfo = list
            _pageEntity.value = generatePageEntity()
        }
    }

    private fun List<String?>.toJson(): String {
        return kotlin.runCatching {
            JSONArray().also { ja ->
                this.filterNotNull()
                    .forEach { e ->
                        ja.put(e)
                    }
            }.toString()
        }.getOrNull().orEmpty()
    }
}