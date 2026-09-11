package com.svarax.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
 * Priority 12 & 22: Adapter for rendering logged call risk assessments
 * and providing full detail inspection on item tap.
 */
class CallHistoryAdapter(
    private val context: Context,
    private val items: MutableList<CallRecord>
) : RecyclerView.Adapter<CallHistoryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

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

        // Tap item to show Call Details Dialog (Checklist item 22)
        holder.card.setOnClickListener {
            showCallDetailsDialog(record)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CallRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun showCallDetailsDialog(record: CallRecord) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_call_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvCaller = dialog.findViewById<TextView>(R.id.tvDetailCaller)
        val tvDemoTag = dialog.findViewById<TextView>(R.id.tvDetailDemoTag)
        val tvTimestampDuration = dialog.findViewById<TextView>(R.id.tvDetailTimestampDuration)
        val tvRiskBadge = dialog.findViewById<TextView>(R.id.tvDetailRiskBadge)
        val containerIndicators = dialog.findViewById<LinearLayout>(R.id.containerDetailIndicators)
        val tvNoIndicators = dialog.findViewById<TextView>(R.id.tvDetailNoIndicators)
        val tvRecommendation = dialog.findViewById<TextView>(R.id.tvDetailRecommendation)
        val tvSnippet = dialog.findViewById<TextView>(R.id.tvDetailSnippet)
        val lblSnippet = dialog.findViewById<TextView>(R.id.lblDetailSnippet)
        val btnDismiss = dialog.findViewById<Button>(R.id.btnDismissDetail)

        tvCaller.text = record.callerNumber
        if (record.isDemoSimulation) {
            tvDemoTag.visibility = View.VISIBLE
        } else {
            tvDemoTag.visibility = View.GONE
        }

        val formattedDate = dateFormat.format(Date(record.timestamp))
        tvTimestampDuration.text = "$formattedDate • ${record.durationSeconds}s duration"

        val colorRes = when (record.riskLevel) {
            "CRITICAL" -> R.color.accent_red
            "HIGH" -> R.color.accent_red
            "MEDIUM" -> R.color.accent_amber
            else -> R.color.accent_green
        }
        val color = ContextCompat.getColor(context, colorRes)

        tvRiskBadge.text = "${record.finalRiskScore}% ${record.riskLevel}"
        tvRiskBadge.setTextColor(color)

        // Populate Indicators
        containerIndicators.removeAllViews()
        if (record.detectedIndicators.isEmpty()) {
            tvNoIndicators.visibility = View.VISIBLE
        } else {
            tvNoIndicators.visibility = View.GONE
            for (ind in record.detectedIndicators) {
                val itemLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }

                val bullet = TextView(context).apply {
                    text = "• "
                    textSize = 14f
                    setTextColor(color)
                }

                val text = TextView(context).apply {
                    this.text = ind
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemLayout.addView(bullet)
                itemLayout.addView(text)
                containerIndicators.addView(itemLayout)
            }
        }

        tvRecommendation.text = record.recommendation

        if (record.fullTranscriptSnippet.isNotBlank()) {
            lblSnippet.visibility = View.VISIBLE
            tvSnippet.visibility = View.VISIBLE
            tvSnippet.text = "\"${record.fullTranscriptSnippet}\""
        } else {
            lblSnippet.visibility = View.GONE
            tvSnippet.visibility = View.GONE
        }

        btnDismiss.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

