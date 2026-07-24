# Task 012-06 - Implementar gateway Health Connect e mapeadores de passos, BPM e perfil

## Status

Planejada.

## Objetivo

Implementar o gateway Health Connect e os mapeadores de `StepsRecord`, `HeartRateRecord`, `WeightRecord` e `HeightRecord` com IDs estáveis, versões monotônicas e política write-only.

## Contexto

Depois de estabilizar Android base e fila persistida, a fase precisa mapear os agregados simples que não dependem de workflow de atividade explícita.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-03-add-health-connect-sdk-manifest-rationale-and-availability.md`
- `docs/tasks/012-native-health-connect/012-05-implement-sync-queue-reconciliation-and-repository-contracts.md`

## Dependências

- `012-03`
- `012-05`

## Entradas

- SDK e manifesto já integrados;
- fila persistida e contratos estabilizados.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-06-implement-health-connect-gateway-and-steps-heart-profile-mappers.md`
- novos arquivos Kotlin de gateway, mapper e Hilt estritamente relacionados ao Health Connect
- arquivos Kotlin existentes de infraestrutura necessários ao wiring

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `feature/home/**`
- `feature/health/**`
- `feature/exercises/**`
- `core/database/**`

## Escopo

- implementar `HealthConnectGateway` Android-free e implementação Android concreta;
- mapear `DailySummary -> StepsRecord`;
- mapear `MANUAL_MEASUREMENT` e `WATCH_D1_HISTORY -> HeartRateRecord`;
- mapear dados locais existentes de peso e altura;
- aplicar `clientRecordId` e `clientRecordVersion` estáveis.

## Fora do escopo

- mapeadores de atividade;
- coordinator global;
- UI de permissões;
- triggers pós-persistência.

## Passos

1. Implementar gateway com disponibilidade, permissões concedidas e insert.
2. Implementar mapeadores de passos, BPM, peso e altura.
3. Aplicar política de IDs e versões.
4. Atualizar spec e overview.

## Critérios de aceite

- gateway encapsula `HealthConnectClient`;
- passos usam um `StepsRecord` por resumo diário;
- BPM manual e D1 usam uma operação por medição válida;
- peso e altura só geram records quando houver dado elegível.

## Validação manual

- revisão de mapeamento e metadados sem executar provider real.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- lista de classes de gateway;
- lista de mapeadores;
- diff de Hilt e wiring de infraestrutura.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-06` como concluída;
- registrar mapeadores simples estabilizados.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
