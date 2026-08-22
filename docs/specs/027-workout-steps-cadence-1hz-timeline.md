# Spec 027 — Passos, cadência e timeline temporal de atividades

## Status

Draft para execução independente após a Spec 026.

## Ticker

`027`.

O ticker foi reutilizado após exclusão integral da branch anterior `spec/027-phone-workouts-gps-bpm-health-connect`. Nenhum artefato daquela direção faz parte desta spec.

## Contexto

O fluxo de produto permanece centrado em treinos iniciados e gravados no Champion C001:

1. o usuário inicia o treino no relógio;
2. o app importa D3/D4 explicitamente;
3. o parser persiste atividade, intervalos e amostras de BPM;
4. a atividade passa por revisão/derivação local;
5. o usuário sincroniza explicitamente com Health Connect.

Treinos iniciados pelo celular, GPS, rota, mapa, `ExerciseRoute` e BLE live em sessão phone continuam fora da direção futura do produto.

O D4 já entrega total de passos da atividade e passos por intervalo. O app usa esses dados para derivação local, inclusive distância por comprimento de passo quando aplicável. A projeção Health Connect de atividade, porém, ainda não publica passos nem cadência como dados do treino.

O Health Connect possui `StepsRecord` para contagem de passos em um intervalo e `StepsCadenceRecord` para série temporal de cadência. A documentação oficial orienta que cada passo seja reportado uma única vez e que `StepsRecord` não tenha sobreposição que duplique passos.

Existe ainda um fluxo independente de passos diários: a Spec 016 migrou `DailySummary` para incrementos imutáveis. O `DailySummary` local continua cumulativo e o sync calcula somente a diferença ainda não enfileirada. Esse total diário é a referência autoritativa de passos do relógio para o usuário.

Como a sincronização é manual, é possível sincronizar uma atividade antes do próximo `DailySummary`. Isso permite que passos do treino sejam publicados primeiro, desde que o próximo incremento diário desconte exatamente os passos da atividade já aceitos pelo Health Connect. A ordem inversa precisa falhar fechada para não duplicar contagem.

Após a interoperabilidade de passos/cadência, esta spec também criará uma timeline derivada comum de 1 Hz para BPM e cadência, preservando integralmente os dados D4 de origem.

## Objetivos

### Gate A — Passos e cadência no Health Connect

- publicar passos de uma `WorkoutActivity` quando eles ainda não estiverem cobertos por passos diários já sincronizados;
- publicar `StepsCadenceRecord` derivado dos intervalos D4;
- preservar o acumulado diário autoritativo sem inflação por duplicidade;
- manter retry e identidade remota idempotentes;
- validar fisicamente o caminho `Champion Companion -> Health Connect -> Health Sync -> Strava` sem assumir suporte não comprovado do Health Sync;
- manter o sync manual e explícito.

### Gate B — Timeline derivada de 1 Hz

Gerar uma timeline temporal comum:

```text
segundo | BPM | cadência
0       | 138 | 164
1       | 138 | 165
2       | 139 | 165
...
300     | 151 | 172
301     | 151 | 173
```

A timeline é derivada e reamostrada. Ela não substitui nem reescreve amostras/intervalos D4 de origem.

## Não objetivos

- treino iniciado pelo celular;
- GPS, rota, mapa ou `ExerciseRoute`;
- importar Adidas Running ou Strava;
- leitura do Health Connect;
- sincronização automática/background;
- alterar a regra de distância canônica por passos;
- alterar o contador cumulativo local de `DailySummary`;
- reconstruir sinal cardíaco real não fornecido pelo relógio;
- preencher gaps D4 como se fossem medições reais;
- criar granularidade temporal falsa para o resumo diário além do mínimo necessário para evitar sobreposição remota;
- migrar ou reescrever históricos remotos já sincronizados sem necessidade concreta;
- testes automatizados, salvo pedido explícito do usuário.

## Princípios autoritativos

