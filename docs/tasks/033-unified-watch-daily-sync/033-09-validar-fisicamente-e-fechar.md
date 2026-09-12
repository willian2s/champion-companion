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
5. Bucket D1 que intersecta D4 não é somado às métricas diárias compartilhadas.
6. Home usa FEA1 para total de passos e distância medida/composição quando
   disponível.
7. Saúde não possui CTA BLE/manual; histórico antigo permanece.
8. Exercícios não possui CTA de importar; workout novo aparece após watch sync.
9. Perfil não expõe calibração por passo.
10. Watch sync não escreve Health Connect automaticamente.
11. `Sincronizar agora` explícito escreve fila elegível.
12. Cutover preserva dia anterior e impede DailySummary STEPS pós-cutover.
13. Partial success preserva componentes já persistidos.
14. Migration real v17 -> v18 preserva dados da 032.

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
