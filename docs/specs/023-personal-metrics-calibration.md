# 023 — Perfil, métricas pessoais e calibração por distância de referência

- **Ticker:** `023`
- **Fase:** 16
- **Status:** `pending`
- **Base:** `main` após merge da Spec 022
- **Branch:** `feat/023-personal-metrics-calibration`
- **Progresso planejado:** `4/7`
- **Decisão principal:** ADR-022

## Contexto

A Fase 16 histórica previa perfil e cálculos pessoais, mas a evolução do
Champion Companion entregou parte desse escopo por specs independentes antes da
fase ser executada formalmente.

Na `main` atual:

- `UserProfile` já possui altura;
- existe um `StepLengthProfile` base editável;
- existe sugestão de comprimento do passo baseada em altura;
- `REFERENCE_DISTANCE_CALIBRATION` existe como `StepLengthSource`, sem fluxo
  funcional;
- pesos já possuem entidade, leitura do último valor e suporte de escrita no
  Health Connect, mas a UI ainda não permite cadastro e histórico completo;
- a meta diária de passos já existe no DataStore e é consumida pelo app;
- o banco está em Room v12;
- a Spec 022 adicionou `CYCLING`, cuja distância é independente de passos;
- snapshots de derivação de treinos do relógio são imutáveis por atividade.

Esta spec executa a Fase 16 revisada e não assume relação geral entre ticker e
número de fase.

## Resultado da auditoria `023-01`

A auditoria do estado real foi concluída sem alteração de código. Os contratos
de ligação da evidência ao perfil/atividade, unicidade por atividade, agregação
determinística, seleção após `confirmedType`, guards de workflow, atividade
sincronizada, provenance Health Connect, enqueue de altura/peso e consumers estão
congelados em:

`docs/integration/023-01-personal-metrics-and-calibration-audit.md`.

`023-02` pode implementar schema/domínio conforme esse documento. Persistência
de calibração usa migration aditiva v12 -> v13; snapshots v2, parser `d4-v3`,
DataStore e contratos Health Connect existentes permanecem compatíveis.

## Objetivo

Completar somente os dados pessoais que possuem uso real no produto e usar
distâncias externas de referência para aprender progressivamente comprimentos de
passo separados para corrida e caminhada.

O Champion Companion continua sem GPS próprio.

Fluxo principal de calibração:

```text
Treino D3/D4 importado
        ↓
Revisão confirma RUNNING ou WALKING
        ↓
usuário informa distância de referência
        ↓
decodedSteps elegíveis + distância externa
        ↓
comprimento observado do passo
        ↓
calibração da atividade, quando segura
        ↓
evidência entra no perfil aprendido do tipo
        ↓
novos treinos sem distância externa usam o perfil aprendido
```

## Escopo funcional da Fase 16

### Incluído

- edição de altura;
- edição da meta diária de passos;
- cadastro manual de peso;
- histórico local de peso;
- manutenção do comprimento de passo base;
- perfis aprendidos separados para corrida e caminhada;
- calibração por distância de referência em atividade D3/D4;
- histórico/correção/remoção de evidências de calibração;
- uso do perfil contextual em novas derivações;
- rederivação explícita da atividade usada como referência quando segura;
- integração com os consumidores locais existentes;
- preservação da sincronização write-only de altura/peso;
- preservação dos contratos Health Connect de treinos.

### Excluído

- nome;
- nascimento;
- sexo;
- meta de peso;
- preferência imperial/unidades configuráveis;
- zonas de frequência cardíaca;
- FC máxima estimada;
- GPS, localização, rota e mapa;
- leitura de distância do Health Connect;
- integração direta com Adidas Running, Strava ou outras APIs;
- ML/regressão por pace, cadência ou inclinação;
- calibração automática sem confirmação;
- reescrita silenciosa de histórico;
- reconciliação genérica pós-sync;
- testes automatizados novos ou execução de suites sem pedido explícito.

## R1 — Perfil pessoal enxuto

A Fase 16 mantém somente campos com consumidor funcional.

### Altura

- unidade de armazenamento: centímetros;
- valor opcional, positivo e finito;
- editável na superfície de Perfil;
- alteração de altura recalcula apenas a sugestão de comprimento do passo;
- sugestão continua exigindo aceite explícito;
- não substituir automaticamente o perfil base existente;
- alteração deve reutilizar o fluxo Health Connect write-only de `HeightRecord`
  quando a integração estiver habilitada.

### Meta diária de passos

- permanece em `AppPreferences`/DataStore;
- valor positivo;
- editável explicitamente pelo usuário;
- Home, Saúde e Calendário devem refletir o novo valor pelas fontes já
  existentes;
- nenhuma migration Room é necessária para a meta.

