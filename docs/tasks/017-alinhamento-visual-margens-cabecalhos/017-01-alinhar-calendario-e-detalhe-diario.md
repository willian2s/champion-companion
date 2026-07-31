# 017-01 — Alinhar Calendário e detalhe diário

- **Ticker:** `017`
- **Número:** `01`
- **Status:** concluído

## Objetivo e resultado esperado

Alinhar margem e cabeçalho de Calendário e detalhe diário ao padrão predominante
do app. Resultado esperado: duas superfícies com margem externa de `24 dp`,
ritmo vertical de `16 dp` e header em card, sem regressão na grade, no conteúdo
diário ou na navegação.

## Requisitos e critérios cobertos

- R1, R2, R3, R4, R7, R8, R9 e R10 da spec 017.
- Critérios de aceite 1 a 5, 8, 9 e 10.

## Escopo incluído

- margem externa de `CalendarScreen`;
- header de Calendário com título e texto de apoio existentes;
- header de detalhe diário com título, data e ação contextual existente;
- tokens de spacing diretamente relacionados aos dois headers e shells;
- heading semântico único por superfície;
- adaptação da grade e dos headers à largura útil, fonte aumentada e landscape;
- screenshots e registro de validação desta subtarefa.

## Escopo excluído

- regras do calendário, read model, ViewModel, datas, marcadores e estados;
- alteração de textos de requisito, navegação mensal ou abertura do dia;
- mudança no back stack ou nos destinos relacionados;
- redesign dos cards de passos, distância, BPM, atividades, peso ou sync;
- banco, repository, Health Connect, BLE, Fase 13 e testes automatizados.

## Dependências

- Spec e overview 017 aprovados.
- Nenhuma dependência em outra subtarefa 017.
- Primeira subtarefa elegível do pacote.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt`:
  `CalendarScreen` e novo ou ajustado header local.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`:
  `CalendarDayScreen` e `DayHeader`.
- Referências sem edição prevista: `ChampionSpacing`, `ChampionShapeLarge`,
  `HealthHubHeader`, `ExercisesHeader` e `MetricDetailHeader`.
- Este arquivo e `017-00-overview.md` para status e evidências.

## Passos de implementação

1. Revalidar padrão dos headers de Saúde, Exercícios, Configurações e detalhes
   métricos no código vigente antes da edição.
2. Trocar margem de página de Calendário por
   `ChampionSpacing.extraLarge` em todos os lados e manter
   `ChampionSpacing.large` entre blocos principais.
3. Encapsular título `Calendário` e texto de apoio em card de header coerente
   com padrão real, preservando copy e adicionando heading semântico único.
4. Transformar `DayHeader` em header card equivalente, preservando data
   localizada e callback de retorno; manter ação final de retorno e ordem de
   foco compreensível, sem duplicar anúncio de heading.
5. Não alterar `MonthHeader`, seleção, marcadores, intents, estados nem conteúdo
   dos cards além do mínimo exigido para acomodar largura útil.
6. Inspecionar diff para confirmar ausência de mudança funcional.
7. Validar Calendário e detalhe diário em portrait, fonte de pelo menos `1,3x`
   e landscape, incluindo ida ao dia e retorno.
8. Registrar comandos, resultados, screenshots, riscos residuais e status;
   marcar somente `017-01` no overview após conclusão real.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

Executar após implementação:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke manual obrigatório:

- abrir Calendário e navegar entre meses;
- confirmar grade com sete colunas, Hoje, seleção, marcadores e abrir dia;
- confirmar detalhe com data longa, conteúdo, detalhes relacionados e retorno;
- repetir com fonte aumentada e landscape representativo;
- confirmar ausência de novo BLE, Health Connect, permissão ou escrita.

## Evidências esperadas

- screenshot portrait de Calendário;
- screenshot portrait de detalhe diário;
- screenshot com fonte aumentada cobrindo ambas as superfícies ou uma por
  superfície quando necessário para demonstrar ausência de corte;
- screenshot landscape representativo das duas superfícies;
- resultados dos comandos e roteiro manual registrados neste arquivo.

## Definição de pronto

