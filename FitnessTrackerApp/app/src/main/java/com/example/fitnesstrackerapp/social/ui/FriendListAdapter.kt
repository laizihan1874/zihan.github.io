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

class FriendListAdapter(
    private val onRemoveClicked: (UserProfile) -> Unit
) : ListAdapter<UserProfile, FriendListAdapter.FriendViewHolder>(UserDiffCallback()) { // Reusing UserDiffCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, onRemoveClicked)
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendNameTextView: TextView = itemView.findViewById(R.id.textViewFriendName)
        private val removeButton: Button = itemView.findViewById(R.id.buttonRemoveFriend)

        fun bind(
            friend: UserProfile,
            onRemoveClicked: (UserProfile) -> Unit
        ) {
            friendNameTextView.text = friend.displayName ?: friend.email ?: "Unknown Friend"
            removeButton.setOnClickListener {
                onRemoveClicked(friend)
            }
        }
    }
    
    // Assuming UserDiffCallback is accessible (defined in UserSearchAdapter or a common place)
    // If not, define it here:
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
