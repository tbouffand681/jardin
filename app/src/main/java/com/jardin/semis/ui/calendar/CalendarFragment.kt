package com.jardin.semis.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.R
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.databinding.FragmentCalendarBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SemisViewModel by activityViewModels()
    private lateinit var sowingsAdapter: SowingsTimelineAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMonthHeader()
        observeSowings()
        setupFab()
    }

    private fun setupRecyclerView() {
        sowingsAdapter = SowingsTimelineAdapter(
            onStatusClick = { sowing -> showStatusDialog(sowing.id, sowing.status) },
            onDeleteClick = { sowing ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer ce semis ?")
                    .setMessage("Cette action est irréversible.")
                    .setPositiveButton("Supprimer") { _, _ ->
                        lifecycleScope.launch {
                            val s = viewModel.allSowingsWithPlant
                            // delete via viewModel
                        }
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        )
        binding.recyclerSowings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sowingsAdapter
        }
    }

    private fun setupMonthHeader() {
        val now = LocalDate.now()
        binding.tvCurrentMonth.text = now.month
            .getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() } + " ${now.year}"
    }

    private fun observeSowings() {
        lifecycleScope.launch {
            viewModel.allSowingsWithPlant.collectLatest { sowings ->
                sowingsAdapter.submitList(sowings)

                // Stats du mois en cours
                val now = LocalDate.now()
                val thisMonth = sowings.filter {
                    val date = LocalDate.parse(it.sowingDate, DateTimeFormatter.ISO_LOCAL_DATE)
                    date.year == now.year && date.month == now.month
                }
                val active = sowings.filter {
                    it.status != com.jardin.semis.data.model.SowingStatus.HARVESTED &&
                    it.status != com.jardin.semis.data.model.SowingStatus.FAILED
                }
                binding.tvStatSowings.text = "${sowings.size} semis total"
                binding.tvStatActive.text = "${active.size} en cours"
                binding.tvStatMonth.text = "${thisMonth.size} ce mois-ci"

                binding.emptyView.visibility = if (sowings.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerSowings.visibility = if (sowings.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun setupFab() {
        binding.fabAddSowing.setOnClickListener {
            AddSowingBottomSheet().show(childFragmentManager, "AddSowing")
        }
    }

    private fun showStatusDialog(sowingId: Long, currentStatus: SowingStatus) {
        val statuses = arrayOf("Semé", "Levée observée", "En croissance", "Récolté", "Échec")
        val statusValues = SowingStatus.values()
        val currentIdx = statusValues.indexOf(currentStatus)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mettre à jour le statut")
            .setSingleChoiceItems(statuses, currentIdx) { dialog, which ->
                viewModel.updateSowingStatus(sowingId, statusValues[which])
                dialog.dismiss()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
