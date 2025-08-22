package com.example.tripmate.myAuthFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tripmate.MyAuthMVVM.UserRepository
import com.example.tripmate.MyAuthMVVM.UserViewModel
import com.example.tripmate.MyAuthMVVM.UserViewModelFactory
import com.example.tripmate.R
import com.example.tripmate.databinding.FragmentForgotBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgotFragment : Fragment() {
    lateinit var binding: FragmentForgotBinding
    lateinit var viewModel: UserViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_forgot,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logButton.setOnClickListener {
                  val email = binding.editTextName.text.toString()
                  if(email.isEmpty()){
                      Toast.makeText(requireContext(),"Email is mandatory",Toast.LENGTH_SHORT).show()
                  }else{
                      val repository = UserRepository()
                      val factory = UserViewModelFactory(repository)
                      viewModel = ViewModelProvider(this,factory)[UserViewModel::class.java]
                      viewModel.hitreset(email)
                      viewModel.user.observe(viewLifecycleOwner){ userlist ->
                          userlist.onSuccess {
                              lifecycleScope.launch {
                                  makeToast("Reset Link Send To Email")
                                  delay(5000)
                                  findNavController().navigate(R.id.action_forgotFragment_to_logInFragment)
                              }

                          }
                          userlist.onFailure { userlist ->
                              makeToast(userlist.message.toString())

                          }
                      }

              }
        }

        binding.backToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_forgotFragment_to_logInFragment)
        }


    }


}