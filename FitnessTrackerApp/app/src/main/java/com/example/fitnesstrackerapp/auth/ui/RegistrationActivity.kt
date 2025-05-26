package com.example.fitnesstrackerapp.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.auth.viewmodel.RegistrationViewModel

class RegistrationActivity : AppCompatActivity() {

    private val registrationViewModel: RegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val nameEditText = findViewById<EditText>(R.id.editTextName)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val loginLink = findViewById<TextView>(R.id.textViewLoginLink)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            registrationViewModel.registerUser(email, password, name)
        }

        registrationViewModel.registrationResult.observe(this) { result ->
            val (success, message) = result
            if (success) {
                Toast.makeText(this, "Registration successful. UID: $message", Toast.LENGTH_LONG).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("USER_NAME", nameEditText.text.toString().trim()) // Pass the name
                intent.putExtra("USER_EMAIL", emailEditText.text.toString().trim()) // Pass the email
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Registration failed: $message", Toast.LENGTH_LONG).show()
            }
        }

        loginLink.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // finish() // Optional: finish this activity or not
        }
    }
}
