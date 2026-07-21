# Task 005-08 — Implementar Estimador Técnico De BPM Por F002

## Status

Concluida em 2026-07-21.

## Objetivo

Implementar estimador técnico de BPM a partir de F002, com score de qualidade e regra final de seleção de valor da sessão.

## Contexto

PoC precisa demonstrar que F002 pode produzir BPM útil sem persistir sinal bruto e sem depender sempre de D0. Implementação deve ser pura, testável e alimentada por fixtures sintéticas.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependências

- `005-07` concluída.

## Pré-Condições

- sessão D0/F002 técnica disponível.

## Entradas

- buffer F002 da sessão;
- duração usada;
- D0 da sessão atual.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`, se modelo técnico compartilhado for necessário
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-08-implement-f002-bpm-estimator.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `docs/protocol/*.md`

## Escopo

- cálculo de sample rate;
- descarte do início instável;
- remoção de baseline;
- suavização;
- detecção de picos;
- filtragem de intervalos;
- score de qualidade;
- seleção do valor final da sessão.

## Fora Do Escopo

- persistência do resultado;
- comparações clínicas;
- UX final de medição.

## Passos

1. Implementar estimador puro conforme parâmetros da spec.
2. Implementar score de qualidade categórico.
3. Implementar regra final `D0 atual -> estimativa válida -> indisponível`.
4. Criar fixtures sintéticas determinísticas.
5. Cobrir sinal válido, dados insuficientes, qualidade baixa e seleção final com testes.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não persistir sinal bruto;
- não reutilizar D0 antigo;
- não inventar BPM quando dados forem insuficientes.

## Critérios De Aceite

- estimador é puro e testável;
- score de qualidade é reproduzível;
- seleção final respeita ordem definida na spec;
- fixtures são sintéticas e sanitizadas.

## Testes

- JVM: sinal sintético válido, insuficiente, qualidade, fallback e valor final.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Estimator*"
./gradlew app:testDebugUnitTest --tests "*F002*"
```

## Evidências Esperadas

- testes determinísticos com fixtures sintéticas.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar ausência de dados reais de saúde commitados.
