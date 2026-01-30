package com.example.wastesegregationapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPasswordText: TextView
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var createAccountButton: Button
    private lateinit var loginButton: Button

    private lateinit var loginContainer: View
    private lateinit var otpContainer: View
    private lateinit var submitOtpButton: Button
    private lateinit var resendCodeText: TextView

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

        emailEditText = view.findViewById(R.id.editEmail)
        passwordEditText = view.findViewById(R.id.editPassword)
        createAccountButton = view.findViewById(R.id.buttonCreateAccount)
        loginButton = view.findViewById(R.id.buttonLogin)
        loginContainer = view.findViewById(R.id.loginContainer)
        otpContainer = view.findViewById(R.id.otpContainer)
        forgotPasswordText = view.findViewById(R.id.textForgotPassword)

        forgotPasswordText.setOnClickListener {
            handleForgotPassword()
        }
        submitOtpButton = view.findViewById(R.id.buttonSubmitOtp)
        resendCodeText = view.findViewById(R.id.textResendCode)

        // Button Listeners
        createAccountButton.setOnClickListener {
            handleSignUp()
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please enter your email first",
                    Toast.LENGTH_SHORT).show()
            } else {
                checkEmailAndProceed(email)
            }
        }

        submitOtpButton.setOnClickListener {
            handleVerificationCheck()
        }

        resendCodeText.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(),
                                "A new verification link has been sent to ${user.email}",
                                Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(),
                                "Failed to send: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(requireContext(),
                    "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show()
                otpContainer.visibility = View.GONE
                loginContainer.visibility = View.VISIBLE
            }
        }

        return view
    }

    private fun handleSignUp() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(),
                "Email and password cannot be empty.",
                Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPasswordStrictlyValid(password)) {
            val errorMessage = """
                Password is weak! Requirements:
                - At least 8 characters
                - 1 uppercase (A-Z), 1 lowercase (a-z)
                - 1 number (0-9), 1 special symbol
            """.trimIndent()
            Toast.makeText(requireContext(),
                errorMessage,
                Toast.LENGTH_LONG).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            Toast.makeText(requireContext(),
                                "Registration successful! Check your email to verify.",
                                Toast.LENGTH_LONG).show()
                            auth.signOut()
                        }
                    }
                } else {
                    handleSignUpFailure(task.exception)
                }
            }
    }

    private fun handleSignUpFailure(exception: Exception?) {
        val message = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
            is FirebaseAuthUserCollisionException -> "Account already exists."
            else -> "Error: ${exception?.localizedMessage ?: "Unknown Error"}"
        }
        Toast.makeText(requireContext(),
            message, Toast.LENGTH_LONG).show()
    }

    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (password.isEmpty()) {
            Toast.makeText(requireContext(),
                "Please enter your password.",
                Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {
                        Log.d("Login", "User verified. Navigating Home.")
                        mainActivity?.saveLoginState(true)
                        mainActivity?.navigateToHome()
                    } else if (user != null) {
                        loginContainer.visibility = View.GONE
                        otpContainer.visibility = View.VISIBLE
                    }
                } else {
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    Toast.makeText(requireContext(),
                        "Authentication failed. Check your password.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkEmailAndProceed(email: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        showAccountNotFoundDialog()
                    } else {
                        handleLogin()
                    }
                } else {
                    Log.e("AuthError",
                        "Error checking email", task.exception)
                    Toast.makeText(requireContext(),
                        "Error checking account.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleVerificationCheck() {
        val user = auth.currentUser

        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    Toast.makeText(requireContext(),
                        "Verification Successful!", Toast.LENGTH_SHORT).show()
                    mainActivity?.saveLoginState(true)
                    mainActivity?.navigateToHome()
                } else {
                    Toast.makeText(requireContext(),
                        "Email not verified yet. Please check your inbox.",
                        Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(),
                    "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAccountNotFoundDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Account Not Found")
            .setMessage("This email is not registered. Please create a new account.")
            .setPositiveButton("Create Account") { dialog, _ ->
                handleSignUp()
                dialog.dismiss()
            }
            .setNegativeButton("Try Again") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun handleForgotPassword() {
        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(requireContext(),
                "Please enter your email address first.",
                Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password?")
            .setMessage("Send a password reset link to $email?")
            .setPositiveButton("Send") { _, _ ->
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(),
                                "Reset link sent! Please check your email.",
                                Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(),
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}