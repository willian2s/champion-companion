# ADR-010 — D1 local-first e fronteira com Health Connect

## Status

Aceita

## Contexto

Ao fim da Fase 08, app ja entrega hub de Saude, tela especifica de frequencia cardiaca, medicao manual funcional, ultimo BPM persistido na Home e gate foreground compartilhado entre refresh diario e medicao manual.

Auditoria registrada em `009-01` confirmou tambem que codigo atual ja possui base parcial para D1:

- `HeartRateSource.WATCH_D1_HISTORY`, `D1RecordKey` e `HeartRateDeduplicationKey.D1History` no dominio;
- `heart_rate_measurements.deduplicationKey` unico no Room;
- `health_sync_operations` com unicidade por `(aggregateId, recordType)`;
- criacao parcial de `HealthSyncOperationEntity(state = PENDING)` para D1 elegivel novo em `RoomRepositories.import(...)`.

Ao mesmo tempo, roadmap antigo da Fase 09 ainda colocava escrita real no Health Connect dentro da mesma fase. Isso criava dependencia circular:

```text
Fase 09 produz registros D1 locais
        ↓
Fase 12 implementa provider Health Connect real
```

Essa mistura tambem conflita com principios ja aceitos no projeto:

- banco local e fonte operacional primaria;
- UI nao depende de provider externo para mostrar historico;
- falha futura de provider nao deve desfazer dado local valido;
- integracao Health Connect nao substitui repositories locais como fonte visual da area Saude.

## Decisao

Fase 09 passa a ser formalmente owner do fluxo D1 local-first.

### 1. Responsabilidades da Fase 09

Fase 09 deve:

- iniciar D1 somente por acao explicita do usuario;
- consultar historico D1 do dia atual do relogio;
- validar e deduplicar pontos D1;
- persistir localmente todos registros D1 validos;
- criar ou garantir `HealthSyncOperation.PENDING` por medicao D1 elegivel;
- manter historico, grafico, agregacoes e ultima importacao D1 usando fontes locais;
- preservar falhas, lacunas e pendencias sem marcar registros como sincronizados.

### 2. Responsabilidades fora da Fase 09

Fase 09 nao deve implementar:

- `HealthConnectClient`;
- verificacao de disponibilidade do provider;
- permissao Health Connect;
- mapeamento para `HeartRateRecord`;
- escrita real no provider;
- update/delete no provider;
- lotes reais para provider;
- transicoes `SYNCING` ou `SYNCED` para registros D1;
- scheduler de reenvio do provider.

### 3. Responsabilidades da Fase 12

Fase 12 passa a ser owner da integracao real com Health Connect para D1 e demais tipos suportados.

Fase 12 deve:

- processar operacoes D1 pendentes acumuladas localmente;
- solicitar disponibilidade e permissao quando necessario;
- mapear cada medicao valida para record do provider;
- executar escrita idempotente;
- manter falhas parciais como pendentes ou failed para retry seguro;
- nao substituir Room/repositories locais como fonte visual primaria.

### 4. Fonte visual local

Historico, grafico, agregacoes, ultimo BPM e ultima importacao D1 devem ser derivados de repositories locais, nao do estado remoto do Health Connect.

Registros D1 pendentes nao devem aparecer para usuario como enviados apenas porque sao elegiveis para sync futuro.

### 5. Regra de produto preservada

Todos registros D1 com BPM valido continuam elegiveis para envio futuro integral ao Health Connect.

Nao existe selecao manual registro a registro para D1.

### 6. Metadata D1

`DeviceImportMetadata.lastImportedAt` continua sendo metadata generica do dispositivo e nao representa exclusivamente ultima importacao D1.

Decisao adicional de schema/modelo para metadata D1 especifica pode permanecer dentro da mesma linha arquitetural e sera detalhada por `009-03` e `009-05`. Nao e necessario ADR separado neste momento.

## Consequencias

### Positivas

- remove dependencia circular entre Fase 09 e Fase 12;
- protege escopo da Fase 09 como entrega local-first utilizavel;
- preserva banco local como fonte visual primaria de Saude;
- permite fila `PENDING` idempotente antes da integracao real com provider;
- garante que falha futura do Health Connect nao apaga nem invalida dados locais D1.

### Negativas

- exige metadata D1 especifica em vez de reusar `lastImportedAt` generico;
- adiciona etapa documental e de migracao futura para consolidar importacao D1 e fila pendente;
- obriga Fase 12 a consumir backlog local ja produzido por Fase 09.

## Alternativas consideradas

- manter escrita real no Health Connect dentro da Fase 09;
- depender do provider para exibir estado visual primario da tela de frequencia cardiaca;
- tratar `DeviceImportMetadata.lastImportedAt` como se fosse ultima importacao D1;
- adiar fila `PENDING` para mesma fase do provider real.

Alternativas rejeitadas por criarem dependencia circular, acoplarem UI local ao provider externo, esconderem semantica incorreta em metadata generica ou atrasarem regra de produto de elegibilidade integral futura.
