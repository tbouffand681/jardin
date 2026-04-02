package com.jardin.semis.data.model

data class WeatherData(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val description: String,
    val icon: String,
    val windSpeed: Double,
    val tempMin: Double,
    val tempMax: Double,
    val precipitation: Double = 0.0,       // mm aujourd'hui = L/m²
    val evapotranspiration: Double = 0.0,  // ET₀ aujourd'hui en mm = L/m²
    val et0Cumul2days: Double = 0.0,       // ET₀ cumulée aujourd'hui + demain (L/m²)
    val et0Cumul5days: Double = 0.0,       // ET₀ cumulée sur 5 jours (L/m²)
    val precipCumul5days: Double = 0.0     // Précipitations cumulées 5 jours (L/m²)
) {
    fun sowingAdvice(): String = when {
        temperature < 3  -> "❄️ Gel possible — protégez vos cultures, aucun semis en pleine terre."
        temperature < 8  -> "🥶 Trop froid. Réservez les semis à la serre ou sous châssis."
        temperature in 8.0..12.0 -> "🌡️ Frais. Semis d'espèces rustiques : épinard, mâche, radis."
        temperature in 12.0..17.0 -> "✅ Bonnes conditions : carotte, laitue, poireau, persil."
        temperature in 17.0..27.0 -> "🌞 Idéal pour la plupart des semis et repiquages."
        temperature > 32 -> "🔥 Chaleur excessive — semez tôt le matin, arrosez abondamment."
        else -> "✅ Conditions favorables."
    }

    /** ET₀ en L/m² — identique aux mm mais rappelé pour la lisibilité */
    fun irrigationAdvice(): String {
        val deficit = (evapotranspiration - precipitation).coerceAtLeast(0.0)
        return when {
            precipitation >= evapotranspiration ->
                "💧 Pluie suffisante (${f1(precipitation)} L/m²) — arrosage non nécessaire."
            deficit < 1.5 ->
                "💧 Déficit faible (${f1(deficit)} L/m²) — vérifiez si le sol est sec avant d'arroser."
            deficit < 3.5 ->
                "💧💧 Apportez environ ${f1(deficit)} L/m² (${f1(deficit * 10)} L/10m²)."
            else ->
                "💧💧💧 Arrosage important : ${f1(deficit)} L/m² (${f1(deficit * 10)} L/10m²)."
        }
    }

    private fun f1(v: Double) = String.format("%.1f", v)
}
