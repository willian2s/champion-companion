# Task 002-01 — Auditar Fundação Visual

## Status

Concluída em 2026-07-20.

## Dependência

- `002-00` lida.

## Objetivo

Inventariar tema, recursos e cores existentes; registrar linha de base e exclusão explícita de identidade MotionLog.

## Pode modificar

- Este arquivo.

## Inventário auditado

### Tema Compose

| Área | Estado atual | Migração |
| --- | --- | --- |
| `ui/theme/Color.kt` | Paleta padrão gerada: `Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40`. | `002-02` substitui por paletas próprias completas, seed `#006782`. |
| `ui/theme/Theme.kt` | Material 3, `darkColorScheme`/`lightColorScheme` parciais, tema segue sistema; Material You está ativo em API 31+. Parâmetro atual chama-se `dynamicColor`. | `002-02` move para `core.designsystem`, completa todos papéis, adiciona shapes e renomeia contrato para `useDynamicColors`. |
| `ui/theme/Type.kt` | `Typography` Material 3, fonte padrão; somente `bodyLarge` customizado. | `002-02` preserva fonte do sistema e define escala centralizada necessária. |
| `MainActivity.kt` | Aplica `ChampionCompanionTheme` antes de `ChampionNavigation`. | `002-07` troca import para tema central, sem mudar fluxo. |

Dependência `libs.androidx.compose.material3` é única biblioteca Material declarada em `app/build.gradle.kts`. Busca não encontrou import ou dependência Material 2.

### Shell e telas Compose

| Caminho | Estado atual | Migração |
| --- | --- | --- |
| `core/navigation/ChampionNavigation.kt` | `Scaffold`, `NavigationBar`, `NavigationBarItem`, `DropdownMenu` e textos Material 3; sete destinos preservados. | `002-07` aplica tokens/componentes; não altera rotas. |
| `feature/PlaceholderScreen.kt` | Texto usa tipografia Material 3; `24.dp` e `12.dp` locais. | `002-07` substitui spacing local por `ChampionSpacing` e aplica componentes de feedback quando adequado. |
| `feature/*/*Screen.kt` | Telas delegam para placeholder; nenhuma cor local encontrada. | `002-07` mantém contratos e remove dependência indireta de tema legado. |

### Recursos Android

| Caminho | Estado atual | Migração |
| --- | --- | --- |
| `AndroidManifest.xml` | Usa `@mipmap/ic_launcher`, `@mipmap/ic_launcher_round` e `Theme.ChampionCompanion`; não declara splash. | `002-08` configura splash Android 12+ e recursos de marca. |
| `res/mipmap-anydpi/ic_launcher*.xml` | Ícone adaptativo atual usa foreground padrão também como monocromático. | `002-08` cria marca própria e vetor monocromático dedicado. |
| `res/drawable/ic_launcher_background.xml` e `ic_launcher_foreground.xml` | Vetores/gradiente de template Android; fundo verde `#3DDC84`, robô Android e branco/preto translúcido. | `002-08` substitui por vetores próprios; não criar bitmap novo. |
| `res/mipmap-*/ic_launcher*.webp` | Rasters legados do launcher gerado. | `002-08` remove/substitui somente após confirmar fallback de ícone compatível. |
| `res/values/colors.xml` | Cores padrão roxa, teal, preto e branco; sem consumidor encontrado no código Compose. | `002-02`/`002-08` remove ou substitui referências necessárias após migração. |
| `res/values/themes.xml` | `android:Theme.Material.Light.NoActionBar` mínimo. | `002-08` adapta tema XML ao SplashScreen; Compose continua dono do tema visual. |

### Cores, fontes e acessibilidade

- Cores literais existem somente no tema gerado e recursos launcher listados acima. Nenhuma feature declara `Color`, hex, `Color.White` ou `Color.Black`.
- Trecho comentado em `ui/theme/Theme.kt` contém exemplos padrão com `Color.White` e hex; `002-02` deve removê-lo ao substituir arquivo.
- Fonte atual é `FontFamily.Default`; não há arquivo de fonte externa.
- Não existem spacing, shapes ou tokens de elevação centralizados. Contraste dos novos pares ainda não foi validado; responsabilidade de `002-02` e `002-09`.

## Exclusão MotionLog

- Busca case-insensitive por `motionlog` em `app`, Gradle e configuração não encontrou referência.
- Paleta e ícones atuais são recursos padrão de template Android, não identificadores MotionLog. Eles não são aprovados para produto e serão substituídos.
- Fonte autorizada para nova identidade: somente spec/direção da Fase 02, com seed primário `#006782`. Não importar paleta, token, componente, tela ou asset do MotionLog.

## Riscos de migração

- Mover tema altera import de `MainActivity`; manter wrapper único para evitar tema duplicado.
- Esquemas atuais são parciais; preencher papéis Material 3 antes de migrar shell evita fallback visual acidental.
- `monochrome` atual reaproveita foreground multicolorido; criar vetor dedicado antes de validar launcher/Android 13+.
- Raster launcher legado pode prevalecer em dispositivos antigos; confirmar recursos por densidade antes de remover.
- `PlaceholderScreen` possui spacing local; migrar sem alterar semântica de heading ou textos seguros.

## Estado Git na auditoria

`git status --short`, `git diff --name-status` e `git diff --cached --name-status` não retornaram alterações no momento da auditoria. Nenhum arquivo de código ou recurso foi modificado nesta task.

## Evidência de busca

```text
rg -n -i "motionlog|androidx\\.compose\\.material|Color\\.|Color\\(|#[0-9A-Fa-f]{6,8}|MaterialTheme|dynamic.*ColorScheme" app --glob '*.{kt,kts,xml}'
```

Resultado: Material 3 somente; cores literais concentradas no tema e recursos gerados identificados acima; zero ocorrência de `motionlog`.

## Passos

1. Inventariar `ui/theme`, `AndroidManifest.xml`, recursos `drawable`, `mipmap`, `values` e telas Compose existentes.
2. Registrar Material 3/Material 2, esquema dinâmico, cores literais, fontes, ícones e recursos gerados presentes.
3. Buscar referências a MotionLog, cores/tokens copiados e nomes de cores acoplados a feature.
4. Registrar arquivos que serão migrados por `002-02`, `002-07` e `002-08`; não os alterar nesta task.
5. Registrar `git status --short` pré-existente e preservar alterações alheias.

## Aceite

- [x] Inventário reproduzível, com caminhos e riscos de migração.
- [x] Origem independente da paleta azul-petróleo registrada; nenhuma referência MotionLog aprovada.
