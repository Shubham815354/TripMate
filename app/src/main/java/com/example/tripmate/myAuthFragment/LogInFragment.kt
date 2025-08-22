package com.example.tripmate.myAuthFragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.tripmate.MyAuthMVVM.UserRepository
import com.example.tripmate.MyAuthMVVM.UserViewModel
import com.example.tripmate.MyAuthMVVM.UserViewModelFactory
import com.example.tripmate.R
import com.example.tripmate.databinding.FragmentLogInBinding
import com.example.tripmate.myActivity.MainActivity2

class LogInFragment : Fragment() {
lateinit var binding: FragmentLogInBinding
lateinit var viewmodel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
         binding = DataBindingUtil.inflate(inflater,R.layout.fragment_log_in,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sign.text = createSpan()
        binding.sign.setOnClickListener {
            findNavController().navigate(R.id.action_logInFragment_to_signUpFragment)
        }
        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                val isValid = isValidEmail(email)

                if (!isValid) {
                    binding.emailConditions.visibility = View.VISIBLE
                    binding.emailConditions.text = "• Enter a valid email address (e.g., abc@example.com)"
                    binding.emailConditions.setTextColor(Color.RED)
                } else {
                    binding.emailConditions.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        val repository = UserRepository()
        val factory = UserViewModelFactory(repository)
        viewmodel = ViewModelProvider(this,factory)[UserViewModel::class.java]
        binding.logButton.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.pass.text.toString()
            viewmodel.hitlogin(email,password)
            binding.progress.visibility = View.VISIBLE
        }
        viewmodel.user.observe(viewLifecycleOwner){ userlist ->
            binding.progress.visibility = View.GONE
            userlist.onSuccess{
                startActivity(Intent(requireContext(), MainActivity2::class.java))
            }
            userlist.onFailure { userlist ->
                makeToast("Wrong Credentials")
            }

        }
        binding.forgot.setOnClickListener {
            findNavController().navigate(R.id.action_logInFragment_to_forgotFragment)
        }


    }


}

fun Fragment.makeToast(message:String){
    Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
}

fun createSpan(): CharSequence{
    val fullText = "Don't have an account? Sign Up"
    val spannable = SpannableString(fullText)

    val blackPart = "Don't have an account? "
    val bluePart = "Sign Up"

    // Set black for the first part
    spannable.setSpan(
        ForegroundColorSpan(Color.BLACK),
        0,
        blackPart.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    // Set blue for the second part
    spannable.setSpan(
        ForegroundColorSpan(Color.parseColor("#2196F3")), // Material Blue
        blackPart.length,
        fullText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannable

}
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}