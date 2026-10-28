package com.novanest.mpesatracker
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
        
        viewLifecycleOwner.lifecycleScope.launch {
            val state = db.appStateDao().getState()
            if (state != null) {
                _binding?.tvGreeting?.text = "Hello, ${state.userName}"
                
                val now = System.currentTimeMillis()
                val trialEnd = state.trialStart + TimeUnit.DAYS.toMillis(60)
                val isSubscribed = state.subscriptionExpiry != null && state.subscriptionExpiry > now
                
                if (isSubscribed) {
                    _binding?.tvTrialBadge?.text = "Premium Active"
                    _binding?.tvTrialBadge?.setBackgroundColor(Color.parseColor("#00E676"))
                } else {
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(trialEnd - now).toInt()
                    if (daysLeft > 0) {
                        _binding?.tvTrialBadge?.text = "Trial: $daysLeft days left"
                        _binding?.tvTrialBadge?.setBackgroundColor(Color.parseColor("#00E676"))
                    } else {
                        _binding?.tvTrialBadge?.text = "Trial Expired"
                        _binding?.tvTrialBadge?.setBackgroundColor(Color.parseColor("#E53935"))
                    }
                }
            }

            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfWeek = cal.timeInMillis
            val now = System.currentTimeMillis()

            val allTxns = db.transactionDao().getAll().first()
            
            val weekExp = allTxns.filter { it.timestamp >= startOfWeek && !it.isLoan }.sumOf { it.amount }
            val monthExp = allTxns.filter { it.timestamp >= (now - TimeUnit.DAYS.toMillis(30)) && !it.isLoan }.sumOf { it.amount }
            val deposits = allTxns.filter { it.isLoan }.sumOf { it.amount }

            _binding?.tvWeekTotal?.text = String.format("Ksh %,.2f", weekExp)
            _binding?.tvMonthTotal?.text = String.format("Ksh %,.2f", monthExp)
            _binding?.tvExternalDeposits?.text = String.format("External Deposits: Ksh %,.2f", deposits)
            
            // Runway
            val latestBalance = allTxns.firstOrNull { it.balance != null }?.balance ?: 0.0
            val weeklySpend = allTxns.filter { it.timestamp >= (now - TimeUnit.DAYS.toMillis(7)) && !it.isLoan }.sumOf { it.amount }
            val dailyBurn = if (weeklySpend > 0) weeklySpend / 7 else 1.0
            val runwayDays = if (latestBalance > 0 && dailyBurn > 0) (latestBalance / dailyBurn).toInt() else 0
            
            if (runwayDays > 0) {
                _binding?.tvRunway?.text = "M-Pesa Runway: $runwayDays Days"
                _binding?.tvRunway?.setTextColor(if (runwayDays < 3) Color.parseColor("#E53935") else Color.parseColor("#00E676"))
            } else {
                _binding?.tvRunway?.text = "M-Pesa Runway: Add a transaction"
            }

            // Bar Chart
            val weekTxns = allTxns.filter { it.timestamp >= startOfWeek && !it.isLoan }
            val dailyTotals = DoubleArray(7)
            for (txn in weekTxns) {
                val txnCal = Calendar.getInstance()
                txnCal.timeInMillis = txn.timestamp
                var day = txnCal.get(Calendar.DAY_OF_WEEK) - 2
                if (day < 0) day = 6
                dailyTotals[day] += txn.amount
            }
            val chartData = dailyTotals.map { it.toFloat() }
            val chartLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            _binding?.barChart?.setData(chartData, chartLabels)
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