### Peso

- nova medição manual em quilogramas;
- valor positivo e finito;
- `measuredAt` explícito, usando agora como padrão;
- preservar todas as medições válidas em `weight_measurements`;
- exibir último peso e histórico local;
- reutilizar a operação write-only de `WeightRecord` já existente;
- falha remota não desfaz persistência local;
- não criar meta de peso.

## R2 — Perfil de comprimento do passo base

O perfil atual permanece como `BASE`.

Origens vigentes:

```text
MANUAL
HEIGHT_SUGGESTION_ACCEPTED
```

O perfil base continua sendo fallback para qualquer contexto que não possua
perfil aprendido específico.

Mudanças no perfil base:

- afetam apenas novas derivações;
- não rederivam atividades existentes;
- não alteram snapshots ativos antigos;
- não disparam sincronização de treino.

## R3 — Contextos aprendidos

Adicionar resolução semântica para:

```text
RUNNING
WALKING
```

Cada contexto possui perfil aprendido somente quando existe evidência válida.

A fase não precisa persistir um segundo agregado autoritativo se o valor puder ser
recalculado deterministicamente a partir das evidências.

Modelo de leitura esperado:

```text
LearnedStepLengthProfile
- workoutType
- stepLengthMeters
- sampleCount
- totalReferenceDistanceMeters
- totalDecodedSteps
- updatedAt
```

## R4 — Evidência por distância de referência

Uma evidência válida exige:

- atividade `WATCH_D3_D4`;
- `confirmedType` igual a `RUNNING` ou `WALKING`;
- série D4 elegível;
- soma positiva de `decodedSteps` elegíveis;
- distância de referência positiva e finita;
- atividade não descartada;
- uma única evidência ativa por `activityId`.

O usuário fornece a distância total em quilômetros na UI; o armazenamento usa
metros.

Fórmula da evidência:

```text
observedStepLengthMeters = referenceDistanceMeters / totalEligibleDecodedSteps
```

O denominador deve ser a mesma soma de passos que distribui a distância pelos
intervalos da série canônica.

## R5 — Aprendizado progressivo

Para cada contexto:

```text
learnedStepLengthMeters =
    Σ referenceDistanceMeters
    ─────────────────────────
    Σ totalEligibleDecodedSteps
```

Requisitos:

1. não usar média simples dos valores observados;
2. não usar EMA ou peso temporal escondido;
3. evidência maior naturalmente pesa mais por possuir mais passos;
4. substituir uma evidência da mesma atividade não pode duplicar seu peso;
5. remover uma evidência recalcula o perfil futuro;
6. recalcular perfil não altera snapshots históricos;
7. ausência de evidência retorna ao fallback `BASE`.

## R6 — Resolução para novos treinos

Resolver o perfil efetivo no momento da derivação:

```text
RUNNING
  -> learned RUNNING
  -> fallback BASE

WALKING
  -> learned WALKING
  -> fallback BASE

TREADMILL
  -> BASE nesta fase

FREE_WORKOUT
  -> comportamento vigente preservado

CYCLING
  -> nunca usar perfil de passos para distância

DailySummary
  -> BASE
```

A resolução deve congelar no snapshot:

- `stepLengthMetersUsed`;
- `stepLengthSource`;
- contexto/origem suficiente para auditoria;
- derivationVersion.

Nova direção de versão:

```text
watch-d3-d4-v3
```

`d4-v3` do parser permanece inalterado.

## R7 — Momento em que o tipo entra na derivação

A implementação deve evitar usar um perfil contextual incompatível antes de o
tipo ser confirmado.

Direção preferida:

- importação inicial pode continuar usando `BASE`;
- após revisão confirmar `RUNNING`/`WALKING`, a atividade pode ser explicitamente
  rederivada com o perfil contextual vigente antes da sincronização;
- mudança de tipo durante revisão não pode aplicar perfil aprendido do tipo
  anterior;
- `CYCLING` continua seguindo a política da Spec 022.

A task de integração deve auditar o fluxo atual e congelar uma única política sem
criar rederivações silenciosas em background.

## R8 — Aplicar referência à atividade atual

Após confirmar `RUNNING` ou `WALKING`, a atividade ainda não sincronizada pode
receber uma distância de referência explícita.

Quando permitida:

```text
stepLengthMetersUsed = referenceDistanceMeters / totalEligibleDecodedSteps
stepLengthSource = REFERENCE_DISTANCE_CALIBRATION
derivationVersion = watch-d3-d4-v3
```

Consequências:

- a soma canônica dos intervalos deve fechar na referência;
- distância acumulada segue os deltas dos intervalos;
- velocidade e pace são recalculados;
- splits são recalculados;
- BPM permanece baseado nos samples D4 existentes;
- nenhuma amostra GPS é criada;
- a evidência também atualiza o perfil aprendido do contexto.

