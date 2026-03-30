package com.jardin.semis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jardin.semis.data.db.SemisDatabase
import com.jardin.semis.data.db.SowingWithPlant
import com.jardin.semis.data.model.*
import com.jardin.semis.data.repository.SemisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── Application ─────────────────────────────────────────────────────────────

class SemisApplication : Application() {
    val database by lazy { SemisDatabase.getDatabase(this) }
    val repository by lazy {
        SemisRepository(
            database.plantDao(),
            database.sowingDao(),
            this
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Pré-remplissage des plantes par défaut
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            repository.populateDefaultPlants()
        }
    }
}

// ─── ViewModel Principal ──────────────────────────────────────────────────────

class SemisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SemisApplication).repository

    // Plantes
    val allPlants = repository.getAllPlants()
    val allCategories = repository.getAllCategories()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)

    // Semis
    val allSowingsWithPlant = repository.getAllSowingsWithPlant()
    val activeSowings = repository.getActiveSowings()

    // Météo
    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: LiveData<WeatherData?> = _weatherData

    private val _weatherLoading = MutableLiveData(false)
    val weatherLoading: LiveData<Boolean> = _weatherLoading

    private val _weatherError = MutableLiveData<String?>()
    val weatherError: LiveData<String?> = _weatherError

    // Messages
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // ─── Actions Plantes ───────────────────────────────────────────────────────

    fun addPlant(plant: Plant) = viewModelScope.launch {
        repository.insertPlant(plant)
        _message.value = "${plant.emoji} ${plant.name} ajouté à la bibliothèque"
    }

    fun updatePlant(plant: Plant) = viewModelScope.launch {
        repository.updatePlant(plant)
        _message.value = "Plante modifiée"
    }

    fun deletePlant(plant: Plant) = viewModelScope.launch {
        repository.deletePlant(plant)
        _message.value = "${plant.name} supprimé"
    }

    fun searchPlants(query: String) = repository.searchPlants(query)

    fun getPlantsByCategory(category: String) = repository.getPlantsByCategory(category)

    // ─── Actions Semis ─────────────────────────────────────────────────────────

    fun addSowing(
        plantId: Long,
        sowingDate: LocalDate,
        location: String,
        quantity: Int,
        notes: String
    ) = viewModelScope.launch {
        try {
            repository.addSowing(plantId, sowingDate, location, quantity, notes)
            _message.value = "✅ Semis enregistré !"
        } catch (e: Exception) {
            _message.value = "❌ Erreur : ${e.message}"
        }
    }

    fun updateSowingStatus(id: Long, status: SowingStatus) = viewModelScope.launch {
        repository.updateSowingStatus(id, status)
        val label = when (status) {
            SowingStatus.GERMINATED -> "Levée enregistrée 🌱"
            SowingStatus.GROWING -> "En croissance 🌿"
            SowingStatus.HARVESTED -> "Récolte enregistrée 🎉"
            SowingStatus.FAILED -> "Semis marqué comme échoué"
            else -> "Statut mis à jour"
        }
        _message.value = label
    }

    fun deleteSowing(sowing: Sowing) = viewModelScope.launch {
        repository.deleteSowing(sowing)
        _message.value = "Semis supprimé"
    }

    fun getSowingsBetweenDates(start: LocalDate, end: LocalDate) =
        repository.getSowingsBetweenDates(start, end)

    // ─── Météo ────────────────────────────────────────────────────────────────

    fun fetchWeatherByCity(city: String) = viewModelScope.launch {
        _weatherLoading.value = true
        _weatherError.value = null
        repository.getWeatherByCity(city).fold(
            onSuccess = {
                _weatherData.value = it
                _weatherLoading.value = false
            },
            onFailure = {
                _weatherError.value = "Impossible de charger la météo. Vérifiez votre connexion."
                _weatherLoading.value = false
            }
        )
    }

    fun fetchWeatherByLocation(lat: Double, lon: Double) = viewModelScope.launch {
        _weatherLoading.value = true
        _weatherError.value = null
        repository.getWeatherByLocation(lat, lon).fold(
            onSuccess = {
                _weatherData.value = it
                _weatherLoading.value = false
            },
            onFailure = {
                _weatherError.value = "Localisation impossible. Entrez votre ville manuellement."
                _weatherLoading.value = false
            }
        )
    }

    fun clearMessage() {
        _message.value = null
    }
}
