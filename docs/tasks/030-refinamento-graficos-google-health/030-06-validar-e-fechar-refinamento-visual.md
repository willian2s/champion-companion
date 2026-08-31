# 030-06 — Validar e fechar refinamento visual

- **Ticker:** `030`
- **Número:** `06`
- **Status:** `pending`
- **Depends on:** `030-05`

## Objetivo e resultado esperado

Consolidar inventário, validar todas as superfícies visuais revisadas, remover
somente legado comprovadamente sem uso e fechar documentação da revisão integral
inspirada no Google Health/Fitbit.

## Requisitos e critérios cobertos

- R1–R6; todos os critérios da Spec 030.

## Escopo incluído

- reauditoria de shell, Home, Saúde, Calendário, Exercícios, Evolução,
  Dispositivo, Perfil e Configurações;
- busca final de Canvas/Vico, imports e consumidores;
- smoke integrado de fluxos, estados, controles e gráficos;
- evidências sanitizadas, atualização da Spec, overview e subtarefas.

## Escopo excluído

- remoção de APIs cobertas por previews/testes sem decisão explícita;
- refactor funcional, engine nova, testes automatizados ou novas telas;
- mudanças em dados, domínio, BLE, Health Connect, navegação ou integrações.

## Dependências

- `030-04` e `030-05` devem estar concluídas.

## Arquivos e símbolos prováveis

- `core/designsystem/ChampionDisplayComponents.kt`;
- `core/designsystem/ChampionDisplayPreviews.kt`;
- `core/designsystem/chart/*`;
- todas as features cobertas pela Spec 030;
- documentação da Spec 030 e overview.

## Passos de implementação

1. Reauditar consumidores produtivos, shell e legado.
2. Remover legado visual somente com evidência de ausência de uso.
3. Executar validação completa e smoke de todas as superfícies.
4. Registrar cenários `N/A`, riscos, evidências e limitações.
5. Atualizar status, progresso e critérios reais na documentação.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Não criar ou executar testes automatizados.

## Definição de pronto

- todas as superfícies produtivas foram smoke-testadas;
- todos os estados e controles exercitados ou marcados `N/A`;
- legado removido somente com evidência;
- builds, lint, instalação e evidências sanitizadas registrados;
- Spec 030, overview e subtarefas refletem estado final real.

## Riscos e cuidados

- preservar `HealthChart` se previews/testes ainda exigirem API;
- não declarar sucesso físico sem dispositivo disponível;
- não declarar paridade funcional com Google Health;
- manter limites de privacidade e direção watch-first.
