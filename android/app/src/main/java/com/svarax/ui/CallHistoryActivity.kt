package com.svarax.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svarax.R
import com.svarax.history.CallHistoryRepository

/**
 * Priority 12: Call Analysis History Activity.
 */
class CallHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnClear: Button
    private lateinit var btnBack: ImageButton
    private lateinit var repo: CallHistoryRepository
    private lateinit var adapter: CallHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        repo = CallHistoryRepository(this)

        rvHistory = findViewById(R.id.rvCallHistory)
        layoutEmpty = findViewById(R.id.layoutEmptyHistory)
        btnClear = findViewById(R.id.btnClearHistory)
        btnBack = findViewById(R.id.btnBackHistory)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = CallHistoryAdapter(this, mutableListOf())
        rvHistory.adapter = adapter

        btnBack.setOnClickListener { finish() }
        btnClear.setOnClickListener {
            repo.clearHistory()
            loadHistory()
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val records = repo.getAllRecords()
        if (records.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            adapter.updateData(records)
        }
    }
}
