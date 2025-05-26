package com.example.fitnesstrackerapp.goal.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.goal.model.GoalType
import com.example.fitnesstrackerapp.goal.model.UserGoal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class UserGoalsAdapter(
    private val onDeleteClicked: (UserGoal) -> Unit
) : ListAdapter<UserGoal, UserGoalsAdapter.UserGoalViewHolder>(UserGoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user_goal, parent, false)
        return UserGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserGoalViewHolder, position: Int) {
        val goal = getItem(position)
        holder.bind(goal, onDeleteClicked)
    }

    class UserGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewGoalDescription)
        private val progressTextView: TextView = itemView.findViewById(R.id.textViewGoalProgress)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarGoal)
        private val statusTextView: TextView = itemView.findViewById(R.id.textViewGoalStatus)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteGoal)

        fun bind(goal: UserGoal, onDeleteClicked: (UserGoal) -> Unit) {
            val goalTypeEnum = try { GoalType.valueOf(goal.goalType) } catch (e: IllegalArgumentException) { null }
            val unit = goalTypeEnum?.unit ?: ""

            var description = goalTypeEnum?.displayName ?: goal.goalType
            if (goalTypeEnum?.requiresActivityFilter == true && !goal.activityTypeFilter.isNullOrEmpty()){
                description = "${goal.activityTypeFilter} - $description"
            }
            descriptionTextView.text = "$description (Target: ${goal.targetValue} $unit)"
            
            if (goal.targetDate != null) {
                val sdf = SimpleDateFormat("by MMM d, yyyy", Locale.getDefault())
                descriptionTextView.append(" ${sdf.format(Date(goal.targetDate))}")
            }


            // Progress Text and ProgressBar
            val currentProgress = goal.currentValue
            val targetProgress = goal.targetValue
            
            progressTextView.text = String.format(Locale.getDefault(),"Progress: %.1f / %.1f %s", currentProgress, targetProgress, unit)

            if (targetProgress > 0) {
                 if (goalTypeEnum == GoalType.WEIGHT_TARGET) {
                    // For weight target, progress is tricky. If current is target, 100%.
                    // If target is lower (loss), progress increases as current decreases.
                    // If target is higher (gain), progress increases as current increases.
                    // This simplified version assumes target is always "more" for progress bar
                    // A more accurate representation might need a different UI for weight goals.
                    val initialWeightForGoal = goal.startDate // This is not initial weight.
                    // We'd need to store initial weight at goal creation if we want to show % of weight lost/gained.
                    // For now, let's show current vs target.
                    // If target is to lose weight (e.g. target 70kg, current 75kg)
                    // We can represent progress as how much of the *difference* is covered.
                    // This requires knowing the starting weight when the goal was set.
                    // For simplicity, if current <= target for weight loss, or current >= target for weight gain, it's 100%.
                    // This is a placeholder and needs better logic for weight goals.
                    progressBar.max = targetProgress.roundToInt()
                    progressBar.progress = currentProgress.roundToInt().coerceAtMost(targetProgress.roundToInt())

                } else { // For cumulative goals like distance, duration, steps
                    progressBar.max = targetProgress.roundToInt()
                    progressBar.progress = currentProgress.roundToInt().coerceAtMost(targetProgress.roundToInt())
                }
            } else {
                progressBar.max = 100
                progressBar.progress = 0
            }


            // Status Text
            val currentTime = System.currentTimeMillis()
            when {
                goal.isCompleted -> {
                    statusTextView.text = "Completed!"
                    statusTextView.setTextColor(Color.GREEN)
                    progressBar.progress = progressBar.max // Ensure full if completed
                }
                !goal.isActive -> {
                    statusTextView.text = "Inactive"
                    statusTextView.setTextColor(Color.GRAY)
                }
                goal.targetDate != null && goal.targetDate < currentTime -> {
                    statusTextView.text = "Expired"
                    statusTextView.setTextColor(Color.RED)
                }
                else -> {
                    statusTextView.text = "Active"
                    statusTextView.setTextColor(Color.BLUE) // Or a less prominent color
                }
            }

            deleteButton.setOnClickListener {
                onDeleteClicked(goal)
            }
        }
    }

    class UserGoalDiffCallback : DiffUtil.ItemCallback<UserGoal>() {
        override fun areItemsTheSame(oldItem: UserGoal, newItem: UserGoal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserGoal, newItem: UserGoal): Boolean {
            return oldItem == newItem
        }
    }
}
