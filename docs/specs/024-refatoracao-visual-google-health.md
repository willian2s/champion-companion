# 024 — Refatoração visual inspirada no Google Health

- **Ticker:** `024`
- **Status:** in_progress
- **Tipo:** refatoração visual incremental

## Contexto e objetivo

O Champion Companion já possui design system Material 3, temas claro e escuro,
dynamic color, navegação funcional e treze superfícies vigentes. A interface,
porém, mantém densidade desigual, repetição de cards, cabeçalhos locais,
metadados técnicos em excesso e padrões de status não uniformes.

Objetivo: refatorar a experiência tela por tela, usando como guia de estilo e
design o Google Health atual, construído sobre o Fitbit e distribuído no pacote
Android `com.fitbit.FitbitMobile`. A entrega deve adaptar hierarquia editorial,
resumos mais claros, métricas prioritárias, superfícies tonais e listas
compactas sem copiar marca, ativos ou funcionalidades Google/Fitbit.

Refatoração permanece visual e de usabilidade. Dados, domínio, sincronização,
BLE, persistência, navegação e fluxos vigentes não mudam.

## Fontes de referência

- https://healthapp.google/;
- https://healthapp.google/your-health-connected;
- https://healthapp.google/privacy;
- https://play.google.com/store/apps/details?id=com.fitbit.FitbitMobile;
- instância do Google Health instalada no aparelho autorizado pelo usuário,
  inspecionada somente quanto à estrutura visual e sem registrar dados pessoais;
- Material 3 já integrado ao projeto.

Google Fit (`com.google.android.apps.fitness`) e Health Connect
(`com.google.android.apps.healthdata`) não são a referência visual desta spec.
Health Connect continua apenas integração write-only já existente.

## Requisitos

### R1 — Google Health como guia, com identidade própria

- Usar Google Health/Fitbit atual como guia de estilo e design para hierarquia,
  agrupamento, densidade, cards, listas, gráficos, superfícies, ritmo visual e
  navegação calma.
- Adaptar os padrões ao conteúdo e aos contratos do Champion Companion, sem
  exigir reprodução literal ou paridade entre produtos.
- Preservar nome, ícones, conteúdo, paleta fallback azul-petróleo, dynamic color
  e tipografia do sistema do Champion Companion.
- Não copiar logo, Google Sans, quatro cores Google, screenshots, ilustrações,
  ativos, textos, Coach, IA, premium ou layout pixel a pixel.

### R2 — Fundação visual aditiva

- Evoluir tokens e componentes somente quando necessários às telas planejadas.
- Novos componentes devem ser stateless, baseados em slots, textos e callbacks,
  sem `ViewModel`, DAO, repository, BLE ou Health Connect.
- Não alterar globalmente tema, shapes ou tipografia de modo que telas ainda não
  migradas sejam redesenhadas implicitamente.
- Promover abstração compartilhada somente após uso real em pelo menos duas
  superfícies, salvo elementos invariantes do shell.
- A direção cromática pode abranger o app inteiro como resultado final, mas sua
  adoção deve ser opt-in por superfície; não alterar tema raiz para redesenhar
  consumidores ainda não migrados.

### R3 — Navegação preservada

- Manter Navigation 3, back stack manual, `AppDestination`, save tokens,
  animações, restauração, destinos e menu `Mais`.
- Preservar barra inferior, seleção de subárvore, retorno contextual e diálogos
  de saída.
- Refatoração do shell altera apresentação, insets e acessibilidade, não
  arquitetura nem informação disponível.

### R4 — Hierarquia comum

Cada tela migrada deve oferecer:

- título principal único e semanticamente marcado;
- resumo ou valor prioritário antes de detalhes técnicos;
- seções nomeadas e separadas por ritmo visual consistente;
- ações primárias distinguíveis de ações secundárias e destrutivas;
- origem, unidade, período e estado apresentados quando relevantes;
- estados vazio, loading, erro e pendência coerentes com o design system.
- A variedade cromática deve usar roles e containers Material 3 por intenção,
  preservando contraste, identidade Champion e sinais textuais/icônicos além da
  cor.

### R5 — Usabilidade e acessibilidade

- Preservar ordem de foco, descrições e significado dos controles.
- Alvos acionáveis devem manter pelo menos `48 dp`.
- Cor não pode ser sinal único.
- Fonte `1,3x`, landscape representativo, temas claro/escuro e conteúdo longo não
  podem ocultar ação essencial, cortar texto ou causar sobreposição.
