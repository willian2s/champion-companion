# 027-03 — Publicar passos da atividade no Health Connect

## Objetivo

Adicionar `StepsRecord` como projeção da `WorkoutActivity` quando os passos ainda não estiverem cobertos por um Daily Summary previamente sincronizado.

## Pré-condições

- `027-01` e `027-02` concluídas;
- política de cobertura definida;
- persistência/identidade pronta.

## Elegibilidade

Uma atividade pode publicar passos somente quando:

- possui total de passos válido e positivo;
- janela temporal válida;
- tipo de atividade admite passos;
- não está descartada/inválida;
- ainda não possui projeção de passos aceita;
- não está coberta por Daily Summary já sincronizado segundo a política de `027-01`.

Se cobertura diária anterior existir, marcar estado local equivalente a `ALREADY_COVERED_BY_DAILY` e não escrever `StepsRecord`.

## Mapeamento

Criar `StepsRecord` com:

- `count` derivado do total de passos persistido da atividade;
- janela da atividade ou janela segura definida pelo ADR;
- metadata coerente com origem no relógio;
- `clientRecordId` estável;
- `clientRecordVersion` monotônico;
- payload congelado para retry.

## Crédito

O crédito não nasce ao enfileirar.

Fluxo obrigatório:

```text
operação pendente
  -> escrita remota
  -> Health Connect aceita
  -> operação local marcada como sucesso
  -> crédito identificado fica ativo
```

Se a escrita falhar antes do aceite, nenhum crédito pode reduzir o Daily.

Se o efeito remoto for aceito e ocorrer falha local posterior, preservar identidade para retry/reconciliação sem criar segundo record ou segundo crédito.

## Idempotência

- sincronizar a mesma atividade novamente não duplica passos;
- retry usa mesma identidade congelada;
- reimportação da mesma atividade não cria novo crédito sem nova versão semanticamente válida;
- passos históricos não são promovidos automaticamente.

## Fora de escopo

- descontar crédito do Daily Summary — `027-04`;
- `StepsCadenceRecord` — `027-05`;
- timeline 1 Hz.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não executar testes automatizados sem pedido explícito.
