package com.novanest.mpesatracker
import android.os.Bundle
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

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private var currentList: List<Transaction> = emptyList()

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
                currentList = list
                _binding?.rvTransactions?.adapter = TransactionAdapter(list)
            }
        }

        // ADD EXPENSE
        _binding?.btnAdd?.setOnClickListener {
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
                                db.transactionDao().insert(Transaction(amount = amount, recipient = recipientStr, categoryId = selectedCategoryId, timestamp = System.currentTimeMillis(), isExternalDeposit = false, isLoan = false))
                                Toast.makeText(requireContext(), "Expense added!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.setNegativeButton("Cancel", null).show()
            }
        }

        // ADD EXTERNAL DEPOSIT
        _binding?.btnExtDeposit?.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
            dialogView.findViewById<View>(R.id.spinnerCategory).visibility = View.GONE
            dialogView.findViewById<View>(R.id.tvCategoryLabel)?.visibility = View.GONE

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Add External Deposit")
                .setPositiveButton("Save") { _, _ ->
                    val amountStr = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount).text.toString()
                    val recipientStr = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRecipient).text.toString()
                    val amount = amountStr.toDoubleOrNull()

                    if (amount != null && recipientStr.isNotBlank()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            db.transactionDao().insert(Transaction(amount = amount, recipient = recipientStr, categoryId = null, timestamp = System.currentTimeMillis(), isExternalDeposit = true, isLoan = false))
                            Toast.makeText(requireContext(), "External Deposit saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.setNegativeButton("Cancel", null).show()
        }

        // MARK AS LOAN
        _binding?.btnLoan?.setOnClickListener {
            if (currentList.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val items = currentList.map { "Ksh ${it.amount} - ${it.recipient}" }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select to Mark as Loan")
                .setItems(items) { _, which ->
                    val selectedTxn = currentList[which]
                    viewLifecycleOwner.lifecycleScope.launch {
                        db.transactionDao().updateLoanStatus(selectedTxn.id, !selectedTxn.isLoan)
                        val status = if (!selectedTxn.isLoan) "Marked as Loan" else "Unmarked as Loan"
                        Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        // REMOVE
        _binding?.btnRemove?.setOnClickListener {
            if (currentList.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to remove", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val items = currentList.map { "Ksh ${it.amount} - ${it.recipient}" }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select to Remove")
                .setItems(items) { _, which ->
                    val selectedTxn = currentList[which]
                    viewLifecycleOwner.lifecycleScope.launch {
                        db.transactionDao().deleteById(selectedTxn.id)
                        Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class TransactionAdapter(private val list: List<Transaction>) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = list[position]
        holder.binding.tvRecipient.text = txn.recipient
        
        val typeStr = when {
            txn.isExternalDeposit -> "External Deposit"
            txn.isLoan -> "Loan"
            else -> "Expense"
        }
        val costStr = if (txn.transactionCost > 0) " (Fee: ${txn.transactionCost})" else ""
        holder.binding.tvType.text = "$typeStr$costStr"
        
        holder.binding.tvAmount.text = String.format("Ksh %,.2f", txn.amount)
    }

    override fun getItemCount() = list.size
}
