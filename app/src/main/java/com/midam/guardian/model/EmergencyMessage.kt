package com.midam.guardian.model

import kotlinx.serialization.Serializable
 
@Serializable
data class EmergencyMessage(
    val alerta: String,
    val mensaje: String
) 