package com.example.fitnesstrackerapp.gamification.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.gamification.model.Achievement
import com.example.fitnesstrackerapp.gamification.model.UserAchievement
import java.text.SimpleDateFormat
import java.util.*

data class AchievementDisplayItem(
    val achievement: Achievement,
    val userAchievement: UserAchievement? // Null if not unlocked
)

class AchievementsAdapter :
    ListAdapter<AchievementDisplayItem, AchievementsAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.imageViewAchievementIcon)
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewAchievementName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewAchievementDescription)
        private val statusTextView: TextView = itemView.findViewById(R.id.textViewAchievementStatus)
        private val itemLayout: View = itemView.findViewById(R.id.constraintLayoutItemAchievement) // Or the root CardView

        fun bind(item: AchievementDisplayItem) {
            nameTextView.text = item.achievement.name
            descriptionTextView.text = item.achievement.description
            iconImageView.setImageResource(R.drawable.ic_achievement_generic) // Use generic for now

            if (item.userAchievement != null) { // Unlocked
                statusTextView.text = "Unlocked: ${formatTimestamp(item.userAchievement.unlockedTimestamp)}"
                iconImageView.clearColorFilter()
                itemLayout.alpha = 1.0f
                statusTextView.setTextColor(Color.parseColor("#008000")) // Green color for "Unlocked"
            } else { // Locked
                statusTextView.text = "Locked"
                iconImageView.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                itemLayout.alpha = 0.6f // Dim the item
                statusTextView.setTextColor(Color.GRAY)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class AchievementDiffCallback : DiffUtil.ItemCallback<AchievementDisplayItem>() {
        override fun areItemsTheSame(oldItem: AchievementDisplayItem, newItem: AchievementDisplayItem): Boolean {
            return oldItem.achievement.id == newItem.achievement.id
        }

        override fun areContentsTheSame(oldItem: AchievementDisplayItem, newItem: AchievementDisplayItem): Boolean {
            return oldItem == newItem // Compares Achievement and UserAchievement (if not null)
        }
    }
}
