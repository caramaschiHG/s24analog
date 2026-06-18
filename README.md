# Roll24 - Analog Camera Engine

Roll24 é uma câmera Android de alta fidelidade que produz fotos inspiradas em filme fotográfico real, partindo de uma captura de dados brutos (RAW/YUV) do sensor e processando-os por meio de um pipeline físico-químico simulado em tempo real, em vez de aplicar filtros cosméticos sobre JPEGs prontos.

## Filosofia

> Não capturar uma foto digital moderna e jogar filtro por cima.  
> Capturar uma base digital o mais neutra, crua e controlada possível, reduzir o look computacional moderno do celular e depois reconstruir a imagem com um pipeline próprio inspirado em filme.

## Características

### 1. Capture Engine (Suporte RAW e YUV)
- Controle de câmera de baixo nível usando a **Camera2 API**.
- Captura de frames **RAW (Bayer Demosaic)** e YUV_420_888 de alta qualidade para o sensor do Galaxy S24 Ultra.
- Bypass dos processamentos nativos do celular:
  - HDR computacional agressivo desabilitado.
  - Redução de ruído e nitidez digital (edge enhancement) minimizados.
  - Balanço de branco e exposição gerenciados manualmente ou travados na captura.
- Renderizador RAW personalizado (`RawRenderer.kt`) que extrai os dados lineares do sensor com compressão de highlights via knee curve a 0.8.

### 2. Physical Film Emulation Pipeline
A revelação é feita inteiramente em espaço de cor float linear de alta precisão através do buffer físico (`FilmPixelBuffer`), simulando as etapas físicas de revelação e escaneamento de um filme:
1. **Exposure Adjustment**: Compensação linear de exposição baseada no ISO e sensibilidade nominal.
2. **H&D Density Curve**: Simulação da curva característica de densidade (Hurter & Driffield), emulando as regiões de *Toe*, *Linear* e *Shoulder* (com compressão suave de altas luzes).
3. **Negative-to-Positive Scanner Transform**: Simulação do escaneamento do negativo colorido com elevação de pretos física (black lift).
4. **Physical Color Mapping**: Aplicação de matrizes de cor 3D personalizadas para cada perfil de filme, saturação linear e split-toning.
5. **Physical Halation & Bloom**: Simulação física da difusão da luz na emulsão do filme (gradação vermelha ao redor de altas luzes) e dispersão ótica (bloom).
6. **Structured Grain**: Simulação de grão baseada em tamanho e densidade de cristais de haleto de prata.
7. **Softness**: Suavidade ótica difusa no final para quebrar a nitidez excessiva das lentes modernas.

---

## Perfis de Filme

Atualmente o Roll24 conta com **20 perfis de filme**, divididos em três categorias principais:

### Perfis Legados (Efeitos Clássicos)
1. **Warm Gold 200**: Filme colorido quente com tons dourados marcantes e sombras suaves.
2. **Soft Portrait 400**: Cores suaves e elegantes, calibrado especialmente para tons de pele naturais.
3. **Night Tungsten 800**: Balanceado para tungstênio (luz artificial), com halation proeminente em fontes de luz.
4. **Green Street 400**: Filme urbano com sombras frias e leves tons ciano/esverdeados.
5. **Mono Press 400**: Preto e branco de alto contraste documental, pretos densos e grão marcante.

### Perfis Otimizados para S24 Ultra
6. **S24 1x Clean Negative**: Base negativa limpa e de alta resolução ajustada para a lente principal.
7. **S24 1x Street 400**: Negativo de rua contrastado e dinâmico para o sensor principal.
8. **S24 3x Portrait 400**: Calibrado para o sensor teleobjetiva de 3x, com excelente compressão óptica e tons macios.
9. **S24 5x Chrome 200**: Tons densos e decaimento rápido de altas luzes otimizado para a teleobjetiva de 5x.
10. **S24 Night 800**: Converte o ruído de alta sensibilidade do sensor em grão estético e natural.

### Estoques Canônicos (FilmStocks)
Modelos de resposta de cor e sensibilidade física real calibrados:
11. **Kodak Portra 400**: Famoso por tons de pele naturais, latitude de exposição extrema e cores pastel.
12. **Kodak Ektar 100**: Cores ultra-saturadas, grão extremamente fino e nitidez extraordinária.
13. **Fuji Pro 400H**: Tons frios, verdes exuberantes e suavidade nos tons médios.
14. **Fuji Velvia 50**: Contraste altíssimo, saturação vibrante para paisagens, pretos profundos.
15. **CineStill 800T**: Filme de cinema balanceado para tungstênio com halation vermelho intenso ao redor de pontos de luz.
16. **Kodak Vision3 250D**: Filme cinematográfico balanceado para luz do dia com reprodução tonal suave e ampla latitude.
17. **Kodak Gold 200**: Clássico filme amador quente, grão agradável e saturação equilibrada.
18. **Fujicolor C200**: Filme colorido de uso geral com verdes característicos e contraste natural.
19. **Ilford HP5 Plus 400**: Filme preto e branco versátil com contraste moderado e grão clássico.
20. **Kodak Tri-X 400**: O preto e branco mais famoso do mundo, contraste dramático e grão estruturado.

