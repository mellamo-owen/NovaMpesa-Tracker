package com.novanest.mpesatracker
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Transaction
import com.novanest.mpesatracker.databinding.FragmentTransactionsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.get(requireContext())
        
        viewLifecycleOwner.lifecycleScope.launch {
            db.transactionDao().getAll().collect { list ->
                if (list.isEmpty()) {
                    _binding?.tvList?.text = "No transactions yet."
                } else {
                    _binding?.tvList?.text = list.joinToString("\n\n") { "Ksh ${it.amount} - ${it.recipient} ${if(it.isLoan) "(Deposit)" else ""}" }
                }
            }
        }

        // OPEN ADD EXPENSE DIALOG
        _binding?.btnAdd?.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
            val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerCategory)
            
            // Load categories into spinner
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
                                db.transactionDao().insert(Transaction(
                                    amount = amount, 
                                    recipient = recipientStr, 
                                    categoryId = selectedCategoryId, 
                                    timestamp = System.currentTimeMillis()
                                ))
                                Toast.makeText(requireContext(), "Expense added!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Please enter amount and description", Toast.LENGTH_SHORT).show()
                        }
                    }.setNegativeButton("Cancel", null).show()
            }
        }

        // REMOVE EXPENSE DIALOG
        _binding?.btnRemove?.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val list = db.transactionDao().getAll().first()
                if (list.isEmpty()) {
                    Toast.makeText(requireContext(), "No transactions to remove", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val items = list.map { "Ksh ${it.amount} - ${it.recipient}" }.toTypedArray()
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Expense to Remove")
                    .setItems(items) { _, which ->
                        val selectedTxn = list[which]
                        viewLifecycleOwner.lifecycleScope.launch {
                            db.transactionDao().deleteById(selectedTxn.id)
                            Toast.makeText(requireContext(), "Expense Removed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
