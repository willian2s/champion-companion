# 014-05 — Resolver duração efetiva, índices e plausibilidade

- **Ticker:** `014`
- **Número:** `05`
- **Status:** pending

## Objetivo e resultado esperado

Aplicar a política temporal da ADR-018 aos intervalos existentes, exigir índices
contínuos e únicos e registrar plausibilidade sem redistribuir passos.

Resultado: janelas positivas, ordenadas e sem síntese, com elegibilidade e
plausibilidade prontas para a derivação canônica.

## Dependências

- `014-04` concluída.

## Arquivos permitidos

- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- contratos temporais em `core/model`;
- mappers/repositories de intervalos estritamente necessários;
- documentação de protocolo e overview.

## Arquivos proibidos

- UI, perfil, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- sessões phone e rota histórica.

## Escopo

### Índices

A série V1 exige:

```text
0, 1, 2, ..., N-1
```

São inelegíveis:

- primeiro índice diferente de zero;
- índice ausente;
- índice duplicado;
- ordem inconsistente após normalização autorizada.

Não calcular a posição temporal apenas pela quantidade de linhas quando houver
lacuna.

### Duração

Com `T`, `N` elegível e quantum de 60 segundos:

- `T = N×60`: todos completos;
- `(N−1)×60 < T < N×60`: último recebe o restante;
- `T <= (N−1)×60`: série inelegível;
- `T > N×60`: todos completos e cauda sem bucket.

O exemplo `1.637 s` produz 27 intervalos de 60 s e último de 17 s.

### Plausibilidade

- A duração residual permanece mesmo quando a cadência resultante for alta.
- Registrar `durationResolution = LAST_INTERVAL_RESIDUAL` quando aplicável.
- Calcular indicadores tipados de plausibilidade definidos em `014-02`.
- Não alterar passos, duração ou distância para “corrigir” plausibilidade.
- Projeção granular futura deve poder bloquear série implausível.

## Fora do escopo

- corrigir timestamps de origem sem evidência;
- sintetizar intervalo ausente;
- calcular distância, ritmo ou split;
- escolher novo limiar fora do contrato de `014-02`;
- testes automatizados.

## Passos de implementação

1. Validar índices antes da política `T/N`.
2. Centralizar o resolvedor temporal em função determinística.
3. Aplicar o restante positivo somente ao último intervalo elegível.
4. Preservar cauda sem criar bucket.
5. Persistir duração efetiva, resolução, elegibilidade e plausibilidade.
6. Propagar `endedAt` resolvido sem inflar duração ativa.
7. Garantir convenção temporal consistente para BPM.
8. Atualizar protocolo e overview.

## Critérios de aceite

- Lacuna, duplicata ou primeiro índice não zero bloqueia a série.
- `1.637 s` produz último intervalo de 17 s.
- `T=N×60` mantém todos com 60 s.
- Caso curto falha fechado sem truncar fatos.
- Caso longo preserva a sessão sem inventar intervalo.
- Cadência implausível gera indicador, não redistribuição.
- Janelas válidas são positivas, ordenadas, não sobrepostas e contidas.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- matriz índice → elegibilidade;
- matriz `T/N → duração/resolução`;
- registro sanitizado do exemplo de 1.637 s;
- matriz de plausibilidade;
- builds e smoke local.

## Definição de pronto

Política temporal, índices e plausibilidade persistidos sem síntese, com item
`014-05` marcado.

## Rollback

Desativar o resolvedor para novas derivações e preservar campos raw/measured já
adicionados. Não apagar intervalos.
