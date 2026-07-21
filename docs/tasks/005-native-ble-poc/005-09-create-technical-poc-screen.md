# Task 005-09 — Criar Tela Técnica Da PoC

## Status

Concluida em 2026-07-21.

## Objetivo

Criar tela técnica, preferencialmente debug-only, para executar manualmente cada etapa da PoC e observar estados seguros.

## Contexto

Fase 05 exige interface temporária para validação física. Ela não pode se tornar a feature final de dispositivo.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/decisions/adr-005-compose-navigation3.md`

## Dependências

- `005-03`, `005-04`, `005-06`, `005-07` e `005-08` concluídas.

## Pré-Condições

- coordenador e estados da PoC disponíveis.

## Entradas

- área `feature/device`;
- design system existente.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/**`, se necessário
- `app/src/androidTest/**`, se UI test for adicionado
- `docs/tasks/005-native-ble-poc/005-09-create-technical-poc-screen.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `docs/roadmap/champion-companion-roadmap.md`

## Escopo

- tela técnica temporária;
- ações manuais seguras;
- exibição de estados técnicos e erro sanitizado;
- restrição preferencial a debug.

## Fora Do Escopo

- tela final de dispositivo;
- UX final de saúde;
- background ou automação de ciclo.

## Passos

1. Criar tela técnica na área de dispositivo.
2. Ligar tela ao coordenador da PoC.
3. Exibir permissões, scan, candidato, conexão, capabilities, handshake, FEA1, F002, D0, estimativa, valor final, duração e erro sanitizado.
4. Desabilitar ações inválidas por estado.
5. Garantir que a tela não mostre MAC, payload, hex, bytes, senha ou stack trace.
6. Adicionar screenshot no PR futuro quando task for executada.
7. Atualizar `Status` e overview.

## Regras De Segurança

- não transformar tela em feature final;
- não expor dados sensíveis;
- não disparar scan ou conexão automaticamente.

## Critérios De Aceite

- usuário consegue acionar manualmente cada etapa prevista;
- ações inválidas ficam bloqueadas ou retornam erro seguro;
- tela é técnica, não definitiva;
- nenhum dado sensível é renderizado.

## Testes

- UI/instrumentado mínimo para estados principais, se necessário.

## Comandos De Validação

```bash
./gradlew app:connectedDebugAndroidTest
```

## Evidências Esperadas

- screenshot da tela técnica no PR futuro;
- estados principais navegáveis sem crash.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar restrição preferencial a debug.