- Gráficos preservam descrição acessível, período, unidade, lacunas e estado
  vazio sem dados sintéticos.

### R6 — Compatibilidade funcional

- Preservar estados, intents, callbacks, lifecycle, repositories, confirmação
  de saída e significado de todos os textos de segurança.
- Home continua refresh explícito; Saúde hub não inicia BLE; frequência cardíaca
  mantém medição e D1 explícitos; Exercícios mantém importação e sync explícitos;
  Calendário continua somente leitura; Configurações mantém Health Connect
  write-only.
- Nenhuma alteração de Room, schema, DataStore, BLE, Health Connect, permissões,
  serviços, manifesto ou dependências é esperada.

### R7 — Entrega tela por tela

- Fundação, shell e tela-piloto antecedem demais superfícies.
- Cada subtarefa de tela deve ser revisável e reversível isoladamente.
- Saúde hub é piloto; demais telas só avançam após aceite visual registrado.
- Inconsistência temporária entre telas antigas e migradas é aceita durante o
  rollout.

### R8 — Validação física por ADB

- Não criar, alterar nem executar testes automatizados.
- Para cada subtarefa com código: executar lint, builds debug/release,
  instalação debug e smoke de usabilidade pelo ADB.
- Validar abertura, rolagem, retorno, barra inferior, ações não destrutivas,
  portrait, fonte `1,3x`, landscape e temas representativos.
- Registrar screenshot sanitizado da superfície alterada; estados indisponíveis
  ficam `N/A`, nunca simulados como sucesso.
- Configurações temporárias do aparelho devem ser registradas e restauradas.

### R9 — Privacidade da evidência

- Não persistir dumps ou screenshots contendo métricas pessoais, títulos livres,
  identificadores completos, dados de conta ou payload BLE.
- Não disparar importação, sincronização, exclusão ou medição real apenas para
  gerar evidência visual.
- Hierarquias ADB podem registrar somente labels genéricos, bounds e estrutura
  necessários à análise.

### R10 — Legado protegido

- `WorkoutTypeSelectionShell`, `WorkoutPreparationShell`, `ActiveWorkoutShell`,
  recuperação phone, GPS, localização, rota, mapa e PoC debug ficam fora.
- Código histórico permanece preservado e bloqueado por `PhoneWorkoutPolicy`.

## Critérios de aceite

1. Google Health/Fitbit atual está documentado como referência correta; Google
   Fit e Health Connect não são tratados como referência visual.
2. Baseline sanitizado compara hierarquia, cards, listas, gráficos, navegação e
   estados entre referência e app atual sem expor dados pessoais.
3. Fundação visual evolui aditivamente e não redesenha telas não migradas por
   efeito colateral.
4. Shell mantém destinos, back stack, restauração, menu `Mais`, seleção de
   subárvore e confirmações de saída.
5. Cada uma das treze superfícies vigentes possui subtarefa própria e aceite
   visual registrado.
6. Hub Saúde valida a linguagem visual como piloto antes da continuação.
7. Todas as telas priorizam resumo e ação útil antes de metadados técnicos,
   preservando origem, unidade e estados necessários.
8. Fonte ampliada, landscape e temas representativos não cortam conteúdo nem
   tornam ações essenciais inacessíveis.
9. Callbacks, intents, lifecycle, BLE, sincronização, persistência e dados
   permanecem funcionalmente equivalentes.
10. Nenhum teste automatizado é criado, alterado ou executado.
11. Cada subtarefa com código registra `git diff --check`,
    `./gradlew lint assembleDebug assembleRelease`, `./gradlew installDebug` e
    smoke ADB sanitizado.
12. Nenhum ativo, fonte, marca, texto ou funcionalidade proprietária do Google
    Health/Fitbit é copiado.
13. Nenhum fluxo phone, GPS, localização, rota ou mapa é reativado ou expandido.
14. Consolidação final remove somente legado visual comprovadamente sem uso e
    repete smoke integrado das superfícies migradas.

## Comportamento atual encontrado

- `ChampionCompanionTheme` usa Material 3, fonte do sistema, dynamic color por
  padrão e paletas próprias claro/escuro como fallback.
- `ChampionSpacing`, shapes e elevação já existem; estados semânticos usam
  tokens fixos claro/escuro.
