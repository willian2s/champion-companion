# Evidência baseline visual 029-01

- **Status:** baseline parcial; smoke R8 parcial e aceite visual final pendentes
- **Fonte normativa:** somente aplicativo instalado `com.fitbit.FitbitMobile`
- **Comparação:** Champion Companion debug instalado no mesmo aparelho
- **Política:** screenshots e dumps brutos foram temporários e não são
  versionados; este documento contém apenas observações estruturais e
  reconstruções textuais sanitizadas.

## Configuração observada

| Item | Referência instalada | Champion debug |
|---|---|---|
| Pacote | `com.fitbit.FitbitMobile` | `br.com.mwconsulting.championcompanion.dev` |
| Versão | `5.06.1.health-mobile-1104101009-966217170` | `1.0-dev` |
| `versionCode` | `1104101009` | `1` |
| Activity resolvida | `com.fitbit.HealthBrandedAlias` | `br.com.mwconsulting.championcompanion.MainActivity` |
| Activity de superfície observada | `HomeActivity` | `MainActivity` |
| Aparelho | Galaxy S22, `SM-S901E` | Galaxy S22, `SM-S901E` |
| Transporte ADB | autorizado via TLS | autorizado via TLS |
| Resolução física | `1080x2340` | `1080x2340` |
| Densidade física / override | `480` / `450` | `480` / `450` |
| Escala de fonte | `1.0` | `1.0` |
| Orientação observada | portrait | portrait |
| Tema observado | escuro | escuro |
| Região observável | `pt-BR`; strings da referência em português | `pt-BR` |
| Data da captura | 2026-09-01 | 2026-09-01 |

Conta, foto, títulos, horários, BPM, peso, distâncias, identificadores de
atividade e contagens exatas foram removidos desta evidência.

## Reconstrução estrutural sanitizada

Reconstruções abaixo preservam somente composição observável: fundo escuro,
header, controles de período, cards arredondados, gráfico/calendário, filtros e
barra de navegação. Não são screenshots, não usam marca, fonte, asset ou texto
proprietário, e não podem ser usados como assets de produção.

### Home / Hoje

```text
[header: ícone dispositivo]             [título Hoje]             [perfil]

[anel circular de passos]   [card dias com exercício]
                            [card distância]
                            [card calorias]

[ação registrar] [ação iniciar] [ação secundária]
[card histórico]

[Hoje] [Saúde] [Calendário] [Exercícios] [Mais]
```

### Métrica / Saúde

```text
[voltar] [título da métrica]                                      [menu]
[D] [S] [M] [3M] [A]
[janela selecionada]                       [anterior] [próximo] [hoje]
[valor sanitizado / estado vazio]
[gráfico com eixo e lacunas, quando houver amostras]
[card explicativo ou estado vazio]
```

### Exercícios

```text
[voltar] [Exercício]                                              [menu]
[Semana]                                      [Mês]
[janela]                                      [anterior] [próximo] [hoje]
[resumo de dias] ou [calendário mensal de atividades]
[filtro de conteúdo abaixo do gráfico/calendário]
[cards de atividades ou estado vazio]
```

### Calendário

```text
[Calendário]
[mês anterior] [mês atual] [próximo mês]
[dom] [seg] [ter] [qua] [qui] [sex] [sáb]
[grade mensal com marcadores somente onde há dados]
[dia selecionado] [Abrir dia]
[legenda de estados]
```

## Matriz comparativa de cobertura

Legenda: `coberto` = superfície aberta e estrutura observada; `vazio` = estado
sem dados aberto; `N/A` = estado não alcançado com motivo concreto. Valores
reais, texto de conta e identificadores não fazem parte da matriz.

