# Task 007-05 — Implementar Meta E Política De Estimativas

## Status

Pendente.

## Objetivo

Adicionar meta diária padrão de 8000 passos e política mínima de estimativa de distância/calorias sem inventar dados pessoais.

## Contexto

Home precisa mostrar meta, progresso e estimativas. Perfil completo e edição de meta ficam para fase futura.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/003-domain-and-persistence-foundation.md`

## Dependências

- `007-02`

## Entradas

- `AppPreferences.kt`
- `AppPreferencesStore.kt`
- `PreferencesModule.kt`
- `RepositoryModule.kt`
- `UserProfileDao`
- `WeightMeasurementEntity`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppPreferences.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/AppPreferencesStore.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/PreferencesModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- docs desta fase

## Arquivos Proibidos

- `HomeScreen.kt`
- `DeviceManagementViewModel.kt`
- `ChampionDatabase.kt`, salvo se necessidade real de schema for comprovada
- `AndroidManifest.xml`
- Gradle files

## Escopo

- `dailyStepsGoal = 8000` com decode compatível.
- Binding Hilt para `AppPreferencesRepository` se ausente.
- Consulta mínima de altura/peso/passada disponível, ou contrato explícito de indisponibilidade.
- Fórmulas da spec.
- Testes pequenos de codec, progresso e estimativas.

## Fora Do Escopo

- Tela de edição de meta.
- Formulário de perfil.
- Histórico de peso completo.
- Passada manual completa.
- Recálculo histórico.

## Passos

1. Adicionar campo de preferência com default 8000.
2. Garantir valor positivo.
3. Criar helper/política de progresso.
4. Criar helper/política de estimativas com `null` quando faltar dado.
5. Adicionar consulta mínima ou resultado indisponível claro.
6. Atualizar overview.

## Critérios De Aceite

- Preferências antigas decodificam com meta 8000.
- Progresso não divide por zero.
- Distância/calorias não usam valores inventados.
- Falta de dados pessoais vira `null`, não zero real.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- Documentar defaults e comportamento sem perfil.

## Atualização Do Overview

- Marcar `007-05` como concluída.

## Validação Final

- Confirmar que Fase 16 não foi antecipada.
