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


class LoginViewModel(application: Application) : AndroidViewModel(application) { 

    private val auth: FirebaseAuth = Firebase.auth
    private val userProfileDao: UserProfileDao 
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // Firestore instance
    val loginResult = MutableLiveData<Pair<Boolean, String?>>() 
    val currentUser = MutableLiveData<FirebaseUser?>()

    init {
        currentUser.value = auth.currentUser
        userProfileDao = AppDatabase.getDatabase(application).userProfileDao() 
    }

    fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            loginResult.value = Pair(false, "Email and password cannot be empty.")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    currentUser.value = firebaseUser // Update LiveData for current Firebase user
                    firebaseUser?.let { user ->
                        viewModelScope.launch {
                            var userProfile = userProfileDao.getUserProfileSuspending(user.uid)
                            if (userProfile == null) {
                                userProfile = UserProfile(
                                    userId = user.uid,
                                    displayName = user.displayName,
                                    email = user.email,
                                    xpPoints = 0L,
                                    level = 1
                                )
                            } else {
                                // Optionally update display name and email if they changed in Firebase
                                userProfile = userProfile.copy(
                                    displayName = user.displayName ?: userProfile.displayName,
                                    email = user.email ?: userProfile.email
                                )
                            }
                            userProfileDao.upsertUserProfile(userProfile)

                            // Sync to Firestore
                            val firestoreUser = hashMapOf(
                                "uid" to user.uid,
                                "displayName" to userProfile.displayName, // Use potentially updated name from profile
                                "email" to userProfile.email             // Use potentially updated email
                            )
                            firestore.collection("users").document(user.uid)
                                .set(firestoreUser, SetOptions.merge())
                                .addOnSuccessListener { Log.d("FirestoreSync", "User profile synced to Firestore on login.") }
                                .addOnFailureListener { e -> Log.w("FirestoreSync", "Error syncing user profile on login", e) }
                        }
                        loginResult.value = Pair(true, user.uid)
                    } ?: run {
                        loginResult.value = Pair(false, "Login succeeded but FirebaseUser is null.")
                    }
                } else {
                    currentUser.value = null
                    loginResult.value = Pair(false, task.exception?.message ?: "Login failed.")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        currentUser.value = null
    }
}