| Superfície | Referência | Champion | Estados / interação observados | Evidência sanitizada |
|---|---|---|---|---|
| Hoje / Home | coberto | coberto | header, cards de passos/exercício/distância/calorias, histórico, retorno e rolagem | reconstrução Home; captures temporárias `home-reference`, `home-champion` |
| Saúde | coberto | coberto | hub com cards de frequência cardíaca, peso, passos e distância; retorno | reconstrução Métrica; captures temporárias `health-reference`, `health-champion` |
| Frequência cardíaca | coberto | coberto | diário com série intradia disponível; semanal; mensal; período sem amostras com estado vazio; retorno | reconstrução Métrica; captures temporárias `heart-day`, `heart-week`, `heart-month`, `heart-empty` |
| Peso | coberto | coberto | períodos exibidos, gráfico, lista/resumo e retorno; valores sanitizados | reconstrução Métrica; captures temporárias `weight-reference`, `weight-champion` |
| Passos | coberto | coberto | diário, semanal e mensal; gráfico, card explicativo, rolagem e retorno | reconstrução Métrica; captures temporárias `steps-reference`, `steps-champion` |
| Distância | coberto | coberto | diário, semanal e mensal; gráfico, estado/retorno e rolagem | reconstrução Métrica; captures temporárias `distance-reference`, `distance-champion` |
| Calendário mensal | coberto | coberto | mês atual, navegação anterior/próximo, marcadores, legenda, dia com dados e dia vazio | reconstrução Calendário; captures temporárias `calendar-reference`, `calendar-champion` |
| Detalhe diário com dados | N/A | coberto | abertura somente leitura, panorama, métricas e retorno | capture temporária `calendar-day-detail`; valores removidos |
| Detalhe diário vazio | N/A | coberto | estado vazio, ausência segura e retorno | capture temporária `calendar-empty-detail` |
| Exercícios / Semana | coberto | `N/A` observado | referência possui aba; Champion instalado exibe lista única sem aba equivalente | reconstrução Exercícios; capture temporária `exercise-week` |
| Exercícios / Mês | coberto | `N/A` observado | referência possui aba/calendário; Champion instalado não expõe aba equivalente | reconstrução Exercícios; capture temporária `exercise-month` |
| Filtro de exercícios — Dias com exercícios | coberto | `N/A` observado | filtro aberto na referência; Champion não possui filtro equivalente com esse nome/semântica | capture temporária `reference-exercise-filter-days` |
| Filtro de exercícios — Duração | coberto | `N/A` observado | filtro aberto na referência; Champion não possui filtro equivalente com esse nome/semântica | capture temporária `reference-exercise-filter-duration` |
| Filtro de exercícios — Distância | coberto | `N/A` observado | filtro aberto na referência; Champion não possui filtro equivalente com esse nome/semântica | capture temporária `reference-exercise-filter-distance` |
| Detalhe de exercício | coberto | coberto | abertura de card, status/origem/tipo, estado de reprocessamento e retorno | capture temporária `exercise-detail`; valores removidos |
| Filtro `ALL` / Todas | N/A | coberto | selecionado; lista não vazia | capture temporária `filter-all` |
| Filtro `PENDING_REVIEW` | N/A | coberto | selecionado; lista não vazia | capture temporária `filter-pending-review` |
| Filtro `PENDING_CALIBRATION` | N/A | coberto | selecionado; estado vazio explícito | capture temporária `filter-pending-calibration` |
| Filtro `READY_TO_SYNC` | N/A | coberto | selecionado; lista não vazia | capture temporária `filter-ready-to-sync` |
| Filtro `SYNCED` | N/A | coberto | selecionado; lista não vazia | capture temporária `filter-synced` |
| Filtro `SYNC_FAILED` | N/A | coberto | selecionado; lista não vazia | capture temporária `filter-sync-failed` |
| Filtro `DISCARDED` | N/A | coberto | selecionado; estado vazio explícito | capture temporária `filter-discarded` |
| Dispositivo | coberto | coberto | referência: conexões e dispositivos; Champion: dispositivo associado, estado, busca e conexão; ações sem execução | captures temporárias `reference-devices`, `device-champion` |
| Perfil | coberto | coberto | referência: menu de conta; Champion: formulário de altura/meta/passo; campos não alterados | captures temporárias `reference-account-menu`, `profile-champion` |
| Configurações | coberto | coberto | referência: data/unidades, tema, notificações e preferências; Champion: Health Connect write-only e permissões | captures temporárias `reference-settings`, `settings-champion` |

### Cobertura de referência sem equivalente Champion

Cards e affordances específicos da referência foram abertos e registrados como
`coberto` somente para comparação visual. Não possuem equivalente semântico no
Champion e não são requisito para criar rota: registrar exercício, iniciar
exercício, perfil da conta, explicação de precisão, feedback editorial e
controles proprietários de período/conteúdo.

| Elemento específico da referência | Estado | Evidência |
|---|---|---|
| Registrar exercício | `N/A` para ativação; não acionar por risco de criar dado | ação visível em `reference-home` |
| Iniciar exercício | `N/A` para ativação; não acionar por risco de iniciar sessão | ação visível em `reference-home` |
| Perfil da conta | coberto somente na referência; conteúdo pessoal removido | `reference-account-menu` |
| Explicação de precisão cardíaca | coberto somente na referência; texto sanitizado | `reference-heart-detail` |
| Feedback editorial de métrica | coberto somente na referência; resposta não acionada | `reference-steps-detail` |
| Controles proprietários de período/conteúdo | coberto somente na referência; mapear apenas padrão estrutural | `reference-metric-periods`, `reference-exercise-filters` |

## Estados não alcançados e N/A

