package com.elgenium.recordkeeper

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.adapters.CreditsAdapter
import com.elgenium.recordkeeper.adapters.ExpenseAdapter
import com.elgenium.recordkeeper.adapters.MerchandiseSummaryAdapter
import com.elgenium.recordkeeper.adapters.PaymentAdapter
import com.elgenium.recordkeeper.adapters.PriceAdapter
import com.elgenium.recordkeeper.databinding.ActivityAllReportsBinding
import com.elgenium.recordkeeper.databinding.DialogAddExpenseBinding
import com.elgenium.recordkeeper.firebase.models.Credits
import com.elgenium.recordkeeper.firebase.models.Expense
import com.elgenium.recordkeeper.firebase.models.MerchandiseSummary
import com.elgenium.recordkeeper.firebase.models.Payment
import com.elgenium.recordkeeper.firebase.models.PriceItem
import com.elgenium.recordkeeper.firebase.models.TallyRecord
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class AllReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllReportsBinding
    private lateinit var database: DatabaseReference
    private val tallyRecords = mutableListOf<TallyRecord>()
    private lateinit var paymentAdapter: PaymentAdapter
    private var credits = mutableListOf<Credits>()
    private val payments = mutableListOf<Payment>()
    private lateinit var priceAdapter: PriceAdapter
    private var recordName: String = ""
    private var totalCashIncurred: Double = 0.0
    private var totalKilosPaid = 0.0
    private var totalCashPaid = 0.0
    private var totalKilosCredit = 0.0
    private var totalCashCredit = 0.0
    private var totalKilosGrandTotal = 0.0
    private var totalCashGrandTotal = 0.0
    private lateinit var merchandiseSummaryAdapter: MerchandiseSummaryAdapter
    private val groupedMerchandiseTypes = setOf("Katambak Small", "Katambak Big", "Keros Small", "Keros Big", "Sari")
    private lateinit var expensesAdapter: ExpenseAdapter
    private var expenses: MutableList<Expense> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        recordName = intent.getStringExtra("RECORD_NAME").toString()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_tally_record -> {
                    val intent = Intent(this, AddRecordActivity::class.java)
                    intent.putExtra("RECORD_NAME", recordName)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_reports -> {
                    // Handle Reports click
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.menu_reports

        // Initialize RecyclerView for payments
        paymentAdapter = PaymentAdapter(payments)
        binding.recyclerViewPayments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPayments.adapter = paymentAdapter

        // Initialize RecyclerView for prices
        priceAdapter = PriceAdapter(emptyList()) // Initialize PriceAdapter with an empty list
        binding.recyclerViewPrice.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPrice.adapter = priceAdapter

        // Initialize RecyclerView for expenses
        expensesAdapter = ExpenseAdapter(expenses)
        binding.recyclerViewExpenses.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewExpenses.adapter = expensesAdapter

        // Set up ItemTouchHelper for swipe to delete payments
        val itemTouchHelperCallbackForPayments = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We are not supporting move operation
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                showDeleteConfirmationDialogForPayment(position)
            }
        }

        val itemTouchHelperForPayments = ItemTouchHelper(itemTouchHelperCallbackForPayments)
        itemTouchHelperForPayments.attachToRecyclerView(binding.recyclerViewPayments)

