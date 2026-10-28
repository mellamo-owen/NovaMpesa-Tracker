package com.novanest.mpesatracker
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Category
import com.novanest.mpesatracker.databinding.FragmentBudgetsBinding
import com.novanest.mpesatracker.databinding.ItemBudgetBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetsFragment : Fragment() {
    private var _binding: FragmentBudgetsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.get(requireContext())
        binding.rvBudgets.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val categories = db.categoryDao().getAll().first()
            val allTxns = db.transactionDao().getAll().first()
            
            // Exclude External Deposit from the spending breakdown
            val spendingCategories = categories.filter { it.name != "External Deposit" }

            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val startOfWeek = cal.timeInMillis
            val now = System.currentTimeMillis()

            val categoryTotals = spendingCategories.map { category ->
                val totalSpent = allTxns.filter { 
                    it.categoryId == category.id && 
                    it.timestamp >= startOfWeek && 
                    !it.isLoan 
                }.sumOf { it.amount }
                
                category to totalSpent
            }

            _binding?.rvBudgets?.adapter = CategorySpendingAdapter(categoryTotals)
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class CategorySpendingAdapter(private val list: List<Pair<Category, Double>>) : 
    RecyclerView.Adapter<CategorySpendingAdapter.ViewHolder>() {
    
    class ViewHolder(val binding: ItemBudgetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (category, totalSpent) = list[position]
        holder.binding.tvCategoryName.text = category.name
        holder.binding.tvBudgetStatus.text = String.format("Ksh %,.2f", totalSpent)
        
        // Calculate relative progress bar against the highest spending category
        val maxSpent = list.maxOfOrNull { it.second } ?: 1.0
        val progress = if (maxSpent > 0) ((totalSpent / maxSpent) * 100).toInt() else 0
        holder.binding.progressBar.progress = progress.coerceIn(0, 100)
    }

    override fun getItemCount() = list.size
}
