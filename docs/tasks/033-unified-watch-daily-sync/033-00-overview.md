# 033 — Overview: sincronização unificada e dados diários granulares

- **Spec:** `docs/specs/033-unified-watch-daily-sync.md`
- **ADR:** `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`
- **Status:** `in_progress`
- **Progresso:** `1/9`
- **Branch de integração:** `feat/033-unified-watch-daily-sync`

## Objetivo

Unificar FEA1, D1 e D3/D4 em um único watch sync iniciado pela Home, promover D1
completo para fatos diários granulares, compor D1/D4 sem sobreposição e fazer
cutover seguro da projeção Health Connect diária sem reescrever histórico.

## Sequência

```text
033-01 Auditoria e contratos                         ✓
   |\
   | +--> 033-02 Parser/collector D1 v2
   | +--> 033-03 Persistência D1 / Room v18
   |          |
   +----------+
              v
033-04 Composição diária FEA1 + D1 + D4
              |
       +------+------+
       |             |
       v             v
033-05 Watch sync   033-06 HC cutover/granular
       |             |
       +------+------+
              v
033-07 Produto/UI e remoção de entrypoints BLE
              |
033-08 Read models, legacy e compatibilidade
              |
033-09 Validação física e fechamento
```

## Checklist

- [x] `033-01-auditar-e-congelar-d1-v2-e-cutover.md`
- [ ] `033-02-implementar-parser-collector-d1-v2.md`
- [ ] `033-03-persistir-fatos-diarios-granulares.md`
- [ ] `033-04-compor-dados-diarios-fea1-d1-d4.md`
- [ ] `033-05-unificar-watch-sync-na-home.md`
- [ ] `033-06-integrar-health-connect-granular-e-cutover.md`
- [ ] `033-07-remover-entrypoints-ble-e-calibracao-passo-da-ui.md`
- [ ] `033-08-integrar-read-models-legacy-e-compatibilidade.md`
- [ ] `033-09-validar-fisicamente-e-fechar.md`

## Decisões congeladas em 033-01

- Home pull-to-refresh será o único entrypoint de produto para leitura do relógio;
- FEA1 = total diário atual de passos/validação;
- D1 = fatos granulares diários fora de workouts;
- D4 = autoridade dentro de workouts;
- D1 v2 preserva passos, distância, calorias, sportValue e raw adicionais mesmo
  com BPM inválido;
- BPM D1 v2 válido em 30..200;
- bucket D1 que intersecta D4 é inicialmente suprimido por inteiro;
- sync do relógio não executa Health Connect automaticamente;
- cutover HC ocorre por dia local inteiro e não reescreve histórico;
- step length sai da UI, mas contratos legacy/fallback permanecem;
- medição manual de FC e importação D1 saem da Saúde como ações de produto;
- importação D3/D4 sai de Exercícios como ação de produto;
- semântica Health Connect de calorias permanece bloqueada até evidência.

## Paralelismo

Depois de 033-01, `033-02` e `033-03` podem andar em worktrees separados. A
composição `033-04` depende de ambos.

## Guard rails

- não criar testes automatizados sem pedido explícito;
- não introduzir GPS/rota/mapa de produção;
- não remover dados históricos de medição manual, D1, step length ou workout;
- não alterar atividades `SYNCED` silenciosamente;
- não criar efeitos Health Connect em migration/watch sync;
- não fabricar fatos diários antigos que nunca foram persistidos;
- não usar header total D4 como fonte de comportamento;
- cada task atualiza este overview.
