# 034-05 — Renderizar zonas e carga

- **Ticker:** `034`
- **Número:** `05`
- **Status:** `completed`

## Objetivo

Adicionar ao detalhe de Exercícios a seção visual de zonas, carga cardiovascular
e cobertura, preservando gráficos existentes, acessibilidade e estados seguros.

## Resultado esperado

Após o gráfico/área de **Frequência cardíaca** e antes de **Cadência**, o usuário vê
zonas em ordem Z5..Z1, bucket abaixo de Z1, duração, percentual, carga local,
cobertura e mensagens corretas para ausência ou configuração necessária.

## Requisitos cobertos

- R6 da Spec 034;
- critérios de aceite 12 a 14 e 18;
- supporting text de Perfil já criado em `034-03`.

## Escopo incluído

- seção `Zonas de frequência cardíaca`;
- linhas/barra ou representação equivalente de duração e percentual;
- `Carga cardiovascular` com arredondamento apenas visual;
- `Cobertura de FC` com percentual formatado;
- aviso de FC acima da máxima configurada;
- estados sem configuração, sem samples, cobertura parcial e erro;
- semantics/headings/content descriptions acessíveis;
- preservação de gráfico de FC e seção de Cadência.

## Escopo excluído

- alteração da policy ou cálculo;
- persistência de qualquer valor visual;
- classificação Baixa/Moderada/Alta;
- baseline, histórico pessoal ou comparação de workouts;
- interação nova de gráfico que não seja necessária para a seção;
- Health Connect, BLE, parser e workflow.

## Dependências

- `034-04` concluída;
- tokens e componentes visuais atuais do design system;
- strings brasileiras com acentuação correta;
- screenshot será produzido somente no gate físico de `034-06`.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt` — `WorkoutActivityDetailContent`,
  `DetailSectionHeading` e rows/gráficos existentes;
- `feature/exercises/ExercisesFeatureContract.kt` — estado exposto;
- `app/src/main/res/values/strings.xml` — rótulos e mensagens;
- componentes de semantics e design system usados no detalhe.

## Passos de implementação

1. Inserir seção depois de Frequência cardíaca e antes de Cadência.
2. Renderizar buckets em ordem descendente, sem depender de ordem incidental da
   lista de domínio.
3. Mostrar duração e percentual calculados pelo read model, formatando somente
   na camada visual.
4. Mostrar carga com nome exato “Carga cardiovascular” e unidade/precisão
   acordadas pela apresentação.
5. Mostrar cobertura com destaque quando ratio for menor que 1.
6. Mostrar aviso de acima da máxima sem alterar BPM exibido.
7. Mostrar configuração necessária com ação/instrução para Perfil, sem zero.
8. Mostrar indisponibilidade para ausência de samples, sem fabricar valor.
9. Aplicar headings, descrição agregada e labels acessíveis a rows/barras.
10. Conferir que gráfico de FC existente e Cadência continuam presentes e na
    ordem original.

## Testes e comandos de validação

- inspeção visual em estados disponíveis e ausentes;
- revisão de semantics e strings;
- `git diff --check`;
- não criar ou executar testes automatizados nesta task.

## Definição de pronto

- ordem visual correta e seção posicionada corretamente;
- carga arredondada apenas na UI;
- cobertura exibida quando parcial;
- no-config/no-samples sem zero enganoso;
- acessibilidade cobre título, zona, duração, percentual, carga e cobertura;
- gráficos existentes preservados;
- screenshot físico pode ser capturado sem expor dados desnecessários.

## Riscos e cuidados

- Não chamar seção “Carga Cardio”.
- Não mostrar percentuais usando duração total quando denominador é cobertura.
- Não ocultar `BELOW_ZONE_1` da soma semântica.
- Não introduzir classificação de intensidade nesta spec.
- Não colocar cálculo ou regra de arredondamento de domínio no composable.

## Registro da execução

- **Status:** `completed` — seção visual integrada após `034-04`.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`.
- **Decisões e desvios:** `WorkoutActivityDetailContent` recebe o read model
  pronto e renderiza a seção somente para atividades `WATCH_D3_D4`. Buckets são
  ordenados explicitamente de Z5 a Z1, com “Abaixo de Z1” incluído. Duração e
  percentuais usam valores do domínio; carga recebe arredondamento apenas visual
  em pontos. Cobertura parcial mantém destaque textual/cromático. Sem
  configuração, samples ou análise válida, a UI informa estado seguro sem zero;
  nenhuma ação nova de navegação foi criada, e a instrução aponta para Perfil.
- **Comandos executados:** leitura de baseline, Spec 034, ADR-018, ADR-034,
  overview, subtarefas e `AGENTS.md`; inspeção de contratos, detalhe, gráfico,
  Cadência e semantics; `./gradlew app:compileDebugKotlin`; `./gradlew lint
  assembleDebug assembleRelease`; `git diff --check`.
- **Resultados e evidências:** compilação Kotlin, lint e builds debug/release
  passaram. A seção foi inserida entre gráfico/área de FC e Cadência, preservando
  o Canvas e métricas existentes. Rows e seção têm descrições agregadas
  acessíveis para zonas, duração, percentual, carga e cobertura. Aviso de sample
  acima da máxima preserva BPM observado e identifica Z5. Revisão independente
  aprovou a diff sem blocker/high.
- **Riscos residuais:** validação física, screenshot sanitizada, cálculo
  independente, migration/persistência e confirmação de ausência de operações
  Health Connect permanecem em `034-06`. Coberturas parciais próximas de 100%
  usam uma casa decimal para não ocultar parcialidade.
