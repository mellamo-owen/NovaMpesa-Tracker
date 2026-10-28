package com.novanest.mpesatracker
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.databinding.FragmentReportsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.get(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val startOfMonth = now - (30L * 24 * 60 * 60 * 1000)
            
            val total = db.transactionDao().totalBetween(startOfMonth, now) ?: 0.0
            val leakage = db.transactionDao().leakageTotal(startOfMonth, now) ?: 0.0
            
            _binding?.tvTotalSpent?.text = String.format("Ksh %,.2f", total)
            _binding?.tvLeakage?.text = String.format("Ksh %,.2f", leakage)
            
            // Idea 17: Monthly Financial Report Card
            val grade: String
            val advice: String
            
            if (total == 0.0) {
                grade = "N/A"
                advice = "No spending recorded this month."
            } else {
                // Calculate leakage percentage
                val leakagePercentage = (leakage / total) * 100
                
                if (leakagePercentage < 10) {
                    grade = "A"
                    advice = "Excellent! You are highly disciplined. Very little money is leaking into small snacks and fees."
                } else if (leakagePercentage < 25) {
                    grade = "B"
                    advice = "Good job. You are mostly in control, but keep an eye on those small daily snacks."
                } else if (leakagePercentage < 40) {
                    grade = "C"
                    advice = "Average. A lot of your money is being drained by small things (smokies, airtime, boda). Try to cut back."
                } else if (leakagePercentage < 60) {
                    grade = "D"
                    advice = "Warning! You are bleeding money on small purchases. Almost half your budget is leaking."
                } else {
                    grade = "F"
                    advice = "Critical! You need to stop buying small things immediately. Most of your money is disappearing on impulses."
                }
            }
            
            _binding?.tvReportGrade?.text = grade
            _binding?.tvReportAdvice?.text = advice
            
            val categories = db.categoryDao().getAllOnce()
            var topCategory = "None"
            var maxSpent = 0.0
            for (cat in categories) {
                val spent = db.transactionDao().categoryTotalBetween(cat.id, startOfMonth, now) ?: 0.0
                if (spent > maxSpent) { maxSpent = spent; topCategory = cat.name }
            }

            _binding?.tvTopCategory?.text = topCategory
            _binding?.tvTopCategoryAmount?.text = String.format("Ksh %,.2f", maxSpent)
        }

        _binding?.btnExportCsv?.setOnClickListener { exportToCsv() }
    }

    private fun exportToCsv() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = db.transactionDao().getAll().first()
            
            if (list.isEmpty()) { Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show(); return@launch }
            
            val csv = StringBuilder("Date,Recipient,Amount,Code\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for (txn in list) csv.append("${sdf.format(Date(txn.timestamp))},\"${txn.recipient}\",${txn.amount},${txn.txnCode ?: "Manual"}\n")
            
            try {
                val filename = "NovaMpesa_Analytics_${System.currentTimeMillis()}.csv"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = requireContext().contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csv.toString().toByteArray())
                        }
                        Toast.makeText(requireContext(), "Saved to Downloads folder!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, filename)
                    FileOutputStream(file).use { it.write(csv.toString().toByteArray()) }
                    Toast.makeText(requireContext(), "Saved to Downloads folder!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