## R9 — Guards de atividade

A calibração não deve alterar atividade quando houver risco de conflito remoto.

### Pode rederivar a própria atividade

Somente em estados locais em que nenhuma escrita remota incompatível tenha sido
aceita/preparada de forma irreconciliável.

A task deve auditar os guards atuais e preferir fail-closed.

### Não pode rederivar silenciosamente

- `SYNCING`;
- `SYNCED`;
- atividade com efeito remoto parcial cuja projeção não possa ser reconciliada
  pelo contrato atual.

A Fase 16 não cria edição pós-sync genérica.

Caso uma atividade histórica sincronizada seja futuramente autorizada apenas
como evidência para melhorar o perfil, isso não pode alterar a própria atividade
nem seus records remotos; essa possibilidade deve ser explicitamente decidida
na task de auditoria antes de ser habilitada.

## R10 — Persistência da calibração

Direção prevista de schema:

```text
Room v12 -> v13
```

Nova tabela preferida:

```text
step_length_calibrations
- id TEXT PK
- profileId TEXT NOT NULL
- activityId TEXT NOT NULL UNIQUE
- workoutType TEXT NOT NULL
- referenceDistanceMeters REAL NOT NULL
- decodedSteps INTEGER NOT NULL
- acceptedAtEpochMillis INTEGER NOT NULL
- updatedAtEpochMillis INTEGER NOT NULL
```

Regras:

- FK para perfil e atividade quando compatível com os guards de retenção;
- índice por `profileId + workoutType`;
- uma calibração ativa por atividade;
- migration aditiva;
- linhas existentes não são reprocessadas;
- nenhuma migration cria snapshots ou operações HC;
- schema v13 exportado;
- sem wipe.

Se a auditoria mostrar que FK rígida dificultaria preservar evidência após alguma
operação legítima de retenção, documentar a decisão antes de implementar.

## R11 — Correção e remoção de evidência

O usuário deve conseguir:

- visualizar evidências de corrida;
- visualizar evidências de caminhada;
- ver distância, passos e comprimento observado;
- corrigir a distância da mesma atividade;
- remover uma evidência incorreta.

Após correção/remoção:

- recalcular somente o perfil aprendido futuro;
- não alterar automaticamente atividades históricas;
- não sincronizar nada remotamente por causa da alteração do perfil.

## R12 — UI de calibração no detalhe de exercício

Para atividade revisada como corrida/caminhada e elegível:

```text
Distância estimada atual
4,08 km

Calibrar com distância de referência
Distância de referência
[ 4,23 ] km

Passos usados
4.987

Comprimento observado do passo
0,848 m

Perfil de corrida atual
0,844 m

Após esta calibração
0,846 m

[ Aplicar calibração ]
```

A UI deve deixar claro que:

- o valor vem de fonte externa;
- o Champion Companion não coletou GPS;
- a calibração atualiza a atividade somente quando permitido;
- o perfil aprendido será usado em treinos futuros.

## R13 — UI de Perfil

A tela Perfil deve consolidar:

### Dados básicos

- altura;
- meta diária de passos;
- último peso/atalho para histórico.

### Comprimento do passo base

- valor atual;
- origem;
- atualização manual;
- sugestão por altura.

### Perfis aprendidos

- Corrida: valor, quantidade de calibrações, distância total de referência;
- Caminhada: valor, quantidade de calibrações, distância total de referência;
- estado vazio quando sem evidência;
- acesso ao histórico de calibrações.

Não mostrar esses valores como se fossem medidas diretas do relógio.

## R14 — Histórico de peso

A superfície de Saúde/Peso deixa de ser placeholder e passa a permitir:

- registrar nova medição;
- mostrar último valor;
- listar histórico temporal;
- preservar origem manual;
- refletir estado local mesmo se Health Connect estiver indisponível.

Gráfico de peso é opcional nesta fase; lista/histórico funcional é suficiente.
Não criar gráfico apenas para cumprir o roadmap antigo.

## R15 — Health Connect: altura e peso

Reutilizar os contratos atuais:

- `HeightRecord` manual;
- `WeightRecord` manual;
- IDs/versionamento já existentes;
- persistência local primeiro;
- retry foreground;
- sem readback.

A fase não deve recriar a fila nem criar outro mecanismo de sync para esses
agregados.

## R16 — Health Connect: treino calibrado

Sessão, BPM e calorias continuam com a semântica atual.

Distância/velocidade derivadas diretamente de uma referência digitada precisam
ter provenance coerente com ADR-022.

A task deve auditar o contrato congelado pós-Spec 022 e escolher o menor ajuste
compatível:

