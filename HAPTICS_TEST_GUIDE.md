# Sistema Háptico Roll24 - Guia de Teste Manual

## Visão Geral

O sistema háptico foi implementado com sucesso no Roll24, fornecendo feedback tátil premium para todas as interações principais.

## Arquitetura

### Arquivos Criados

1. **Roll24Haptics.kt** - Classe principal com API semântica
   - `filmTick()` - Micro tick ao navegar filmes
   - `filmLoaded()` - Clique de encaixe ao selecionar filme
   - `dialStep()` - Tick preciso para dials (ISO/EV/WB)
   - `shutterHalfPress()` - Pré-clique leve
   - `shutterRelease()` - Obturador mecânico
   - `developingStart()` - Início do processamento
   - `developingProgress()` - Progresso sutil
   - `developingComplete()` - Conclusão do processamento
   - `saveSuccess()` - Confirmação de salvamento
   - `unavailable()` - Ação indisponível (dois pulsos)
   - `discard()` - Descarte sutil

2. **Roll24HapticEffect.kt** - Definições de efeitos
   - Timings e amplitudes para cada efeito
   - Suporte a primitivas avançadas (Android 8+)

3. **RememberRoll24Haptics.kt** - Helper para Compose
   - `rememberRoll24Haptics()` - Cria e lembra instância

### Arquivos Modificados

1. **AndroidManifest.xml**
   - Adicionada permissão `VIBRATE`

2. **FilmSelector.kt**
   - `filmTick()` ao scroll (detecta mudança de item visível)
   - `filmLoaded()` ao selecionar filme

3. **CaptureButton.kt**
   - `shutterHalfPress()` no início do press
   - `shutterRelease()` ao soltar

4. **ReviewScreen.kt**
   - `saveSuccess()` ao salvar
   - `discard()` ao descartar

5. **CameraScreen.kt**
   - `developingStart()` quando processamento inicia
   - `developingComplete()` quando processamento termina

## Como Testar

### 1. Navegação entre Filmes

**Ação:** Scroll horizontal no seletor de filmes

**Esperado:** 
- Micro tick seco a cada filme que passa
- Sensação de catraca pequena
- Não deve ser forte

**Verificação:**
```
1. Abra o app
2. Deslize o dedo pelo seletor de filmes
3. Sinta ticks leves e secos a cada filme
```

### 2. Seleção de Filme

**Ação:** Toque em um filme específico

**Esperado:**
- Clique médio de encaixe
- Mais forte que filmTick
- Sensação de cartucho entrando no lugar

**Verificação:**
```
1. Toque em diferentes filmes
2. Sinta o "clack" de encaixe
3. Deve ser satisfatório mas curto
```

### 3. Captura de Foto

**Ação:** Pressione o botão de captura

**Esperado:**
- Tick muito leve ao pressionar (shutterHalfPress)
- Clique seco e encorpado ao soltar (shutterRelease)
- Sensação de obturador mecânico moderno

**Verificação:**
```
1. Pressione o botão de captura
2. Sinta o pré-clique leve
3. Solte e sinta o clique encorpado
4. Deve parecer câmera real
```

### 4. Processamento da Foto

**Ação:** Aguarde o processamento após captura

**Esperado:**
- Pulso suave ao iniciar (developingStart)
- Clique de conclusão ao terminar (developingComplete)
- Sensação de "máquina trabalhando" e "pronto"

**Verificação:**
```
1. Capture uma foto
2. Sinta o pulso suave quando o loading aparece
3. Aguarde o processamento
4. Sinta o clique de conclusão quando a review aparece
```

### 5. Salvamento da Foto

**Ação:** Toque no botão "Salvar" na tela de review

**Esperado:**
- Confirmação curta e limpa
- Sensação de carimbo ou trava fechando

**Verificação:**
```
1. Após processamento, toque em "Salvar"
2. Sinta a confirmação tátil
3. Deve ser satisfatório e definitivo
```

