package com.jardin.semis.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.DialogAddEditPlantBinding

class AddEditPlantDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditPlantBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SemisViewModel by activityViewModels()
    private var existingPlant: Plant? = null

    companion object {
        private const val ARG_PLANT_ID = "plant_id"

        fun newInstance(plant: Plant): AddEditPlantDialog {
            return AddEditPlantDialog().apply {
                existingPlant = plant
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingPlant?.let { plant ->
            binding.tvDialogTitle.text = "Modifier ${plant.name}"
            binding.etName.setText(plant.name)
            binding.etLatinName.setText(plant.latinName)
            binding.etEmoji.setText(plant.emoji)
            binding.etCategory.setText(plant.category)
            binding.etSowingMonths.setText(plant.sowingMonths)
            binding.etOccupationDays.setText(plant.occupationDays.toString())
            binding.etSpacingCm.setText(plant.spacingCm.toString())
            binding.etGerminationDays.setText(plant.germinationDays.toString())
            binding.etNotes.setText(plant.notes)
        } ?: run {
            binding.tvDialogTitle.text = "Nouvelle plante"
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.tilName.error = "Nom requis"
                return@setOnClickListener
            }

            val plant = (existingPlant ?: Plant(name = name)).copy(
                name = name,
                latinName = binding.etLatinName.text.toString().trim(),
                emoji = binding.etEmoji.text.toString().trim().ifEmpty { "🌱" },
                category = binding.etCategory.text.toString().trim().ifEmpty { "Légume" },
                sowingMonths = binding.etSowingMonths.text.toString().trim(),
                occupationDays = binding.etOccupationDays.text.toString().toIntOrNull() ?: 90,
                spacingCm = binding.etSpacingCm.text.toString().toIntOrNull() ?: 30,
                germinationDays = binding.etGerminationDays.text.toString().toIntOrNull() ?: 10,
                notes = binding.etNotes.text.toString().trim()
            )

            if (existingPlant != null) {
                viewModel.updatePlant(plant)
            } else {
                viewModel.addPlant(plant)
            }
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
