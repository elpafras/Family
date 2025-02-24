package org.sabda.family.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<T : ViewBinding> : Fragment() {
    private var _binding: T? = null
    protected val binding get() = _binding!!

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T
    open fun onBackPressed() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBinding(inflater, container)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (parentFragmentManager.backStackEntryCount > 0) {
                    parentFragmentManager.popBackStack()
                } else {
                    handleExit()
                }
            }
        })

        return binding.root
    }

    private fun handleExit() {
        if (shouldCloseApp()) {
            requireActivity().finish() // Menutup aplikasi jika fragment terakhir
        } else {
            onBackPressed() // Panggil metode yang bisa di-override di fragment lain
        }
    }

    open fun shouldCloseApp(): Boolean {
        // Default-nya hanya menutup jika tidak ada fragment lain di back stack
        return parentFragmentManager.backStackEntryCount == 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}