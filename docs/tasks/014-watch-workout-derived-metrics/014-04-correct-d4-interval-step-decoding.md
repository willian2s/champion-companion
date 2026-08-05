# 014-04 — Corrigir decodificação de passos dos intervalos D4

- **Ticker:** `014`
- **Número:** `04`
- **Status:** pending

## Objetivo e resultado esperado

Decodificar passos temporais pelo byte superior do u24 e preservar valor bruto
integral e campos temporais não comprovados. Resultado: sequência observada soma
`3.737` passos sem atribuir semântica aos bits inferiores.

## Contexto

`ChampionD4WorkoutParser.parseSamplePacket` usa hoje todo
`readUInt24LittleEndian(7)` como `stepsDelta`.

## Requisitos cobertos

- R3;
- critério de aceite da sequência de referência.

## Dependências

- `014-03` concluída.

## Arquivos permitidos

- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- contratos D4 estritamente necessários;
- mappers/repository de importação para gravar raw e measured;
- `docs/protocol/workouts-d3-d4.md`;
- overview.

Símbolos prováveis: `parseSamplePacket`, `SampleData`, `WatchWorkoutParserInput`,
`WorkoutInterval`/contrato measured e persistência de intervalos.

## Arquivos proibidos

- UI, perfil, Health Connect, Gradle, manifesto;
- migrations e schemas após conclusão de `014-03`, salvo correção bloqueadora
  revisada na própria task;
- infraestrutura phone/Fase 013.

## Escopo

- `rawMotionValue`;
- `decodedSteps = rawMotionValue ushr 16`;
- preservação raw de distância e calorias temporais;
- validação de limites e parcialidade existente.

## Fora do escopo

- duração efetiva;
- distância canônica;
- interpretação do componente inferior;
- novos testes automatizados.

## Passos de implementação

1. Ler u24 uma única vez como `rawMotionValue`.
2. Derivar passos por deslocamento de 16 bits.
3. Persistir raw e decoded nos contratos definidos.
4. Parar de promover distância/calorias temporais raw como derivadas.
5. Preservar regras de BPM, ordenação, duplicatas e `PARTIAL`.
6. Atualizar protocolo com classificação explícita.
7. Atualizar overview.

## Critérios de aceite

- Exemplo `0x0064002B` produz raw `6.553.643` e 100 passos.
- Sequência fornecida soma exatamente `3.737`.
- 16 bits inferiores ficam sem nome semântico.
- Valores raw sobrevivem ao round-trip Room.
- Totais da atividade não são silenciosamente substituídos.

## Validação manual

- Importar ou reprocessar atividade de referência disponível.
- Comparar os 28 passos decodificados e soma.
- Inspecionar detalhe/diagnóstico sem expor payload em log.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela sanitizada índice → passos decodificados;
- soma de referência;
- builds e smoke de importação.

## Definição de pronto

Parser e persistência usam passo decodificado, preservam raw e item `014-04` está
marcado.

## Riscos e cuidados

- Aplicar shift somente a pacotes 4+, não aos offsets dos pacotes 1–3.
- Não alterar heurística de layout sem evidência adicional.
- Reimportação não pode apagar revisão.

## Rollback

Restaurar decoder anterior somente antes de dados novos; se schema já contém raw,
preservar colunas e interromper uso do decoder sem apagar valores.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-04` e registrar evidência da soma.
