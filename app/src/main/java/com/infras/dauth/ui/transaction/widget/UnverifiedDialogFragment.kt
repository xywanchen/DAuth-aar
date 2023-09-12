package com.infras.dauth.ui.transaction.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.infras.dauth.databinding.DialogFragmentUnverifiedBinding
import com.infras.dauth.ext.setDebouncedOnClickListener
import com.infras.dauth.ui.transaction.KycSubmitActivity
import com.infras.dauth.widget.dialog.BottomDialogFragment

class UnverifiedDialogFragment : BottomDialogFragment() {

    companion object {
        const val TAG = "UnverifiedDialogFragment"
        private const val EXTRA_USER_ID = "EXTRA_USER_ID"

        fun newInstance(userId: String): UnverifiedDialogFragment {
            return UnverifiedDialogFragment().also {
                it.arguments = Bundle().apply { putString(EXTRA_USER_ID, userId) }
            }
        }
    }

    private var _binding: DialogFragmentUnverifiedBinding? = null
    private val binding get() = _binding!!
    private val userId get() = requireArguments().getString(EXTRA_USER_ID)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentUnverifiedBinding.inflate(inflater, container, false)
        binding.tvDauthId.text = userId.toString()
        binding.tvLatter.setDebouncedOnClickListener {
            dismiss()
        }
        binding.tvGetVerified.setDebouncedOnClickListener {
            dismiss()
            KycSubmitActivity.launch(it.context)
        }
        return binding.root
    }
}