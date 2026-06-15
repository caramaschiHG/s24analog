# Roll24 - Analog Camera Engine

Roll24 é uma câmera Android que tenta produzir fotos o mais parecidas possível com filme fotográfico real, começando pelo jeito que a câmera captura e processa a imagem.

## Filosofia

> Não capturar uma foto digital moderna e jogar filtro por cima.  
> Capturar uma base digital o mais neutra, crua e controlada possível, reduzir o look computacional moderno do celular e depois reconstruir a imagem com um pipeline próprio inspirado em filme.

## Características

### Capture Engine
- Uso de Camera2 API para controle manual
- Captura em YUV_420_888 (prioridade) ou JPEG (fallback)
- Redução de processamento digital automático:
  - HDR automático desabilitado
  - Sharpening reduzido
  - Noise reduction minimizado
  - Tone mapping controlado
- Controle manual quando disponível:
  - ISO
  - Tempo de exposição
  - Balanço de branco
  - Compensação de exposição

### Film Development Engine
Pipeline de processamento inspirado em filme:
1. Normalização
2. Redução do look digital
3. Curva tonal
4. Compressão de highlights
5. Controle de sombras
6. Ajuste de cor
7. Conversão P&B (se aplicável)
8. Halation (brilho vermelho/laranja em áreas claras)
9. Bloom (brilho suave em áreas claras)
10. Vinheta
11. Grão procedural
12. Softness final

## Perfis de Filme

### Warm Gold 200
- Filme colorido quente
- Luz de fim de tarde
- Contraste médio
- Amarelos e dourados presentes
- Sombras suaves
- Grão fino

### Soft Portrait 400
- Retrato suave
- Pele natural
- Contraste baixo/médio
- Cores elegantes
- Highlights macios

### Night Tungsten 800
- Noite e interiores
- Postes e luzes de rua
- Sombras frias
- Luzes quentes
- Halation visível
- Grão mais forte

### Green Street 400
- Urbano e rua
- Verde/ciano
- Clima de filme casual
- Contraste médio
- Sombras frias

### Mono Press 400
- Preto e branco documental
- Contraste forte
- Pretos densos
- Highlights preservados
- Grão visível
- Leve softness óptico

## Requisitos Técnicos

- Android 8.0 (API 26) ou superior
- Câmera com suporte a Camera2 API
- Otimizado para Galaxy S24 Ultra

## Como Compilar

1. Abra o projeto no Android Studio
2. Aguarde a sincronização do Gradle
3. Conecte um dispositivo Android ou inicie um emulador
4. Clique em "Run" ou pressione Shift+F10

## Estrutura do Projeto

```
app/src/main/java/com/roll24/
├── MainActivity.kt              # Activity principal e navegação
├── Roll24ViewModel.kt           # ViewModel para gerenciar estado
├── camera/
│   ├── CameraScreen.kt          # Tela da câmera
│   ├── CameraPermission.kt      # Gerenciamento de permissões
│   ├── Camera2Controller.kt     # Controle da câmera via Camera2
│   ├── CameraCapabilities.kt    # Modelo de capacidades da câmera
│   └── CaptureEngine.kt         # Motor de captura com controles manuais
├── film/
│   ├── FilmProfile.kt           # Modelo de perfil de filme
│   ├── FilmProfileRepository.kt # Repositório de perfis
│   ├── FilmDevelopmentEngine.kt # Motor de processamento de filme
│   └── processors/
│       ├── ToneCurveProcessor.kt
│       ├── ColorProcessor.kt
│       ├── GrainProcessor.kt
│       ├── HalationProcessor.kt
│       ├── BloomProcessor.kt
│       ├── VignetteProcessor.kt
│       └── SoftnessProcessor.kt
├── image/
│   ├── YuvConverter.kt          # Conversão YUV para Bitmap
│   └── ImageSaver.kt            # Salvamento de imagens
├── review/
│   └── ReviewScreen.kt          # Tela de revisão da foto
└── ui/
    ├── components/
    │   ├── CaptureButton.kt     # Botão de captura
    │   └── FilmSelector.kt      # Seletor de perfis
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Limitações Conhecidas

- **RAW/DNG**: Não implementado no MVP, mas arquitetura preparada para futuro
- **Preview com filtro**: Não há preview em tempo real com simulação de filme
- **Controles manuais**: Dependem do suporte do hardware. Fallbacks automáticos quando não disponíveis
- **Performance**: Processamento pode ser lento em imagens grandes. Otimizações futuras com OpenGL/Vulkan são possíveis

## Evoluções Futuras

- Suporte a RAW/DNG
- Preview com simulação em tempo real
- Otimização com GPU (OpenGL/Vulkan/AGSL)
- LUT 3D para perfis mais precisos
- Mais perfis de filme
- Controle manual de foco
- Histograma e zebras
- Bracketing de exposição
- Suporte a múltiplas câmeras

## Licença

Projeto pessoal e experimental.

---

**Roll24** - Uma câmera analógica digital-first, calibrada para capturar menos como celular moderno e revelar mais como filme.
