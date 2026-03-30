package com.jardin.semis.ui.growth

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.databinding.FragmentGrowthBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class GrowthFragment : Fragment() {

    private var _binding: FragmentGrowthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SemisViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                fetchLocationWeather()
            }
            else -> {
                Snackbar.make(binding.root, "Entrez votre ville manuellement", Snackbar.LENGTH_LONG).show()
                binding.cityInputLayout.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGrowthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupWeather()
        observeWeather()
        observeGrowthTimeline()
        requestLocationAndFetchWeather()
    }

    private fun requestLocationAndFetchWeather() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED -> fetchLocationWeather()
            else -> locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun fetchLocationWeather() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.fetchWeatherByLocation(location.latitude, location.longitude)
                } else {
                    binding.cityInputLayout.visibility = View.VISIBLE
                }
            }
        } catch (e: SecurityException) {
            binding.cityInputLayout.visibility = View.VISIBLE
        }
    }

    private fun setupWeather() {
        binding.btnFetchCityWeather.setOnClickListener {
            val city = binding.etCity.text.toString().trim()
            if (city.isNotEmpty()) {
                viewModel.fetchWeatherByCity(city)
                binding.cityInputLayout.visibility = View.GONE
            }
        }

        binding.btnRefreshWeather.setOnClickListener {
            requestLocationAndFetchWeather()
        }
    }

    private fun observeWeather() {
        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            if (weather == null) return@observe

            with(binding) {
                weatherCard.visibility = View.VISIBLE
                tvCity.text = "📍 ${weather.cityName}"
                tvTemperature.text = "${weather.temperature.toInt()}°C"
                tvDescription.text = weather.description
                tvHumidity.text = "💧 Humidité : ${weather.humidity}%"
                tvWind.text = "💨 Vent : ${weather.windSpeed} m/s"
                tvTempMinMax.text = "↓${weather.tempMin.toInt()}° / ↑${weather.tempMax.toInt()}°"
                tvSowingAdvice.text = weather.sowingAdvice()

                // Charger l'icône météo
                val iconUrl = "https://openweathermap.org/img/wn/${weather.icon}@2x.png"
            }
        }

        viewModel.weatherLoading.observe(viewLifecycleOwner) { loading ->
            binding.weatherProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.weatherError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.cityInputLayout.visibility = View.VISIBLE
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeGrowthTimeline() {
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val displayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
        val today = LocalDate.now()

        lifecycleScope.launch {
            viewModel.activeSowings.collectLatest { sowings ->
                if (sowings.isEmpty()) {
                    binding.emptyGrowth.visibility = View.VISIBLE
                    binding.growthContainer.visibility = View.GONE
                    return@collectLatest
                }

                binding.emptyGrowth.visibility = View.GONE
                binding.growthContainer.visibility = View.VISIBLE

                // Trier par date de récolte
                val sorted = sowings.sortedBy { it.expectedHarvestDate }

                // Prochaines récoltes (30 jours)
                val upcoming = sorted.filter {
                    val harvest = LocalDate.parse(it.expectedHarvestDate, dateFormatter)
                    val daysLeft = ChronoUnit.DAYS.between(today, harvest)
                    daysLeft in 0..30
                }

                if (upcoming.isNotEmpty()) {
                    binding.tvUpcomingTitle.visibility = View.VISIBLE
                    binding.tvUpcomingHarvests.visibility = View.VISIBLE
                    binding.tvUpcomingHarvests.text = upcoming.joinToString("\n") {
                        val harvest = LocalDate.parse(it.expectedHarvestDate, dateFormatter)
                        val daysLeft = ChronoUnit.DAYS.between(today, harvest)
                        val daysText = if (daysLeft == 0L) "aujourd'hui !" else "dans $daysLeft j"
                        "🌱 Semis #${it.id} — récolte $daysText (${harvest.format(displayFmt)})"
                    }
                } else {
                    binding.tvUpcomingTitle.visibility = View.GONE
                    binding.tvUpcomingHarvests.visibility = View.GONE
                }

                // Stats globales
                val germinated = sowings.count { it.status == SowingStatus.GERMINATED }
                val growing = sowings.count { it.status == SowingStatus.GROWING }
                val sowed = sowings.count { it.status == SowingStatus.SOWED }
                binding.tvStatsGrowth.text = """
                    🌰 Semés : $sowed
                    🌱 Levée : $germinated
                    🌿 En croissance : $growing
                    Total actifs : ${sowings.size}
                """.trimIndent()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