- `HealthCard`, `MetricCard`, `HealthChart`, feedbacks, banners e botões são
  stateless, mas telas também mantêm estruturas locais duplicadas.
- Padrão predominante atual: margem de `24 dp`, ritmo de `16 dp`, header em card
  e várias superfícies empilhadas com peso visual semelhante.
- Home possui resumo diário, refresh, progresso, status do relógio, BPM e
  pendências; mantém algumas cores locais.
- Saúde é hub de frequência cardíaca, peso, passos e distância. Detalhes de
  métricas seguem resumo, gráfico e histórico.
- Calendário apresenta grade mensal e detalhe diário com navegação cruzada.
- Exercícios reúne importação, filtros, lista e detalhe complexo no mesmo
  arquivo e ViewModel compartilhado.
- Dispositivo concentra permissões, scan, associação, conexão, erro e
  diagnóstico.
- Perfil reúne altura, meta, comprimento do passo e calibrações contextuais.
- Configurações expõe Health Connect write-only, permissões e fila local.
- `ChampionNavigation` concentra back stack, restauração, animação, guards e
  bloqueio de treino phone; não usa `NavHost` nem `NavController`.
- Testes existentes cobrem partes do design system e contratos, mas, conforme
  pedido e regras atuais, não farão parte desta entrega.

## Abordagem escolhida

Rollout: baseline físico sanitizado → fundação aditiva → shell compatível →
estratégia cromática transversal → hub Saúde piloto → demais telas uma por vez →
consolidação. Componentes antigos
permanecem durante a migração. Cada tela recebe validação e aceite antes da
próxima; rollback ocorre pela própria subtarefa.

Decisão duradoura foi registrada durante `024-01` no próximo ADR disponível:
`docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`.

## Alternativas descartadas

- Refatoração big-bang: revisão e rollback inseguros.
- Copiar visual Google Health integralmente: viola identidade e inclui escopo
  inexistente.
- Migrar navegação para `NavController`: não há necessidade funcional.
- Reorganizar app nas áreas do Google Health: quebraria arquitetura e conteúdo.
- Mudar somente paleta: não resolve hierarquia, densidade ou adaptação.
- Criar catálogo completo de componentes antes do piloto: abstração prematura.
- Reativar telas phone para uniformidade: direção de produto proíbe esse fluxo.

## Arquivos, módulos e contratos afetados

Módulo único `:app`. Arquivos prováveis:

- `core/designsystem/ChampionTokens.kt`;
- `core/designsystem/ChampionTheme.kt` e `ChampionColorScheme.kt`, somente se a
  adoção aditiva exigir ajuste sem impacto implícito;
