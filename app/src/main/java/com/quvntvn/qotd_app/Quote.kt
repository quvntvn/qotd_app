package com.quvntvn.qotd_app

// 3. Quote.kt (Modèle de données)
data class Quote(
    val citation: String,
    val auteur: String,
    @Json(name = "date_creation") val dateCreation: String
)