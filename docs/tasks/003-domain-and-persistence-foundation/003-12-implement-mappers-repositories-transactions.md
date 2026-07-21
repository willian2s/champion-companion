# Task 003-12 — Implementar Mapeadores, Repositories e Transações

## Status

Concluída.

## Dependências

- `003-10` e `003-11` concluídas.

## Objetivo

Implementar conversões explícitas e casos compostos atômicos entre contratos, Room e DataStore.

## Pode modificar

- `data/**`, mapeadores, módulos Hilt, testes JVM/Room correspondentes.
- Este arquivo.

## Passos

1. Criar mapeadores entity ↔ domínio e entrada parser ↔ domínio; manter nulidade, proveniência, tempo e valores ausentes sem default inventado.
2. Implementar repositories por agregado com dispatchers/coroutines adequados; expor `Flow`/suspensão conforme contrato, sem classes Room.
3. Executar em transação: payload + derivados; atividade + intervalos; revisão + métricas; calibração; finalização/recuperação; sync com sucesso/falha parcial; reprocessamento.
4. Reimportação D3/D4 atualiza parser e diagnósticos preservando confirmação, revisão, calibração e descarte.
5. Validar sujeito de sync no repository e gravar operação na mesma transação; não criar FK polimórfica.
6. Limpar payloads por retenção sem remover derivados; nunca logar conteúdo sensível.
7. Testar rollback, idempotência, edição preservada, falha parcial BPM e limpeza.

## Aceite

- Operações compostas não deixam estado parcialmente gravado.
- Camada data mantém separação domínio/Room/parser/DataStore.
