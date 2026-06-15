package com.roll24.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Sistema háptico centralizado para Roll24
 * Fornece feedback tátil premium sem espalhar chamadas de vibração pela UI
 */
class Roll24Haptics(private val context: Context) {
    
    companion object {
        private const val TAG = "Roll24Haptics"
    }
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Micro tick seco - como catraca pequena
     * Usado ao navegar entre filmes
     */
    fun filmTick() {
        vibrate(Roll24HapticEffect.FilmTick)
    }
    
    /**
     * Clique médio - sensação de encaixe
     * Usado ao confirmar seleção de filme
     */
    fun filmLoaded() {
        vibrate(Roll24HapticEffect.FilmLoaded)
    }
    
    /**
     * Tick preciso - marcação de dial
     * Usado ao mudar ISO/EV/WB
     */
    fun dialStep() {
        vibrate(Roll24HapticEffect.DialStep)
    }
    
    /**
     * Tick muito leve - pré-clique
     * Usado no meio pressionamento do obturador
     */
    fun shutterHalfPress() {
        vibrate(Roll24HapticEffect.ShutterHalfPress)
    }
    
    /**
     * Clique seco e encorpado - obturador mecânico
     * Usado na captura real
     */
    fun shutterRelease() {
        vibrate(Roll24HapticEffect.ShutterRelease)
    }
    
    /**
     * Pulso suave - máquina começando
     * Usado ao iniciar processamento
     */
    fun developingStart() {
        vibrate(Roll24HapticEffect.DevelopingStart)
    }
    
    /**
     * Micro pulso - progresso sutil
     * Usado durante processamento (opcional)
     */
    fun developingProgress() {
        vibrate(Roll24HapticEffect.DevelopingProgress)
    }
    
    /**
     * Clique curto de conclusão - "pronto"
     * Usado ao terminar processamento
     */
    fun developingComplete() {
        vibrate(Roll24HapticEffect.DevelopingComplete)
    }
    
    /**
     * Confirmação curta - carimbo/trava
     * Usado ao salvar foto
     */
    fun saveSuccess() {
        vibrate(Roll24HapticEffect.SaveSuccess)
    }
    
    /**
     * Dois pulsos curtos - "não encaixou"
     * Usado em ações indisponíveis
     */
    fun unavailable() {
        vibrate(Roll24HapticEffect.Unavailable)
    }
    
    /**
     * Feedback sutil - descarte
     * Usado ao descartar foto
     */
    fun discard() {
        vibrate(Roll24HapticEffect.Discard)
    }
    
    /**
     * Executa vibração com efeito específico
     * Fallback seguro para diferentes versões do Android
     */
    private fun vibrate(effect: Roll24HapticEffect) {
        val vib = vibrator ?: run {
            Log.w(TAG, "Vibrator not available")
            return
        }
        
        if (!vib.hasVibrator()) {
            Log.w(TAG, "Device has no vibrator")
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Usa VibrationEffect com waveform
                val vibrationEffect = VibrationEffect.createWaveform(
                    effect.timings,
                    effect.amplitudes,
                    -1 // Não repetir
                )
                vib.vibrate(vibrationEffect)
            } else {
                // Fallback para API antiga - usa duração simples
                @Suppress("DEPRECATION")
                vib.vibrate(effect.timings.sum())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }
}