### 1. Total diário continua autoritativo

`DailySummary.steps` continua sendo a referência cumulativa do relógio por dia local.

A soma de passos publicada pelo Champion Companion para um dia nunca pode superar o total observado pelo app para esse dia por causa de uma atividade sincronizada.

Invariante desejado após cobertura completa:

```text
passos de atividades já publicados
+
passos residuais publicados pelo DailySummary
=
total cumulativo observado no relógio
```

### 2. Passos da atividade podem antecipar o próximo delta diário

Se a atividade for sincronizada antes do incremento diário que a contém:

```text
último total diário sincronizado = 4.000
atividade                       = 5.000 passos
novo total observado            = 10.000

delta bruto diário              = 6.000
crédito de atividade            = 5.000
delta residual diário           = 1.000
```

O Health Connect termina com `10.000`, não `15.000`.

### 3. Crédito só existe após efeito remoto aceito

Uma atividade não reduz o próximo `DailySummary` apenas por existir localmente ou por possuir operação pendente.

O crédito de passos torna-se elegível somente quando o `StepsRecord` correspondente da atividade tiver sido aceito pelo Health Connect e o estado local puder identificá-lo de forma idempotente.

Falha ou cancelamento antes do aceite remoto não pode causar subcontagem diária.

### 4. Crédito é identificado, não apenas somado

Não usar apenas `pendingWorkoutSteps = N` global.

O estado precisa identificar pelo menos:

- atividade;
- dia local;
- quantidade de passos;
- janela da atividade;
- identidade da projeção remota;
- estado de publicação/consumo.

Reexecutar a mesma atividade não cria novo crédito.

### 5. Ordem inversa falha fechada

Se o `DailySummary` já sincronizado cobriu a atividade antes da sincronização da atividade:

- não publicar um novo `StepsRecord` da atividade que duplique os mesmos passos;
- `StepsCadenceRecord` continua elegível;
- as demais métricas da atividade continuam elegíveis;
- não apagar/repartir automaticamente records diários antigos apenas para promover passos da atividade.

O fluxo preferencial para obter passos associados à atividade é, portanto, atividade primeiro e `DailySummary` depois.

### 6. `StepsRecord` do mesmo produtor não duplica nem sobrepõe passos

A implementação deve respeitar a semântica do Health Connect de que cada passo é reportado uma única vez.

Quando passos da atividade já ocuparem sua janela temporal, o residual diário não deve criar uma segunda contagem sobre a mesma cobertura como se fossem passos adicionais.

Como a granularidade intradiária do `DailySummary` não é objetivo do produto, a implementação pode priorizar correção do acumulado diário sobre precisão temporal do residual, desde que:

- não invente novos passos;
- não duplique passos;
- mantenha janela válida;
- preserve o checkpoint cumulativo real do relógio;
- documente qualquer aproximação temporal necessária;
- falhe fechada se não houver representação remota segura.

A política exata de janela residual deve ser confirmada em `027-01` após auditoria do estado atual da Spec 016.

### 7. Cadência é independente da contabilidade de passos

`StepsCadenceRecord` representa taxa temporal em passos/minuto. Ele não é usado como incremento do total diário.

Portanto:

- pode ser sincronizado mesmo quando `StepsRecord` da atividade estiver bloqueado por cobertura diária prévia;
- não gera nem consome crédito de passos;
- usa dados derivados exclusivamente dos intervalos válidos da atividade.

## Derivação inicial de cadência

Antes da timeline de 1 Hz, a cadência deve ser derivada diretamente de cada intervalo elegível:

```text
cadenceSpm = intervalSteps / intervalDurationSeconds * 60
```

Exemplo:

```text
164 passos / 60 s = 164 spm
86 passos / 30 s  = 172 spm
```

A amostra temporal deve usar uma convenção determinística coerente com a timeline normalizada da atividade, preferencialmente midpoint do intervalo quando compatível com o modelo atual.

