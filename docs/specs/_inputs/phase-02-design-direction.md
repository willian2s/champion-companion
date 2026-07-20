# Direção visual da Fase 02

## Decisão

O Champion Companion não deve reutilizar a identidade visual, a paleta de cores ou os tokens de tema do MotionLog.

O novo aplicativo deve adotar uma identidade própria, baseada nas práticas e nos componentes do Material 3 para Jetpack Compose.

## Objetivos

- criar uma interface coerente com aplicativos Android nativos modernos;
- oferecer suporte completo aos temas claro e escuro;
- utilizar cores semânticas do `MaterialTheme.colorScheme`;
- evitar cores literais dentro de telas e componentes;
- manter contraste e legibilidade adequados;
- permitir evolução do design system sem acoplamento às telas.

## Material 3

Utilizar exclusivamente componentes e APIs de:

```text
androidx.compose.material3
```

Não utilizar componentes do Material 2 quando houver equivalente estável no Material 3.

O tema principal deve ser aplicado por meio de:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    shapes = shapes,
    content = content,
)
```

## Esquemas de cores

Implementar:

- um `LightColorScheme` próprio;
- um `DarkColorScheme` próprio;
- suporte opcional a cores dinâmicas no Android 12 ou superior;
- fallback obrigatório para os esquemas próprios.

As cores próprias devem ser criadas a partir de uma nova cor-base adequada ao Champion Companion.

Não copiar ou adaptar diretamente as cores do MotionLog.

A escolha da cor-base e das paletas tonais deverá ser documentada durante a Fase 02.

## Cores dinâmicas

O aplicativo deve suportar Material You por meio de:

```kotlin
dynamicLightColorScheme(context)
dynamicDarkColorScheme(context)
```

quando disponível.

O uso de cores dinâmicas deve respeitar:

- Android 12 ou superior;
- preferência definida pelo design system;
- fallback para os temas próprios;
- legibilidade das informações de saúde, conexão e atividade.

A implementação não pode depender exclusivamente de cores dinâmicas.

## Papéis semânticos

Telas e componentes devem consumir cores por meio de papéis como:

```text
primary
onPrimary
primaryContainer
onPrimaryContainer

secondary
onSecondary
secondaryContainer
onSecondaryContainer

tertiary
onTertiary
tertiaryContainer
onTertiaryContainer

background
onBackground

surface
onSurface
surfaceVariant
onSurfaceVariant

error
onError
errorContainer
onErrorContainer

outline
outlineVariant
```

Não criar nomes de cores baseados em telas, como:

```text
homeBlue
exerciseGreen
deviceCardGray
```

Quando houver estados de domínio que precisem de representação visual, criar tokens semânticos específicos, sem substituir os papéis básicos do Material 3.

Exemplos possíveis:

```text
connected
disconnected
syncPending
syncSuccess
syncFailure
heartRate
activityRunning
activityPaused
```

Esses tokens devem possuir variantes adequadas para temas claro e escuro.

## Regras para componentes

Os componentes não devem:

- acessar cores hexadecimais diretamente;
- decidir manualmente se o tema é claro ou escuro;
- reutilizar cores do MotionLog;
- usar `Color.White` ou `Color.Black` como substitutos de tokens;
- definir cores locais sem justificativa;
- usar cor como único indicador de estado.

Os componentes devem consumir:

```kotlin
MaterialTheme.colorScheme
MaterialTheme.typography
MaterialTheme.shapes
```

ou tokens próprios definidos centralmente pelo design system.

## Tema claro

O tema claro deve:

- usar superfícies claras, mas evitar branco puro indiscriminadamente;
- diferenciar fundo, superfícies e containers;
- manter contraste adequado entre conteúdo e superfície;
- utilizar elevação tonal de acordo com o Material 3;
- evitar o visual do MotionLog.

## Tema escuro

O tema escuro deve:

- ser projetado separadamente, não apenas inverter o tema claro;
- evitar preto puro como superfície padrão, salvo necessidade específica;
- manter hierarquia entre fundo, superfícies e containers;
- evitar cores excessivamente saturadas;
- preservar contraste e legibilidade;
- representar corretamente estados de erro, conexão e sincronização.

## Configuração do tema

Inicialmente, o aplicativo deve seguir o tema do sistema:

```kotlin
isSystemInDarkTheme()
```

A arquitetura deve permitir futuramente as opções:

```text
Seguir o sistema
Claro
Escuro
```

Não é obrigatório implementar a tela dessa preferência nesta fase, salvo previsão explícita no roadmap.

## Acessibilidade

O design system deve considerar:

- contraste de textos e ícones;
- estados que não dependam apenas de cor;
- escalabilidade de fonte;
- áreas mínimas de toque;
- suporte a leitores de tela;
- temas claro e escuro;
- preview dos componentes em ambos os temas.

## Previews

Os componentes principais devem possuir previews em:

- tema claro;
- tema escuro;
- tamanhos de fonte relevantes quando aplicável;
- estados principais, como carregando, vazio, sucesso e erro.

## Fora do escopo

A Fase 02 não deve:

- copiar telas do MotionLog;
- reproduzir a paleta do MotionLog;
- implementar funcionalidades BLE;
- implementar Health Connect;
- criar lógica real de sincronização;
- definir cores diretamente dentro das features;
- criar uma identidade visual complexa fora do escopo do Material 3.

## Critérios de aceite

A direção visual será considerada atendida quando:

- existir um tema Material 3 centralizado;
- existirem esquemas próprios claro e escuro;
- houver fallback independente de cores dinâmicas;
- nenhuma cor do MotionLog tiver sido copiada;
- telas e componentes utilizarem papéis semânticos;
- componentes principais possuírem previews claro e escuro;
- não houver cores literais espalhadas pelas features;
- o aplicativo respeitar o tema do sistema;
- contraste e legibilidade forem validados nos dois temas.
