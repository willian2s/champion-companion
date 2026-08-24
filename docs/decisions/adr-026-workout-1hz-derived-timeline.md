# ADR-026 — Timeline derivada de treinos em 1 Hz

## Status

Contrato histórico concluído; Gate B aposentado por decisão de produto após
`027-12`.
Implementação e UI removidas em `027-15`; Gate A, BPM raw e cadência D4
permanecem vigentes.

## Data

2026-08-24

## Contexto

A Spec 027 já preserva os fatos D4, a duração oficial e a timeline normalizada
de atividades iniciadas no Champion C001. O Gate A também passou com ajustes:
passos de atividade, crédito pós-aceite, Daily sem inflação e cadência por
intervalos foram validados sem alterar a autoridade dos totais existentes.

O Gate B precisa de uma grade comum de um segundo para BPM e cadência, mas essa
grade não pode transformar timestamp normalizado em medição, atravessar lacunas
estruturais, preencher ausência com zero ou criar uma segunda fonte de verdade.
Também é necessário manter reprocessamento reproduzível sem alterar snapshots
anteriores ou atividades já sincronizadas.

## Decisão

### 1. Camadas semânticas e autoridade

Toda série deve distinguir três camadas:

```text
SOURCE
  D4 persistido e normalizado

DERIVED
  tratamento aplicado somente sobre suporte comprovado pela fonte

RESAMPLED_1HZ
  grade temporal comum derivada de um segundo
```

`SOURCE` nunca é alterado. A timeline 1 Hz não substitui a autoridade dos
totais, intervalos, amostras, médias, mínimos, máximos ou demais estatísticas
existentes. `sampledAt` normalizado pertence a `DERIVED`; não é timestamp
medido pelo relógio.

### 2. Identidade e versionamento

O parser efetivo continua `d4-v3`. Snapshots e derivações existentes, inclusive
`watch-d3-d4-v2` e `watch-d3-d4-v3`, permanecem preservados. A timeline possui
versão independente:

```text
watch-d3-d4-1hz-v1
```

Cada série deve identificar, separadamente:

- versão do parser/source (`d4-v3`);
- versão efetiva da derivação da fonte (`watch-d3-d4-v2` ou
  `watch-d3-d4-v3`); sem essa versão, a série não é gerada;
- versão do algoritmo 1 Hz (`watch-d3-d4-1hz-v1`);
- conjunto de parâmetros `workout-1hz-params-v1` e fingerprint determinístico
  desse conjunto.

O algoritmo e a serialização de seus parâmetros são determinísticos. Alteração
de qualquer componente de identidade gera série separada, nunca atualização
silenciosa da série anterior. `median` usa o valor central ordenado quando a
cardinalidade é ímpar e a média aritmética dos dois valores centrais quando é
par.

O fingerprint usa manifesto plano UTF-8 sem BOM, formado por uma linha
`key=value` por parâmetro, ordenada lexicograficamente por bytes da chave e sem
linha final. Chaves usam somente `[a-z0-9._-]`; valores usam tokens ASCII sem
espaço ou números decimais em base 10, ponto como separador, sem expoente,
zeros à esquerda ou zeros finais. A entrada do fingerprint é esse manifesto e
o resultado é SHA-256 em hexadecimal minúsculo. O manifesto inclui a versão do
algoritmo, o nome do conjunto e todos os parâmetros efetivos.

### 3. Grade temporal

Uma sessão válida exige `endedAt > startedAt`. Quando a janela for vazia ou
invertida, a geração falha fechada e não cria buckets. Para sessão válida, usa-se
a janela semiaberta `[startedAt, endedAt)`. O eixo temporal é um
`Instant` absoluto, expresso em UTC, acompanhado de offset inteiro relativo em
segundos para facilitar consumo:

- o bucket de offset `0` começa em `startedAt`;
- cada bucket começa em `startedAt + offset s`;
- somente buckets cujo início seja menor que `endedAt` existem;
- a duração final parcial é um bucket válido;
- `endedAt` nunca vira ponto da grade.

Zona local serve somente para associação civil conforme as regras existentes de
atividade e Health Connect. Não é eixo temporal da série. Atividades que cruzam
meia-noite continuam usando `Instant`, sem mover o eixo; sua associação civil
segue a política já vigente.

Valores fora da sessão são descartados. Não há extrapolação antes do início ou
depois do fim.

### 4. BPM

A entrada é somente o BPM D4 válido atual, na faixa inclusiva de `30..220`.
Timestamp normalizado é derivado e não medido.

O tratamento inicial é versionado por conjunto de parâmetros:

- Hampel local, radius `2`, multiplier `3`, floor `12 BPM`;
- para cada candidato, usar até dois vizinhos válidos de cada lado, em ordem de
  origem, sem incluir o candidato; com menos de dois vizinhos combinados, a
  amostra não é rejeitada por Hampel;
- calcular `median` dos vizinhos, `MAD = median(abs(v - median))` e rejeitar
  somente quando `abs(candidate - median) > max(floor, 1.4826 * multiplier * MAD)`;
