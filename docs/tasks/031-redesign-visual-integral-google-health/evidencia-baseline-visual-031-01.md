# Evidência 031-01 — Baseline visual sanitizado

- **Data da auditoria:** 2026-09-03
- **Referência:** `com.fitbit.FitbitMobile`
- **Estado:** baseline documental concluído; inspeção física sanitizada parcial

## Identidade e disponibilidade

| Item | Resultado sanitizado | Fonte ou limite |
|---|---|---|
| Pacote | `com.fitbit.FitbitMobile` | Google Play e `pm path` em 2026-09-03 |
| Nome público atual | Google Health (Fitbit) | [Google Play](https://play.google.com/store/apps/details?id=com.fitbit.FitbitMobile) |
| Versão observada | `5.06.1.health-mobile-1104101009-966217170` | `dumpsys package` em 2026-09-03 |
| Version code | `1104101009` | `dumpsys package` em 2026-09-03 |
| Data da versão pública | Atualização indicada em 2026-09-02 | Página pública consultada em 2026-09-03; número da versão não publicado |
| Activity resolvida pelo pacote | `com.fitbit.HealthBrandedAlias` | `cmd package resolve-activity` em 2026-09-03 |
| Activity efetivamente exibida | `com.google.android.apps.fitbit.app.home.impl.HomeActivity` | `topResumedActivity` no início e no fim da inspeção |
| Disponibilidade pública | Listagem acessível | Google Play |
| Disponibilidade no ADB desta execução | Disponível no `SM-S901E` autorizado | `adb devices -l` em 2026-09-03 |

A evidência física anterior está em
[`evidencia-baseline-visual-024-01.md`](../024-refatoracao-visual-google-health/evidencia-baseline-visual-024-01.md),
linhas 6–16 e 144–157. A versão atual acima substitui o contexto histórico de
2026-08-17 para esta auditoria.

## Fontes públicas consultadas

- [Google Health](https://healthapp.google/): associação pública entre Google
  Health, Fitbit e o download do pacote de referência.
- [Your health connected](https://healthapp.google/your-health-connected):
  composição pública de dados conectados e lista de atividades.
- [Privacy](https://healthapp.google/privacy): controles de privacidade e
  proteção de dados apresentados como parte da experiência pública.
- [Google Play](https://play.google.com/store/apps/details?id=com.fitbit.FitbitMobile):
  nome, pacote, disponibilidade, descrição, data de atualização e imagens
  públicas.

As páginas públicas são material de catálogo e marketing. Não foram tratadas como
prova de bounds, navegação instalada, estados de erro ou paridade de qualquer
superfície. Imagens, logos, fontes e assets proprietários não foram copiados nem
adicionados ao app.

## Inspeção física e privacidade

Comandos previstos pela subtarefa:

```text
git diff --check
adb devices -l
adb shell pm path com.fitbit.FitbitMobile
adb shell uiautomator dump --compressed /sdcard/window.xml
```

Resultado em 2026-09-03: a primeira tentativa ocorreu antes da conexão e retornou
`no devices/emulators found`. Após o aparelho ser conectado, a reexecução de
`adb devices -l`, `pm path` e `uiautomator dump` passou. `git diff --check` também
passou. O pacote foi localizado com APK base e splits; a Activity foi resolvida;
o XML permaneceu temporário e foi removido após a leitura estrutural.

Nenhum XML, screenshot, Logcat, conta, identificador, título livre, métrica,
payload BLE ou dado de saúde foi persistido. A inspeção navegou somente por
Home, Saúde, detalhes de métricas e Dispositivos; não executou importação,
medição, sincronização, exclusão ou alteração de conta. Ao final, a referência
ficou na Home e o estado do aparelho não foi alterado.

Descarte confirmado em 2026-09-03: `adb shell 'test ! -e /sdcard/window.xml &&
printf "window_xml_absent\\n" || printf "window_xml_present\\n"'` retornou
`window_xml_absent`. Não houve screenshot bruto criado nesta execução; não há
temporário bruto pendente conhecido no aparelho ou no workspace.

## Observações públicas aproveitáveis

Padrões públicos observáveis, sem equivalência funcional presumida:

- resumo de saúde conectado em um lugar;
- agrupamento de atividades recentes e ação para ver todas;
- cartões/listas com atividade, dispositivo e metadados resumidos;
- acesso explícito a dispositivos, apps e controles de privacidade.

Na inspeção física, a barra inferior apresentou `Hoje` e `Saúde`. O hub exibiu
cards genéricos de `Frequência cardíaca`, `Peso`, `Passos` e `Distância`. Os
detalhes desses quatro cards foram abertos sem alterar dados. Filtros, gráficos,
campos de entrada, estados vazios/loading/erro e superfícies não alcançadas com
fluxo não sensível ficam `N/A`; não se infere paridade a partir de material de
marketing.

## Geometria e estrutura física da referência

Na inspeção física sanitizada, o viewport observado usou bounds até `[1080,2340]`.
Resumo estrutural por superfície:

| Superfície física | Nós | Clicáveis | Roláveis | Bounds distintos | Labels genéricos retidos |
|---|---:|---:|---:|---:|---|
| Home | 63 | 10 | 0 | 42 | `Hoje`, `Saúde`, `Distância` |
| Saúde | 74 | 9 | 1 | 55 | `Frequência cardíaca`, `Peso`, `Passos`, `Distância`, `Hoje`, `Saúde` |
| Frequência cardíaca | 66 | 13 | 1 | 58 | `Frequência cardíaca`, `Hoje`, `Próximo` |
| Peso | 82 | 16 | 1 | 72 | `Peso`, `Próximo` |
| Passos | 75 | 14 | 1 | 65 | `Passos`, `Hoje`, `Próximo` |
| Distância | 75 | 14 | 1 | 65 | `Distância`, `Hoje`, `Próximo` |
| Dispositivos | 47 | 7 | 0 | 35 | `Dispositivos` |

Bounds genéricos retidos para navegação e títulos: barra `Hoje` `[232,2236][308,2281]`,
`Saúde` `[758,2236][861,2281]`; cards do hub com labels em `[79,487][476,544]`,
`[585,487][679,544]`, `[585,1122][719,1179]` e `[585,1647][766,1704]`; título
de detalhe inicia em `[157,131]` e controles `Próximo` ficam próximos de
`[800,450][868,521]`. O restante dos textos foi descartado ou retido apenas
quando label genérico.

## Geometria estática do Champion atual

Bounds de runtime do Champion não foram coletados nesta subtarefa. A leitura
estática confirma o padrão comum:

- raiz `fillMaxSize`, rolagem vertical e margem externa de `24 dp` via
  `ChampionSpacing.extraLarge`;
- ritmo predominante de `16 dp` via `ChampionSpacing.large`;
- cards geralmente com `fillMaxWidth()` e `ChampionShapeLarge` de `16 dp`;
- títulos principais marcados como heading; ações preservam descrições e estados
  semânticos quando existentes.

Fontes: [`ChampionTokens.kt`](../../../app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionTokens.kt),
[`HomeScreen.kt`](../../../app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt),
[`HealthScreen.kt`](../../../app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt).

## Matriz referência → superfície atual

`N/A` na coluna de referência significa que a superfície não foi observada
fisicamente nesta execução. A coluna de bounds não inventa medidas de viewport.

| Superfície | Referência observável | Estrutura atual, usando labels genéricos | Bounds atuais | Direção para subtarefa posterior |
|---|---|---|---|---|
| Home | Física: resumo; labels genéricos `Hoje` e `Saúde` | Header `Hoje`, status do relógio, atualização, resumo diário, passos/meta, BPM, distância, calorias, pendências e atalhos | Ref.: bounds estruturais observados; Champion runtime `N/A`; fonte: coluna `24 dp`, ritmo `16 dp` | Reorganizar resumo; manter ações e ausência segura (`031-05`) |
| Saúde hub | Física: quatro cards de métrica e barra `Hoje`/`Saúde` | Header `Saúde` e quatro cards acionáveis: frequência cardíaca, peso, passos e distância | Ref.: labels em quatro bounds de card; Champion runtime `N/A` | Aplicar composição aprovada sem duplicar Health Connect (`031-06`) |
| Detalhe de frequência cardíaca | Física: título, controle `Próximo` e retorno `Hoje`; gráfico/estado não retidos | Resumo atual, medição manual, importação D1, gráfico, histórico, período e retorno | Ref.: título `[157,131]`; Champion runtime `N/A` | Preservar ações explícitas, lacunas e unidade (`031-06`) |
| Detalhe de peso | Física: título e controle `Próximo`; campos não retidos | Valor atual, origem/data, entrada manual em kg, histórico e estados de ausência | Ref.: título `[157,131]`; Champion runtime `N/A` | Preservar edição e semântica de origem (`031-06`) |
| Detalhe de passos | Física: título, `Hoje` e `Próximo`; gráfico/estado não retidos | Resumo diário, meta/progresso, períodos `7D/30D/90D`, resumo, gráfico e lista acessível | Ref.: título `[157,131]`; Champion runtime `N/A` | Preservar gaps, período e unidade (`031-06`) |
| Detalhe de distância | Física: título, dois labels `Hoje` e `Próximo`; gráfico/estado não retidos | Resumo estimado, períodos `7D/30D/90D`, agregados, gráfico e lista acessível | Ref.: título `[157,131]`; Champion runtime `N/A` | Expor estimativa e origem sem dado sintético (`031-06`) |
| Lista de Exercícios | Público: atividades recentes; tela específica física `N/A` | Header, PoC debug-only, importação explícita, progresso, retry/cancelamento, sync, filtros e lista | `N/A` na referência física; Champion runtime `N/A` | Reorganizar sem iniciar BLE/sync ao renderizar (`031-07`) |
| Detalhe de atividade | `N/A` | Tipo, distância manual, título, observações, plausibilidade, calibração, gráfico, descarte, exclusão e sync | `N/A` | Preservar revisão, callbacks e estados (`031-07`) |
| Calendário | `N/A` | Header, mês anterior/próximo/Hoje, grade mensal, seleção, legenda e detalhe diário | `N/A`; grade Champion de sete colunas em fonte | Tornar entrada contextual de Exercícios/Home/Saúde (`031-07`) |
| Detalhe diário | `N/A` | Passos, distância, BPM, atividades, peso, fila local e navegação relacionada | `N/A` | Preservar somente leitura e retorno contextual (`031-07`) |
| Perfil | `N/A` | Altura, meta, comprimento de passo, perfis aprendidos e histórico/correção de calibração | `N/A` | Aplicar composição sem alterar precisão ou snapshots (`031-08`) |
| Dispositivo | Física: tela `Dispositivos` alcançada; estados de pareamento não exercitados | Permissões BLE, scan, seleção, associação, conexão, desconexão, esquecimento, diagnóstico e exportação sanitizada | Ref.: tela 47 nós; Champion runtime `N/A` | Manter estado assíncrono; PoC de mapa não entra nesta tela (`031-09`) |
| Configurações | Público: controles de privacidade; tela física `N/A` | Health Connect write-only, disponibilidade/provider, toggle, permissões, sync explícito e fila local | `N/A` | Manter distinção entre estado local e confirmação remota (`031-10`) |

## Shell e lacunas de navegação atuais

Leitura estática de [`ChampionNavigation.kt`](../../../app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt)
confirma quatro entradas primárias (`Início`, `Saúde`, `Calendário`, `Exercícios`)
e três adicionais (`Dispositivo`, `Perfil`, `Configurações`). Isso diverge da
decisão da Spec 031, que exige somente `Home` e `Saúde` na barra inferior.

Também ficam registradas estas lacunas para `031-04`:

- Home já abre Dispositivo e Perfil pelo header, mas não possui entrada atual
  para Calendário;
- Saúde possui os quatro detalhes, mas ainda não possui entradas para Exercícios
  e Calendário;
- Exercícios ainda não possui ação de Calendário;
- Perfil ainda não expõe ação de Configurações.

Essas lacunas são baseline, não foram corrigidas nesta subtarefa.

## Estados não observados

| Estado | Resultado | Motivo |
|---|---|---|
| UI instalada atual da referência | Parcialmente observada | Home, Saúde, quatro detalhes e Dispositivos; demais superfícies `N/A` |
| Versão/Activities atuais por ADB | Confirmadas | `5.06.1.health-mobile-1104101009-966217170`; alias resolvido e `HomeActivity` exibida |
| Bounds atuais da referência | Parciais | Bounds e agregados sanitizados das sete telas acessadas; sem XML versionado |
| Filtros e gráficos da referência | `N/A` | Não retidos quando não havia label estrutural seguro; fontes públicas não comprovam controles instalados |
| Loading, erro, vazio e permissões da referência | `N/A` | Não fabricar estados nem abrir fluxo sensível |
| Landscape, fonte ampliada e temas da referência | `N/A` | Inspeção física limitada a configuração atual |
| Dados pessoais e estados de conta | Não coletados | Regra de privacidade |

## Configuração e restauração física

Estado inicial da inspeção efetiva, registrado após abertura explícita da Activity:

- dispositivo: modelo `SM-S901E`; identificador/serial não persistido;
- viewport físico: `1080x2340`; densidade física `480`; override de densidade `450`;
- orientação: portrait (`0`); escala de fonte `1.0`; tema escuro ativo;
- tela inicial da inspeção: Home, Activity `com.google.android.apps.fitbit.app.home.impl.HomeActivity`;
- configurações de sistema, conta e dados da referência: não alterados.

A navegação foi somente leitura. Após voltar das telas acessadas, a Activity foi
verificada novamente na Home; não houve restauração de configuração necessária.

## Decisões, desvios e riscos residuais

- **Decisão:** combinar fontes públicas atuais com inspeção física sanitizada da
  versão instalada em 2026-09-03; não declarar paridade visual não observada.
- **Desvio:** a primeira tentativa ADB ocorreu sem dispositivo e falhou; a
  reexecução após conexão passou. A cobertura física continua parcial, com
  superfícies não alcançadas mantidas como `N/A`.
- **Desvio:** não foram criadas capturas novas. A listagem pública e o relatório
  histórico são suficientes para baseline documental, sem versionar assets
  proprietários.
- **Decisão de privacidade:** dumps foram processados apenas para contagens,
  classes, bounds e labels genéricos; o estado final foi deixado na Home.
- **Risco:** versão, região, conta e testes A/B podem alterar a referência; a
  versão de 2026-09-03 não é contrato futuro.
- **Risco:** fidelidade de filtros, gráficos, estados, telas não alcançadas,
  landscape, fonte ampliada e temas exige inspeção autorizada adicional antes
  do aceite visual das telas.

## Arquivos e evidências

- Este relatório sanitizado.
- `031-01-auditar-referencia-e-baseline.md`, atualizado com status, evidências e
  decisão da revisão.
- `031-00-overview.md`, item `031-01` marcado após revisão independente.
- Nenhum XML, screenshot bruto, Logcat ou dado pessoal foi adicionado.
