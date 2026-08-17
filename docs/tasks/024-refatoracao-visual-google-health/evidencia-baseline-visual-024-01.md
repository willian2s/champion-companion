# Evidência 024-01 — Baseline visual sanitizado
- **Data da inspeção:** 2026-08-17
- **Aparelho:** Galaxy S22 `SM-S901E`, rotação portrait, `1080x2340`, density
  `480`, escala de fonte `1.0`, tema escuro

## Referência confirmada

| Item | Resultado |
|---|---|
| Pacote | `com.fitbit.FitbitMobile` |
| Versão | `5.05.health-mobile-1104094190-958410393` |
| Version code | `1104094190` |
| Activity resolvida | `com.fitbit.HealthBrandedAlias` |
| Aplicativo Champion | `br.com.mwconsulting.championcompanion.dev` |
| Versão Champion | `1.0-dev` |
| Data/hora | 2026-08-17; horário do aparelho não persistido |

Google Fit (`com.google.android.apps.fitness`) e Health Connect
(`com.google.android.apps.healthdata`) não foram usados como referência visual.
Health Connect permanece somente integração write-only existente.

## Limites e privacidade

- A inspeção usou navegação manual, screenshot temporário e hierarquia
  `uiautomator` temporária.
- Dumps foram removidos do aparelho após leitura.
- Screenshots originais não entram no repositório.
- Nenhum valor de métrica, título livre, identificador, conta ou payload BLE foi
  registrado neste documento.
- Não houve login, troca de conta, importação, sincronização, exclusão ou
  medição real.
- As imagens versionadas são reconstruções vetoriais sanitizadas da estrutura
  observada; não reproduzem ativos, marca ou screenshot integral da referência.

## Observação estrutural da referência

Inspeção física alcançou tela principal do Google Health/Fitbit, sem login ou
mudança de conta. A hierarquia observada tinha:

- cabeçalho com marca/título e ação de dispositivos e apps;
- resumo prioritário em grade de cards, com um card principal e cards
  secundários;
- labels genéricos de passos, dias com exercícios, distância e calorias;
- ações separadas para registrar e iniciar;
- ação para personalizar métricas de foco;
- navegação inferior com Hoje e Saúde;
- composição em blocos tonais, cantos amplos e baixa quantidade de metadados
  técnicos na primeira dobra.

Gráficos, estados de erro, permissões e superfícies de privacidade da referência
ficaram `N/A`: não foram abertos para evitar login, sincronização ou exposição de
dados. A comparação desses grupos usa `N/A` explícito para a referência e a
estrutura observada do Champion, sem afirmar paridade visual.

## Baseline das treze superfícies Champion

| Superfície | Composição observada | Ações/estados preservados | Evidência |
|---|---|---|---|
| Saúde hub | Header editorial, quatro cards de métrica e status secundário de Health Connect | Cards abrem frequência cardíaca, peso, passos e distância; hub não inicia BLE | ADB + `HealthScreen.kt` + painel 01 |
| Home | Header com relógio/perfil, resumo diário prioritário, pendência, BPM e cards de distância/calorias | Pull-to-refresh explícito, estado do relógio e banner de revisão | ADB + `HomeScreen.kt` + painel 02 |
| Frequência cardíaca | Intro, resumo atual, medição manual, importação D1, gráfico e histórico | Medição e D1 explícitos; confirmação de saída durante operação | ADB + `HealthMetricDetailScreens.kt` + painel 03 |
| Peso | Intro, resumo atual, editor de quilogramas e histórico local | Adicionar medição, mensagens de sucesso/erro e origem manual | ADB + `HealthMetricDetailScreens.kt` + painel 04 |
| Passos | Resumo diário com meta/progresso, seletor `7D/30D/90D`, resumo, gráfico e lista | Lacunas preservadas; sem BLE ou sync ao abrir | ADB + `StepsHistoryShell.kt` + painel 05 |
| Distância | Resumo atual estimado, seletor de período, agregados, gráfico e lista | Ausência segura; estimativa local identificada | ADB + `DistanceHistoryShell.kt` + painel 06 |
| Calendário | Header, navegação mensal, grade, marcadores, legenda e ação do dia selecionado | Leitura local, navegação mensal e abertura contextual do dia | ADB + `CalendarScreen.kt` + painel 07 |
| Detalhe diário | Cards de passos, distância, BPM, atividades, peso e fila local | Links contextuais; somente leitura; sem sync iniciado | `CalendarDayScreen.kt` + painel 08; smoke do botão `N/A` |
| Lista de Exercícios | CTA de importação, feedback de workflow, filtros, resumo Health Connect e lista | Importação e sync explícitos; cancelamento/retry preservados | ADB + `ExercisesScreen.kt` + painel 09 |
| Detalhe de exercício | Cabeçalho, status canônico, métricas, gráficos, intervalos, splits e revisão | Calibração, descarte, exclusão e sync com confirmação | ADB + `ExercisesScreen.kt` + painel 10 |
| Dispositivo | Resumo de conexão, dispositivo associado, ações, scan e diagnóstico | Conectar, desconectar, esquecer e exportação sanitizada | ADB + `DeviceScreen.kt` + painel 11 |
| Perfil | Dados usados nos cálculos, altura, meta, passo base, perfis aprendidos e calibrações | Edição explícita; snapshots históricos preservados | ADB + `ProfileScreen.kt` + painel 12 |
| Configurações | Header Health Connect, disponibilidade, integração, permissões e fila | Write-only, sincronizar agora e gerenciar acesso explícitos | ADB + `SettingsScreen.kt` + painel 13 |

A captura compósita `captura-sanitizada-superficies-024-01.svg` fornece
referência estrutural visual das treze superfícies. Não contém valores reais e
não substitui smoke funcional das subtarefas de código.

