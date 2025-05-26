package com.example.fitnesstrackerapp.auth.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.viewModelScope // Added
import com.example.fitnesstrackerapp.tracking.model.AppDatabase // Added for DAO
import com.example.fitnesstrackerapp.user.model.UserProfile // Added
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.viewModelScope 
import com.example.fitnesstrackerapp.tracking.model.AppDatabase 
import com.example.fitnesstrackerapp.user.model.UserProfile 
import com.example.fitnesstrackerapp.user.model.UserProfileDao 
import kotlinx.coroutines.launch 
import android.app.Application 
import android.util.Log // For Firestore logging
import com.google.firebase.firestore.FirebaseFirestore // Firestore import
import com.google.firebase.firestore.SetOptions // For merge option

class RegistrationViewModel(application: Application) : AndroidViewModel(application) { 

    private val auth: FirebaseAuth = Firebase.auth
    private val userProfileDao: UserProfileDao 
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // Firestore instance
    val registrationResult = MutableLiveData<Pair<Boolean, String?>>() 

    init { 
        userProfileDao = AppDatabase.getDatabase(application).userProfileDao()
    }

    fun registerUser(email: String, password: String, name: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            registrationResult.value = Pair(false, "Email, password, and name cannot be empty.")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let { user ->
                        viewModelScope.launch {
                            val userProfile = UserProfile(
                                userId = user.uid,
                                displayName = name, 
                                email = user.email,
                                xpPoints = 0L,
                                level = 1
                            )
                            userProfileDao.upsertUserProfile(userProfile)

                            // Sync to Firestore
                            val firestoreUser = hashMapOf(
                                "uid" to user.uid,
                                "displayName" to name, // Use name from form
                                "email" to user.email
                                // Not syncing xpPoints or level to public Firestore user doc for now
                            )
                            firestore.collection("users").document(user.uid)
                                .set(firestoreUser, SetOptions.merge()) // Use merge to avoid overwriting other fields if any
                                .addOnSuccessListener { Log.d("FirestoreSync", "User profile synced to Firestore.") }
                                .addOnFailureListener { e -> Log.w("FirestoreSync", "Error syncing user profile to Firestore", e) }
                        }
                        registrationResult.value = Pair(true, user.uid)
                    } ?: run {
                        registrationResult.value = Pair(false, "Registration succeeded but FirebaseUser is null.")
                    }
                } else {
                    registrationResult.value = Pair(false, task.exception?.message ?: "Registration failed.")
                }
            }
    }
}
