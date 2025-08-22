package com.example.tripmate.myAuthFragment

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tripmate.MyAuthMVVM.UserRepository
import com.example.tripmate.MyAuthMVVM.UserViewModel
import com.example.tripmate.MyAuthMVVM.UserViewModelFactory
import com.example.tripmate.R
import com.example.tripmate.databinding.FragmentSignUpBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {
lateinit var binding : FragmentSignUpBinding
lateinit var viewmodel : UserViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_sign_up,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.log.text = spanCreation()
        binding.log.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_logInFragment)
        }
        binding.passSign.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val conditionsText = StringBuilder()

                if (!password.any { it.isUpperCase() }) {
                    conditionsText.append("• At least 1 uppercase letter\n")
                }
                if (!password.any { it.isLowerCase() }) {
                    conditionsText.append("• At least 1 lowercase letter\n")
                }
                if (!password.any { it.isDigit() }) {
                    conditionsText.append("• At least 1 digit\n")
                }
                if (!password.any { "!@#\$%^&*()-_=+[]{};:'\",.<>?/\\|`~".contains(it) }) {
                    conditionsText.append("• At least 1 special character\n")
                }
                if (password.length < 8) {
                    conditionsText.append("• Minimum 8 characters\n")
                }

                if (conditionsText.isNotEmpty()) {
                    binding.passwordConditions.visibility = View.VISIBLE
                    binding.passwordConditions.text = "Password must contain:\n$conditionsText"
                    binding.passwordConditions.setTextColor(Color.RED)
                } else {
                    binding.passwordConditions.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.emailSign.addTextChangedListener(object : TextWatcher {
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

        binding.firstname.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val firstName = s.toString()
                val isValid = isValidName(firstName)

                if (!isValid) {
                    binding.lastNameConditions.visibility = View.VISIBLE
                    binding.lastNameConditions.text = "• Only letters allowed, min 4 characters"
                } else {
                    binding.lastNameConditions.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.lastnme.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val lastName = s.toString()
                val isValid = isValidName(lastName)

                if (!isValid) {
                    binding.lastNameConditions.visibility = View.VISIBLE
                    binding.lastNameConditions.text = "• Only letters allowed, min 4 characters"
                } else {
                    binding.lastNameConditions.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val repository = UserRepository()
        val factory = UserViewModelFactory(repository)
        viewmodel = ViewModelProvider(this,factory)[UserViewModel::class.java]

        binding.verifyButton.setOnClickListener {
            val first = binding.firstname.text.toString()
            val second = binding.lastnme.text.toString()
            val email = binding.emailSign.text.toString()
            val password = binding.passSign.text.toString()
            val repass = binding.repassSign.text.toString()
            if(password!=repass){
                binding.passwordConditions.visibility = View.VISIBLE
                binding.passwordConditions.text="Password And Re-Enter Password MisMatched"
                return@setOnClickListener
            }else if(first.isEmpty() || second.isEmpty() || email.isEmpty() || password.isEmpty() || repass.isEmpty()){
                makeToast("All * Fields Are Mandatory")
                return@setOnClickListener
            }
            binding.passwordConditions.visibility = View.GONE
            viewmodel.hitSignup(first,second,email,password)
            binding.progressBar.visibility = View.VISIBLE
        }

        viewmodel.user.observe(viewLifecycleOwner) { userList ->
            binding.progressBar.visibility = View.GONE
            userList.onSuccess {
                lifecycleScope.launch {
                    makeToast("Verification Link Send To Email")
                    delay(1000)
                    findNavController().navigate(R.id.action_signUpFragment_to_logInFragment)
                }

            }
            userList.onFailure { userlist ->
                makeToast(userlist.message.toString())
            }
        }


    }



}
fun spanCreation(): CharSequence {
     val fullText = "Already have an account? Log In"
    val spannable = SpannableString(fullText)
    val blackPart = "Already have an account? "
    val bluePart = "Log In"
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
private fun isValidName(name: String): Boolean {
    val namePattern = Regex("^[A-Za-z]{4,}$")
    return namePattern.matches(name)
}


private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun isStrongPassword(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$")
    return passwordPattern.matches(password)
}

private fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}
