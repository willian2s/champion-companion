# 014-04 — Corrigir parser D4 e incrementar `parserVersion`

- **Ticker:** `014`
- **Número:** `04`
- **Status:** pending

## Objetivo e resultado esperado

Corrigir a decodificação de passos temporais, preservar os valores raw e publicar
uma nova versão de parser, inicialmente `d4-v2`.

Resultado: novas importações distinguem claramente o parser corrigido do legado
`d4-v1`, e a sequência observada soma `3.737` passos sem atribuir semântica aos
bits inferiores.

## Dependências

- `014-03` concluída.

## Arquivos permitidos

- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- contratos D4 estritamente necessários;
- coordinator/importação para selecionar `d4-v2`;
- mappers/repository para gravar raw e measured;
- `docs/protocol/workouts-d3-d4.md`;
- overview.

## Arquivos proibidos

- UI, perfil e Health Connect;
- Gradle e manifesto;
- migrations/schemas após `014-03`, salvo bloqueio documentalmente revisado;
- infraestrutura phone/Fase 013.

## Escopo

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

- preservar `rawMotionValue`;
- persistir `decodedSteps` separadamente;
- preservar raw de distância e calorias temporais;
- interromper promoção desses campos raw a métricas semânticas;
- manter BPM, ordenação, duplicatas e parcialidade existentes;
- usar `parserVersion = d4-v2` em novas importações/reimportações corrigidas.

## Fora do escopo

- duração efetiva e plausibilidade;
- distância canônica;
- interpretação dos 16 bits inferiores;
- backfill automático de atividades antigas;
- testes automatizados.

## Passos de implementação

1. Ler u24 uma única vez como `rawMotionValue`.
2. Derivar passos pelo deslocamento de 16 bits.
3. Persistir raw e measured nos contratos de `014-02/03`.
4. Preservar distância/calorias temporais somente como raw.
5. Incrementar a versão do parser para `d4-v2` em todos os pontos de criação.
6. Garantir que `d4-v1` continue identificável em dados legados.
7. Não substituir silenciosamente os passos totais do pacote 2.
8. Atualizar protocolo e overview.

## Critérios de aceite

- `0x0064002B` produz raw `6.553.643` e `100` passos.
- A sequência de referência soma exatamente `3.737`.
- Os 16 bits inferiores permanecem desconhecidos.
- Valores raw sobrevivem ao round-trip Room.
- Novas importações registram `d4-v2`.
- Dados `d4-v1` não são reclassificados sem reprocessamento explícito.
- Totais da atividade não são silenciosamente substituídos.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela sanitizada índice → raw → passos;
- soma de referência;
- parser version antes/depois;
- builds e smoke de importação.

## Definição de pronto

Parser `d4-v2` persiste raw/measured corretamente, preserva legado identificável
e item `014-04` está marcado.

## Rollback

Interromper uso de `d4-v2` sem apagar campos raw já persistidos. Não voltar a
tratar o u24 completo como passos canônicos.