- não quebrar payloads antigos;
- não renumerar filhos existentes sem necessidade;
- não duplicar distância;
- preservar retry determinístico;
- não rotular distância digitada como medição GPS coletada pelo app.

Treinos futuros que apenas usam um perfil aprendido continuam sendo cálculos
automáticos sobre passos do relógio + perfil congelado.

## R17 — Daily Summary

O resumo diário não deve usar automaticamente o perfil aprendido de corrida ou
caminhada porque os passos do dia não possuem contexto de gait confiável.

Continuar usando:

```text
BASE
```

A Fase 16 não altera retroativamente resumos diários persistidos.

## R18 — Compatibilidade

- `WorkoutType.CYCLING` mantém regras da Spec 022;
- parser `d4-v3` não muda;
- snapshots `watch-d3-d4-v2` permanecem legíveis;
- nenhum snapshot antigo é atualizado na migration;
- `TREADMILL` mantém `BASE` até decisão futura;
- perfil base existente permanece válido após upgrade;
- peso/altura já persistidos permanecem intactos;
- meta diária existente em DataStore permanece intacta;
- Health Connect continua write-only/local-first.

## Subtarefas

```text
023-01
   ↓
023-02
   ↓
023-03
   ↓
023-04
   ↓
023-05
   ↓
023-06
   ↓
023-07
```

- `023-01` — auditar estado atual e congelar boundaries da Fase 16;
- `023-02` — adicionar persistência de calibrações e leitura de perfis aprendidos;
- `023-03` — completar Perfil com altura e meta diária de passos;
- `023-04` — implementar cadastro e histórico local de peso;
- `023-05` — implementar calibração de corrida/caminhada por distância de referência;
- `023-06` — integrar perfis contextuais à derivação e às projeções/consumidores;
- `023-07` — validar fluxo físico, banco preservado e fechar a Fase 16.

## Critérios de aceite globais

1. Altura pode ser editada e persiste sem substituir automaticamente o passo base.
2. Meta diária de passos pode ser editada e consumidores existentes refletem a alteração.
3. Peso pode ser registrado manualmente e consultado em histórico local.
4. Perfil base atual permanece funcional.
5. Corrida e caminhada possuem perfis aprendidos independentes.
6. Uma atividade não pode ser contada duas vezes no aprendizado.
7. Perfil aprendido usa `Σ distância / Σ passos`.
8. Distância de referência nunca é obtida por GPS interno do app.
9. Atividade elegível calibrada fecha sua distância canônica na referência.
10. Nova derivação é versionada como `watch-d3-d4-v3` ou equivalente documentado.
11. Treino futuro RUNNING usa perfil aprendido de corrida quando disponível.
12. Treino futuro WALKING usa perfil aprendido de caminhada quando disponível.
13. Sem perfil contextual, fallback é BASE.
14. TREADMILL e Daily Summary continuam usando BASE nesta fase.
15. CYCLING nunca passa a usar distância por passos.
16. Correção/remoção de evidência muda apenas cálculos futuros, não histórico automaticamente.
17. Atividades já sincronizadas não são reescritas silenciosamente.
18. Room v13 preserva dados v12 sem wipe e sem efeitos remotos.
19. Altura/peso reutilizam a fila Health Connect existente.
20. Nenhuma feature de GPS/rota é reintroduzida.
21. Nome, nascimento, sexo, meta de peso, unidades configuráveis e zonas de FC não são adicionados.

## Validação planejada

Sem criar ou executar testes automatizados como etapa padrão.

Validação final:

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug` sobre banco existente;
- confirmar `PRAGMA user_version = 13`;
- confirmar preservação dos dados existentes;
- editar altura e confirmar sugestão sem aceite automático;
- alterar meta de passos e validar Home/Saúde/Calendário;
- registrar ao menos duas medições de peso e verificar histórico/restart;
- calibrar uma corrida D3/D4 com distância externa;
- confirmar fechamento entre distância de referência e nova distância canônica;
- calibrar uma segunda corrida e confirmar `Σ distância / Σ passos`;
- calibrar caminhada e confirmar perfil independente;
- corrigir/remover uma evidência e confirmar recomputação futura;
- importar/reprocessar novo treino sem referência e confirmar uso do perfil contextual;
- confirmar snapshots antigos intactos;
- confirmar que bicicleta mantém a política da Spec 022;
- validar escrita Health Connect somente nos cenários já autorizados e sem readback.

## Relação com roadmap

Esta spec executa a Fase 16 revisada registrada em
`docs/roadmap/post-phase-15-direction.md`.

Após conclusão, a próxima fase macro continua sendo a Fase 17 — Backup e
restauração local, salvo nova decisão explícita.