### Estados físicos não alcançados

| Estado | Resultado | Motivo |
|---|---|---|
| Google Health sem dados | `N/A` | Instância autorizada já possuía conteúdo; não foi permitido apagar ou trocar conta |
| Erro/loading de cada métrica | `N/A` | Não fabricar falha nem disparar sincronização para produzir evidência |
| Permissões Health Connect | `N/A` | Inspeção visual não exigia abrir gerenciamento remoto |
| Falha/retry BLE | `N/A` | Não alterar conexão nem executar operação destrutiva |
| Detalhe diário aberto pelo CTA | `N/A` | CTA não respondeu ao smoke coordenado; estrutura permanece coberta por código |
| Landscape e fonte `1,3x` | `N/A` | Esta subtarefa registra baseline inicial; validação por tela pertence às subtarefas de código |

## Matriz visual de adaptação

| Princípio de referência ou observado | Aplicação possível | Adaptação Champion | Elemento proibido de copiar |
|---|---|---|---|
| Resumo prioritário antes de detalhes | Valor e estado na primeira dobra | Header editorial seguido de resumo atual ou diário | Texto, ícone ou composição proprietária |
| Cards tonais com papéis distintos | Separar prioridade, apoio e feedback | Usar tokens Champion e superfícies opt-in | Quatro cores Google, ilustrações ou assets Fitbit |
| Grade compacta de métricas | Comparar métricas sem empilhar explicações | Cards de Saúde e Home mantendo dados atuais | Nomes, métricas e áreas inexistentes no Champion |
| Ações curtas e explícitas | Ação principal em posição previsível | Botões existentes; importação, medição e sync continuam explícitos | Coach, IA, premium ou CTA de conta |
| Navegação calma e persistente | Retorno previsível entre áreas | Preservar Navigation 3, back stack manual e cinco entradas | Reorganização em áreas Google ou `NavController` |
| Listas com densidade controlada | Mostrar status antes de metadados | Compactar listas somente dentro da tela migrada | Layout pixel a pixel ou textos da referência |
| Gráficos como seção secundária (`N/A` físico na referência) | Contextualizar período e unidade | Reusar gráficos atuais, lacunas e descrição acessível | Estilo, escala ou dados fabricados da referência |
| Feedback textual e semântico (`N/A` físico na referência) | Não depender somente de cor | Manter badges, mensagens e estados Champion | Paleta semântica copiada ou sinal visual sem texto |
| Privacidade discreta (limite documental, sem inspeção de conta) | Explicar origem e limite sem expor dados | Manter local-first, write-only e estados seguros | Conta Google/Fitbit, consentimento, analytics ou rede nova |

## Direção aprovada para implementação

1. Evoluir fundação somente com componentes opt-in e stateless.
2. Manter tema, paleta fallback, dynamic color e tipografia Champion.
3. Preservar shell, destinos, back stack, save tokens, guards e callbacks.
4. Migrar fundação → shell → Saúde hub piloto → uma tela por vez.
5. Exigir aceite visual isolado antes de avançar; rollback ocorre revertendo a
   subtarefa da tela.
6. Não alterar domínio, persistência, BLE, Health Connect, permissões,
   manifesto, dependências ou fluxos phone.

## Arquivos alterados

- `evidencia-baseline-visual-024-01.md`
- `024-01-captura-sanitizada-referencia.svg`
- `024-01-captura-sanitizada-champion.svg`
- `captura-sanitizada-superficies-024-01.svg`
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`
- `024-00-overview.md`
- `024-01-registrar-baseline-e-direcao-visual.md`

## Decisões, desvios e riscos residuais

- **Decisão:** ADR-023 registra rollout aditivo, shell preservado e piloto de
  Saúde.
- **Desvio:** screenshots integrais não foram persistidos; evidência visual foi
  reconstruída em SVG sanitizado para impedir vazamento de dados.
- **Desvio:** detalhe diário ficou `N/A` no smoke físico porque o CTA não
  respondeu; não foi repetida ação potencialmente ambígua nem fabricado sucesso.
- **Risco:** Google Health varia por versão, região, conta e estado de dados;
  versão e data desta inspeção ficam congeladas como contexto, não como contrato.
- **Risco:** landscape, fonte ampliada e estados raros exigem validação nas
  subtarefas correspondentes.

## Reexecução solicitada

Em 2026-08-17, a evidência foi revalidada após alteração deste arquivo. A
reexecução confirmou o mesmo aparelho, pacotes, versões, activity resolvida e
configuração visual registrada acima. A inspeção estrutural continuou limitada a
contagens, classes, bounds e flags genéricas; nenhum label ou valor pessoal foi
persistido.

| Verificação | Resultado |
|---|---|
| `adb devices -l` | Passou; Galaxy S22 `SM-S901E` autorizado |
| `com.fitbit.FitbitMobile` | Presente; `5.05.health-mobile-1104094190-958410393`; version code `1104094190` |
| Activity resolvida do Fitbit | `com.fitbit.HealthBrandedAlias` |
| `br.com.mwconsulting.championcompanion.dev` | Presente; `1.0-dev`; MainActivity resolvida |
| Configuração física | `1080x2340`, density `480`, fonte `1.0`, portrait, tema escuro |
| Hierarquia Fitbit | 67 nós, 9 clicáveis, 0 roláveis, 41 bounds distintos, 7 classes |
| Hierarquia Champion | 77 nós, 8 clicáveis, 1 rolável, 57 bounds distintos, 7 classes |
| `git diff --check` | Passou; sem erro |

Os dumps `uiautomator` foram temporários e removidos do aparelho após a leitura.
Nenhum screenshot bruto foi persistido; as capturas vetoriais sanitizadas
existentes continuam sendo a evidência versionada.