---

## Estrutura do Projeto

```
app/src/main/java/com/roll24/
├── MainActivity.kt              # Activity principal e navegação Jetpack Compose
├── Roll24ViewModel.kt           # ViewModel de orquestração (captura -> revelação -> salvamento)
├── camera/
│   ├── CameraScreen.kt          # Tela da Câmera (UI Jetpack Compose)
│   ├── CameraChrome.kt          # Elementos visuais sobrepostos (Chrome)
│   ├── CameraLayout.kt          # Estrutura espacial e responsiva da UI da Câmera
│   ├── CameraPermission.kt      # Gerenciamento de permissões do sistema
│   ├── Camera2Controller.kt     # Controle da câmera de baixo nível
│   ├── CameraCapabilities.kt    # Detecção de capacidades físicas dos sensores
│   ├── CameraSensorScanner.kt   # Escaneamento das características de hardware
│   ├── CaptureResultStore.kt    # Armazenamento e debug de metadados dos frames capturados
│   ├── RawCaptureSession.kt     # Gerenciamento de captura RAW+JPEG concorrente
│   ├── RawFrame.kt              # Estrutura de dados de frame RAW
│   └── RawRenderer.kt           # Algoritmo de Bayer Demosaic e conversão RAW
├── film/
│   ├── FilmProfile.kt           # Modelo e propriedades dos perfis de filme
│   ├── FilmStock.kt             # Constantes físicas e parâmetros das emulsões canônicas
│   ├── FilmProfileRepository.kt # Repositório central de perfis
│   ├── FeatureFlags.kt          # Alternadores de recursos (ex: pipeline físico)
│   ├── FilmLabSettings.kt       # Ajustes finos do laboratório de revelação
│   ├── FilmDevelopmentEngine.kt # Revelação legada baseada em Bitmap
│   └── pipeline/                # Revelação física linear float
│       ├── FilmPipelineEngine.kt    # Orquestrador do pipeline físico
│       ├── FilmPixelBuffer.kt       # Buffer linear float separado em R, G, B
│       ├── PhysicalToneProcessors.kt # Exposição e resposta de densidade H&D
│       ├── PhysicalColorProcessor.kt # Matrizes de cores dos estoques e balanço
│       ├── PhysicalLightProcessors.kt # Simulação de Halation e Bloom óticos
│       ├── ScannerTransformProcessor.kt # Transformações de inversão de negativo e preto
│       └── StructuredGrainProcessor.kt  # Algoritmo físico-estrutural de grão
├── image/
│   ├── YuvConverter.kt          # Conversor YUV para RGB Bitmap
│   ├── BitmapTransforms.kt      # Manipulações geométricas e coloridas auxiliares
│   ├── CaptureMetadata.kt       # Consolidação de metadados EXIF e do sensor
│   └── ImageSaver.kt            # Gravação assíncrona com registro no MediaStore
├── review/
│   └── ReviewScreen.kt          # Tela de visualização e revelação da foto tirada
└── ui/
    ├── components/
    │   ├── CaptureButton.kt     # Botão físico de disparo com micro-animação
    │   └── FilmSelector.kt      # Carousel deslizante para seleção dos 20 filmes
    └── theme/
        ├── Color.kt             # Paleta de cores escura e quente premium
        ├── Theme.kt             # Tema Material 3 customizado
        └── Type.kt              # Tipografia Outfit/Inter importada
```

---

## Requisitos Técnicos

- **Android 8.0 (API 26)** ou superior
- Suporte completo a **Camera2 API** (`HARDWARE_LEVEL_3` ou `LIMITADO` com suporte a RAW)
- Calibração fina realizada especificamente para o hardware do **Galaxy S24 Ultra**

## Como Compilar e Executar

1. Abra o projeto no **Android Studio (Ladybug ou mais recente)**.
2. Certifique-se de que o SDK do Android 34 está instalado.
3. Conecte um dispositivo físico via USB (recomendado para testar a captura RAW real) ou utilize o Emulador.
4. Execute `gradlew installDebug` ou pressione o botão de **Run (Shift+F10)** no Android Studio.

## Evoluções Recentes & Correções
- **Pipeline Físico Completo (Float Space)**: Transição de processamento em espaço RGB 8-bits para float linear, preservando a latitude de exposição.
- **Suporte a RAW/DNG**: Captura de dados não comprimidos direto do sensor com Demosaic customizado.
- **Estruturação de Módulos**: Divisão do código de processamento óptico (bloom/halation) e tonal (H&D curve/exposure).
