package com.svarax.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.svarax.R
import com.svarax.history.CallRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Priority 12: Adapter for rendering logged call risk assessments.
 */
class CallHistoryAdapter(
    private val context: Context,
    private val items: MutableList<CallRecord>
) : RecyclerView.Adapter<CallHistoryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardHistoryItem)
        val tvCaller: TextView = view.findViewById(R.id.tvHistoryCaller)
        val tvDateDuration: TextView = view.findViewById(R.id.tvHistoryDateDuration)
        val tvBadge: TextView = view.findViewById(R.id.tvHistoryRiskScoreBadge)
        val tvIndicators: TextView = view.findViewById(R.id.tvHistoryIndicators)
        val tvRecommendation: TextView = view.findViewById(R.id.tvHistoryRecommendation)
        val imgIcon: ImageView = view.findViewById(R.id.imgHistoryStatusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = items[position]

        val demoSuffix = if (record.isDemoSimulation) " [DEMO]" else ""
        holder.tvCaller.text = "${record.callerNumber}$demoSuffix"
        val dateStr = dateFormat.format(Date(record.timestamp))
        holder.tvDateDuration.text = "$dateStr • ${record.durationSeconds}s"

        val colorRes = when (record.riskLevel) {
            "CRITICAL" -> R.color.accent_red
            "HIGH" -> R.color.accent_red
            "MEDIUM" -> R.color.accent_amber
            else -> R.color.accent_green
        }
        val color = ContextCompat.getColor(context, colorRes)

        holder.tvBadge.text = "${record.finalRiskScore}% ${record.riskLevel}"
        holder.tvBadge.setTextColor(color)
        holder.card.strokeColor = color

        if (record.detectedIndicators.isNotEmpty()) {
            holder.tvIndicators.visibility = View.VISIBLE
            holder.tvIndicators.text = "⚠ " + record.detectedIndicators.joinToString(", ")
            holder.tvIndicators.setTextColor(color)
        } else {
            holder.tvIndicators.visibility = View.VISIBLE
            holder.tvIndicators.text = "✓ No suspicious indicators detected"
            holder.tvIndicators.setTextColor(ContextCompat.getColor(context, R.color.accent_green))
        }

        holder.tvRecommendation.text = "Action: ${record.recommendation}"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CallRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
