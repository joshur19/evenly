package com.example.evenly

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.evenly.databinding.ActivityMainBinding
import com.example.evenly.databinding.NameCutRowBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var numPeople = 2  // Default number of people

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

    private fun calculateAndDisplayResults() {
        val peopleNames = mutableListOf<String>()
        val peopleCuts = mutableListOf<Float>()

        // Collect names and cut sizes
        for (i in 0 until numPeople) {
            val nameCutRowBinding = NameCutRowBinding.bind(binding.containerNamesAndCuts.getChildAt(i))

            val name = nameCutRowBinding.etName.text.toString().trim()
            val cutSize = nameCutRowBinding.spinnerCutSize.selectedItem.toString().removeSuffix("x").toFloat()

            peopleNames.add(name)
            peopleCuts.add(cutSize)
        }

        // For now, assuming a single person has paid the full amount
        val totalAmount = binding.etTotalAmount.text.toString().toFloatOrNull() ?: 0f
        val totalCut = peopleCuts.sum()

        val results = StringBuilder()
        for (i in 0 until numPeople) {
            val personName = peopleNames[i]
            val personCut = peopleCuts[i]

            // Calculate the share for the person based on their cut size
            val personShare = (personCut / totalCut) * totalAmount
            results.append("$personName pays $personShare\n")
        }

        // Display the results
        binding.tvResults.text = results.toString()
    }
}