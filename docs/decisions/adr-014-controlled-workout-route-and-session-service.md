# ADR-014 — Rota controlada de treino e serviço da sessão ativa

## Status

Aceita

## Data

29/07/2026

## Contexto

A Fase 12 foi concluída sem `ExerciseRoute`, localização persistida ou
foreground service. A Fase 13 precisa permitir exercícios iniciados pelo
celular, recuperar sessão após morte de processo e, para caminhada/corrida,
preservar rota local para revisão e sincronização. A regra histórica de
descartar coordenadas da ADR-004 tornou-se ampla demais para esse escopo, mas
continua correta para logs, backup, exportação, D3/D4 e fases sem autorização.

O `Phase003PrivacyGuardTest` ainda bloqueia termos de localização/rota nos
escopos que a Fase 13 precisará alterar. Ele é dívida conhecida, não deve ser
alterado, contornado ou executado como validação desta decisão.

## Decisão

### 1. Escopo explícito da rota

Rota só existe quando todas as condições forem verdadeiras:

- usuário inicia explicitamente caminhada ou corrida;
- sessão passa por preparação e countdown cancelável;
- captura ocorre entre início efetivo e fim, pausa ou descarte;
- localização precisa foi concedida e serviços de localização estão ativos;
- sessão pertence ao Champion Companion e possui `AppWorkoutSession` como
  owner operacional.

Treino livre e esteira não solicitam, coletam nem persistem localização. Não há
rastreamento passivo, captura por boot, coleta fora da sessão, mapa, tiles,
servidor, exportação automática, GPX, TCX ou FIT.

### 2. Persistência, retenção e ownership

Pontos aceitos são persistidos incrementalmente ou em lotes pequenos. Cada
ponto mantém sequência determinística, timestamp, coordenadas, altitude e
qualidades opcionais, velocidade/bearing opcionais e segmento. Distância é
calculada somente sobre pontos aceitos; lacunas não viram linha reta.

Ownership é:

```text
AppWorkoutSession 1 ─── 0..1 WorkoutRoute 1 ─── N WorkoutRoutePoint
AppWorkoutSession 0..1 ─── 1 WorkoutActivity após finalização
```

A rota não possui owner paralelo direto na atividade. Associação sessão →
atividade conduz a uma única atividade final. Descarte antes da criação da
atividade remove rota e amostras transitórias localmente. O modelo não usa
JSON, blob ou polyline opaca.

Persistência local fica em armazenamento privado do app. `allowBackup=false`,
backup rules e device transfer negados permanecem obrigatórios. Não haverá
sincronização com servidor nem exportação de rota.

### 3. Foreground service mínimo

O foreground service é owner operacional da sessão ativa, não da tela. Inicia
ao fim do countdown enquanto a Activity está visível, usa tipo `location` para
caminhada/corrida e `connectedDevice` somente quando houver BLE contínuo
autorizado. Não solicitar `ACCESS_BACKGROUND_LOCATION` por padrão.

O serviço não é exportado, não possui boot receiver, não inicia sessão após
reboot e prefere `START_NOT_STICKY`. Notificação persistente mostra apenas
estado operacional sanitizado: tipo, duração, pausa, GPS em estado categórico,
relógio, abrir app e ações idempotentes. Finalização e descarte exigem
confirmação segura na UI.

Após morte de processo, estado persistido sem runtime vivo vira
`AWAITING_RECOVERY`. Abrir o app mostra recuperação para decisão do usuário;
retomar cria nova geração e segmento. Nenhum serviço é reiniciado
automaticamente por reboot.

### 4. Privacidade e logs

Logs diagnósticos existem somente em debug e podem conter evento operacional,
`error_code`, estado categórico, tipo de operação e correlação opaca não
pessoal, quando necessária para investigar uma tentativa. Nunca contêm
latitude, longitude, altitude, precisão, velocidade, bearing, rota, sequência
de pontos, BPM, payload BLE, tokens, credenciais, identificadores pessoais,
título ou observações.

Notificação, mensagens de erro, exportações e telas fora do detalhe autorizado
também não exibem coordenadas. O detalhe pode mostrar presença da rota,
contagem de pontos, segmentos, distância e status de sync, sem mapa ou tiles.

### 5. Proveniência e correlação D3/D4

Fontes têm prioridade explícita:

| Dado | Fonte prioritária |
|---|---|
| tipo, início, fim, duração, GPS, rota, distância, ritmo, velocidade, calorias, passos e intervalos | sessão do celular |
| BPM real | amostras do relógio, quando associação temporal e transporte forem seguros |
| título e observações | revisão do usuário |
| CRC, índice, parser e metadata de importação | `WatchWorkoutImport` |