- o conjunto de vizinhos é calculado antes da rejeição de outliers e não muda
  durante a passagem;
- EMA sensível ao tempo, com `alpha = 1 - exp(-deltaT / tau)`;
- `tau` inicial de BPM é `5 s`, identificado no conjunto de parâmetros e não
  tratado como verdade fisiológica;
- amostras interpoladas não entram na EMA;
- a EMA reinicia em cada segmento contínuo.

O primeiro valor elegível de cada segmento inicializa a EMA sem suavização.
`deltaT` é a diferença positiva, em segundos e com a precisão temporal
disponível, entre os timestamps derivados de dois valores-fonte consecutivos.

O pipeline mantém precisão derivada até o consumidor. Quando um boundary exigir
BPM inteiro, o valor é arredondado deterministicamente ao inteiro mais próximo,
com empate em `0,5` para cima; esse arredondamento não altera `SOURCE`.

Interpolação linear é permitida somente entre observações da mesma região
contínua elegível. O valor do bucket é avaliado no início do bucket; timestamp
derivado exatamente igual ao início recebe `OBSERVED_DERIVED`, e valor entre
dois suportes recebe `INTERPOLATED`. Não existe extrapolação antes ou depois do
suporte. Outlier ou valor ausente não é observação; só pode receber
interpolação quando a continuidade estrutural permanecer comprovada.

### 5. Cadência

Cadência só é derivada para tipos D4 cuja semântica seja de passos. `CYCLING`
fica explicitamente fora, conforme ADR-021.

Para cada intervalo válido:

```text
cadenceSpm = intervalSteps / durationSeconds * 60
```

Passos zero e cadência zero são valores válidos quando duração e intervalo são
válidos. O tratamento usa os mesmos conceitos de segmento, EMA sensível ao
tempo e interpolação do BPM, com parâmetros próprios versionados:

- Hampel equivalente, com a mesma fórmula e vizinhança, radius `2`, multiplier
  `3` e floor `20 spm`;
- `tau` inicial de `5 s`;
- primeiro valor elegível de cada segmento inicializa a EMA sem suavização;
- `deltaT` usa a diferença positiva entre timestamps derivados dos midpoints dos
  intervalos;
- sem EMA sobre valores interpolados;
- reset da EMA em cada segmento;
- interpolação linear somente em região contínua elegível, sem extrapolação.

### 6. Continuidade e gaps

`sourceSampleIndex` de BPM e índice do intervalo de passos são a autoridade de
continuidade. Índice ausente, duplicado, fora de ordem ou bucket estrutural
ausente encerra o segmento e não pode ser atravessado por tratamento ou
interpolação.

Quando o índice não existir, pode ser usado fallback temporal versionado com
limite máximo de `90 s`. Esse fallback nunca atravessa marcador estrutural. Se o
índice estiver disponível, timeout não pode substituí-lo.

A grade mantém os buckets mesmo quando não há observação. Cada canal é
independente: ausência resulta em `null`/`UNSUPPORTED`, nunca em zero. Cada
valor deve preservar provenance por canal com as categorias:

```text
OBSERVED_DERIVED
INTERPOLATED
ABSENT/UNSUPPORTED
```

A provenance também identifica que o ponto pertence à camada
`RESAMPLED_1HZ`. BPM ausente não remove cadência presente, e vice-versa.

### 7. Estatísticas, projeções e Health Connect

A grade 1 Hz não recalcula silenciosamente médias, mínimos, máximos ou outras
estatísticas autoritativas. Não substitui BPM `SOURCE`/Health Connect nem a
cadência já derivada/publicada no Health Connect por buckets 1 Hz.

#### Decisão `027-10` — Health Connect

`StepsCadenceRecord` permanece publicado por buckets D4. A validação de `027-06`
observou aceite no Health Connect e cadência preservada no Health Sync/Strava,
mas não demonstrou benefício concreto de uma série 1 Hz, consumidor real,
estabilidade para payload significativamente maior ou segurança de atualização
da identidade/versionamento remoto. A série 1 Hz permanece somente local,
derivada e explicitamente distinguida de observações.

Não houve nova sincronização com Health Connect durante a validação visual de
`027-10`; a atividade foi apenas aberta localmente. Qualquer promoção futura
exige nova decisão explícita, evidência de interoperabilidade e revisão dos
contratos de identidade, retry, tamanho e proveniência.

### 8. Persistência e cache

Esta decisão não cria tabela nem migration. A timeline é calculada sob demanda
sobre snapshot imutável e pode ser cacheada somente em memória. A chave mínima
do cache contém `activityId`, identidade do snapshot, versões da fonte e do
algoritmo e conjunto/fingerprint de parâmetros. Mudança de qualquer identidade
invalida o cache.

Persistência futura exige ADR própria ou atualização explícita desta decisão e
migration aditiva, com vínculo `activityId + version`, regeneração determinística
e exclusão/reprocessamento sem tocar `SOURCE`.

