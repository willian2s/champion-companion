# 017 — Alinhamento visual de margens e cabeçalhos

- **Ticker:** `017`
- **Status:** concluída.
- **Tipo:** pacote visual independente

## Contexto

Calendário, detalhe diário, Dispositivo, Selecionar treino e Preparar já são
superfícies funcionais, mas não seguem integralmente a composição visual
predominante em Saúde, Exercícios e Configurações. Divergências concentram-se
na margem externa, no uso de tokens de espaçamento e na estrutura dos
cabeçalhos.

Fases 006 e 015 estão concluídas. A subtarefa `013-07` também está concluída,
enquanto `013-15` permanece pendente, possui escopo funcional muito mais amplo
e depende de `013-13` e `013-14`. Este pacote não reabre fases concluídas, não
substitui `013-15` e não altera dependências da Fase 13. Ele corrige somente a
apresentação das superfícies que já existem.

## Objetivo

Alinhar cinco superfícies ao padrão visual predominante do aplicativo:

- margem externa baseada em `ChampionSpacing.extraLarge`;
- espaçamento vertical baseado em `ChampionSpacing.large`;
- cabeçalho em `Card`, com título `headlineSmall`, peso seminegrito, texto de
  apoio e semântica de título;
- composição estável com fonte aumentada e em landscape.

Dados, contratos, estado, callbacks, navegação e comportamento permanecem
inalterados.

## Comportamento atual encontrado

- `CalendarScreen.kt` usa `ChampionSpacing.small` (`8 dp`) na margem horizontal
  e `ChampionSpacing.extraLarge` na vertical. Título e texto de apoio ficam
  soltos, fora de card.
- `CalendarDayScreen.kt` já usa margem externa de `24 dp`, mas `DayHeader`
  mantém título, data e ação de retorno em coluna textual sem card.
- `DeviceScreen.kt` usa margem externa e espaçamento vertical de `16.dp`, além
  de espaçamentos internos `8.dp`, `12.dp` e `16.dp`. Cabeçalho permanece
  textual e o título não explicita peso seminegrito nem semântica de heading.
- `WorkoutShells.kt` usa margem externa de `24 dp`, porém
  `WorkoutShellCard` posiciona `Voltar` antes do card e agrupa cabeçalho e todo o
  conteúdo no mesmo card. Seleção e preparação divergem da hierarquia de
  `ExercisesHeader` e das telas de detalhe.
- Padrão predominante real: margem externa `ChampionSpacing.extraLarge`
  (`24 dp`), ritmo vertical `ChampionSpacing.large` (`16 dp`), cabeçalho em
  `Card` com `surfaceContainerLowest`, conteúdo interno com
  `ChampionSpacing.large`, título `headlineSmall` seminegrito e texto de apoio
  `bodyMedium` em `onSurfaceVariant`.

## Requisitos

### R1 — Tokens oficiais

Espaçamentos de página e gaps de layout tocados pelo pacote devem usar
`ChampionSpacing`. Dimensões próprias de componentes, como largura de borda,
tamanho de marcador ou alvo de toque, não devem ser convertidas artificialmente
em tokens de spacing.

### R2 — Margem externa

As cinco superfícies devem usar a mesma margem externa das telas padrão:
`ChampionSpacing.extraLarge` em todos os lados, com itens principais separados
por `ChampionSpacing.large`.

### R3 — Cabeçalho padrão

Cada superfície deve iniciar sua hierarquia de conteúdo com cabeçalho visual
equivalente ao padrão existente:

- `Card` ocupando largura disponível;
- shape e cor de container coerentes com os headers existentes;
- padding interno `ChampionSpacing.large`;
- título em `MaterialTheme.typography.headlineSmall` e `FontWeight.SemiBold`;
- texto de apoio preservado em `bodyMedium` e `onSurfaceVariant`;
- título principal exposto uma única vez como heading na árvore semântica.

### R4 — Calendário e detalhe diário