Intervalos com duração inválida, passos ausentes/inválidos ou fora da janela da sessão não produzem amostra.

A primeira publicação de `StepsCadenceRecord` não depende da timeline de 1 Hz.

## Timeline 1 Hz — modelo conceitual

Três níveis devem permanecer distinguíveis:

```text
SOURCE
  dados D4 persistidos

DERIVED
  valores tratados/suavizados nos instantes suportados pela fonte

RESAMPLED_1HZ
  grade temporal derivada de 1 segundo
```

Não é obrigatório criar três tabelas; a distinção é semântica e deve ser refletida no domínio/persistência escolhidos.

### BPM

Pipeline inicial alvo:

```text
amostras D4
  -> validação de faixa
  -> tratamento robusto de outlier
  -> EMA sensível ao tempo
  -> interpolação somente em trechos elegíveis
  -> grade 1 Hz
```

A EMA deve considerar o intervalo real entre amostras, por exemplo:

```text
alpha = 1 - exp(-deltaT / tau)
```

`tau` é parâmetro versionado do algoritmo e deve ser calibrado na implementação, não hardcoded documentalmente como verdade fisiológica.

### Cadência

Pipeline inicial alvo:

```text
intervalos D4 válidos
  -> passos / duração * 60
  -> tratamento/suavização temporal
  -> interpolação somente em trechos elegíveis
  -> grade 1 Hz
```

### Gaps

Não preencher lacunas estruturais como se houvesse observação.

Quando `sourceSampleIndex`/índice de intervalo indicar perda de bucket ou quando a separação exceder a política versionada, a timeline 1 Hz deve preservar ausência até a próxima região contínua elegível.

### Autoridade estatística

Pontos interpolados não se tornam medições reais.

Média, mínimo, máximo e outras estatísticas autoritativas existentes não devem ser silenciosamente recalculadas sobre milhares de pontos sintetizados sem decisão explícita.

## Persistência e versionamento

`027-01` deve auditar se o schema atual comporta de forma segura:

- filho `StepsRecord` da atividade;
- filho `StepsCadenceRecord`;
- estado idempotente de crédito/cobertura de passos;
- versão da derivação temporal 1 Hz.

Se persistência nova for necessária, criar migration aditiva a partir do Room v13 e schema exportado correspondente. Não reutilizar campos com semântica incompatível.

Qualquer algoritmo derivado persistido deve carregar versão suficiente para permitir reprocessamento futuro sem confundir série antiga e nova.

## Health Connect

A projeção de atividade deve evoluir de forma compatível com a infraestrutura atual de filhos congelados, IDs estáveis, versões monotônicas e retry explícito.

Novos conceitos esperados, sujeitos à auditoria:

```text
HealthProjectionChildKey.WorkoutSteps
HealthProjectionChildKey.StepsCadenceSeries
```

`HealthSyncRecordType`/permissões devem representar `StepsCadenceRecord` sem aliases ambíguos.

Para passos da atividade:

- `StepsRecord` usa a quantidade real persistida da atividade;
- retry conserva identidade e conteúdo congelado;
- aceite remoto ativa crédito idempotente;
- cobertura diária prévia bloqueia o filho de passos antes da escrita.

Para cadência:

- um `StepsCadenceRecord` pode conter múltiplas amostras;
- a primeira versão usa cadência por intervalo D4;
- após o Gate B, uma revisão versionada pode usar a série 1 Hz se houver benefício concreto e sem degradar interoperabilidade.

## Health Sync e Strava

Health Sync é o intermediário usado pelo usuário entre Health Connect e Strava.

Esta spec não presume que todo record do Health Connect será convertido pelo Health Sync para um campo equivalente no Strava.

A validação física deve verificar separadamente:

- total de passos associado à atividade;
- cadência média;
- cadência máxima;
- gráfico temporal de cadência;
- ausência de inflação no total diário de passos.

Resultado permitido:

