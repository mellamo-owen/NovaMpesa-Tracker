package com.novanest.mpesatracker
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Transaction
import com.novanest.mpesatracker.databinding.FragmentTransactionsBinding
import com.novanest.mpesatracker.databinding.ItemTransactionBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
            db.transactionDao().getAll().collect { transactions ->
                currentList = transactions
                _binding?.rvTransactions?.adapter = TransactionAdapter(transactions)
            }
        }

        _binding?.btnAdd?.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val amountStr = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount).text.toString()
                    val recipientStr = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRecipient).text.toString()
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && recipientStr.isNotBlank()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            db.transactionDao().insert(Transaction(0, amount, recipientStr, null, null, null, System.currentTimeMillis(), null, true))
                        }
                    }
                }.setNegativeButton("Cancel", null).show()
        }

        _binding?.btnRemove?.setOnClickListener {
            if (currentList.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to remove", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val items = currentList.map { "Ksh ${it.amount} - ${it.recipient}" }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Expense to Remove")
                .setItems(items) { _, which ->
                    val selectedTxn = currentList[which]
                    viewLifecycleOwner.lifecycleScope.launch {
                        db.transactionDao().delete(selectedTxn)
                        Toast.makeText(requireContext(), "Expense Removed", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        _binding?.btnLoan?.setOnClickListener {
            if (currentList.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val items = currentList.map { "Ksh ${it.amount} - ${it.recipient}" }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Expense to mark as Loan")
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
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class TransactionAdapter(private val list: List<Transaction>) : androidx.recyclerview.widget.RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemTransactionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = list[position]
        val loanTag = if (txn.isLoan) " [LOAN]" else ""
        holder.binding.tvRecipient.text = txn.recipient + loanTag
        holder.binding.tvAmount.text = String.format("Ksh %,.2f", txn.amount)
        holder.binding.tvDate.text = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(txn.timestamp))
    }
    override fun getItemCount() = list.size
}
