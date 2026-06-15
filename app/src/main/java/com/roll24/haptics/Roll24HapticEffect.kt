package com.roll24.haptics

import android.os.Build

/**
 * Definições de efeitos hápticos para Roll24
 * Cada efeito define timings e amplitudes para criar sensações específicas
 */
sealed class Roll24HapticEffect {
    
    abstract val timings: LongArray
    abstract val amplitudes: IntArray
    
    /**
     * Micro tick seco - como catraca pequena
     * Usado ao navegar entre filmes
     */
    object FilmTick : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 8)
        override val amplitudes = intArrayOf(0, 40)
    }
    
    /**
     * Clique médio - sensação de encaixe
     * Usado ao confirmar seleção de filme
     */
    object FilmLoaded : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 15)
        override val amplitudes = intArrayOf(0, 120)
    }
    
    /**
     * Tick preciso - marcação de dial
     * Usado ao mudar ISO/EV/WB
     */
    object DialStep : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 10)
        override val amplitudes = intArrayOf(0, 60)
    }
    
    /**
     * Tick muito leve - pré-clique
     * Usado no meio pressionamento do obturador
     */
    object ShutterHalfPress : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 6)
        override val amplitudes = intArrayOf(0, 30)
    }
    
    /**
     * Clique seco e encorpado - obturador mecânico
     * Usado na captura real
     */
    object ShutterRelease : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 20)
        override val amplitudes = intArrayOf(0, 180)
    }
    
    /**
     * Pulso suave - máquina começando
     * Usado ao iniciar processamento
     */
    object DevelopingStart : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 25)
        override val amplitudes = intArrayOf(0, 80)
    }
    
    /**
     * Micro pulso - progresso sutil
     * Usado durante processamento (opcional)
     */
    object DevelopingProgress : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 8)
        override val amplitudes = intArrayOf(0, 35)
    }
    
    /**
     * Clique curto de conclusão - "pronto"
     * Usado ao terminar processamento
     */
    object DevelopingComplete : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 18)
        override val amplitudes = intArrayOf(0, 140)
    }
    
    /**
     * Confirmação curta - carimbo/trava
     * Usado ao salvar foto
     */
    object SaveSuccess : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 16)
        override val amplitudes = intArrayOf(0, 130)
    }
    
    /**
     * Dois pulsos curtos - "não encaixou"
     * Usado em ações indisponíveis
     */
    object Unavailable : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 12, 40, 12)
        override val amplitudes = intArrayOf(0, 90, 0, 90)
    }
    
    /**
     * Feedback sutil - descarte
     * Usado ao descartar foto
     */
    object Discard : Roll24HapticEffect() {
        override val timings = longArrayOf(0, 10)
        override val amplitudes = intArrayOf(0, 50)
    }
    
    /**
     * Verifica se pode usar primitivas avançadas (Android 8+)
     */
    fun canUsePrimitives(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
