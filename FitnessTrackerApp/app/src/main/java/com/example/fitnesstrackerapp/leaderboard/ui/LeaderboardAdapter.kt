package com.example.fitnesstrackerapp.leaderboard.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.leaderboard.model.LeaderboardItem

class LeaderboardAdapter :
    ListAdapter<LeaderboardItem, LeaderboardAdapter.LeaderboardViewHolder>(LeaderboardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankTextView: TextView = itemView.findViewById(R.id.textViewRank)
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewLeaderboardName)
        private val xpTextView: TextView = itemView.findViewById(R.id.textViewLeaderboardXp)
        private val currentUserIndicatorTextView: TextView = itemView.findViewById(R.id.textViewLeaderboardCurrentUserIndicator)
        private val itemLayout: ConstraintLayout = itemView.findViewById(R.id.layoutLeaderboardItem)


        fun bind(item: LeaderboardItem) {
            rankTextView.text = "${item.rank}."
            nameTextView.text = item.displayName ?: "Unknown User"
            xpTextView.text = "${item.xpPoints} XP"

            if (item.isCurrentUser) {
                currentUserIndicatorTextView.visibility = View.VISIBLE
                nameTextView.setTypeface(null, Typeface.BOLD)
                xpTextView.setTypeface(null, Typeface.BOLD)
                itemLayout.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorSecondaryVeryLight)) // Example highlight
            } else {
                currentUserIndicatorTextView.visibility = View.GONE
                nameTextView.setTypeface(null, Typeface.NORMAL)
                xpTextView.setTypeface(null, Typeface.NORMAL)
                itemLayout.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent)) // Default background
            }
        }
    }

    class LeaderboardDiffCallback : DiffUtil.ItemCallback<LeaderboardItem>() {
        override fun areItemsTheSame(oldItem: LeaderboardItem, newItem: LeaderboardItem): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: LeaderboardItem, newItem: LeaderboardItem): Boolean {
            return oldItem == newItem
        }
    }
}

// Need to add colorSecondaryVeryLight to colors.xml
// For example: <color name="colorSecondaryVeryLight">#FFE0F7</color> (a light pink if secondary is pink)
