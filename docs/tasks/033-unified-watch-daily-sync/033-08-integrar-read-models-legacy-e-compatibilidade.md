# 033-08 — Integrar read models, legacy e compatibilidade

- **Status:** pending
- **Depende de:** 033-04 e 033-07

## Objetivo

Fechar consumidores de Home, Saúde, Calendário, Perfil e Exercícios sobre a nova
fonte diária sem quebrar histórico.

## Escopo

- Home mostra FEA1 steps e métricas diárias medidas/completude;
- Saúde/Calendário usam composição D1/D4 local;
- HR histórico continua incluindo manual antigo, D1 e workout;
- métricas antigas continuam reproduzíveis;
- step length legacy permanece somente quando necessário a históricos/fallback;
- remover código de produto morto somente quando não for necessário a leitura ou
  migration;
- documentação/baseline de mapeamentos atualizada.

## Guard rails

- não inventar valor zero para lacuna;
- não apagar tabelas legacy apenas para reduzir código;
- não mudar semântica de snapshots antigos;
- não alterar workflow de workout já sincronizado.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```
