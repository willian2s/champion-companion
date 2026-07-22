# Task 006-11 — Executar validação manual do gerenciamento

## Status

Concluida em 2026-07-22.

## Objetivo

Executar build obrigatório, instalar debug no Galaxy S22 e validar manualmente o fluxo crítico da Fase 06 com Champion C001.

## Contexto

Fase 06 prioriza funcionalidade. Validação obrigatória é compilação debug/release, instalação debug e smoke test manual. `lint` e `testDebugUnitTest` são informativos e não bloqueiam por falhas anteriores não relacionadas.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/_inputs/phase-06-device-management-direction.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`

## Dependências

- `006-03` a `006-10`.

## Entradas

- Galaxy S22 conectado por ADB.
- Champion C001 disponível e carregado.
- Build debug atual.

## Arquivos Exatos Permitidos

- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-11-run-manual-device-management-validation.md`
- `docs/specs/006-device-management.md` somente para registrar ajuste factual de validação, se necessário

## Arquivos Proibidos

- Qualquer arquivo `.kt`.
- Qualquer arquivo Gradle.
- Room schema.
- ADRs, roadmap, baseline, input da fase e fases concluídas.

## Escopo

- Executar comandos obrigatórios.
- Instalar debug no Galaxy S22.
- Executar smoke test manual.
- Registrar comandos, resultados, falhas conhecidas, regressões introduzidas, validação manual e dívida adiada.
- Executar `lint` e `testDebugUnitTest` só se útil e tempo permitir.

## Fora Do Escopo

- Corrigir falhas antigas não relacionadas.
- Rodar campanha ampla de testes.
- Rodar D1/D3/D4/Health Connect.
- Commit/push.

## Passos

1. Ler documentos obrigatórios.
2. Executar `./gradlew assembleDebug`.
3. Executar `./gradlew assembleRelease`.
4. Executar `./gradlew installDebug`.
5. No Galaxy S22, abrir Dispositivo.
6. Solicitar permissões.
7. Iniciar scan.
8. Encontrar Champion C001.
9. Conectar.
10. Confirmar estado conectado.
11. Sair e voltar para tela.
12. Confirmar dispositivo lembrado.
13. Desconectar.
14. Conectar novamente.
15. Desligar Bluetooth.
16. Verificar estado.
17. Ligar Bluetooth.
18. Tentar novamente.
19. Esquecer dispositivo.
20. Confirmar estado vazio.
21. Associar novamente.
22. Abrir diagnóstico.
23. Exportar relatório sanitizado.
24. Opcional: executar `./gradlew lint` e `./gradlew testDebugUnitTest` como informação.
25. Atualizar overview.

## Critérios De Aceite

- `assembleDebug` executado e registrado.
- `assembleRelease` executado e registrado.
- `installDebug` executado e registrado.
- Smoke test manual executado ou bloqueio físico documentado.
- Regressões causadas pela Fase 06 identificadas e encaminhadas para correção antes de fechar.
- Falhas antigas não relacionadas documentadas como dívida.

## Validação Mínima

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

## Evidência Manual Esperada

- Modelo do aparelho: Galaxy S22.
- Resultado de cada passo do smoke test.
- Caminho/nome do relatório exportado ou confirmação visual de export.
- Lista de falhas conhecidas e regressões introduzidas.

## Atualização Do Overview

- Marcar `006-11` como concluída somente após validação ou bloqueio físico aceito.
- Registrar resultados dos comandos.

## Validação Final

- `git status --short`.

## Execucao

- Validacao manual completa da Fase 06 foi confirmada pelo usuario no Galaxy S22 com Champion C001, cobrindo todas as etapas previstas do smoke test e os ajustes corretivos necessarios durante a validacao.
- Por orientacao explicita do usuario, nenhum comando adicional foi rerodado nesta task quando nao era extremamente necessario; esta task ficou restrita ao registro factual da validacao ja concluida.
- Fluxos validados manualmente: permissoes, scan, encontro do Champion C001, conexao inicial, retorno para tela com dispositivo lembrado, desconectar, reconectar, Bluetooth desligado/ligado com retry, esquecer, retorno ao estado vazio, nova associacao, abertura do diagnostico seguro e exportacao de relatorio sanitizado.
- Nenhuma regressao pendente foi deixada aberta apos os ajustes ja aplicados ao longo das tasks `006-08` a `006-10`.

## Evidencia Final

- Aparelho validado: Galaxy S22.
- Dispositivo validado: Champion C001.
- Resultado do smoke test: todas as etapas previstas em `006-11` foram executadas manualmente e concluidas com sucesso pelo usuario.
- Exportacao: relatorio sanitizado confirmado visualmente apos correcao do caminho aceito pelo `FileProvider`.
- Falhas conhecidas nao bloqueantes que permanecem fora de escopo desta task: dividas antigas de testes automatizados ja registradas no overview da fase.

## Validacao Final

- `./gradlew assembleDebug`, `./gradlew assembleRelease` e `./gradlew installDebug` nao foram rerodados nesta task por orientacao explicita do usuario para evitar execucao desnecessaria, considerando que validacao manual completa ja havia sido realizada e os problemas encontrados ja tinham sido corrigidos.
