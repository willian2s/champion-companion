# 014-07 — Derivar série e distância canônicas

- **Ticker:** `014`
- **Número:** `07`
- **Status:** pending

## Objetivo e resultado esperado

Criar snapshot de derivação V1 por atividade `WATCH_D3_D4` e calcular uma única
distância canônica a partir dos intervalos elegíveis.

Resultado: distância reproduzível, ausente quando a série for inelegível e
independente da distância original, calibrada histórica ou estimativa agregada.

## Dependências

- `014-05` e `014-06` concluídas.

## Arquivos permitidos

- contratos/serviço de derivação em `core/model`;
- `RepositoryContracts.kt`;
- mappers e `RoomRepositories.kt` para persistência transacional;
- consumidores mínimos necessários para expor ausência/canônico;
- overview.

## Arquivos proibidos

- parser/protocolo já fechado;
- UI final, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- infraestrutura phone/rota.

## Escopo

### Snapshot

Congelar:

```text
stepLengthMetersUsed
stepLengthSource
derivationMethod = FIXED_PERSONAL_STEP_LENGTH
derivationVersion
derivedAt
temporalEligibility
plausibility
```

### Série canônica

```text
intervalDistance = decodedSteps × stepLengthMetersUsed
canonicalDistance = soma exata dos intervalDistance
```

- Somar com precisão interna sem arredondamento prematuro.
- Série temporal inelegível mantém distância canônica ausente.
- Intervalo zero produz distância zero.
- Plausibilidade não modifica a fórmula.

### Estimativa agregada diagnóstica

`totalSteps × stepLengthMetersUsed` pode ser calculado apenas como diagnóstico
com nome distinto. Não deve alimentar nenhum consumidor funcional.

### Workflow de esteira

Para origem `WATCH_D3_D4`, confirmar tipo `TREADMILL` pode usar a série canônica
e avançar para `READY_TO_SYNC` sem exigir calibração histórica. Calibração
existente permanece preservada e não substitui a série.

## Fora do escopo

- cadência, velocidade, ritmo e splits;
- reprocessamento em lote de legado;
- calorias derivadas;
- UI final e Health Connect;
- testes automatizados.

## Passos de implementação

1. Resolver perfil elegível no instante da derivação.
2. Congelar valor, origem, método, versão e estado temporal.
3. Validar que a série é elegível antes de calcular canônico.
4. Calcular cada delta exclusivamente por passos decodificados.
5. Somar os deltas para obter a única distância canônica.
6. Persistir snapshot e série atomicamente.
7. Manter estimativa por passos totais em campo diagnóstico separado, quando
   útil.
8. Ajustar a transição de `TREADMILL` somente para `WATCH_D3_D4`.
9. Não substituir snapshot existente sem ação explícita.
10. Atualizar overview.

## Critérios de aceite

- Fórmula do intervalo é exatamente passos × `stepLengthMeters`.
- Distância da atividade é exatamente a soma dos intervalos.
- Não existe fallback para original, calibrada ou estimativa agregada.
- Série inelegível mantém distância canônica ausente.
- Mudança de perfil não altera snapshot antigo.
- Plausibilidade não redistribui passos.
- Zero passos produz zero local.
- `TREADMILL` de relógio não fica bloqueada por calibração histórica quando a
  série for elegível.
- Contratos phone históricos não mudam.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- cálculo manual sanitizado;
- comparação soma temporal = distância canônica;
- snapshot antes/depois de edição do perfil;
- matriz de workflow de esteira;
- builds e round-trip Room.

## Definição de pronto

Série e distância canônicas persistem atomicamente, workflow de esteira está
alinhado e item `014-07` está marcado.

## Rollback

Parar criação/consumo de snapshots e retornar ausência canônica; preservar dados
derivados para diagnóstico. Nunca restaurar distância original como fallback.
