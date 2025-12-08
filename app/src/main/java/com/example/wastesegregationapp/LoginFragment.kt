package com.example.wastesegregationapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class LoginFragment : Fragment() {

    // 1. Initialize Firebase Auth
    private lateinit var auth: FirebaseAuth

    // References to UI elements (Verify these IDs in your XML!)
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var createAccountButton: Button
    private lateinit var loginButton: Button

    private val PASSWORD_REQUIREMENTS =
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#\$%^&+=.\\-_*/()<>,?]).{8,}\$".toRegex()
    private fun isPasswordStrictlyValid(password: String): Boolean {
        return PASSWORD_REQUIREMENTS.matches(password)
    }


    private var mainActivity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_login, container, false)

        emailEditText = view.findViewById(R.id.editEmail) // Example ID
        passwordEditText = view.findViewById(R.id.editPassword) // Example ID
        createAccountButton = view.findViewById(R.id.buttonCreateAccount) // Example ID
        loginButton = view.findViewById(R.id.buttonLogin) // Example ID

        createAccountButton.setOnClickListener {
            handleSignUp()
        }

        loginButton.setOnClickListener {
            handleLogin()
        }

        return view
    }

    // sign up
    private fun handleSignUp() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isPasswordStrictlyValid(password)) {
            val errorMessage = """
                Password is weak! Please use a stronger one:
                - At least 8 characters long
                - Contains 1 uppercase letter (A-Z)
                - Contains 1 lowercase letter (a-z)
                - Contains 1 number (0-9)
                - Contains 1 special symbol (!@#$%^&+=)
            """.trimIndent()

            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            // 🔑 IMPORTANT: STOP execution here to prevent weak password registration
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("SignUp", "createUserWithEmail:success")
                    Toast.makeText(requireContext(), "Account created and logged in.", Toast.LENGTH_SHORT).show()

                    // Inform MainActivity to save session state and navigate to the dashboard
                    mainActivity?.saveLoginState(true)
                    mainActivity?.navigateToHome()
                } else {
                    // Handle various sign-up failure reasons
                    handleSignUpFailure(task.exception)
                }
            }
    }

    private fun handleSignUpFailure(exception: Exception?) {
        val message = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Must be at least 6 characters."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            else -> "Account creation failed: ${exception?.localizedMessage ?: "Unknown Error"}"
        }
        Log.e("SignUp", "Error: $message", exception)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

// Login
    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("Login", "signInWithEmail:success")
                    Toast.makeText(requireContext(), "Login successful.", Toast.LENGTH_SHORT).show()

                    mainActivity?.saveLoginState(true)
                    mainActivity?.navigateToHome()
                } else {
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed. Check your email and password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}