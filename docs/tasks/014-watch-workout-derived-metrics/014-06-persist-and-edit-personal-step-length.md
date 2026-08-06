# 014-06 — Persistir e editar comprimento pessoal do passo

- **Ticker:** `014`
- **Número:** `06`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Permitir valor manual e sugestão baseada na altura, com confirmação explícita,
persistindo `stepLengthMeters`, origem e atualização. Resultado: perfil fornece
um valor fixo elegível sem alterar atividades antigas.

## Contexto

`UserProfile` persiste somente altura. `DailySummaryPolicy` calcula
`strideLengthMeters` por `altura × 0,415`, mas não registra aceite nem snapshot
para treino.

## Requisitos cobertos

- R2 e R9;
- terminologia normativa da spec.

## Dependências

- `014-03` concluída.

## Arquivos permitidos

- contratos de perfil em `core/model`;
- entidades/DAO/mappers/repository de perfil criados em `014-03`;
- `feature/profile` e navegação já existente para perfil;
- recursos de texto relacionados;
- overview.

Símbolos prováveis: `UserProfile`, `PersonalMetricsSnapshot`, repositório de
perfil, `ProfileViewModel`/tela equivalente e `StepLengthSource`.

## Arquivos proibidos

- parser D4, derivação de atividades, Health Connect writer;
- Gradle, manifesto, migrations e schemas;
- UI phone, GPS, rota e Fase 013.

## Escopo

- entrada manual validada;
- sugestão por altura como proposta não persistida;
- confirmação e origem `HEIGHT_SUGGESTION_ACCEPTED`;
- ausência segura e edição futura.

## Fora do escopo

- fluxo de `REFERENCE_DISTANCE_CALIBRATION`;
- passada variável por ritmo/cadência;
- recalcular atividades existentes;
- sincronizar passo pessoal com Health Connect;
- testes automatizados.

## Passos de implementação

1. Expor `stepLengthMeters` e origem no contrato/repositório de perfil.
2. Definir limites de validação coerentes e erro de UI seguro.
3. Mostrar sugestão derivada da altura sem salvá-la automaticamente.
4. Exigir confirmação explícita para salvar sugestão.
5. Salvar entrada manual com origem `MANUAL`.
6. Usar texto **Comprimento do passo**; não introduzir `stride` em contrato novo.
7. Garantir que edição apenas atualize perfil.
8. Atualizar overview.

## Critérios de aceite

- Valor finito/positivo manual persiste e reabre corretamente.
- Sugestão não aceita não muda perfil.
- Sugestão aceita registra origem correta.
- Sem altura, sugestão permanece indisponível sem default.
- `0,8818 m` não aparece como constante/fallback.
- Atividades antigas não mudam após edição.

## Validação manual

- Salvar manual, reiniciar app e conferir valor/origem.
- Gerar sugestão com altura, cancelar e confirmar ausência de mudança.
- Aceitar sugestão e conferir persistência.
- Editar perfil com atividades existentes e confirmar métricas congeladas.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileFeatureContract.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`;
- `app/src/main/res/values/strings.xml`;
- `docs/specs/014-watch-workout-derived-metrics.md` (progresso alinhado);
- este arquivo e o overview.

Não houve alteração de migration, schema, Gradle, parser D4, derivação de
atividades ou Health Connect.

## Decisões e desvios

- A sugestão adotada é `alturaCentímetros / 100 × 0,415`, derivada somente de
  altura válida e exibida como proposta não persistida.
- A confirmação usa diálogo explícito com ações `Confirmar` e `Cancelar`; a
  origem persistida é `HEIGHT_SUGGESTION_ACCEPTED` somente após confirmação.
- Entrada manual aceita valor positivo e finito, com separador decimal do locale,
  e persiste origem `MANUAL`.
- Perfil ausente recebe bootstrap transacional sem altura e sem default de
  comprimento; edição preserva atividades e snapshots existentes.
- O timestamp do comprimento registra o instante recebido da edição; o timestamp
  geral do perfil não retrocede.

## Comandos executados

- `./gradlew lint assembleDebug assembleRelease` — PASS;
- `./gradlew installDebug` — PASS no Galaxy S22 `SM-S901E`;
- `git diff --check` — PASS;
- `git status --short` — PASS, somente arquivos desta subtarefa alterados.

## Resultados e evidências

- Tela Perfil instalada e aberta no dispositivo físico.
- Valor manual `0.7312345678` foi salvo; UI exibiu `0,7312345678 m` e origem
  `Entrada manual`.
- Após encerramento e reinício do app, valor, origem e atualização permaneceram
  visíveis; screenshot capturada em `/tmp/champion-profile-step-length.png`.
- Sem altura persistida no perfil do dispositivo, UI exibiu sugestão indisponível
  sem criar valor padrão.
- Revisão independente final: aprovada sem bloqueadores.
- Testes automatizados não foram criados nem executados, conforme escopo da task
  e `AGENTS.md`.

## Riscos residuais

- Aceite da sugestão não foi exercitado fisicamente porque dispositivo de
  validação não possuía altura persistida; fluxo de confirmação, origem e
  persistência foram revisados no código e cobertos por build/lint.
- A fórmula de altura permanece política de proposta desta task e não altera a
  estimativa diária legada.

## Definição de pronto

Perfil editável e confirmado, sem efeito retroativo, screenshots registradas e
item `014-06` marcado.

## Riscos e cuidados

- Fórmula de altura existente pode ser chamada de `stride`; adaptar terminologia
  sem alterar silenciosamente estimativas diárias fora da task.
- Passo pessoal é dado sensível: não logar nem usar em ID.
- Unidade de UI precisa converter sem perda para metros persistidos.

## Rollback

Ocultar edição/sugestão e parar novas gravações, preservando valores já
persistidos como dados desconhecidos até migration forward.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-06` e registrar fórmula/política de
sugestão efetivamente adotada.