// Set up ItemTouchHelper for swipe to delete expenses
        val itemTouchHelperCallbackForExpenses = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We are not supporting move operation
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                showDeleteConfirmationDialogForExpenses(position)
            }
        }

        val itemTouchHelperForExpenses = ItemTouchHelper(itemTouchHelperCallbackForExpenses)
        itemTouchHelperForExpenses.attachToRecyclerView(binding.recyclerViewExpenses)

        // Inside onCreate after inflating the binding
        binding.recyclerViewCredits.apply {
            layoutManager = LinearLayoutManager(this@AllReportsActivity)
            adapter = CreditsAdapter(credits)
        }

        // Set up Add Payment button
        binding.buttonAddPayment.setOnClickListener {
            showAddPaymentDialog()
        }

        binding.buttonAddParticulars.setOnClickListener {
            showAddExpenseDialog()
        }

        // Initialize RecyclerView for merchandise summary
        merchandiseSummaryAdapter = MerchandiseSummaryAdapter(emptyList()) // Initialize with an empty list
        binding.recyclerViewNumberOfKilos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNumberOfKilos.adapter = merchandiseSummaryAdapter


        // Load records and payments
        loadTallyRecords(recordName)
        loadPayments(recordName)
        loadExpenses()


        binding.exportButton.setOnClickListener {
            sendSummary()
        }
    }

    private fun sendSummary() {
        val totalPayments = getAndUpdateTotalPayments()
        val paymentSummary = buildPaymentSummary()
        val pasilCash = binding.valuePasilCash.text.toString()
        val subtotal = binding.valueSubtotal.text.toString()
        val lessExpenses = binding.valueLessExpenses.text.toString()
        val remaining = binding.valueRemaining.text.toString()
        val totalCredits = getTotalCredits()
        val creditSummary = buildCreditSummary()
        val priceSummary = buildPriceSummary()
        val kiloSummary = buildKiloSummary()
        val expenseSummary = buildExpenseSummary()
        val transactionDate = recordName.split(" ")[0]

        val summaryAndPaymentsMessage = "Summary:\n" +
                "Payments: $totalPayments\n" +
                "Pasil Cash: $pasilCash\n" +
                "Subtotal: $subtotal\n" +
                "Less Expenses: $lessExpenses\n" +
                "Remaining: $remaining\n\n" +
                "Payments\n" +
                "Payment Details:\n$paymentSummary\n" +
                "Total Payments: $totalPayments\n\n"


        val pricesAndCreditsMessage =   "Credits:\n" +
                "Credit Details:\n$creditSummary\n" +
                "Total Credits: $totalCredits\n\n" +
                "Prices:\n" +
                "$priceSummary"

        val numberOfKilosMessage =  "Kilos:\n" +
                "$kiloSummary\n" +
                "Total Cash: ${String.format(Locale.US,"%.2f", totalKilosPaid)}kgs ; ${currencyFormatter(totalCashPaid)}\n" +
                "Total Credit: ${String.format(Locale.US,"%.2f", totalKilosCredit)}kgs ; ${currencyFormatter(totalCashCredit)}\n" +
                "Overall: ${String.format(Locale.US,"%.2f", totalKilosGrandTotal)}kgs ; ${currencyFormatter(totalCashGrandTotal)}\n\n"

        val expensesMessage = "Cash Flow Statement\n" +
                "Pasil Cash: $pasilCash \n" +
                "Payments: $totalPayments \n" +
                "Subtotal: $subtotal \n\n" +
                "Less:\n" +
                "** $transactionDate \n" +
                "$expenseSummary\n" +
                "Total Expenses: $lessExpenses \n" +
                "Remaining: $remaining"




        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("summary_and_payments_message", summaryAndPaymentsMessage)
            putExtra("prices_and_credits_message", pricesAndCreditsMessage)
            putExtra("number_of_kilos_message", numberOfKilosMessage)
            putExtra("expenses_message", expensesMessage)
        }
        startActivity(intent)
    }

    private fun buildExpenseSummary(): String {
        val expenseItems = expensesAdapter.getExpenseSummaries()
        val expenseSummary = StringBuilder()

        expenseItems.forEach { summary ->
            val description = if (summary.expenseDescription.isEmpty()) {
                summary.expenseType
            } else {
                "${summary.expenseType} (${summary.expenseDescription})"
            }

            if (summary.showQuantityAndPrice) {
                val quantity = summary.quantity
                val price = summary.price
                val amount = quantity * price
                expenseSummary.append("$description: $quantity x ${String.format(Locale.US, "%.2f", price)} = ${currencyFormatter(amount)}\n")
            } else {
                expenseSummary.append("$description: ${currencyFormatter(summary.amount)}\n")
            }
        }

        return expenseSummary.toString()
    }

    private fun buildKiloSummary(): String {
        val kiloItems = merchandiseSummaryAdapter.getMerchandiseSummaries()
        val kiloSummary = StringBuilder()

        kiloItems.forEach { summary ->
            kiloSummary.append("${summary.merchandiseType}: ${summary.totalKilos}kgs ; ${currencyFormatter(summary.totalAmount)}\n")
        }

        return kiloSummary.toString()
    }


    private fun buildPaymentSummary(): String {
        val paymentSummary = StringBuilder()
        payments.forEachIndexed { index, payment ->
            paymentSummary.append("${index + 1}. Buyer: ${payment.buyerName}, Amount: ${currencyFormatter(payment.paymentAmount)}\n")
        }
        return paymentSummary.toString()
    }

    private fun buildPriceSummary(): String {
        val priceItems = priceAdapter.getPriceItems()
        val priceSummary = StringBuilder()

        priceItems.forEach { priceItem ->
            val formattedPrices = priceItem.prices.joinToString("/") { DecimalFormat("#,##0.00").format(it) }
            priceSummary.append("${priceItem.merchandiseType}: $formattedPrices\n")
        }

        return priceSummary.toString()
    }



    private fun getTotalCredits(): Double {
        return credits.sumOf { it.totalAmount }
    }

    private fun buildCreditSummary(): String {
        val creditSummary = StringBuilder()
        credits.forEachIndexed { index, credit ->
            creditSummary.append("${index + 1}. Buyer: ${credit.buyer}, Amount: ${currencyFormatter(credit.totalAmount)}\n")
        }
        return creditSummary.toString()
    }


    private fun loadExpenses() {
        database.child("records").child(recordName).child("expenses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenses.clear()
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java)
                    expense?.let { expenses.add(it) }
                }
                expensesAdapter.updateData(expenses)
                getAndUpdateTotalExpenses()
                updateCashFlowStatement()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun showAddExpenseDialog() {
        val dialogViewBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogViewBinding.root)
        val dialog = dialogBuilder.create()


        dialogViewBinding.buttonAddExpense.setOnClickListener {
            val expenseType = dialogViewBinding.spinnerExpense.selectedItem.toString()
            val expenseDescription = dialogViewBinding.editTextExpense.text.toString()
            val quantity = dialogViewBinding.editTextQuantity.text.toString().toIntOrNull() ?: 0
            val price = dialogViewBinding.editTextUnit.text.toString().toDoubleOrNull() ?: 0.0
            val amount = dialogViewBinding.editTextAmount.text.toString().toDoubleOrNull() ?: 0.0
            val showQuantityAndPrice = dialogViewBinding.switchUseQuantityUnit.isChecked

            val expense = Expense(expenseType, expenseDescription, quantity, price, amount, showQuantityAndPrice)
            saveExpenseToFirebase(expense)

            dialog.dismiss()
        }

        dialogViewBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        // Set initial state based on the switch
        dialogViewBinding.switchUseQuantityUnit.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dialogViewBinding.quantityTextInputLayout.isEnabled = true
                dialogViewBinding.priceTextInputLayout.isEnabled = true
                dialogViewBinding.amountTextInputLayout.isEnabled = false
                updateAmountField(dialogViewBinding)
            } else {
                dialogViewBinding.quantityTextInputLayout.isEnabled = false
                dialogViewBinding.priceTextInputLayout.isEnabled = false
                dialogViewBinding.amountTextInputLayout.isEnabled = true
            }
        }

        // Set the initial state of input fields based on the switch state
        val initialSwitchState = dialogViewBinding.switchUseQuantityUnit.isChecked
        dialogViewBinding.quantityTextInputLayout.isEnabled = initialSwitchState
        dialogViewBinding.priceTextInputLayout.isEnabled = initialSwitchState
        dialogViewBinding.amountTextInputLayout.isEnabled = !initialSwitchState

        // Add TextWatchers to update the amount field automatically
        dialogViewBinding.editTextQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (dialogViewBinding.switchUseQuantityUnit.isChecked) {
                    updateAmountField(dialogViewBinding)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogViewBinding.editTextUnit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (dialogViewBinding.switchUseQuantityUnit.isChecked) {
                    updateAmountField(dialogViewBinding)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateAmountField(dialogBinding: DialogAddExpenseBinding) {
        val quantity = dialogBinding.editTextQuantity.text.toString().toIntOrNull() ?: 0
        val price = dialogBinding.editTextUnit.text.toString().toDoubleOrNull() ?: 0.0
        val amount = quantity * price
        dialogBinding.editTextAmount.setText(amount.toString())
    }

    private fun saveExpenseToFirebase(expense: Expense) {
        val expenseId = database.child("records").child(recordName).child("expenses").push().key
        if (expenseId != null) {
            database.child("records").child(recordName).child("expenses").child(expenseId).setValue(expense)
                .addOnSuccessListener {
                    Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add expense", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadTallyRecords(recordName: String) {
        database.child("records").child(recordName).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tallyRecords.clear()
                // Clear credits list before populating it
                credits.clear()

                totalCashIncurred = 0.0 // Reset totalCashIncurred
                // Initialize a Map to store prices grouped by merchandise type
                val pricesByMerchandiseType = mutableMapOf<String, MutableList<Pair<Double, Int>>>()
                val summaryByMerchandiseType = mutableMapOf<String, MerchandiseSummary>()

                for (recordSnapshot in snapshot.children) {
                    val tallyRecord = recordSnapshot.getValue(TallyRecord::class.java)
                    if (tallyRecord != null) {
                        tallyRecords.add(tallyRecord)
                        // Calculate total cash incurred for records with paymentStatus set to "Paid"
                        if (tallyRecord.paymentStatus == "Paid") {
                            totalCashIncurred += tallyRecord.totalAmount
                        }


                        // Group prices by merchandise type
                        val merchandiseType = tallyRecord.merchandiseType
                        val price = tallyRecord.price
                        if (merchandiseType.isNotBlank() && price > 0) {
                            val priceList = pricesByMerchandiseType.getOrPut(merchandiseType) { mutableListOf() }
                            // Check if this price already exists in the list
                            val existingPriceIndex = priceList.indexOfFirst { it.first == price }
                            if (existingPriceIndex != -1) {
                                // Increment the count of occurrences for the existing price
                                val existingPriceCount = priceList[existingPriceIndex].second
                                priceList[existingPriceIndex] = Pair(price, existingPriceCount + 1)
                            } else {
                                // Add the new price with a count of 1
                                priceList.add(Pair(price, 1))
                            }
                        }

                        if (merchandiseType.isNotBlank()){
                            // Group specified merchandise types under "Isda"
                            val groupedMerchandiseType = if (groupedMerchandiseTypes.contains(merchandiseType)) "Isda" else merchandiseType
                            val kilos = tallyRecord.kilos
                            val totalAmount = tallyRecord.totalAmount
                            val paymentStatus = tallyRecord.paymentStatus

                            val summary = summaryByMerchandiseType.getOrPut(groupedMerchandiseType) {
                                MerchandiseSummary(
                                    groupedMerchandiseType,
                                    0.0,
                                    0.0,
                                    0.0,
                                    0.0,
                                    0.0,
                                    0.0
                                )
                            }

                            when (paymentStatus) {
                                "Paid" -> {
                                    summary.kilosInCash += kilos
                                    summary.paidInCash += totalAmount
                                }
                                "Credit" -> {
                                    summary.kilosInCredit += kilos
                                    summary.unpaid += totalAmount
                                }
                            }

                            summary.totalKilos += kilos
                            summary.totalAmount += totalAmount

                            summaryByMerchandiseType[groupedMerchandiseType] = summary


                        }

                        // Add credits data
                        if (tallyRecord.paymentStatus == "Credit") {
                            credits.add(Credits(
                                buyer = tallyRecord.buyer,
                                kilos = tallyRecord.kilos,
                                price = tallyRecord.price,
                                merchandiseType = tallyRecord.merchandiseType,
                                totalAmount = tallyRecord.totalAmount
                            ))
                        }





                    }
                }

                // Prepare a list for the RecyclerView
                val summaryList = summaryByMerchandiseType.values.toList()
                updateMerchandiseSummaryRecyclerView(summaryList)

                // Once all records are loaded, update the RecyclerView with records having paymentStatus as "Credit"
                val creditRecords = tallyRecords
                    .filter { it.paymentStatus == "Credit" }
                    .map { record ->
                        Credits(
                            buyer = record.buyer,
                            kilos = record.kilos,
                            price = record.price,
                            merchandiseType = record.merchandiseType,
                            totalAmount = record.totalAmount
                        )
                    }

                updateRecyclerView(creditRecords)
                // Update the RecyclerView with prices and merchandise types
                updatePriceRecyclerView(pricesByMerchandiseType)

                // Process prices grouped by merchandise type
                processPricesByMerchandiseType(pricesByMerchandiseType)

                calculateTotals(summaryList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AllReportsActivity, "Failed to load records", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateMerchandiseSummaryRecyclerView(summaryList: List<MerchandiseSummary>) {
        merchandiseSummaryAdapter.updateData(summaryList)
    }

    private fun processPricesByMerchandiseType(pricesByMerchandiseType: Map<String, List<Pair<Double, Int>>>) {
        // Iterate over the map and display the merchandise type and its prices
        for ((merchandiseType, prices) in pricesByMerchandiseType) {
            // Display the merchandise type
            Log.d("Prices", "Merchandise Type: $merchandiseType")

            // Display each price and its count of occurrences
            for ((price, count) in prices) {
                Log.d("Prices", "Price: $price, Count: $count")
            }
        }
    }

    private fun showAddPaymentDialog() {
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Payment")
            .setPositiveButton("Add") { _, _ ->
                // Handle adding the payment
                val spinnerBuyerName: Spinner = dialogView.findViewById(R.id.spinnerBuyerName)
                val editTextPaymentAmount: TextInputEditText = dialogView.findViewById(R.id.editTextPaymentAmount)

                val buyerName = spinnerBuyerName.selectedItem.toString()
                val paymentAmount = editTextPaymentAmount.text.toString().toDouble()
                val newPayment = Payment(buyerName, paymentAmount)


                savePaymentToFirebase(newPayment)
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()


        dialog.show()
    }

    private fun showDeleteConfirmationDialogForPayment(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Payment")
            .setMessage("Are you sure you want to delete this payment?")
            .setPositiveButton("Yes") { _, _ ->
                // Get the payment from the adapter
                val payment = paymentAdapter.getPaymentAt(position)
                // Call function to get payment ID and delete payment
                getPaymentId(payment)
            }
            .setNegativeButton("No") { _, _ ->
                paymentAdapter.notifyItemChanged(position) // Restore the item
            }
            .setCancelable(false)
            .show()
    }

    private fun showDeleteConfirmationDialogForExpenses(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expenses")
            .setMessage("Are you sure you want to delete this expenses?")
            .setPositiveButton("Yes") { _, _ ->
                // Get the payment from the adapter
                val expenses = expensesAdapter.getExpensesAt(position)
                // Call function to get payment ID and delete payment
                getAndDeleteExpenseById(expenses)
            }
            .setNegativeButton("No") { _, _ ->
                expensesAdapter.notifyItemChanged(position) // Restore the item
            }
            .setCancelable(false)
            .show()
    }

    private fun getAndDeleteExpenseById(expenses: Expense) {
        database.child("records").child(recordName).child("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val expenseType = data.child("expenseType").getValue(String::class.java)

                        if (expenseType == expenses.expenseType) {
                            val expenseId = data.key
                            if (expenseId != null) {
                                // Delete expense directly from Firebase
                                database.child("records").child(recordName).child("expenses").child(expenseId)
                                    .removeValue()
                                    .addOnSuccessListener {
                                        // Since we can't get the position here, we can't directly remove it from the adapter
                                        Toast.makeText(this@AllReportsActivity, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AllReportsActivity, "Failed to delete expense", Toast.LENGTH_SHORT).show()
                                    }
                                return
                            } else {
                                Toast.makeText(this@AllReportsActivity, "Failed to find expense ID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AllReportsActivity", "Failed to get expense ID", error.toException())
                }
            })
    }

    private fun getPaymentId(payment: Payment) {
        database.child("records").child(recordName).child("payments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val buyerName = data.child("buyerName").getValue(String::class.java)
                        val amount = data.child("paymentAmount").getValue(Double::class.java)
                        if (buyerName == payment.buyerName && amount == payment.paymentAmount) {
                            val paymentId = data.key
                            if (paymentId != null) {
                                // Delete payment directly from Firebase
                                database.child("records").child(recordName).child("payments").child(paymentId)
                                    .removeValue()
                                    .addOnSuccessListener {
                                        // Since we can't get the position here, we can't directly remove it from the adapter
                                        Toast.makeText(this@AllReportsActivity, "Payment deleted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AllReportsActivity, "Failed to delete payment", Toast.LENGTH_SHORT).show()
                                    }
                                return
                            } else {
                                Toast.makeText(this@AllReportsActivity, "Failed to find payment ID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AllReportsActivity", "Failed to get payment ID", error.toException())
                }
            })
    }

    private fun savePaymentToFirebase(payment: Payment) {
        val paymentId = database.child("records").child(recordName).child("payments").push().key
        if (paymentId != null) {
            database.child("records").child(recordName).child("payments").child(paymentId).setValue(payment)
                .addOnSuccessListener {
                    Toast.makeText(this, "Payment added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add payment", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadPayments(recordName: String) {
        database.child("records").child(recordName).child("payments").addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                payments.clear()
                for (paymentSnapshot in snapshot.children) {
                    val payment = paymentSnapshot.getValue(Payment::class.java)
                    if (payment != null) {
                        payments.add(payment)
                    }
                }
                paymentAdapter.notifyDataSetChanged()
                val totalPayments = getAndUpdateTotalPayments()
                setupSummaryCardInfo(totalPayments)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AllReportsActivity, "Failed to load payments", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getAndUpdateTotalPayments():Double {
        val totalPayments = payments.sumOf { it.paymentAmount }
        binding.totalPayments.text = currencyFormatter(totalPayments)


        return totalPayments
    }

    private fun currencyFormatter(monetaryValue: Double): String {
        val formattedAmount = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("PHP")
        }.format(monetaryValue)

        return formattedAmount
    }

    private fun setupSummaryCardInfo(totalPayments: Double) {
        // Append value for Total Payments
        binding.valuePayments.text = currencyFormatter(totalPayments)

        // Append value for Pasil Cash
        binding.valuePasilCash.text = currencyFormatter(totalCashIncurred)

        val subTotal = totalPayments + totalCashIncurred

        //Calculate and append subtotal
        binding.valueSubtotal.text = currencyFormatter(subTotal)

        // Append value for Total Payments on Cash Flow Statement
        binding.paymentsValue.text = currencyFormatter(totalPayments)

    }


    private fun updateRecyclerView(creditRecords: List<Credits>) {
        // Get the RecyclerView instance from binding
        val recyclerViewCredits = binding.recyclerViewCredits

        // Create or update the adapter with the filtered credit records
        recyclerViewCredits.adapter = CreditsAdapter(creditRecords)

        // Calculate total credits
        val totalCredits = creditRecords.sumOf { it.totalAmount }

        // Format and update the totalCredits TextView
        binding.totalCredits.text = currencyFormatter(totalCredits)
    }

    private fun updatePriceRecyclerView(pricesByMerchandiseType: Map<String, List<Pair<Double, Int>>>) {
        // Convert the Map<String, List<Pair<Double, Int>>> to List<PriceItem> for the adapter
        val priceItems = pricesByMerchandiseType.map { (merchandiseType, prices) ->
            PriceItem(merchandiseType, prices.map { it.first })
        }

        // Now, update the RecyclerView adapter with the list of PriceItems
        priceAdapter.updateData(priceItems)
    }

    private fun calculateTotals(summaryList: List<MerchandiseSummary>) {
        for (summary in summaryList) {
            totalKilosPaid += summary.kilosInCash
            totalCashPaid += summary.paidInCash
            totalKilosCredit += summary.kilosInCredit
            totalCashCredit += summary.unpaid
            totalKilosGrandTotal += summary.totalKilos
            totalCashGrandTotal += summary.totalAmount
        }

        // Update the TextViews
        updateTotalTextViews(totalKilosPaid, totalCashPaid, totalKilosCredit, totalCashCredit, totalKilosGrandTotal, totalCashGrandTotal)
    }

    private fun updateTotalTextViews(
        totalKilosPaid: Double, totalCashPaid: Double,
        totalKilosCredit: Double, totalCashCredit: Double,
        totalKilosGrandTotal: Double, totalCashGrandTotal: Double
    ) {
        val locale = Locale.getDefault()
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        binding.totalKilosPaid.text = getString(R.string.kilos_in_cash, numberFormat.format(totalKilosPaid))
        binding.totalCashPaid.text = getString(R.string.paid_in_cash, numberFormat.format(totalCashPaid))
        binding.totalKilosCredit.text = getString(R.string.kilos_in_credit, numberFormat.format(totalKilosCredit))
        binding.totalCashCredit.text = getString(R.string.unpaid, numberFormat.format(totalCashCredit))
        binding.totalKilosGrandTotal.text = getString(R.string.total_kilos, numberFormat.format(totalKilosGrandTotal))
        binding.totalCashGrandTotal.text = getString(R.string.total_amount, numberFormat.format(totalCashGrandTotal))
    }

    private fun getAndUpdateTotalExpenses(): Double {
        val totalExpenses = expenses.sumOf { it.amount }
        binding.totalExpenses.text = currencyFormatter(totalExpenses)

        // Append value for total expenses
        binding.valueLessExpenses.text = currencyFormatter(totalExpenses)

        return totalExpenses
    }

    private fun updateCashFlowStatement() {
        // Extracting date from the record name
        val recordNameParts = recordName.split(" ")
        val date = if (recordNameParts.isNotEmpty()) {
            recordNameParts.firstOrNull()
        } else {
            return
        }

        binding.cashFlowStatementDate.text = date
        binding.pasilCashValue.text = currencyFormatter(totalCashIncurred)

        // Calculate subtotal
        val subtotal = totalCashIncurred + getAndUpdateTotalPayments()

        // Set subtotal to TextView
        binding.subtotalValue.text = currencyFormatter(subtotal)

        val remainingMoney = subtotal - getAndUpdateTotalExpenses()

        // Set both TextViews to the same value
        binding.remainingMoneyValue.text = currencyFormatter(remainingMoney)


        binding.valueRemaining.text = currencyFormatter(remainingMoney)
    }













}
