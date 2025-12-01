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

    // Reference to MainActivity to call navigation and session functions
    private var mainActivity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Find UI elements based on your layout
        emailEditText = view.findViewById(R.id.editEmail) // Example ID
        passwordEditText = view.findViewById(R.id.editPassword) // Example ID
        createAccountButton = view.findViewById(R.id.buttonCreateAccount) // Example ID
        loginButton = view.findViewById(R.id.buttonLogin) // Example ID

        // Set up the listeners
        createAccountButton.setOnClickListener {
            handleSignUp() // This function registers the user
        }

        loginButton.setOnClickListener {
            handleLogin() // This function logs in an existing user
        }

        return view
    }

    // ----------------------------------------------------------------------
    // --- SIGN UP LOGIC (CREATE ACCOUNT) ---
    // ----------------------------------------------------------------------

    private fun handleSignUp() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the Firebase method to create the user with the provided credentials
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign up successful! User is automatically logged in.
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

    // ----------------------------------------------------------------------
    // --- LOGIN LOGIC (SIGN IN) ---
    // ----------------------------------------------------------------------

    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the Firebase method to sign in an existing user
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("Login", "signInWithEmail:success")
                    Toast.makeText(requireContext(), "Login successful.", Toast.LENGTH_SHORT).show()

                    // Inform MainActivity to handle session and navigation
                    mainActivity?.saveLoginState(true)
                    mainActivity?.navigateToHome()
                } else {
                    // Login failed
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