# Roll24 Assets Integration Guide

## Assets Incluídos

O asset pack `roll24_asset_pack_v0_1.zip` foi integrado ao projeto com os seguintes componentes:

### 1. Design Tokens (`ui/theme/Color.kt`)
- **Roll24Colors**: Paleta completa com cores premium (InkBlack, WarmGold, Paper, etc.)
- **Roll24Radius**: Raios de borda (Sm=8dp, Md=14dp, Lg=22dp, Xl=32dp)
- **Roll24Spacing**: Espaçamentos (Xs=8dp, Sm=12dp, Md=16dp, Lg=24dp, Xl=32dp)

### 2. Componentes Compose (`ui/components/`)
- **Roll24CaptureButton.kt**: Botão de captura tátil com animação de pressão e gradient radial
- **Roll24TactilePanel.kt**: Container com gradiente vertical e borda sutil

### 3. Drawables (`res/drawable/`)
- `bg_roll24_gold_button.xml`: Seletor de botão dourado com estados
- `bg_roll24_panel.xml`: Shape de painel com borda
- `ic_save.xml`: Ícone de salvar (vector)
- `ic_trash.xml`: Ícone de descartar (vector)
- `ic_compare.xml`: Ícone de comparação (vector)
- `ic_flash.xml`: Ícone de flash (vector)

### 4. Cards de Perfil (`res/drawable-nodpi/`)
- `warm_gold_200_card.png`
- `soft_portrait_400_card.png`
- `night_tungsten_800_card.png`
- `green_street_400_card.png`
- `mono_press_400_card.png`

### 5. Overlays e Texturas (`assets/`)
Para uso no FilmDevelopmentEngine via AssetManager:

**Overlays:**
- `roll24_halation_warm_sample_overlay_1024.png`: Overlay de halation quente
- `roll24_micro_grain_overlay_fine_1024.png`: Grão fino
- `roll24_micro_grain_overlay_medium_1024.png`: Grão médio
- `roll24_optical_vignette_soft_1024.png`: Vinheta óptica suave

**Texturas:**
- `roll24_brushed_dark_metal_radial_512.png`: Textura metálica radial
- `roll24_capture_button_tactile_512.png`: Textura tátil para botão
- `roll24_surface_clean_tactile_1024.png`: Superfície tátil limpa (grande)
- `roll24_surface_clean_tactile_512.png`: Superfície tátil limpa (média)

## Como Usar os Overlays

Os overlays podem ser usados no FilmDevelopmentEngine para efeitos mais realistas:

```kotlin
// Exemplo: Carregar overlay de grão dos assets
fun loadGrainOverlay(context: Context, type: String = "medium"): Bitmap {
    val filename = when (type) {
        "fine" -> "roll24_micro_grain_overlay_fine_1024.png"
        "medium" -> "roll24_micro_grain_overlay_medium_1024.png"
        else -> "roll24_micro_grain_overlay_medium_1024.png"
    }
    
    return context.assets.open(filename).use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}

// Aplicar overlay com blend mode
fun applyGrainOverlay(base: Bitmap, overlay: Bitmap, opacity: Float): Bitmap {
    val result = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        alpha = (opacity * 255).toInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
    }
    
    // Redimensionar overlay para caber na imagem
    val scaledOverlay = Bitmap.createScaledBitmap(overlay, base.width, base.height, true)
    canvas.drawBitmap(scaledOverlay, 0f, 0f, paint)
    
    return result
}
```

## Guia Visual

**Estilo correto:**
- Microtextura limpa
- Bordas com leve highlight
- Botões pressionáveis com feedback tátil
- Detalhes metálicos quentes (WarmGold)
- Gradientes sutis
- Cantos arredondados generosos

**Evitar:**
- Arranhão exagerado
- Ferrugem
- Poeira fake
- Couro rachado
- Câmera antiga destruída
- Vintage caricato

## Cores Principais

```kotlin
// Backgrounds
Roll24Colors.InkBlack      // #080807 - Fundo principal
Roll24Colors.Charcoal      // #11110F - Fundo secundário
Roll24Colors.Panel         // #151412 - Painéis
Roll24Colors.Raised        // #24221E - Elementos elevados

// Accents
Roll24Colors.WarmGold      // #DCA94A - Dourado principal
Roll24Colors.WarmGoldDeep  // #A87622 - Dourado pressionado
Roll24Colors.AmberLight    // #F2C76B - Highlights dourados

// Text
Roll24Colors.Paper         // #D9CCB7 - Texto principal
Roll24Colors.MutedText     // #A99F8D - Texto secundário

// Borders
Roll24Colors.Stroke        // #34312B - Bordas sutis
```

## Próximos Passos

1. **Integrar overlays no FilmDevelopmentEngine**
   - Substituir grão procedural por overlay de grão real
   - Usar overlay de vinheta óptica
   - Aplicar halation via overlay

2. **Adicionar mais ícones conforme necessário**
   - Configurações
   - Galeria
   - Timer
   - Grid

3. **Criar splash screen com textura metálica**
   - Usar `roll24_brushed_dark_metal_radial_512.png`

4. **Implementar preview de perfil com card**
   - Mostrar card PNG ao selecionar perfil
   - Animação de transição suave

## Notas Técnicas

- Os PNGs em `drawable-nodpi` não são redimensionados pelo Android
- Overlays em `assets/` são acessados via `context.assets.open()`
- Todos os ícones vector usam `WarmGold` (#DCA94A) como cor principal
- Design tokens são centralizados em `Roll24Colors`, `Roll24Radius`, `Roll24Spacing`

---

**Roll24** - Tátil e analógico não significa velho. Significa intenção, controle, matéria e resposta física.