### 9. Reprocessamento

Reprocessamento é somente explícito. Atualização do app, abertura de tela,
mudança de perfil, migration ou scheduler não muta snapshots antigos nem
atividade sincronizada silenciosamente, e não inicia resync automático. Uma nova
versão produz série separada, preservando as séries anteriores.

## Consequências

### Positivas

- BPM e cadência compartilham eixo absoluto e grade determinística;
- gaps e ausência permanecem observáveis, sem zeros sintéticos;
- origem, tratamento e reamostragem continuam auditáveis;
- novas versões podem ser comparadas e reprocessadas sem reescrever fatos;
- `CYCLING` não recebe cadência baseada em passos;
- nenhuma mudança de schema é necessária para iniciar o Gate B.

### Negativas e riscos residuais

- parâmetros de outlier e `tau` exigem validação das subtarefas seguintes;
- cache em memória não sobrevive ao processo e exige regeneração determinística;
- fallback temporal de `90 s` continua uma política versionada, não prova de
  continuidade quando o índice estrutural está ausente;
- interpolação pode deixar canais parcialmente ausentes e não representa
  medições do relógio;
- interoperabilidade física da cadência 1 Hz permanece limitada à evidência de
  `027-06`; em `027-11`, duas atividades foram importadas e sincronizadas
  localmente após recarga do relógio, sem readback Health Connect e sem nova
  execução Health Sync/Strava. `StepsCadenceRecord` continua por buckets D4, e
  a promoção para 1 Hz segue não autorizada.
- A revisão final de `027-11` identificou três correções necessárias antes do
  fechamento do Gate B: intervalo estrutural inválido deve preservar gaps em vez
  de falhar a série de cadência inteira; BPM deve exigir parser `VALID`; e
  `fallbackGapSeconds` de BPM deve respeitar limite máximo de `90 s`. Essas
  correções exigem subtarefa explícita e não alteram SOURCE nem Gate A.

### Correção `027-12`

`deriveCadence1Hz` trata intervalo estrutural inválido como fronteira de
segmento, preservando buckets ausentes e impedindo interpolação através da
ruptura. `deriveBpm1Hz` exige `ParserValidity.VALID`, e o fallback temporal de
BPM aceita somente `1..90 s`. Essas correções não alteram SOURCE, snapshots,
estatísticas, identidade existente, projeções Health Connect ou Gate A.

### Aposentadoria posterior do Gate B

Por decisão de produto registrada após `027-12`, a timeline local 1 Hz, sua UI e
seus derivados foram removidos em `027-15`. O fluxo vigente não usa essa camada:
Health Connect recebe BPM raw e `StepsCadenceRecord` por buckets D4. Esta ADR e
as tasks anteriores preservam o contrato como histórico; a remoção não promove
cadência 1 Hz, não altera Gate A e não apaga evidências anteriores.

### Fechamento em `027-16`

Gate A foi revalidado como `go_with_adjustments`; Gate B foi encerrado como
`retired_by_product_decision`. A auditoria confirmou ausência de implementação,
estado e UI ativos de 1 Hz, preservação de identidades, payloads, retry, Daily e
crédito de Gate A, além de Health Connect write-only. Não houve nova
sincronização física nem readback.

## Alternativas rejeitadas

- alterar ou substituir D4 para armazenar pontos de 1 Hz;
- tratar timestamp normalizado como medição real;
- usar somente `startedAt + sampleIndex × 60 s` como eixo da grade;
- criar ponto em `endedAt` ou extrapolar fora da sessão;
- usar zero para representar ausência;
- atravessar gaps com timeout quando há índice de origem;
- interpolar através de índice ausente, duplicado, fora de ordem ou bucket
  estrutural ausente;
- aplicar perfil de cadência por passos a `CYCLING`;
- recalcular estatísticas autoritativas com pontos interpolados;
- substituir BPM SOURCE/Health Connect ou cadência HC pela série 1 Hz sem
  decisão explícita;
- persistir tabela/migration antes de evidência de necessidade;
- reprocessar silenciosamente ou ressincronizar automaticamente;
- usar Kalman, GPS, localização ou outra fonte externa de sinal.

## Relação com decisões anteriores

- **ADR-013:** preserva read models derivados, ausência distinta de zero,
  janelas semiabertas e associação civil separada do eixo instantâneo.
- **ADR-018:** preserva raw/measured/derived/projection, snapshots imutáveis,
  versionamento, reprocessamento explícito e Health Connect write-only.
- **ADR-019:** preserva duração oficial, timeline D4 normalizada, índices de
  origem, `Instant` e distinção entre fatos e timestamps derivados.
- **ADR-021:** mantém `CYCLING` fora de cadência e derivados baseados em passos.
- **ADR-025:** mantém independência da cadência em relação ao crédito de passos,
  cobertura diária e projeções de passos.

Esta ADR detalha o contrato do Gate B da Spec 027 e não reabre treino phone,
GPS, rota, localização ou qualquer projeção remota já sincronizada.
