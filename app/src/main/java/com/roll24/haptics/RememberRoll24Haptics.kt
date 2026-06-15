package com.roll24.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Helper para usar Roll24Haptics em composables
 * Cria e lembra uma instância do sistema háptico
 */
@Composable
fun rememberRoll24Haptics(): Roll24Haptics {
    val context = LocalContext.current
    return remember { Roll24Haptics(context) }
}
