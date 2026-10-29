package com.novanest.mpesatracker
import android.content.ContentValues
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
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
            val startOfMonth = now - TimeUnit.DAYS.toMillis(30)
            
            val allTxns = db.transactionDao().getAll().first()
            val categories = db.categoryDao().getAll().first()

            // Total Spent (Excluding External Deposits)
            val totalSpent = allTxns.filter { it.timestamp >= startOfMonth && !it.isExternalDeposit }.sumOf { it.amount + it.transactionCost }
            _binding?.tvTotalSpent?.text = String.format("Ksh %,.2f", totalSpent)

            // The Leakage
            val leakage = allTxns.filter { it.timestamp >= startOfMonth && it.amount < 200 && !it.isExternalDeposit }.sumOf { it.amount + it.transactionCost }
            _binding?.tvLeakage?.text = String.format("Ksh %,.2f", leakage)

            // Top Category
            var topCatName = "None"
            var topCatAmount = 0.0
            for (cat in categories) {
                val catSpent = allTxns.filter { it.categoryId == cat.id && it.timestamp >= startOfMonth && !it.isExternalDeposit }.sumOf { it.amount + it.transactionCost }
                if (catSpent > topCatAmount) {
                    topCatAmount = catSpent
                    topCatName = cat.name
                }
            }
            _binding?.tvTopCategory?.text = topCatName
            _binding?.tvTopCategoryAmount?.text = String.format("Ksh %,.2f", topCatAmount)
        }

        _binding?.btnDownloadMonthly?.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
            
            if (dayOfMonth < 28) {
                Toast.makeText(requireContext(), "Report will be active at the end of the month.", Toast.LENGTH_LONG).show()
            } else {
                downloadMonthlyReport()
            }
        }
    }

    private fun downloadMonthlyReport() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allTxns = db.transactionDao().getAll().first()
            if (allTxns.isEmpty()) {
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val csv = StringBuilder("Date,Recipient,Amount,IsDeposit,TransactionCost\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for (txn in allTxns) {
                csv.append("${sdf.format(Date(txn.timestamp))},\"${txn.recipient}\",${txn.amount},${txn.isExternalDeposit},${txn.transactionCost}\n")
            }

            try {
                val filename = "NovaMpesa_Monthly_Report_${System.currentTimeMillis()}.csv"
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
