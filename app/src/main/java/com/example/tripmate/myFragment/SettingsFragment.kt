package com.example.tripmate.myFragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tripmate.MyAuthMVVM.UserRepository
import com.example.tripmate.MyAuthMVVM.UserViewModel
import com.example.tripmate.MyAuthMVVM.UserViewModelFactory
import com.example.tripmate.R
import com.example.tripmate.SplashScreen
import com.example.tripmate.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    lateinit var binding: FragmentSettingsBinding
    lateinit var viewmodel : UserViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         binding = DataBindingUtil.inflate(inflater,R.layout.fragment_settings,container , false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = UserRepository()
        val factory = UserViewModelFactory(repository)
        viewmodel = ViewModelProvider(this,factory)[UserViewModel::class.java]
        viewmodel.getUserName()

            viewmodel.userName.observe(viewLifecycleOwner) { result ->
                result.onSuccess { name ->
                    binding.userName.text = "Hi $name"
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }

        viewmodel.logout.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                // Navigate to login screen
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), SplashScreen::class.java))
                // Example: findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }

        binding.logOut.setOnClickListener {
            viewmodel.hitLogout()
        }



    }


}