package com.example.fitnesstrackerapp.social.repository

import android.util.Log
import com.example.fitnesstrackerapp.user.model.UserProfile // Assuming UserProfile is also used for Firestore user data
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Generic Result class
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class FriendRepository(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "FriendRepository"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FRIENDSHIPS = "friendships"

        // User fields (matching UserProfile and Firestore public user doc)
        const val FIELD_UID = "uid"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_EMAIL = "email"

        // Friendship fields
        const val FIELD_USER_IDS = "userIds" // List<String>
        const val FIELD_STATUS = "status"     // "pending", "accepted"
        const val FIELD_REQUESTER_ID = "requesterId" // UID of user who sent request

        // Friendship statuses
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
    }

    suspend fun sendFriendRequest(currentUserUid: String, targetUserUid: String): Result<Unit> {
        if (currentUserUid == targetUserUid) return Result.Error(IllegalArgumentException("Cannot send friend request to yourself."))
        
        val sortedUserIds = listOf(currentUserUid, targetUserUid).sorted()
        val friendshipDocId = sortedUserIds.joinToString("_") // Example: "uid1_uid2"

        return try {
            val existingRequest = firestore.collection(COLLECTION_FRIENDSHIPS)
                .document(friendshipDocId)
                .get().await()

            if (existingRequest.exists()) {
                val status = existingRequest.getString(FIELD_STATUS)
                if (status == STATUS_ACCEPTED) {
                    return Result.Error(Exception("You are already friends."))
                } else if (status == STATUS_PENDING) {
                    return Result.Error(Exception("Friend request already pending."))
                }
                // Potentially handle other states or re-sending a declined request if allowed
            }
            
            val friendRequestData = hashMapOf(
                FIELD_USER_IDS to sortedUserIds,
                FIELD_STATUS to STATUS_PENDING,
                FIELD_REQUESTER_ID to currentUserUid,
                "timestamp" to FieldValue.serverTimestamp() // Optional: for ordering requests
            )

            firestore.collection(COLLECTION_FRIENDSHIPS).document(friendshipDocId)
                .set(friendRequestData)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.Error(e)
        }
    }

    suspend fun acceptFriendRequest(currentUserUid: String, requestingUserUid: String): Result<Unit> {
        val sortedUserIds = listOf(currentUserUid, requestingUserUid).sorted()
        val friendshipDocId = sortedUserIds.joinToString("_")

        return try {
            val docRef = firestore.collection(COLLECTION_FRIENDSHIPS).document(friendshipDocId)
            val snapshot = docRef.get().await()

            if (snapshot.exists() &&
                snapshot.getString(FIELD_STATUS) == STATUS_PENDING &&
                snapshot.getString(FIELD_REQUESTER_ID) == requestingUserUid) {
                
                docRef.update(FIELD_STATUS, STATUS_ACCEPTED).await()
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Friend request not found or already handled."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.Error(e)
        }
    }

    suspend fun declineOrRemoveFriend(currentUserUid: String, otherUserUid: String): Result<Unit> {
        val sortedUserIds = listOf(currentUserUid, otherUserUid).sorted()
        val friendshipDocId = sortedUserIds.joinToString("_")
        
        return try {
            firestore.collection(COLLECTION_FRIENDSHIPS).document(friendshipDocId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining/removing friend", e)
            Result.Error(e)
        }
    }

    fun getFriendRequests(currentUserUid: String): Flow<List<UserProfile>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_FRIENDSHIPS)
            .whereArrayContains(FIELD_USER_IDS, currentUserUid)
            .whereEqualTo(FIELD_STATUS, STATUS_PENDING)
            // .whereNotEqualTo(FIELD_REQUESTER_ID, currentUserUid) // This line is crucial
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen error for friend requests", e)
                    close(e) // Close the flow with an error
                    return@addSnapshotListener
                }

                val requestersUids = snapshots?.documents
                    ?.filter { it.getString(FIELD_REQUESTER_ID) != currentUserUid } // Ensure current user is not the requester
                    ?.mapNotNull { it.getString(FIELD_REQUESTER_ID) }
                    ?: emptyList()

                if (requestersUids.isNotEmpty()) {
                    firestore.collection(COLLECTION_USERS)
                        .whereIn(FIELD_UID, requestersUids)
                        .get()
                        .addOnSuccessListener { userDocs ->
                            val profiles = userDocs.mapNotNull { it.toObject<UserProfile>() }
                            trySend(profiles).isSuccess
                        }
                        .addOnFailureListener { userFetchError ->
                            Log.w(TAG, "Error fetching user profiles for friend requests", userFetchError)
                            trySend(emptyList()).isSuccess // Send empty list on error
                        }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    fun getFriends(currentUserUid: String): Flow<List<UserProfile>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_FRIENDSHIPS)
            .whereArrayContains(FIELD_USER_IDS, currentUserUid)
            .whereEqualTo(FIELD_STATUS, STATUS_ACCEPTED)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen error for friends list", e)
                    close(e)
                    return@addSnapshotListener
                }

                val friendUids = snapshots?.documents?.mapNotNull { doc ->
                    val userIdsList = doc.get(FIELD_USER_IDS) as? List<String>
                    userIdsList?.firstOrNull { it != currentUserUid }
                } ?: emptyList()

                if (friendUids.isNotEmpty()) {
                    firestore.collection(COLLECTION_USERS)
                        .whereIn(FIELD_UID, friendUids)
                        .get()
                        .addOnSuccessListener { userDocs ->
                            val profiles = userDocs.mapNotNull { it.toObject<UserProfile>() }
                            trySend(profiles).isSuccess
                        }
                        .addOnFailureListener { userFetchError ->
                             Log.w(TAG, "Error fetching user profiles for friends list", userFetchError)
                            trySend(emptyList()).isSuccess
                        }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun searchUsers(query: String, currentUserUid: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        return try {
            // Basic prefix search on displayName. Case-sensitive.
            // For case-insensitive, you'd typically store a normalized (e.g., lowercase) version.
            val usersQuery = firestore.collection(COLLECTION_USERS)
                .whereGreaterThanOrEqualTo(FIELD_DISPLAY_NAME, query)
                .whereLessThanOrEqualTo(FIELD_DISPLAY_NAME, query + '\uf8ff') // Standard trick for prefix
                .orderBy(FIELD_DISPLAY_NAME) // Firestore requires orderBy for range queries
                .limit(20)
                .get()
                .await()

            usersQuery.documents.mapNotNull { doc ->
                doc.toObject<UserProfile>()?.takeIf { it.userId != currentUserUid }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            emptyList()
        }
    }
}
