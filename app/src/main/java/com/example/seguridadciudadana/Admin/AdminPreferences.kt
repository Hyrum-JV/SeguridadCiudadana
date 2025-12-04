package com.example.seguridadciudadana.Admin

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase para manejar las preferencias del administrador
 * Incluye configuración de radio de cobertura y notificaciones
 */
object AdminPreferences {
    
    private const val PREFS_NAME = "admin_preferences"
    private const val KEY_RADIO_COBERTURA = "radio_cobertura_km"
    private const val KEY_NOTIFICACIONES_ACTIVAS = "notificaciones_activas"
    
    // Valor por defecto del radio de cobertura en km
    const val DEFAULT_RADIO_KM = 25f
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Obtiene el radio de cobertura configurado (en kilómetros)
     */
    fun getRadioCobertura(context: Context): Float {
        return getPrefs(context).getFloat(KEY_RADIO_COBERTURA, DEFAULT_RADIO_KM)
    }
    
    /**
     * Guarda el radio de cobertura (en kilómetros)
     */
    fun setRadioCobertura(context: Context, radioKm: Float) {
        getPrefs(context).edit().putFloat(KEY_RADIO_COBERTURA, radioKm).apply()
    }
    
    /**
     * Obtiene el radio de cobertura como Double (para cálculos)
     */
    fun getRadioCoberturaDouble(context: Context): Double {
        return getRadioCobertura(context).toDouble()
    }
    
    /**
     * Verifica si las notificaciones están activas
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICACIONES_ACTIVAS, true)
    }
    
    /**
     * Activa o desactiva las notificaciones
     */
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICACIONES_ACTIVAS, enabled).apply()
    }
}
