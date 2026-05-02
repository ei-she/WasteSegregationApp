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
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginFragment : Fragment() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var createAccountButton: Button
    private lateinit var loginButton: Button
    private var mainActivity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Initialize UI Elements
        emailEditText = view.findViewById(R.id.editEmail)
        passwordEditText = view.findViewById(R.id.editPassword)
        createAccountButton = view.findViewById(R.id.buttonCreateAccount)
        loginButton = view.findViewById(R.id.buttonLogin)

        // Login Button Click Listener
        loginButton.setOnClickListener {
            val username = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLocalLogin(username, password)
        }

        // SignUp Button - Now directed to the database
        createAccountButton.setOnClickListener {
            val username = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Fill in details to register", Toast.LENGTH_SHORT).show()
            } else {
                performRegister(username, password)
            }
        }

        return view
    }

    private fun performLocalLogin(user: String, pass: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "${config.BASE_URL}login.php"

        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        Toast.makeText(requireContext(), "Login Successful!", Toast.LENGTH_SHORT).show()
                        mainActivity?.saveLoginState(true)
                        mainActivity?.navigateToHome()
                    } else {
                        Toast.makeText(requireContext(), json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LOGIN_ERROR", "Parsing error: ${e.message}")
                    Toast.makeText(requireContext(), "Server error. Try again.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("LOGIN_ERROR", "Connection failed: ${error.message}")
                Toast.makeText(requireContext(), "Cannot reach Raspberry Pi. Check Wi-Fi.", Toast.LENGTH_LONG).show()
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["username"] = user
                params["password"] = pass
                return params
            }
        }
        queue.add(request)
    }

    private fun performRegister(user: String, pass: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "${config.BASE_URL}register.php"

        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        Toast.makeText(requireContext(), "Account Created! You can now login.", Toast.LENGTH_LONG).show()
                        // Optional: Clear fields after successful registration
                        emailEditText.text.clear()
                        passwordEditText.text.clear()
                    } else {
                        Toast.makeText(requireContext(), json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("REG_ERROR", "Parsing error: ${e.message}")
                }
            },
            { error ->
                Log.e("REG_ERROR", "Connection failed: ${error.message}")
                Toast.makeText(requireContext(), "Registration failed. Check connection.", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["username"] = user
                params["password"] = pass
                return params
            }
        }
        queue.add(request)
    }
}