Calendário deve adotar margem horizontal oficial sem quebrar grade mensal,
marcadores, seleção, navegação de mês ou ação de abrir dia. Detalhe diário deve
receber header em card, preservando data, retorno ao calendário, estados e
atalhos para detalhes relacionados.

### R5 — Dispositivo

Dispositivo deve adotar margem e cabeçalho padrão. Literais que representam
spacing no layout tocado devem ser substituídos pelo token equivalente, sem
alterar estados BLE, permissões, scan, conexão, associação, diálogos,
diagnóstico ou exportação.

### R6 — Selecionar treino e Preparar

Seleção e preparação devem separar cabeçalho de conteúdo e adotar hierarquia
compatível com `ExercisesHeader`. A ação `Voltar` continua disponível com o
mesmo callback e a preparação mantém tipo, countdown, GPS aplicável,
notificação, relógio opcional, mensagens e início exatamente como hoje.

Mudanças em shells de treino ativo, recuperação ou confirmação não fazem parte
deste pacote.

### R7 — Compatibilidade funcional

Não alterar `ViewModel`, estado, intents, modelos, repositories, persistência,
permissões, serviços, navegação, back stack, callbacks ou textos de requisito.
Mudança deve permanecer estritamente Compose/visual nas superfícies indicadas.

### R8 — Acessibilidade e adaptação

Preservar descrições existentes, estados semânticos, ordem de foco e alvos de
toque. Títulos devem ter semântica de heading sem anúncios duplicados. Layouts
não podem cortar, sobrepor nem tornar ações inacessíveis com fonte aumentada de
pelo menos `1,3x` ou em landscape representativo.

### R9 — Evidência visual

Cada superfície alterada exige screenshot. Cada subtarefa deve registrar também
capturas representativas de fonte aumentada e landscape, sem expor dados
pessoais, payload BLE, identificadores completos ou métricas sensíveis fora de
ambiente privado.

### R10 — Validação operacional

Após implementação de cada subtarefa, executar somente pelo wrapper:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar, alterar nem executar testes automatizados. Validar visualmente no app
instalado e registrar resultados, screenshots e eventuais bloqueios no arquivo
da subtarefa.

## Critérios de aceite

1. Cinco superfícies usam margem externa `ChampionSpacing.extraLarge` e ritmo
   vertical `ChampionSpacing.large`.
2. Cinco superfícies exibem header em card coerente com Saúde, Exercícios e
   Configurações.
3. Títulos usam `headlineSmall`, peso seminegrito e heading semântico único.
4. Calendário mantém sete colunas, marcadores, mês, seleção e abertura do dia
   sem corte após aumento da margem horizontal.
5. Detalhe diário mantém data, estados, conteúdo, retorno e navegação contextual.
6. Dispositivo mantém todos os estados e ações existentes, sem novo fluxo BLE.
7. Seleção e preparação mantêm tipos, callbacks, countdown, GPS, permissões,
   notificações e saída sem criar sessão antes do fluxo atual.
8. Fonte `1,3x` ou superior e landscape representativo não ocultam título,
   texto de apoio nem ação essencial.
9. Há screenshot de cada superfície alterada e evidência dos comandos exigidos.
10. Nenhum contrato, dado, requisito, teste automatizado ou fase concluída é
    alterado.

## Abordagem escolhida

Aplicar ajustes locais e coesos nos Composables existentes. Repetir a pequena
estrutura visual do header onde necessário, respeitando componentes e tokens já
presentes, sem criar abstração global prematura. Separar conteúdo do header nos
shells de seleção e preparação, mas limitar a mudança a essas duas entradas para
não afetar treino ativo e recuperação.

## Alternativas descartadas

- Reabrir Fases 006 ou 015: ambas estão concluídas e seus requisitos funcionais
  permanecem válidos.
- Absorver a correção em `013-15`: subtarefa possui integração ampla, está
  bloqueada por dependências funcionais e não é necessária para este ajuste.
- Criar componente global obrigatório de header: ampliaria escopo e acoplamento
  sem decisão arquitetural necessária.