- `N/A` na referência para detalhe diário e filtros de workflow do Champion: não
  há equivalente semântico navegável; a marcação não significa tentativa
  incompleta. Dispositivo, Perfil e Configurações da referência foram cobertos
  e comparados com as superfícies correspondentes do Champion.
- `N/A` nesta captura para ativação de Registrar/Iniciar, editar, excluir,
  sincronizar, medir, importar, resetar, pagamento e ações de conta: escopo da
  subtarefa proíbe efeitos em dados, integrações ou conta.
- A captura baseline inicial usou portrait, tema escuro e escala `1.0`. Smoke
  posterior alcançou fonte `1.3`, tema claro e rotação `1`, mas fica `N/A` para
  cobertura completa de cada tela nessas variantes: não houve rolagem e revisão
  tela a tela. Configurações foram restauradas ao final.
- `N/A` nesta captura para marker, tooltip e rolagem horizontal: não foram
  observáveis nos estados alcançados; não converter em aceite presumido.
- Nenhuma ação destrutiva ou geradora de dados foi usada para fabricar estado:
  sem login, troca de conta, sync, importação, medição, exclusão, reset ou
  alteração de perfil.

### Inventário adicional percorrido

| Grupo | Telas/estados seguros percorridos | Paralelo Champion |
|---|---|---|
| Referência — conta | menu da conta, amizades e família, dados do Google Health carregados | `N/A` por identidade/conta externa ao Champion e fora do escopo de login |
| Referência — conexões e histórico médico | dispositivos, apps de parceiros, históricos médicos | paralelo estrutural parcial em Champion Dispositivo/Configurações; integrações e leitura externa `N/A` |
| Referência — configurações | data/hora/unidades, tema, notificações push, notificações por e-mail, atividade, sono, estresse/mindfulness, nutrição/hidratação | Champion Configurações coberta; preferências sem equivalente ficam `N/A` por item |
| Referência — detalhes | detalhe de peso, detalhe de passos, detalhe de distância, detalhe de calorias, detalhe cardíaco, detalhe de exercício, histórico e estados com/sem dados | cards e detalhes equivalentes de Saúde/Exercícios cobertos; detalhes sem equivalente `N/A` |
| Champion — shell | Hoje, Saúde, Calendário, Exercícios, Mais | paralelo estrutural registrado |
| Champion — Saúde | hub e detalhes de frequência cardíaca, peso, passos e distância | paralelo registrado; ações de medição/importação não acionadas |
| Champion — workflow | lista, detalhe, sete filtros: Todas, Aguardando revisão, Aguardando calibração, Prontas, Sincronizadas, Com erro, Descartadas; sem abas Semana/Mês no APK observado | paralelo parcial com exercícios da referência; ausência das abas registrada como `N/A` |
| Champion — suporte | Dispositivo, Perfil, Configurações, estado conectado/conectando, estado Health Connect e permissões de escrita | paralelo parcial com conexões/configurações da referência; Health Connect e workflow próprios ficam `N/A` |

Menus externos de conta, ajuda, feedback, privacidade, termos, upgrade e
pagamento foram classificados `N/A`: saem do fluxo visual do aplicativo ou
violam limites de conta/site/pagamento da subtarefa.

### Smoke físico adicional de R8

| Variante | Fitbit | Champion | Resultado |
|---|---|---|---|
| Portrait, tema escuro, fonte `1.0` | Home carregou e dump foi capturado | Home, Saúde, Calendário, Exercícios, Dispositivo, Perfil e Configurações carregaram em navegação segura | smoke estrutural concluído; cobertura de interações permanece parcial |
| Portrait, tema escuro, fonte `1.3` | Home carregou e dump foi capturado | Home carregou; rótulos da navegação quebraram em linhas e alguns cards ficaram fora da viewport | smoke parcial; rolagem e acessibilidade tela a tela pendentes |
| Portrait, tema claro, fonte `1.0` | Home carregou | Home, Saúde, Calendário, Exercícios, Dispositivo, Perfil e Configurações carregaram; rolagem vertical observada | smoke parcial; retorno e acessibilidade tela a tela pendentes |
| Rotação física `1` / landscape solicitado | Home carregou em landscape | Champion permaneceu em portrait após solicitação de rotação `1` | limitação observada; orientação horizontal do Champion não foi alcançada |

Ao final, `font_scale=1.0`, modo noturno `yes`, `accelerometer_rotation=1` e
rotação do usuário `free` foram restaurados. Este smoke não aciona
sincronização, medição, importação, exclusão, reset ou qualquer ação de conta.

### Períodos por aplicativo

