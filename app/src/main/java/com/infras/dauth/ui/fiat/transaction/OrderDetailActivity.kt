package com.infras.dauth.ui.fiat.transaction

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.infras.dauth.R
import com.infras.dauth.app.BaseActivity
import com.infras.dauth.databinding.ActivityOrderDetailBinding
import com.infras.dauth.entity.FiatOrderState
import com.infras.dauth.ext.launch
import com.infras.dauth.ext.setDebouncedOnClickListener
import com.infras.dauth.manager.AppManagers
import com.infras.dauth.repository.FiatTxRepository
import com.infras.dauth.ui.fiat.transaction.fragment.OrderDetailCancelFragment
import com.infras.dauth.ui.fiat.transaction.fragment.OrderDetailCompleteFragment
import com.infras.dauth.ui.fiat.transaction.fragment.OrderDetailDisputeFragment
import com.infras.dauth.ui.fiat.transaction.fragment.OrderDetailPendingChainFragment
import com.infras.dauth.ui.fiat.transaction.fragment.OrderDetailPendingPayFragment
import com.infras.dauth.ui.fiat.transaction.fragment.OrderDetailPendingReleaseFragment
import com.infras.dauth.ui.fiat.transaction.widget.NeedHelpDialogFragment
import com.infras.dauth.util.ToastUtil
import com.infras.dauth.widget.LoadingDialogFragment
import com.infras.dauthsdk.login.model.OrderAppealParam
import com.infras.dauthsdk.login.model.OrderCancelParam
import com.infras.dauthsdk.login.model.OrderDetailParam
import com.infras.dauthsdk.login.model.OrderDetailRes
import kotlinx.coroutines.launch

class OrderDetailActivity : BaseActivity(), NeedHelpDialogFragment.HelpDialogCallback {

    companion object {
        private const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"
        fun launch(context: Context, orderId: String) =
            context.launch(OrderDetailActivity::class.java) {
                it.putExtra(EXTRA_ORDER_ID, orderId)
            }
    }

    private var _binding: ActivityOrderDetailBinding? = null
    private val binding get() = _binding!!
    private val orderId get() = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
    private val repo = FiatTxRepository()
    private val loadingDialog = LoadingDialogFragment.newInstance()
    private val resourceManager get() = AppManagers.resourceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityOrderDetailBinding.inflate(LayoutInflater.from(this))
        binding.initView()
        setContentView(binding.root)
        refresh()
    }

    private fun ActivityOrderDetailBinding.initView() {
        ivBack.setDebouncedOnClickListener {
            finish()
        }
        binding.tvCancel.setDebouncedOnClickListener {
            lifecycleScope.launch {
                loadingDialog.show(supportFragmentManager, LoadingDialogFragment.TAG)
                val r = repo.orderCancel(OrderCancelParam(orderId))
                loadingDialog.dismissAllowingStateLoss()
                ToastUtil.show(this@OrderDetailActivity, resourceManager.getResponseDigest(r))
                if (r != null && r.isSuccess()) {
                    refresh()
                }
            }
        }
    }

    private fun refresh() {
        if (false) {
            replaceFragment(OrderDetailRes.Data())
        } else {
            lifecycleScope.launch {
                loadingDialog.show(supportFragmentManager, LoadingDialogFragment.TAG)
                val o = if (false) {
                    "1703754270844653568"
                } else orderId
                val r = repo.orderDetail(OrderDetailParam(o))
                loadingDialog.dismissAllowingStateLoss()
                if (r != null && r.isSuccess()) {
                    val data = r.data
                    if (data != null) {
                        replaceFragment(data)
                    }
                } else {
                    ToastUtil.show(
                        this@OrderDetailActivity,
                        resourceManager.getResponseDigest(r)
                    )
                }
            }
        }
    }

    private fun replaceFragment(data: OrderDetailRes.Data) {
        val state = if (false) {
            FiatOrderState.PAID
        } else {
            data.state ?: return
        }
        binding.tvCancel.visibility = View.GONE
        val f = when (state) {
            FiatOrderState.APPEAL -> {
                OrderDetailDisputeFragment.newInstance(data)
            }

            FiatOrderState.UNPAID -> {
                binding.tvCancel.visibility = View.VISIBLE
                OrderDetailPendingPayFragment.newInstance(data)
            }

            FiatOrderState.PAID -> {
                OrderDetailPendingReleaseFragment.newInstance(data)
            }

            FiatOrderState.COMPLETED -> {
                OrderDetailPendingChainFragment.newInstance(data)
            }

            FiatOrderState.WITHDRAW_FAIL, FiatOrderState.WITHDRAW_SUCCESS -> {
                OrderDetailCompleteFragment.newInstance(data)
            }

            FiatOrderState.CANCEL -> {
                OrderDetailCancelFragment.newInstance(data)
            }

            FiatOrderState.CREATE_FAIL -> {
                OrderDetailCompleteFragment.newInstance(data)
            }

            else -> {
                OrderDetailCompleteFragment.newInstance(data)
            }
        }

        supportFragmentManager.beginTransaction().apply {
            replace(
                R.id.cl_fragment_container,
                f
            )
        }.commitAllowingStateLoss()
    }

    override fun onHelpItemClick(index: Int) {
        lifecycleScope.launch {
            loadingDialog.show(supportFragmentManager, LoadingDialogFragment.TAG)
            val r = repo.orderAppeal(OrderAppealParam(orderId))
            loadingDialog.dismissAllowingStateLoss()
            ToastUtil.show(
                this@OrderDetailActivity,
                resourceManager.getResponseDigest(r)
            )
            if (r != null && r.isSuccess()) {
                refresh()
            }
        }
    }
}