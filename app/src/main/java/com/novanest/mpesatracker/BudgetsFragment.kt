package com.novanest.mpesatracker
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.databinding.FragmentBudgetsBinding
import com.novanest.mpesatracker.databinding.ItemBudgetBinding
import kotlinx.coroutines.flow.combine
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
            combine(db.categoryDao().getAll(), db.budgetDao().getAll()) { categories, budgets ->
                val budgetList = mutableListOf<Pair<String, Pair<Double, Double>>>()
                val cal = Calendar.getInstance()
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val startOfWeek = cal.timeInMillis
                val now = System.currentTimeMillis()

                for (category in categories) {
                    val budget = budgets.find { it.categoryId == category.id }
                    val spent = db.transactionDao().categoryTotalBetween(category.id, startOfWeek, now) ?: 0.0
                    val budgetAmount = budget?.weeklyAmount ?: 0.0
                    budgetList.add(category.name to Pair(spent, budgetAmount))
                }
                budgetList
            }.collect { list ->
                _binding?.rvBudgets?.adapter = BudgetAdapter(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BudgetAdapter(private val list: List<Pair<String, Pair<Double, Double>>>) : 
    androidx.recyclerview.widget.RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemBudgetBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvCategoryName.text = item.first
        val spent = item.second.first
        val budget = item.second.second
        holder.binding.tvBudgetProgress.text = String.format("Ksh %,.2f / Ksh %,.2f", spent, budget)
        if (budget > 0) {
            holder.binding.progressBar.progress = ((spent / budget) * 100).toInt().coerceAtMost(100)
        }
    }
    override fun getItemCount() = list.size
}
