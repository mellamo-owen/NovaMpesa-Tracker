package com.novanest.mpesatracker
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Transaction
import com.novanest.mpesatracker.databinding.FragmentTransactionsBinding
import com.novanest.mpesatracker.databinding.ItemTransactionBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    
    private var fullList: List<Transaction> = emptyList()
    private var currentFilter = "All"
    private var currentSearch = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.get(requireContext())
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        
        viewLifecycleOwner.lifecycleScope.launch {
            db.transactionDao().getAll().collect { list ->
                fullList = list
                applyFilters()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s.toString().lowercase()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.pillAll.setOnClickListener { setFilter("All") }
        binding.pillOut.setOnClickListener { setFilter("Out") }
        binding.pillIn.setOnClickListener { setFilter("In") }

        _binding?.fabAdd?.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
            val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerCategory)
            
            viewLifecycleOwner.lifecycleScope.launch {
                val categories = db.categoryDao().getAll().first()
                val categoryNames = categories.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setPositiveButton("Save") { _, _ ->
                        val amountStr = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount).text.toString()
                        val recipientStr = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRecipient).text.toString()
                        val amount = amountStr.toDoubleOrNull()
                        val selectedPos = spinner.selectedItemPosition
                        val selectedCategoryId = if (selectedPos >= 0) categories[selectedPos].id else null

                        if (amount != null && recipientStr.isNotBlank()) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                db.transactionDao().insert(Transaction(amount = amount, recipient = recipientStr, categoryId = selectedCategoryId, timestamp = System.currentTimeMillis(), isExternalDeposit = false, isLoan = false, isMoneyIn = false))
                                Toast.makeText(requireContext(), "Expense added!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.setNegativeButton("Cancel", null).show()
            }
        }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        binding.pillAll.setBackgroundResource(R.drawable.bg_pill_inactive_dark)
        binding.pillAll.setTextColor(Color.WHITE)
        binding.pillOut.setBackgroundResource(R.drawable.bg_pill_inactive_dark)
        binding.pillOut.setTextColor(Color.WHITE)
        binding.pillIn.setBackgroundResource(R.drawable.bg_pill_inactive_dark)
        binding.pillIn.setTextColor(Color.WHITE)

        val activePill = when(filter) {
            "All" -> binding.pillAll
            "Out" -> binding.pillOut
            "In" -> binding.pillIn
            else -> binding.pillAll
        }
        activePill.setBackgroundResource(R.drawable.bg_pill_active_dark)
        activePill.setTextColor(Color.parseColor("#121212"))

        applyFilters()
    }

    private fun applyFilters() {
        val filteredList = fullList.filter { txn ->
            val matchesSearch = txn.recipient.lowercase().contains(currentSearch)
            val matchesFilter = when(currentFilter) {
                "In" -> txn.isMoneyIn
                "Out" -> !txn.isMoneyIn
                else -> true
            }
            matchesSearch && matchesFilter
        }
        _binding?.rvTransactions?.adapter = TransactionAdapter(filteredList, onItemClick = { txn ->
            showTransactionOptions(txn)
        })
    }

    private fun showTransactionOptions(txn: Transaction) {
        val options = arrayOf(
            "Set Category (Food, Transport, etc.)",
            if (txn.isExternalDeposit) "Unmark External Deposit" else "Mark as External Deposit",
            if (txn.isLoan) "Unmark Loan" else "Mark as Loan",
            "Delete Transaction"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("${txn.recipient} - Ksh ${txn.amount}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCategorySelector(txn)
                    1 -> toggleExternalDeposit(txn)
                    2 -> toggleLoan(txn)
                    3 -> deleteTransaction(txn)
                }
            }
            .show()
    }

    private fun showCategorySelector(txn: Transaction) {
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = db.categoryDao().getAll().first()
            val categoryNames = categories.map { it.name }.toTypedArray()
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Category")
                .setItems(categoryNames) { _, which ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        db.transactionDao().updateCategory(txn.id, categories[which].id)
                        Toast.makeText(requireContext(), "Categorized as ${categories[which].name}", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun toggleExternalDeposit(txn: Transaction) {
        viewLifecycleOwner.lifecycleScope.launch {
            db.transactionDao().updateDepositStatus(txn.id, !txn.isExternalDeposit)
            val status = if (!txn.isExternalDeposit) "Marked as External Deposit" else "Unmarked External Deposit"
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleLoan(txn: Transaction) {
        viewLifecycleOwner.lifecycleScope.launch {
            db.transactionDao().updateLoanStatus(txn.id, !txn.isLoan)
            val status = if (!txn.isLoan) "Marked as Loan" else "Unmarked Loan"
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteTransaction(txn: Transaction) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction?")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    db.transactionDao().deleteById(txn.id)
                    Toast.makeText(requireContext(), "Transaction Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class TransactionAdapter(
    private val list: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = list[position]
        holder.binding.tvRecipient.text = txn.recipient
        
        val typeStr = when {
            txn.isMoneyIn -> "Received"
            txn.isExternalDeposit -> "External Deposit"
            txn.isLoan -> "Loan"
            else -> "Send money"
        }
        val costStr = if (txn.transactionCost > 0 && !txn.isMoneyIn) " • Fee: Ksh ${txn.transactionCost}" else ""
        
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = sdf.format(Date(txn.timestamp))
        
        holder.binding.tvType.text = "$timeStr • $typeStr$costStr"
        
        if (txn.isMoneyIn) {
            holder.binding.tvAmount.text = "+Ksh ${txn.amount}"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#00E676"))
        } else {
            holder.binding.tvAmount.text = "-Ksh ${txn.amount}"
            holder.binding.tvAmount.setTextColor(Color.WHITE)
        }

        // MAKE ITEM CLICKABLE
        holder.itemView.setOnClickListener {
            onItemClick(txn)
        }
    }

    override fun getItemCount() = list.size
}
