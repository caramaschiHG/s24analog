# Roll24 - Build Instructions

## Pré-requisitos

- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Android SDK com API 34
- Dispositivo Android com API 26+ (Android 8.0+) ou emulador

## Como Compilar

### 1. Abrir o Projeto

1. Abra o Android Studio
2. Selecione "Open an existing project"
3. Navegue até a pasta `24mm cam`
4. Clique em "OK"

### 2. Sincronizar Gradle

O Android Studio deve automaticamente detectar o projeto e iniciar a sincronização do Gradle. Se não iniciar automaticamente:

1. Clique em "File" → "Sync Project with Gradle Files"
2. Ou clique no ícone de elefante com a seta azul na toolbar

Aguarde até que a sincronização complete (pode levar alguns minutos na primeira vez).

### 3. Configurar Dispositivo

**Opção A: Dispositivo Físico (Recomendado)**
1. Ative o "Developer Options" no seu Galaxy S24 Ultra:
   - Configurações → Sobre o telefone → Informações de software
   - Toque 7 vezes em "Número da compilação"
2. Ative "Depuração USB" em Developer Options
3. Conecte o dispositivo via USB
4. Autorize a depuração quando solicitado

**Opção B: Emulador**
1. Abra o "Device Manager" no Android Studio
2. Clique em "Create Device"
3. Selecione um dispositivo com API 34 (recomendado Pixel 7 ou similar)
4. Baixe a imagem do sistema se necessário
5. Inicie o emulador

### 4. Compilar e Executar

1. Selecione o dispositivo na toolbar (dropdown ao lado do botão "Run")
2. Clique no botão "Run" (triângulo verde) ou pressione `Shift+F10`
3. Aguarde a compilação e instalação

### 5. Primeira Execução

1. O app solicitará permissão de câmera - clique em "Grant Permission"
2. A câmera traseira será aberta
3. As capacidades da câmera serão logadas no Logcat (filtre por "Camera2Controller")
4. Selecione um perfil de filme na parte inferior
5. Toque no botão circular branco para capturar
6. Aguarde o processamento (loading aparecerá)
7. Na tela de revisão, toque "Save" para salvar ou "Discard" para descartar

## Verificar Logs

Para ver as capacidades da câmera e logs de processamento:

1. Abra o "Logcat" no Android Studio (parte inferior)
2. Filtre por:
   - `Camera2Controller` - capacidades da câmera
   - `CaptureEngine` - configurações de captura aplicadas
   - `FilmDevelopmentEngine` - processamento de imagem

## Problemas Comuns

### "Gradle sync failed"
- Verifique se o JDK 17 está instalado e configurado
- Verifique sua conexão com a internet (Gradle precisa baixar dependências)
- Tente "File" → "Invalidate Caches / Restart"

### "Camera permission denied"
- Vá em Configurações do Android → Apps → Roll24 → Permissões
- Ative a permissão de câmera
- Ou desinstale e reinstale o app

### "No back camera found"
- Verifique se o dispositivo tem câmera traseira
- Em emuladores, configure uma câmera virtual nas configurações do AVD

### "Image capture failed"
- Verifique se há espaço disponível no dispositivo
- Verifique as permissões de armazenamento (Android 10+)
- Tente reiniciar o app

### "Processing takes too long"
- Normal no MVP - processamento é feito em CPU
- Imagens grandes podem levar 2-5 segundos
- Futuras versões usarão GPU para aceleração

## Estrutura de Arquivos Gerados

Após a primeira compilação, você verá:

```
24mm cam/
├── .gradle/              # Cache do Gradle (gerado)
├── .idea/                # Configurações do Android Studio (gerado)
├── app/
│   ├── build/           # Arquivos compilados (gerado)
│   └── src/             # Código fonte
├── build/               # Build outputs (gerado)
└── local.properties     # Configurações locais (gerado)
```

## Próximos Passos

Após confirmar que o app compila e executa:

1. Teste todos os 5 perfis de filme
2. Verifique as capacidades da câmera no Logcat
3. Teste em diferentes condições de iluminação
4. Compare as fotos processadas com fotos normais do celular
5. Ajuste os parâmetros dos perfis em `FilmProfileRepository.kt`

## Suporte

Este é um projeto experimental pessoal. Para questões técnicas, revise:
- `README.md` - Visão geral do projeto
- Código fonte - Comentários explicativos
- Logcat - Logs detalhados de operação

---

**Roll24** - Analog Camera Engine