- Normalizar todos os `dp` do aplicativo: refatoração ampla e sem relação com o
  pedido.
- Alterar navegação ou estado para acomodar layout: incompatível com pacote
  exclusivamente visual.

## Arquivos, módulos e contratos afetados

Módulo único `:app`; arquivos de produção prováveis:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`.

Símbolos de referência, sem previsão de edição:

- `ChampionSpacing`, `ChampionShapeLarge` e `MaterialTheme`;
- `ExercisesHeader`, `HealthHubHeader`, `SettingsHeaderCard` e
  `MetricDetailHeader`.

Contratos funcionais afetados: nenhum. Não há mudança prevista em Gradle,
resources, navegação, domínio, banco, DataStore, BLE, Health Connect, serviço ou
ADR.

## Riscos e mitigação

- **Grade mensal mais estreita:** validar sete colunas, textos e marcadores em
  portrait, fonte aumentada e landscape; não alterar semântica das células.
- **Textos longos no detalhe diário:** manter scroll e verificar data localizada
  e botões sem corte.
- **Muitos estados em Dispositivo:** validar ao menos estado disponível no
  aparelho e inspecionar que branches, intents e callbacks não mudaram.
- **Hotspot compartilhado em `WorkoutShells.kt`:** editar somente seleção,
  preparação e layout local necessário; comprovar ausência de diff funcional em
  treino ativo, recuperação e confirmação.
- **Ação Voltar duplicada ou perdida:** manter uma ordem de foco clara e os
  callbacks atuais; validar ida e volta manualmente.
- **Conflito com Fase 13 em andamento:** executar uma subtarefa por vez e não
  absorver alterações funcionais de `013-08` a `013-15`.

## Estratégia de validação

Validação é manual e visual, apoiada por lint e builds. Cada subtarefa deve:

1. revisar diff para confirmar escopo exclusivamente visual;
2. executar `git diff --check`;
3. executar os comandos Gradle definidos em R10;
4. navegar pela superfície e confirmar callbacks e estados existentes;
5. repetir smoke com fonte aumentada e landscape;
6. capturar e registrar screenshots sanitizados;
7. atualizar somente seu status, evidências e item correspondente no overview.

Nenhum teste automatizado será criado, alterado ou executado.

## Ordem das subtarefas

```text
017-01 → 017-02 → 017-03
```

Arquivos de produção são distintos, portanto não há dependência técnica entre as
três subtarefas. Ordem acima é operacional, favorece execução e aceite de uma
subtarefa por vez e reduz conflito documental no overview.

## Premissas

- `ChampionSpacing.extraLarge` continua valendo `24 dp` e
  `ChampionSpacing.large`, `16 dp`.
- Headers atuais de Saúde, Exercícios, Configurações e detalhes métricos
  continuam sendo referência visual no momento da implementação.
- Textos funcionais existentes permanecem, salvo ajuste estritamente necessário
  de composição sem mudança de significado.
- Dispositivo físico conectado será necessário para `installDebug` e evidências
  visuais; indisponibilidade deve ser registrada como bloqueio, não como sucesso.
- Nenhuma decisão arquitetural duradoura é introduzida; ADR não é necessária.

## Fora do escopo

- refatoração ampla do design system;
- componente universal de scaffold ou header;
- mudança de requisito, copy funcional, estado, fluxo ou navegação;
- correção funcional de Calendário, Dispositivo ou Fase 13;
- treino ativo, recuperação, confirmação de finalização ou novas telas;
- alteração de banco, contratos, dependências, manifesto ou resources;
- testes automatizados;
- reabertura ou fechamento das Fases 006, 013 ou 015.

## Rastreabilidade

| Subtarefa | Requisitos principais | Superfícies |
|---|---|---|
| `017-01` | R1, R2, R3, R4, R7, R8, R9, R10 | Calendário e detalhe diário |
| `017-02` | R1, R2, R3, R5, R7, R8, R9, R10 | Dispositivo |
| `017-03` | R1, R2, R3, R6, R7, R8, R9, R10 | Selecionar treino e Preparar |