### 6. Descarte da Foto

**Ação:** Toque no botão "Descartar" na tela de review

**Esperado:**
- Feedback sutil
- Mais leve que saveSuccess
- Sensação de "jogar fora"

**Verificação:**
```
1. Capture uma foto
2. Na review, toque em "Descartar"
3. Sinta o feedback sutil
4. Deve ser discreto
```

## Características dos Efeitos

### FilmTick
- **Duração:** 8ms
- **Amplitude:** 40 (baixa)
- **Sensação:** Catraca pequena, seca

### FilmLoaded
- **Duração:** 15ms
- **Amplitude:** 120 (média)
- **Sensação:** Encaixe, cartucho entrando

### ShutterHalfPress
- **Duração:** 6ms
- **Amplitude:** 30 (muito baixa)
- **Sensação:** Pré-clique, quase imperceptível

### ShutterRelease
- **Duração:** 20ms
- **Amplitude:** 180 (alta)
- **Sensação:** Obturador mecânico, encorpado

### DevelopingStart
- **Duração:** 25ms
- **Amplitude:** 80 (média-baixa)
- **Sensação:** Máquina começando

### DevelopingComplete
- **Duração:** 18ms
- **Amplitude:** 140 (média-alta)
- **Sensação:** Conclusão satisfatória

### SaveSuccess
- **Duração:** 16ms
- **Amplitude:** 130 (média-alta)
- **Sensação:** Carimbo, trava fechando

### Discard
- **Duração:** 10ms
- **Amplitude:** 50 (baixa)
- **Sensação:** Descarte sutil

### Unavailable
- **Duração:** 12ms + 40ms pausa + 12ms
- **Amplitude:** 90 (média)
- **Sensação:** Dois pulsos secos, "não encaixou"

## Compatibilidade

### Android 8+ (API 26+)
- Usa `VibrationEffect.createWaveform()`
- Suporte a timings e amplitudes customizados
- Efeitos mais precisos

### Android 7 e anteriores
- Fallback para `Vibrator.vibrate(duration)`
- Usa duração total do efeito
- Menos preciso mas funcional

### Dispositivos sem Vibrador
- Log de warning
- Não quebra o app
- Experiência silenciosa

## Design Sensorial

### ✅ Correto
- Preciso
- Curto
- Seco
- Premium
- Mecânico
- Controlado
- Analógico moderno

### ❌ Evitado
- Vibração longa
- Vibração genérica de notificação
- Efeito de brinquedo
- Efeito de celular barato
- Excesso de feedback
- Vibração em toda microinteração

## Troubleshooting

### Não sinto vibração
1. Verifique se o dispositivo tem motor háptico
2. Verifique se as configurações de vibração estão ativas
3. Verifique logs no Logcat: `adb logcat | grep Roll24Haptics`

### Vibração muito forte
- Ajuste amplitudes em `Roll24HapticEffect.kt`
- Reduza valores de amplitude (max 255)

### Vibração muito fraca
- Aumente amplitudes em `Roll24HapticEffect.kt`
- Verifique se o dispositivo suporta as amplitudes usadas

### Spam de vibração
- Verifique se `distinctUntilChanged()` está sendo usado
- Confirme que haptics só dispara em ações reais do usuário

## Próximos Passos (Opcional)

1. **Dials de ISO/EV/WB**
   - Implementar quando sliders forem adicionados
   - Usar `dialStep()` com throttling

2. **Primitivas Avançadas**
   - Testar `VibrationEffect.Composition` em Android 12+
   - Usar primitivas como `PRIMITIVE_CLICK` se disponível

3. **Customização por Perfil**
   - Diferentes intensidades por perfil de filme
   - Mono Press 400: mais seco e mecânico
   - Soft Portrait 400: mais suave

## Conclusão

O sistema háptico está implementado e integrado em todos os pontos principais da UI. A experiência deve parecer tátil, premium e analógica moderna, reforçando a sensação de câmera física sem ser caricata.
