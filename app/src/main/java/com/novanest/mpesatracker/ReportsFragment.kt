package com.novanest.mpesatracker
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfWriter
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Transaction
import com.novanest.mpesatracker.databinding.FragmentReportsBinding
import com.novanest.mpesatracker.databinding.ItemReportRowBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ReportsFragment : Fragment() {
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private var allTxns: List<Transaction> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.get(requireContext())
        binding.rvReportTable.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            db.transactionDao().getAll().collect { list ->
                allTxns = list
                updateUI()
            }
        }

        _binding?.btnDownloadPdf?.setOnClickListener {
            generatePdf()
        }
    }

    private fun updateUI() {
        val now = System.currentTimeMillis()
        val startOfMonth = now - TimeUnit.DAYS.toMillis(30)

        val totalSpent = allTxns.filter { 
            it.timestamp >= startOfMonth && 
            !it.isExternalDeposit && !it.isLoan && !it.isMoneyIn 
        }.sumOf { it.amount + it.transactionCost }

        val totalCosts = allTxns.filter { 
            it.timestamp >= startOfMonth && !it.isMoneyIn 
        }.sumOf { it.transactionCost }

        _binding?.tvTotalSpent?.text = String.format("Ksh %,.2f", totalSpent)
        _binding?.tvTotalCosts?.text = String.format("Ksh %,.2f", totalCosts)

        // Update Bar Chart
        val weeklyTotals = DoubleArray(7)
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val startOfWeek = cal.timeInMillis

        allTxns.filter { it.timestamp >= startOfWeek && !it.isMoneyIn && !it.isExternalDeposit && !it.isLoan }.forEach {
            val txnCal = Calendar.getInstance()
            txnCal.timeInMillis = it.timestamp
            var day = txnCal.get(Calendar.DAY_OF_WEEK) - 2
            if (day < 0) day = 6
            weeklyTotals[day] += (it.amount + it.transactionCost)
        }

        val chartData = weeklyTotals.map { it.toFloat() }
        val chartLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        _binding?.reportChart?.setData(chartData, chartLabels)

        // Update Table
        _binding?.rvReportTable?.adapter = ReportAdapter(allTxns.take(50))
    }

    private fun generatePdf() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val now = System.currentTimeMillis()
                val startOfMonth = now - TimeUnit.DAYS.toMillis(30)
                
                val totalSpent = allTxns.filter { it.timestamp >= startOfMonth && !it.isExternalDeposit && !it.isLoan && !it.isMoneyIn }.sumOf { it.amount + it.transactionCost }
                val totalCosts = allTxns.filter { it.timestamp >= startOfMonth && !it.isMoneyIn }.sumOf { it.transactionCost }

                // 1. Create PDF Document
                val document = Document()
                val filename = "NovaMpesa_Report_${System.currentTimeMillis()}.pdf"
                val file = File(requireContext().cacheDir, filename)
                PdfWriter.getInstance(document, FileOutputStream(file))
                document.open()

                // 2. Add Title
                val titleFont = Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD, BaseColor.BLACK)
                val normalFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.DARK_GRAY)
                
                val title = Paragraph("NovaMpesa Financial Report", titleFont)
                title.alignment = Element.ALIGN_CENTER
                document.add(title)
                document.add(Paragraph("Generated on: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}", normalFont))
                document.add(Paragraph("\n"))

                // 3. Add Summary
                document.add(Paragraph("Summary (Last 30 Days)", Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)))
                document.add(Paragraph("Total Spent: Ksh ${String.format("%,.2f", totalSpent)}", normalFont))
                document.add(Paragraph("Total Transaction Costs: Ksh ${String.format("%,.2f", totalCosts)}", normalFont))
                document.add(Paragraph("\n"))

                // 4. Add Chart Image
                val chartView = binding.reportChart
                if (chartView.width > 0 && chartView.height > 0) {
                    val bitmap = Bitmap.createBitmap(chartView.width, chartView.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    chartView.draw(canvas)
                    
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val bitmapArray = stream.toByteArray()
                    
                    val image = Image.getInstance(bitmapArray)
                    image.scaleToFit(500f, 250f)
                    image.alignment = Element.ALIGN_CENTER
                    document.add(image)
                }
                document.add(Paragraph("\n"))

                // 5. Add Transaction Table
                document.add(Paragraph("Recent Transactions", Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)))
                val table = PdfPTable(3)
                table.widthPercentage = 100f
                table.setWidths(floatArrayOf(1f, 2f, 1f))

                // Header
                table.addCell(PdfPCell(Phrase("Date", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE))))
                table.addCell(PdfPCell(Phrase("Recipient", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE))))
                table.addCell(PdfPCell(Phrase("Amount", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE))))
                
                table.rows[0].cells.forEach { it.backgroundColor = BaseColor.DARK_GRAY }

                // Data
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                for (txn in allTxns.take(20)) {
                    table.addCell(sdf.format(Date(txn.timestamp)))
                    table.addCell(txn.recipient)
                    val amt = if (txn.isMoneyIn) "+${txn.amount}" else "-${txn.amount}"
                    table.addCell(amt)
                }

                document.add(table)
                document.close()

                // 6. Save to Downloads
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = requireContext().contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(file.readBytes())
                        }
                    }
                }

                Toast.makeText(requireContext(), "PDF Saved to Downloads!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class ReportAdapter(private val list: List<Transaction>) : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemReportRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemReportRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = list[position]
        holder.binding.tvName.text = txn.recipient
        holder.binding.tvDate.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(txn.timestamp))

        if (txn.isMoneyIn) {
            holder.binding.tvAmount.text = "+${txn.amount}"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#00E676"))
        } else {
            holder.binding.tvAmount.text = "-${txn.amount}"
            holder.binding.tvAmount.setTextColor(Color.WHITE)
        }
    }

    override fun getItemCount() = list.size
}