- `go`: dados chegam como esperado e total diário permanece correto;
- `go_with_adjustments`: integração parcial, mas sem corrupção/duplicidade;
- `no_go`: Health Sync/Strava não preserva a informação necessária ou a contabilidade de passos não pode ser mantida com segurança.

Falha de interoperabilidade com Strava não autoriza violar invariantes do Health Connect nem alterar dados de origem.

## Fluxos obrigatórios

### A. Atividade antes do Daily Summary

```text
sincronizar atividade
  -> StepsRecord aceito
  -> crédito ativo
  -> StepsCadenceRecord aceito/independente

sincronizar DailySummary
  -> calcular delta bruto cumulativo
  -> descontar créditos elegíveis do mesmo dia/cobertura
  -> publicar somente residual seguro
  -> avançar checkpoint cumulativo real
  -> marcar créditos consumidos somente após transação/efeito remoto consistente
```

### B. Daily Summary antes da atividade

```text
DailySummary já sincronizado
  -> atividade posteriormente sincronizada
  -> StepsRecord da atividade = bloqueado/ALREADY_COVERED
  -> StepsCadenceRecord = elegível
  -> demais filhos = elegíveis
```

### C. Retry da atividade

Retry da mesma atividade nunca cria segundo `StepsRecord`, segundo crédito ou segunda contagem de passos.

### D. Múltiplas atividades antes do Daily

Créditos são independentes por atividade e somados somente quando elegíveis para o mesmo dia/cobertura.

### E. Mudança de dia

Crédito não atravessa silenciosamente reset diário do relógio. Atividade que cruza meia-noite exige política explícita/fail-closed na auditoria inicial.

## Segurança e privacidade

- não logar amostras de BPM, passos, cadência ou payloads congelados;
- não logar conteúdo Health Connect;
- manter sync explícito/write-only;
- preservar backup desabilitado;
- não adicionar permissões além das necessárias para os records efetivamente escritos;
- não introduzir localização.

## Gates de execução

### Gate A — integridade e interoperabilidade

`027-01 -> 027-06`

1. auditar Spec 016, D4, projeções de atividade e definir decisão duradoura;
2. modelar persistência/identidades para passos e cadência;
3. implementar passos de atividade e crédito idempotente;
4. integrar crédito ao Daily Summary sem duplicidade;
5. implementar `StepsCadenceRecord` por intervalos D4;
6. validar fisicamente Champion -> HC -> Health Sync -> Strava e acumulado diário.

Gate A bloqueia Gate B se houver risco de inflar passos.

### Gate B — timeline 1 Hz

`027-07 -> 027-11`

7. definir contrato/versionamento do algoritmo temporal;
8. implementar BPM tratado + reamostragem 1 Hz;
9. implementar cadência tratada + reamostragem 1 Hz;
10. integrar timeline comum ao detalhe/gráficos e avaliar promoção da cadência 1 Hz para HC;
11. validar fisicamente e fechar documentação.

## Critérios finais de aceite

- passos de atividade podem ser publicados sem elevar o total diário acima do observado;
- ordem atividade -> daily desconta crédito corretamente;
- ordem daily -> atividade não duplica passos;
- retry é idempotente;
- múltiplas atividades não criam crédito duplicado;
- cadência por intervalos é publicada independentemente do crédito;
- resultado Health Sync -> Strava é registrado com evidência factual, sem inferência;
- dados D4 de origem permanecem intactos;
- timeline 1 Hz preserva gaps e distingue estimativa de observação;
- BPM e cadência usam a mesma grade temporal quando ambos existem;
- nenhuma mudança reativa treino phone/GPS/rota;
- mudanças de código/Gradle passam `./gradlew lint assembleDebug assembleRelease`;
- nenhum teste é criado ou executado sem pedido explícito.

## Ordem das tasks

`027-01 -> 027-02 -> 027-03 -> 027-04 -> 027-05 -> 027-06 -> 027-07 -> 027-08 -> 027-09 -> 027-10 -> 027-11`.
