package com.example.evenly

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evenly.databinding.ActivityMainBinding
import com.example.evenly.databinding.NameCutRowBinding
import com.example.evenly.databinding.PaidByRowBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var numPeople = 2  // Default number of people
    private var numPayers = 1  // Current number of payers
    private val maxPayers = 3  // Maximum payers allowed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        updateNameAndCutInputs(numPeople)  // Initialize with one person
    }

    private fun setupButtons() {
        // Decrement Button
        binding.btnDecrement.setOnClickListener {
            if (numPeople > 2) {  // Prevent going below 1
                numPeople--
                updateNameAndCutInputs(numPeople)
                binding.tvNumPeople.text = numPeople.toString()
            } else {
                Toast.makeText(this, "Minimum two people", Toast.LENGTH_SHORT).show()
            }
        }

        // Increment Button
        binding.btnIncrement.setOnClickListener {
            numPeople++
            updateNameAndCutInputs(numPeople)
            binding.tvNumPeople.text = numPeople.toString()
        }

        // Calculate Button
        binding.btnCalculate.setOnClickListener {
            calculateAndDisplayResults()
        }

        // Add payers Button
        binding.btnAddPaidBy.setOnClickListener {
            if (numPayers < maxPayers) {
                addPaidByRow()
                numPayers++
            } else {
                Toast.makeText(this, "Maximum of $maxPayers payers allowed", Toast.LENGTH_SHORT).show()
            }
        }

        // Remove payers Button
        binding.btnRemPaidBy.setOnClickListener {
            if (numPayers > 1) {
                removePaidByRow()
                numPayers--
            } else {
                Toast.makeText(this, "At least one payer must remain.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNameAndCutInputs(count: Int) {
        // Clear the existing name and cut size inputs
        binding.containerNamesAndCuts.removeAllViews()

        // Dynamically add rows for each person
        for (i in 1..count) {
            val nameCutRowBinding = NameCutRowBinding.inflate(LayoutInflater.from(this))

            // Initialize with a default cut size of 1
            nameCutRowBinding.spinnerCutSize.setSelection(1)

            // Add the row to the container
            binding.containerNamesAndCuts.addView(nameCutRowBinding.root)
        }
    }

    private fun addPaidByRow() {
        val paidByRowBinding = PaidByRowBinding.inflate(LayoutInflater.from(this))
        binding.containerPaidBy.addView(paidByRowBinding.root)
    }

    private fun removePaidByRow() {
        // If there's more than one row, remove a row
        if (binding.containerPaidBy.childCount > 0) {
            val paidByRowBinding = PaidByRowBinding.bind(binding.containerPaidBy.getChildAt(binding.containerPaidBy.childCount-1))
            binding.containerPaidBy.removeView(paidByRowBinding.root)
        }
    }

    private fun calculateAndDisplayResults() {
        val peopleNames = mutableListOf<String>()
        val peopleCuts = mutableListOf<Float>()

        // Collect names and cut sizes
        for (i in 0 until numPeople) {
            val nameCutRowBinding = NameCutRowBinding.bind(binding.containerNamesAndCuts.getChildAt(i))
            val name = nameCutRowBinding.etName.text.toString().trim()
            val cutSize = nameCutRowBinding.spinnerCutSize.selectedItem.toString().removeSuffix("x").toFloat()

            peopleNames.add(name)
            if (name == ""){
                Toast.makeText(this, "At least one name empty", Toast.LENGTH_SHORT).show()
                return
            }

            peopleCuts.add(cutSize)
        }

        // Collect paid amounts
        val paidAmounts = mutableListOf<Pair<String, Float>>()

        // First payer
        if (binding.etPaidBy.text.toString().trim().isNotEmpty()) {
            paidAmounts.add(
                binding.etPaidBy.text.toString().trim() to
                        (binding.etTotalAmount.text.toString().toFloatOrNull() ?: 0f)
            )
        } else {
            Toast.makeText(this, "No one is paying", Toast.LENGTH_SHORT).show()
        }

        // Dynamically added payers
        for (i in 0 until binding.containerPaidBy.childCount) {
            val paidByRowBinding = PaidByRowBinding.bind(binding.containerPaidBy.getChildAt(i))
            val name = paidByRowBinding.etPaidBy.text.toString().trim()
            val amountPaid = paidByRowBinding.etTotalAmount.text.toString().toFloatOrNull() ?: 0f

            if (name.isNotEmpty()) {
                paidAmounts.add(name to amountPaid)
            }
        }

        Log.v("DEBUG", "$paidAmounts")

        val results = StringBuilder()

        // Add payer's share if not in list of names
        for ((payer, _) in paidAmounts) {
            val index = peopleNames.indexOf(payer)
            if (index == -1) {
                // Payer not in the list, add them with a cut of 1x
                peopleNames.add(payer)
                peopleCuts.add(1f)
            }
        }

        Log.v("DEBUG", "$peopleNames")
        Log.v("DEBUG", "$peopleCuts")

        // Total cuts and total amount
        val totalCuts = peopleCuts.sum()
        var totalAmount = binding.etTotalAmount.text.toString().toFloatOrNull() ?: 0f

        // Add amounts of dynamically added payers
        for (i in 0 until binding.containerPaidBy.childCount) {
            val paidByRowBinding = PaidByRowBinding.bind(binding.containerPaidBy.getChildAt(i))
            val name = paidByRowBinding.etPaidBy.text.toString().trim()
            val amountPaid = paidByRowBinding.etTotalAmount.text.toString().toFloatOrNull() ?: 0f

            if (name.isNotEmpty()) {
                totalAmount += amountPaid
            }
        }

        // Calculate each person's share and net balance
        val balances = mutableMapOf<String, Float>()
        for (i in peopleNames.indices) {
            val name = peopleNames[i]
            val cut = peopleCuts[i]
            val share = (cut / totalCuts) * totalAmount
            val paid = paidAmounts.find { it.first == name }?.second ?: 0f
            balances[name] = paid - share
        }

        // Separate creditors and debtors
        val creditors = balances.filter { it.value > 0 }.toList().sortedByDescending { it.second }.toMutableList()
        val debtors = balances.filter { it.value < 0 }.toList().sortedBy { it.second }.toMutableList()

        // Resolve debts
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtor = debtors.first()
            val creditor = creditors.first()

            val debtAmount = -debtor.second
            val creditAmount = creditor.second

            // Minimum amount to settle this transaction
            val payment = minOf(debtAmount, creditAmount)

            // Record transaction
            results.append("${debtor.first} owes ${creditor.first} ${"%.2f".format(payment)}\n")

            // Update balances
            balances[debtor.first] = debtor.second + payment
            balances[creditor.first] = creditor.second - payment

            // Remove settled parties or update remaining balances
            if (balances[debtor.first] == 0f) debtors.removeAt(0) else debtors[0] = debtor.first to balances[debtor.first]!!
            if (balances[creditor.first] == 0f) creditors.removeAt(0) else creditors[0] = creditor.first to balances[creditor.first]!!
        }

        // Debugging: Ensure no unresolved balances
        for ((name, balance) in balances) {
            if (balance > 0.001) {
                results.append("$name has an unresolved balance of ${"%.2f".format(balance)}\n")
            }
        }

        // Display results
        binding.tvResults.text = results.toString()
    }
}