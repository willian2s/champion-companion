# 029-03 — Implementar períodos e filtros por esporte

- **Ticker:** `029`
- **Número:** `03`
- **Status:** `pending`
- **Depends on:** `029-02`

## Objetivo

Implementar janelas temporais e filtros que definem a população usada por todas as agregações da Spec 029.

## Escopo

- períodos 7, 30 e 90 dias;
- período anterior equivalente para comparação;
- filtro consolidado e por tipo elegível;
- limites temporais determinísticos no timezone local vigente;
- ordenação estável das atividades;
- exclusão coerente de atividades inelegíveis.

## Regras

- o período atual e o anterior não se sobrepõem;
- filtros são aplicados antes das agregações;
- modalidades com semântica incompatível não são misturadas em métricas específicas;
- atividade antiga entra no período correspondente à data real da atividade, não à data de importação.

## Critérios de aceite

1. 7/30/90 dias retornam populações determinísticas.
2. Comparação usa janela anterior do mesmo tamanho.
3. Filtro por esporte é aplicado antes dos cálculos.
4. Data de importação não desloca atividade antiga para período incorreto.
5. Estados vazios não geram valores sintéticos.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```
