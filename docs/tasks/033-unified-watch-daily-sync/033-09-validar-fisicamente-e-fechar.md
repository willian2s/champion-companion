# 033-09 — Validar fisicamente e fechar

- **Status:** pending
- **Depende de:** 033-02 a 033-08

## Objetivo

Validar no Champion C001 e em banco real a linha completa da 033 antes do merge.

## Gates físicos mínimos

1. Home pull-to-refresh executa FEA1 + D1 + D3/D4 numa tentativa composta.
2. Segundo refresh não duplica D1 nem D4.
3. D1 v2 resolve dayOffset/hora e persiste fatos completos.
4. BPM inválido não descarta passos/distância/calorias e não impede conclusão.
5. FEA1 permanece total autoritativo de passos e `Σ D1.steps` é usado como
   cobertura/diagnóstico; D4 não aumenta novamente o total diário.
6. Overlap temporal D1/D4 não causa supressão global de bucket nem soma ingênua;
   autoridade/substituição segue a política por métrica da 033-04.
7. Distância diária usa D1 nativo quando elegível; D4 permanece canônico no
   workout e só substitui contribuição diária com prova de cobertura.
8. FC combinada prefere D4 dentro de workout apenas quando houver cobertura D4
   válida; D1 não é descartado sem cobertura substituta.
9. Saúde não possui CTA BLE/manual; histórico antigo permanece.
10. Exercícios não possui CTA de importar; workout novo aparece após watch sync.
11. Perfil não expõe calibração por passo.
12. Watch sync não escreve Health Connect automaticamente.
13. `Sincronizar agora` explícito escreve fila elegível.
14. Cutover preserva dia anterior, impede DailySummary STEPS pós-cutover e não
    duplica D1 com `WorkoutSteps`/DISTANCE D4.
15. Partial success preserva componentes já persistidos.
16. Migration real v17 -> v18 preserva dados da 032.
17. Cenário `dayOffset > 0` é exercitado ou permanece risco residual explícito se
    não houver dado físico disponível.

## Evidência já disponível

O checkpoint `docs/integration/033-03-d1-v2-physical-validation.md` já confirmou
Room v18, D1 v2, reimportação idempotente, fechamento D1/FEA1 e a necessidade da
política de overlap por métrica. A validação final deve reaproveitar esse cenário e
exercitar a integração restante.

## Evidência

Registrar somente agregados sanitizados, sem payload BLE integral, MAC, IDs
pessoais ou dados desnecessários.

## Validação técnica

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

Ao concluir, atualizar Spec, ADR, overview, baseline e protocolos afetados; marcar
riscos residuais explicitamente antes do merge.
