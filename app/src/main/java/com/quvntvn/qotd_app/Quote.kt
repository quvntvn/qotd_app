package com.quvntvn.qotd_app

import com.google.gson.annotations.SerializedName

// 3. Quote.kt (Modèle de données)
data class Quote(
    @SerializedName("id") val id: Int,
    @SerializedName("citation") val citation: String,
    @SerializedName("auteur") val auteur: String,
    @SerializedName("date_creation") val dateCreation: String?
)