# 014-11 — Reprocessar atividades existentes explicitamente

- **Ticker:** `014`
- **Número:** `11`
- **Status:** pending

## Objetivo e resultado esperado

Oferecer ação explícita e idempotente para derivar novamente atividades
`WATCH_D3_D4` existentes com o contrato atual, incluindo legado `d4-v1` quando o
raw for recuperável.

Resultado: atividades antigas podem receber novo snapshot sem mutação automática,
perda de revisão ou sincronização implícita.

## Dependências

- `014-08` concluída.

## Arquivos permitidos

- contratos/repository de derivação e reimportação;
- `RoomRepositories.kt` e DAOs necessários;
- `ExercisesViewModel.kt` e superfície de detalhe/lista para ação explícita;
- recursos de texto, screenshot e overview.

## Arquivos proibidos

- parser/protocolo, schema/migration e Health Connect writer;
- Gradle e manifesto;
- atividades phone, GPS, rota e WorkManager.

## Elegibilidade

### `d4-v2`

Usar raw/measured persistidos e o contrato de derivação vigente.

### `d4-v1`

Quando a migration de `014-03` preservou o valor legado como `rawMotionValue`:

```text
decodedSteps = rawMotionValue ushr 16
```

O reprocessamento cria measured/derived atuais sem alterar o valor legado.

### Falha fechada

- raw necessário ausente;
- parser version desconhecida;
- índices inelegíveis;
- perfil sem `stepLengthMeters` válido;
- inconsistência temporal não resolvível.

Não exigir BLE quando os fatos locais forem suficientes. Não reconstruir raw
com heurística não versionada.

## Preservação obrigatória

- `recordedAt`;
- revisão e tipo confirmado;
- título e observações;
- calibração histórica;
- raw e import identity;
- workflow compatível;
- operações Health Connect existentes até ação explícita futura.

Para `TREADMILL` de relógio, aplicar o contrato canônico vigente sem apagar a
calibração histórica.

## Fora do escopo

- lote silencioso, abertura do app ou scheduler;
- reparse remoto sem payload local;
- alterar distância original/raw;
- sincronizar ou reconciliar Health Connect;
- testes automatizados.

## Passos de implementação

1. Listar atividades sem snapshot atual ou com versão antiga.
2. Expor ação com resumo e confirmação explícita.
3. Resolver raw/measured conforme parser version conhecida.
4. Aplicar decoder atual a `d4-v1` recuperável.
5. Criar snapshot transacional com versão atual.
6. Preservar campos de usuário e ordem temporal da ADR-016.
7. Não preparar nem enviar operações Health Connect automaticamente.
8. Para `SYNCED`, exigir aviso específico antes de qualquer projeção futura.
9. Tornar repetição com mesmos inputs/versão idempotente.
10. Registrar screenshots e atualizar overview.

## Critérios de aceite

- Nenhuma atividade muda sem confirmação.
- `d4-v1` recuperável pode ser derivada sem BLE.
- Parser version desconhecida falha com motivo seguro.
- Mesmos inputs e versão não criam revisão inútil.
- Revisão, `recordedAt`, título, notas e calibração permanecem.
- Atividade sem perfil continua sem derivação.
- Reprocessamento não acessa Health Connect nem ressincroniza.
- `TREADMILL` usa a nova série sem perder histórico de calibração.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots da confirmação e resultado;
- matriz parser version/estado → resultado;
- registro sanitizado de preservação de campos;
- repetição idempotente e reinício do app.

## Definição de pronto

Reprocessamento explícito, idempotente e local funciona para legado recuperável;
item `014-11` marcado.

## Rollback

Ocultar a ação e parar novos snapshots. Preservar snapshots já criados e não
alterar estados remotos.