- Duas superfícies atendem margem, ritmo, card, tipografia e semântica da spec.
- Grade e detalhe permanecem funcionais e acessíveis.
- Lint, builds e instalação passam.
- Screenshots sanitizados e validação manual estão registrados.
- Nenhum teste automatizado é tocado ou executado.
- Status muda para concluído e somente item `017-01` é marcado no overview.

## Riscos e cuidados

- A margem horizontal maior reduz largura da grade; não resolver com scroll
  horizontal, largura fixa ou remoção de semântica.
- Data por extenso pode ocupar múltiplas linhas; permitir wrap sem esconder ação.
- Não confundir `MonthHeader` com header principal da tela.
- Preservar duas rotas de retorno existentes e evitar callbacks novos ou
  navegação para pai em vez de pop.
- Não alterar trabalho válido das Fases 015 ou 013.

## Registro de execução

### Status

Concluído. Subtarefa limitada às duas superfícies de Calendário; nenhuma
subtarefa seguinte foi iniciada.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`;
- capturas sanitizadas em `docs/tasks/017-alinhamento-visual-margens-cabecalhos/`:
  `017-01-calendario-portrait.png`, `017-01-detalhe-portrait.png`,
  `017-01-calendario-fonte-1-3x.png`, `017-01-detalhe-fonte-1-3x.png`,
  `017-01-calendario-landscape-fonte-1-3x.png`,
  `017-01-detalhe-landscape-fonte-1-3x.png`,
  `017-01-calendario-landscape-grade-fonte-1-3x.png` e
  `017-01-calendario-landscape-grade-final-fonte-1-3x.png`.

### Decisões e desvios

- Criados headers locais `CalendarHeader` e `DayHeader`, repetindo o padrão
  vigente de `Card`, `ChampionShapeLarge`, `surfaceContainerLowest`,
  `ChampionSpacing.large`, `headlineSmall`, peso seminegrito e apoio em
  `bodyMedium`.
- `CalendarScreen` passou a usar `ChampionSpacing.extraLarge` nos quatro lados;
  o ritmo entre blocos permaneceu em `ChampionSpacing.large`.
- `DayHeader` preservou data localizada, ação contextual e botão final de
  retorno no fluxo válido. Estado de data inválida também recebe header com um
  único heading, mantém somente o botão final de retorno existente e evita ação
  contextual duplicada, sem alterar erro, callback ou navegação.
- `MonthHeader`, grade, seleção, marcadores, intents, ViewModel, estados,
  conteúdo dos cards e destinos não foram alterados.
- Não houve desvio funcional. Não foram criados, alterados ou executados testes
  automatizados.

### Comandos executados e resultados

- `git diff --check` — passou antes e após a correção final.
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL` após a
  correção final; relatório lint sem erros.
- `./gradlew installDebug` — `BUILD SUCCESSFUL` após a correção final; instalado
  em `SM-S901E - 16`.
- `adb devices` — dispositivo físico disponível para validação visual.

### Evidências visuais e smoke manual

- Portrait: header de Calendário em card, grade com sete colunas, navegação
  mensal, Hoje, seleção e marcadores; header de detalhe com data extensa e
  ação `Voltar para calendário`.
- Fluxo: meses anterior/próximo e Hoje foram acionados; data com marcadores foi
  selecionada, `Abrir dia` abriu detalhe diário populado e ação de retorno levou
  de volta ao Calendário.
- Fonte aumentada: `font_scale=1.3`; títulos, datas, headers, grade e ações
  permaneceram visíveis sem corte no conteúdo validado.
- Landscape: headers, grade de sete colunas, marcadores e detalhe diário foram
  observados; ação contextual permaneceu acessível.
- Nenhum prompt de BLE, Health Connect, permissão, escrita ou fluxo novo foi
  disparado. Capturas não expõem identificadores, payloads BLE ou dados pessoais;
  validação populada foi feita somente no aparelho privado.
- Estado de data inválida foi conferido estaticamente: header possui um único
  heading, erro permanece presente e só `BackButton` fica disponível; fluxo
  suportado não oferece rota para gerar esse estado manualmente.

### Riscos residuais

- Com fonte `1,3x`, rótulos da navegação inferior podem quebrar em duas linhas;
  permanecem visíveis e não pertencem ao layout alterado nesta subtarefa.
- Conteúdo diário extenso continua dependendo de rolagem vertical, preservada
  pelo shell existente.