Antes de criar atividade independente para D3/D4, procurar atividade da sessão
do celular com início em janela absoluta de até 60 segundos, sobreposição
relevante, candidato único e mesmo dispositivo quando dispositivo estiver
associado. Correlação segura preserva somente a atividade phone, vincula a
importação para proveniência e deduplicação, mantém CRC e metadata e associa
somente BPM real elegível. Não copia passos, calorias, distância, tempo, tipo,
ritmo, velocidade, intervalos ou rota do relógio. Nunca substitui métricas do
celular.

CRC repetido permanece idempotente. Sem candidato seguro, cria-se atividade
independente. Ambiguidade não faz merge silencioso e permanece pendente para
revisão com diagnóstico sanitizado.

Quando houver candidato phone seguro, o import D3/D4 do relógio não cria nem
mantém uma segunda atividade. A retenção técnica de `WatchWorkoutImport` não
constitui merge de métricas: ela existe para CRC, proveniência e reimportação
idempotente. Ausência de BPM elegível não autoriza fallback de qualquer outra
métrica do relógio.

### 6. Retry e finalização

Persistência local ocorre antes de qualquer provider externo. A finalização
para captura, grava lote final, consolida métricas, cria atividade, associa
rota/amostras, finaliza sessão, encerra serviço e abre revisão. Falha local
mantém estado recuperável e não duplica atividade.

Retry usa a mesma identidade local, operação idempotente e limite explícito.
Captura ativa não usa WorkManager. Retentativas da sessão ativa são controladas
pelo serviço e pela UI; tarefas adiáveis posteriores pertencem à Fase 14.

### 7. Health Connect e identidade

`ExerciseRoute` é projeção da mesma `ExerciseSessionRecord`, nunca um segundo
record de sessão. A sessão usa identidade canônica determinística derivada de
`applicationId + aggregateType + aggregateId + EXERCISE_SESSION`; rota,
distância, calorias e BPM usam operações complementares quando elegíveis.

Sessão base é enviada primeiro. Enriquecimento com rota usa o mesmo
`clientRecordId` e `clientRecordVersion` monotônico persistido, maior que a
versão anterior. Sem `WRITE_EXERCISE_ROUTE`, sessão e complementos autorizados
podem sincronizar e rota local fica pendente. Concessão posterior atualiza a
mesma sessão; retry não apaga sessão, métricas ou rota e nenhum upsert futuro
pode rebaixar sessão que já possui rota local elegível.

O complemento `DISTANCE` usa records temporais por intervalo, com delta
positivo, janela válida, ordem determinística e IDs próprios. Sessão phone usa
somente deltas da série phone; watch-only usa intervalos D3/D4 elegíveis; uma
atividade correlacionada nunca usa total ou intervalos D3/D4 para distância.
Correção de uma projeção agregada legada deve remover ou substituir somente o
record exato do app antes de inserir a série, sem apagar dados de terceiros e
sem deixar agregado e intervalos somando em duplicidade.

A Fase 12 e atividades D3/D4 permanecem sem rota. A permissão de rota só é
considerada para a sessão phone de caminhada/corrida autorizada nesta ADR.

## Consequências

### Positivas

- permite recuperação e retry da rota sem criar segundo owner ou record;
- mantém Fase 12, D3/D4, treino livre e esteira sem localização;
- mantém backup, device transfer, logs e exportação protegidos;
- separa owner operacional da sessão da UI;
- preserva proveniência do relógio e evita merge ambíguo.

### Negativas e riscos

- schema e repositórios passam a lidar com dados sensíveis de localização;
- `Phase003PrivacyGuardTest` ficará incompatível até dívida ser tratada em
  decisão própria;
- provider Health Connect pode negar rota ou impor limites de tamanho;
- recuperação depende de checkpoints pequenos e validação física;
- retenção e deleção precisam permanecer cobertas por contrato e revisão.

## Alternativas consideradas

- **Descartar coordenadas após calcular métricas:** rejeitada porque impede
  recuperação, revisão de rota e retry de `ExerciseRoute`.
- **Persistir rota somente em memória:** rejeitada porque morte de processo
  perderia sessão ativa e tornaria retry não determinístico.
- **Usar `WorkManager` para sessão ativa:** rejeitada porque captura contínua
  exige foreground service iniciado visivelmente; WorkManager permanece na
  Fase 14 para tarefas adiáveis.
- **Criar segundo `ExerciseSessionRecord` para rota:** rejeitada porque
  duplicaria a sessão remota.
- **Correlacionar D3/D4 somente por horário:** rejeitada por risco de falso
  merge; janela, sobreposição, unicidade e dispositivo são necessários.
- **Permitir rota para todos os tipos ou rastreamento passivo:** rejeitada por
  ampliar coleta além da autorização explícita.
