package com.example.fitnesstrackerapp.dashboard.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.ui.ActivityDetailActivity // Import ActivityDetailActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ActivityListAdapter(private val onItemClicked: (ActivityLog) -> Unit) : // onItemClicked is now for detail nav
    ListAdapter<ActivityLog, ActivityListAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activityLog = getItem(position)
        holder.bind(activityLog)
        holder.itemView.setOnClickListener {
            // onItemClicked(activityLog) // Original lambda usage for Toast
            val context = holder.itemView.context
            val intent = Intent(context, ActivityDetailActivity::class.java).apply {
                putExtra(ActivityDetailActivity.EXTRA_ACTIVITY_ID, activityLog.id)
            }
            context.startActivity(intent)
        }
    }

    // The onItemClicked lambda in constructor might be unused now, or repurposed if needed
    // For now, direct navigation is implemented in onBindViewHolder's click listener.
    // If the lambda was intended for other click interactions, it can remain.
    // If it was ONLY for the Toast, it's effectively replaced.
    // Let's assume for now it's fine to keep the constructor param even if not directly used by this change.

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeTextView: TextView = itemView.findViewById(R.id.textViewActivityType)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewActivityDate)
        private val durationTextView: TextView = itemView.findViewById(R.id.textViewActivityDuration)
        private val caloriesTextView: TextView = itemView.findViewById(R.id.textViewActivityCalories)

        fun bind(log: ActivityLog) {
            typeTextView.text = log.type
            dateTextView.text = "Date: ${formatTimestamp(log.timestamp)}"
            durationTextView.text = "Duration: ${formatDuration(log.durationMillis)}"
            caloriesTextView.text = "Calories: ${log.caloriesBurned} kcal"
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        private fun formatDuration(durationMillis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            if (minutes < 60) {
                return "$minutes min"
            }
            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
            val remainingMinutes = minutes % 60
            return String.format("%d hr %02d min", hours, remainingMinutes)
        }
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean {
            return oldItem == newItem
        }
    }
}
