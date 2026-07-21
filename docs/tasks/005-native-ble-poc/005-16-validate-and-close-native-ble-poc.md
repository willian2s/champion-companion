# Task 005-16 — Validar E Encerrar Prova De Conceito BLE Nativa

## Status

Planejada.

## Objetivo

Consolidar evidências da Fase 05, revisar rastreabilidade final, fechar checklist e registrar se Gate A foi aprovado ou bloqueado.

## Contexto

Fechamento final precisa combinar validação automatizada, evidência física e atualização documental sem reabrir escopo funcional.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- todas tasks `005-01` a `005-15`
- docs de protocolo atualizados pela fase, se houver

## Dependências

- `005-15` concluída.

## Pré-Condições

- gate final executado ou bloqueio explícito documentado.

## Entradas

- código da PoC;
- testes automatizados;
- evidências físicas e relatórios sanitizados.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-16-validate-and-close-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist e status final
- `docs/specs/005-native-ble-poc.md`, somente se fechamento exigir ajuste factual

## Arquivos Proibidos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`, salvo trabalho futuro explícito

## Escopo

- validar critérios finais da spec;
- consolidar evidências automatizadas e físicas;
- fechar checklist da fase;
- listar ADRs pendentes e lacunas residuais.

## Fora Do Escopo

- commit;
- push;
- novas features fora da PoC.

## Passos

1. Revisar checklist, rastreabilidade e critérios de aceite da spec.
2. Executar validações automatizadas finais necessárias.
3. Consolidar evidências físicas e status do Gate A.
4. Atualizar overview com checklist final.
5. Atualizar `Status` desta task com resumo conclusivo.

## Regras De Segurança

- não declarar fase concluída sem status claro do Gate A;
- não apagar divergências ou bloqueios residuais;
- não tratar fake como validação física.

## Critérios De Aceite

- checklist final consolidado;
- Gate A aprovado ou bloqueio final explicitado;
- arquivos alterados e evidências resumidos;
- fase fica executável e auditável sem contexto externo.

## Testes

- `lint`
- `testDebugUnitTest`
- `assembleDebug`
- `assembleRelease`
- `connectedDebugAndroidTest`, se aplicável

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
git status --short
```

## Evidências Esperadas

- resumo final da arquitetura PoC, state machine, A1, FEA1, D0/F002, estimador, permissões e relatório;
- resultado formal do Gate A;
- lista de ADRs potenciais e lacunas remanescentes.

## Atualização Documental

- atualizar `Status`;
- consolidar checklist de `005-00`.

## Validação Final

- confirmar ausência de commit e push;
- confirmar se roadmap, ADRs e fases anteriores permaneceram intactos.
