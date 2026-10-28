package com.novanest.mpesatracker

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.get(requireContext())
        
        loadData()
        setupDownloadButton()
        setupThemeToggle()
        
        _binding?.cardAdvisory?.setOnClickListener {
            val url = "https://novanest-group.vercel.app/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun setupThemeToggle() {
        _binding?.btnThemeToggle?.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
            val isDarkMode = prefs.getBoolean("is_dark_mode", false)
            
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.edit().putBoolean("is_dark_mode", false).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefs.edit().putBoolean("is_dark_mode", true).apply()
            }
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val state = db.appStateDao().getOnce()
            if (state != null) {
                val firstName = state.userName.split(" ").firstOrNull() ?: "User"
                _binding?.tvGreeting?.text = "Hello, $firstName"
            }

            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfWeek = cal.timeInMillis
            val now = System.currentTimeMillis()

            val weekTotal = db.transactionDao().totalBetween(startOfWeek, now) ?: 0.0
            val monthTotal = db.transactionDao().totalBetween(now - (30L*24*60*60*1000), now) ?: 0.0
            _binding?.tvWeekTotal?.text = String.format("Ksh %,.2f", weekTotal)
            _binding?.tvMonthTotal?.text = String.format("Ksh %,.2f", monthTotal)

            // Runway
            val recentTxns = db.transactionDao().getAll().first()
            val latestBalance = recentTxns.firstOrNull { it.balance != null }?.balance ?: 0.0
            val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
            val weeklySpend = db.transactionDao().totalBetween(sevenDaysAgo, now) ?: 1.0
            val dailyBurnRate = if (weeklySpend > 0) weeklySpend / 7 else 1.0
            val runwayDays = if (latestBalance > 0 && dailyBurnRate > 0) (latestBalance / dailyBurnRate).toInt() else 0
            
            if (runwayDays > 0) {
                _binding?.tvRunway?.text = "M-Pesa Runway: $runwayDays Days"
                _binding?.tvRunway?.setTextColor(if (runwayDays < 3) Color.parseColor("#E53935") else Color.parseColor("#00E676"))
            } else {
                _binding?.tvRunway?.text = "M-Pesa Runway: Add a transaction"
                _binding?.tvRunway?.setTextColor(Color.parseColor("#00E676"))
            }

            // Loans
            val loans = db.transactionDao().getAllLoans().first()
            val totalLoans = loans.sumOf { it.amount }
            _binding?.tvLoansOwed?.text = String.format("Owed to you: Ksh %,.2f", totalLoans)

            // Bar Chart (Grouped by Day)
            val weekTxns = recentTxns.filter { it.timestamp >= startOfWeek }
            val dailyTotals = DoubleArray(7) 
            for (txn in weekTxns) {
                val txnCal = Calendar.getInstance()
                txnCal.timeInMillis = txn.timestamp
                var dayOfWeek = txnCal.get(Calendar.DAY_OF_WEEK) - 2
                if (dayOfWeek < 0) dayOfWeek = 6
                dailyTotals[dayOfWeek] += txn.amount
            }
            val chartData = dailyTotals.map { it.toFloat() }
            val chartLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            _binding?.barChart?.setData(chartData, chartLabels)
        }
    }

    private fun setupDownloadButton() {
        _binding?.btnDownloadReport?.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val list = db.transactionDao().getAll().first()
                if (list.isEmpty()) {
                    Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val csv = StringBuilder("Date,Recipient,Amount\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                for (txn in list) csv.append("${sdf.format(Date(txn.timestamp))},\"${txn.recipient}\",${txn.amount}\n")
                
                try {
                    val filename = "NovaMpesa_Report_${System.currentTimeMillis()}.csv"
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
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
