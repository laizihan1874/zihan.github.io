package com.example.fitnesstrackerapp.social.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.user.model.UserProfile

class FriendRequestAdapter(
    private val onAcceptClicked: (UserProfile) -> Unit,
    private val onDeclineClicked: (UserProfile) -> Unit
) : ListAdapter<UserProfile, FriendRequestAdapter.FriendRequestViewHolder>(UserDiffCallback()) { // Reusing UserDiffCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_friend_request, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, onAcceptClicked, onDeclineClicked)
    }

    class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val requesterNameTextView: TextView = itemView.findViewById(R.id.textViewRequesterName)
        private val acceptButton: Button = itemView.findViewById(R.id.buttonAcceptRequest)
        private val declineButton: Button = itemView.findViewById(R.id.buttonDeclineRequest)

        fun bind(
            requester: UserProfile,
            onAcceptClicked: (UserProfile) -> Unit,
            onDeclineClicked: (UserProfile) -> Unit
        ) {
            requesterNameTextView.text = requester.displayName ?: requester.email ?: "Unknown User"
            acceptButton.setOnClickListener {
                onAcceptClicked(requester)
            }
            declineButton.setOnClickListener {
                onDeclineClicked(requester)
            }
        }
    }
    
    // Re-using UserDiffCallback from UserSearchAdapter as it works for UserProfile
    // If it's not in the same package or accessible, it should be defined here or made common.
    // Assuming UserDiffCallback is accessible or defined as:
    /*
    class UserDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem.userId == newItem.userId
        }
        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem == newItem
        }
    }
    */
}
