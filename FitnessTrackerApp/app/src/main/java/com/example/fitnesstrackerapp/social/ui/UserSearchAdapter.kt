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

class UserSearchAdapter(
    private val onSendRequestClicked: (UserProfile) -> Unit
) : ListAdapter<UserProfile, UserSearchAdapter.UserSearchViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user_search, parent, false)
        return UserSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserSearchViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, onSendRequestClicked)
    }

    class UserSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        private val emailTextView: TextView = itemView.findViewById(R.id.textViewUserEmail)
        private val sendRequestButton: Button = itemView.findViewById(R.id.buttonSendFriendRequest)

        fun bind(user: UserProfile, onSendRequestClicked: (UserProfile) -> Unit) {
            nameTextView.text = user.displayName ?: "N/A"
            emailTextView.text = user.email ?: "No email"
            sendRequestButton.setOnClickListener {
                onSendRequestClicked(user)
            }
            // Optionally, disable button if request already sent or they are already friends.
            // This would require more complex state management or data passed to the adapter.
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem == newItem
        }
    }
}
