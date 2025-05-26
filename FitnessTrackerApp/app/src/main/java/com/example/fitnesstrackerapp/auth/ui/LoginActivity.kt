package com.example.fitnesstrackerapp.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.MainActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.auth.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        loginViewModel.currentUser.observe(this) { user ->
            if (user != null) {
                // User is signed in, navigate to MainActivity
                navigateToMain()
                return@observe // Prevent further processing if already navigating
            }
            // No user signed in, proceed with LoginActivity setup
            setContentView(R.layout.activity_login)
            setupUI()
        }
    }

    private fun setupUI() {
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerLink = findViewById<TextView>(R.id.textViewRegisterLink)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            loginViewModel.loginUser(email, password)
        }

        loginViewModel.loginResult.observe(this) { result ->
            val (success, message) = result
            if (success) {
                Toast.makeText(this, "Login successful. UID: $message", Toast.LENGTH_LONG).show()
                navigateToMain()
            } else {
                Toast.makeText(this, "Login failed: $message", Toast.LENGTH_LONG).show()
            }
        }

        registerLink.setOnClickListener {
            // Navigate to RegistrationActivity
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToMain(userName: String? = null, userEmail: String? = null) {
        val intent = Intent(this, MainActivity::class.java)
        userName?.let { intent.putExtra("USER_NAME", it) }
        userEmail?.let { intent.putExtra("USER_EMAIL", it) }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Check if activity was started from RegistrationActivity with extras
        val userName = intent.getStringExtra("USER_NAME")
        val userEmail = intent.getStringExtra("USER_EMAIL")

        if (loginViewModel.currentUser.value != null) {
             // If user is already logged in (e.g. app restart), no need to pass extras from registration
            navigateToMain()
        } else if (userName != null && userEmail != null) {
            // This means we came from Registration.
            // The loginResult observer will handle navigation after a successful login attempt
            // which should happen automatically if registration was successful and led here.
            // For now, we ensure the UI is set up if not auto-navigating.
            if (findViewById<EditText>(R.id.editTextEmail) == null) { // Avoid calling setContentView multiple times
                 setContentView(R.layout.activity_login)
                 setupUI() // Ensure UI is set up if not already
            }
            // Pre-fill login fields if coming from registration (optional, good UX)
            findViewById<EditText>(R.id.editTextEmail)?.setText(userEmail)
        }
    }

    // Modified to call navigateToMain with potential extras
    private fun setupUI() {
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerLink = findViewById<TextView>(R.id.textViewRegisterLink)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            loginViewModel.loginUser(email, password)
        }

        loginViewModel.loginResult.observe(this) { result ->
            val (success, message) = result
            if (success) {
                Toast.makeText(this, "Login successful. UID: $message", Toast.LENGTH_LONG).show()
                // Pass name and email if available from registration intent
                val regName = intent.getStringExtra("USER_NAME")
                val regEmail = intent.getStringExtra("USER_EMAIL")
                navigateToMain(regName, regEmail)
            } else {
                Toast.makeText(this, "Login failed: $message", Toast.LENGTH_LONG).show()
            }
        }

        registerLink.setOnClickListener {
            // Navigate to RegistrationActivity
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }
}