- `core/designsystem/ChampionDisplayComponents.kt`;
- `core/designsystem/ChampionFeedbackComponents.kt`;
- `core/designsystem/ChampionActionAndStatusComponents.kt`;
- possível novo arquivo de componentes visuais no mesmo pacote;
- `core/navigation/ChampionNavigation.kt`, somente apresentação do shell;
- `feature/home/HomeScreen.kt`;
- `feature/health/HealthScreen.kt`;
- `feature/health/HealthMetricDetailScreens.kt`;
- charts específicos de Saúde quando a tela correspondente exigir;
- `feature/calendar/CalendarScreen.kt`;
- `feature/calendar/CalendarDayScreen.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- `feature/device/DeviceScreen.kt`;
- `feature/profile/ProfileScreen.kt`;
- `feature/settings/SettingsScreen.kt`.

Contratos preservados: `AppDestination`, back stack e saver, intents e estados
de ViewModels, repositories, modelos, Room v13, DataStore, BLE, fila Health
Connect, callbacks e `PhoneWorkoutPolicy`.

## Dependências e migrações

- Nenhuma dependência Gradle nova esperada; Material 3 e ícones já existem.
- Nenhuma migration Room, alteração de schema ou conversão de dados.
- Nenhuma migration de navegação ou preferência.
- ADR-005, ADR-009 e contratos das specs funcionais permanecem autoritativos.

## Riscos e mitigação

- **Referência mutável:** registrar versão/data e baseline; tratar imagens
  públicas como referência, não contrato.
- **Dados pessoais no aparelho:** sanitizar evidências e não retornar valores.
- **ADB indisponível:** subtarefa permanece bloqueada; não substituir por aceite
  presumido.
- **ADR registrado:** `024-01` registra decisão antes de código de produção em
  ADR-023; alterações futuras devem preservar essa decisão ou atualizar ADR.
- **Big-bang oculto por tema:** usar componentes opt-in e revisar consumidores.
- **Abstração prematura:** exigir dois consumidores reais antes da promoção.
- **Hotspots grandes:** limitar alterações ao ramo visual da superfície e
  revisar ausência de diff funcional.
- **Fonte ampliada/landscape:** validar em cada tela e restaurar ajustes do
  aparelho.
- **Estados raros BLE/sync:** marcar `N/A`; inspecionar branches sem fabricar
  eventos.
- **Dynamic color:** validar cenário dinâmico e fallback, mantendo contraste e
  sinais textuais.
- **Cópia de marca:** usar apenas componentes Material e ativos Champion.

## Estratégia de testes e validação

Não criar, alterar ou executar testes automatizados.

Validação de cada subtarefa com código:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: abrir superfície, rolar, navegar para detalhe quando aplicável, voltar,
trocar área, confirmar ações não destrutivas, testar portrait, fonte `1,3x`,
landscape e tema previsto pela subtarefa. Capturas ficam sanitizadas na pasta da
task. Configuração original de orientação, fonte e tema deve ser anotada e
restaurada.

Validação estrutural do Google Health usa pacote `com.fitbit.FitbitMobile` e se
limita a labels genéricos, hierarquia, bounds e padrões. Não exportar dados, não
ler banco, não coletar Logcat amplo e não registrar métricas.

## Ordem das subtarefas

```text
024-01 → 024-02 → 024-03 → 024-04 (estratégia cromática)
       → 024-05 (piloto/aceite) → 024-06 → 024-07 → 024-08 → 024-09
       → 024-10 → 024-11 → 024-12 → 024-13 → 024-14
       → 024-15 → 024-16 → 024-17 → 024-18
```

## Premissas explícitas

- Google Health instalado no aparelho é evolução do Fitbit e usa
  `com.fitbit.FitbitMobile`.
- Usuário autoriza inspeção estrutural pelo ADB, sem autorização para persistir
  ou divulgar dados pessoais.
- Dispositivo alvo continua Galaxy S22 com variante debug instalada em paralelo.
- Google Health serve como guia de estilo e design; não existe meta de cópia ou
  paridade funcional.
- Estrutura funcional e informação disponível nas telas atuais são adequadas.
- Dynamic color permanece habilitado por padrão e paleta própria continua
  fallback.
- Ausência de cobertura automatizada nova é decisão explícita; lint, builds e
  validação física continuam obrigatórios.
- Se o executor não receber permissão operacional para `adb`, subtarefas com
  aceite físico ficam bloqueadas.

## Fora do escopo

- Coach, Gemini, insights médicos, sono, nutrição, ciclo, prontuário ou premium;
- login, conta Google/Fitbit, backend, analytics ou rede nova;
- leitura do Health Connect;
- novas métricas, metas, fontes ou cálculos;
- reestruturação de navegação ou módulos;
- GPS, localização, rota, mapa ou treino iniciado pelo celular;
- telas técnicas de PoC debug;
- testes automatizados;
- cópia de marca ou ativos Google/Fitbit.

## Rastreabilidade

| Subtarefa | Requisitos | Superfície |
|---|---|---|
| `024-01` | R1, R7, R8, R9 | Baseline e decisão visual |
| `024-02` | R1, R2, R4, R5 | Fundação visual |
| `024-03` | R2, R3, R5, R6 | Shell e navegação |
| `024-04` | R1, R2, R4, R5, R7–R9 | Estratégia cromática transversal |
| `024-05` | R1, R4–R8 | Saúde hub piloto |
| `024-06` | R4–R9 | Home |
| `024-07` | R4–R9 | Frequência cardíaca |
| `024-08` | R4–R9 | Peso |
| `024-09` | R4–R9 | Passos |
| `024-10` | R4–R9 | Distância |
| `024-11` | R4–R9 | Calendário |
| `024-12` | R4–R9 | Detalhe diário |
| `024-13` | R4–R10 | Lista de Exercícios |
| `024-14` | R4–R10 | Detalhe de exercício |
| `024-15` | R4–R9 | Dispositivo |
| `024-16` | R4–R9 | Perfil |
| `024-17` | R4–R9 | Configurações |
| `024-18` | R1–R10 | Consolidação e smoke final |