| Aplicativo | Superfície | Períodos/abas realmente observados |
|---|---|---|
| Fitbit | Passos, distância, calorias e peso | Dia, Semana, Mês, 3M e Ano; controles anterior, próximo e hoje quando disponíveis |
| Fitbit | Frequência cardíaca | Dia, Semana, Mês, 3M e Ano; estado com dados e janela sem dados |
| Fitbit | Exercícios | `Semana` e `Mês`; filtros Dias com exercícios, Duração e Distância |
| Champion | Frequência cardíaca | Hoje, últimos 7 dias e últimos 30 dias |
| Champion | Passos e distância | Últimos 7, 30 e 90 dias |
| Champion | Exercícios | sem abas Semana/Mês no APK observado; lista única e sete filtros de workflow |

O intervalo de períodos é paralelo por intenção, não por rótulo: a referência
expõe mais janelas que os contratos atuais do Champion. Nenhum período ausente
foi marcado como coberto.

## Comandos e evidências

Executados sem testes automatizados, conforme R9 e escopo da subtarefa:

```bash
adb devices -l
adb shell pm list packages com.fitbit.FitbitMobile
adb shell pm list packages br.com.mwconsulting.championcompanion.dev
adb shell cmd package resolve-activity --brief com.fitbit.FitbitMobile
adb shell dumpsys package com.fitbit.FitbitMobile
adb shell dumpsys package br.com.mwconsulting.championcompanion.dev
adb shell wm size
adb shell wm density
adb shell settings get system font_scale
adb shell cmd uimode night
adb shell getprop persist.sys.locale
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
adb shell monkey -p com.fitbit.FitbitMobile 1
adb shell input tap <x> <y>
adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>
adb shell input keyevent 4
adb shell uiautomator dump /sdcard/<capture>.xml
adb shell cat /sdcard/<capture>.xml
adb shell settings put system font_scale 1.3
adb shell cmd uimode night no
adb shell wm user-rotation lock 1
adb shell settings put system font_scale 1.0
adb shell cmd uimode night yes
adb shell settings put system accelerometer_rotation 1
adb shell wm user-rotation free
git diff --check
```

Screenshots e dumps foram capturados em diretório temporário fora do
repositório, revisados visualmente para localizar dados sensíveis e removidos
antes da persistência desta evidência. Não há screenshot bruto, EXIF, XML ou
payload de saúde versionado.

Na retomada de 2026-09-01, com ADB TLS novamente disponível, o Champion teve
todos os sete filtros de workflow selecionados individualmente: `ALL`,
`PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCED`,
`SYNC_FAILED` e `DISCARDED`. Os estados preenchidos e vazios renderizaram; a
seleção não alterou dados. Também foram selecionados `Hoje`, `7 dias` e `30
dias` na tendência cardíaca. Toque prolongado e arrasto horizontal no gráfico
foram tentados sem marker ou tooltip observável; não converter em aceite.

Na retomada adicional, tema claro percorreu Home, Saúde, Calendário, Exercícios,
Dispositivo, Perfil e Configurações, incluindo rolagem vertical em superfícies
longas. Fonte `1.3` carregou Home, Saúde, Calendário, Exercícios e o menu Mais;
os rótulos da navegação quebraram em linhas e parte do conteúdo ficou fora da
viewport. Ao solicitar rotação `1` com `accelerometer_rotation=0`, a referência
carregou em landscape, mas o Champion permaneceu em portrait; nenhum workaround
foi aplicado.

Como evidência exploratória de acessibilidade via `uiautomator`, a hierarquia
carregou em todas as superfícies. Contagem de nós clicáveis sem `text` ou
`content-desc` próprio: Home `6/8`, Saúde `8/8`, Calendário `35/37`, Exercícios
`6/6`, Mais `3/3`, Dispositivo `8/8`, Perfil `5/5` e Configurações `6/8`.
Esses containers podem possuir texto descendente, mas não foram tratados como
aceite de acessibilidade; TalkBack não foi habilitado nem validado.

## Resultado e risco residual

Baseline estrutural parcial para superfícies navegáveis permitidas pelos limites
da subtarefa no estado observado; R1-R3 estão documentados e o smoke físico de
R8 avançou, mas permanece parcial. A ausência de abas Semana/Mês no APK do
Champion continua registrada como `N/A` observado e impede aceite final desta
subtarefa.
Referência visual normativa permanece exclusivamente `com.fitbit.FitbitMobile`.
Não houve alteração em código, dados, integração, configuração de produção ou
contratos. Replicação deve usar esta matriz como paralelo; nenhuma ação de
produção foi executada nesta subtarefa.

Riscos residuais: variação de conteúdo por conta/versão/região; smoke de tema
claro e fonte ampliada exige revisão de cada tela, rolagem e acessibilidade;
landscape exige a mesma cobertura tela a tela; marker/tooltip e rolagem
horizontal não foram observáveis nesta retomada e exigem validação explícita
nas subtarefas que tocarão gráficos. Nenhum risco foi convertido em aceite
presumido.
