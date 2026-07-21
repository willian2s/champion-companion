# Task 005-13 — Resolver Achados Físicos E Atualizar Protocolos

## Status

Planejada.

## Objetivo

Corrigir achados da primeira validação física, criar fixtures sanitizadas derivadas e atualizar documentação de protocolo distinguindo inferido de confirmado.

## Contexto

Depois da primeira execução real, divergências de UUID, property, timing, A1, FEA1, D0, F002, stop e disconnect podem exigir ajustes pequenos no core, coordenador ou testes.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-12-run-first-physical-protocol-validation.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependências

- `005-12` concluída com evidências ou bloqueio claro.

## Pré-Condições

- existir lista de achados reais para resolver.

## Entradas

- relatório sanitizado;
- notas da validação física;
- código atual da PoC.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/tasks/005-native-ble-poc/005-13-resolve-physical-findings-and-update-protocol-docs.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`, salvo se task explicitamente concluir que ADR é necessária em trabalho futuro

## Escopo

- corrigir achados físicos relacionados à PoC;
- ajustar testes e fixtures;
- atualizar docs de protocolo com evidência confirmada.

## Fora Do Escopo

- gate final de dez ciclos;
- reabrir escopo para D1/D3/D4.

## Passos

1. Priorizar achados que bloqueiam ciclo completo.
2. Corrigir código minimamente.
3. Ajustar ou adicionar testes e fixtures sanitizadas.
4. Atualizar docs de protocolo distinguindo claramente inferido de confirmado físico.
5. Revalidar fluxo exploratório necessário.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não commitar dados reais de saúde;
- não reescrever silenciosamente evidência anterior;
- não ampliar escopo fora da PoC.

## Critérios De Aceite

- bloqueios principais da primeira validação física foram resolvidos ou isolados;
- docs de protocolo refletem evidência física nova;
- fixtures derivadas permanecem sanitizadas.

## Testes

- testes automatizados afetados pela correção;
- reexecução física dirigida quando necessário.

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
git status --short
```

## Evidências Esperadas

- lista de achados resolvidos;
- docs atualizados;
- fixtures sanitizadas.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar que nenhum dado real sensível entrou em docs ou fixtures versionadas.
