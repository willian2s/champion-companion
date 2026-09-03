# 031-10 — Redesenhar Configurações

- **Ticker:** `031`
- **Número:** `10`
- **Status:** pending

## Objetivo

Aplicar composição da referência a Configurações, mantendo Health Connect
write-only, permissões, fila e ações explícitas.

## Requisitos e aceite

- Configurações continua acessível pela ação `Configurações` do Perfil, com
  retorno ao Perfil, fora da barra inferior.
- Disponibilidade, provider, toggle, permissões, gerenciar acesso e sync now
  mantêm callbacks e estado.
- Fila local continua observável sem leitura remota do Health Connect.
- Textos de segurança, erro e indisponibilidade permanecem claros.

## Escopo

Inclui layout, agrupamento, filtros visuais, cards, headers e estados. Exclui
leitura Health Connect, novos tipos de record, permissões, sync automático ou
mudança de manifesto.

## Dependências

- `031-04` a `031-09` e ADR-012.
- Contratos de Settings e fila persistida.

## Arquivos e símbolos prováveis

- `feature/settings/SettingsScreen.kt`.
- `SettingsViewModel.kt`, `SettingsFeatureContract.kt`.

## Passos

1. Confirmar entrada contextual definida no shell.
2. Reorganizar integração, permissões e fila pela hierarquia da referência.
3. Preservar launcher, lifecycle, provider update e estados fail-safe.
4. Validar ações sem disparar sync apenas para evidência.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de abertura, rolagem, retorno, fonte ampliada, landscape e
temas. Não criar ou executar testes automatizados.

## Definição de pronto

Configurações acessível por origem documentada e visualmente aprovada, sem
mudança no contrato write-only, permissões, fila ou sync; estado inicial/restaurado
do aparelho e revisão independente aprovados.

## Riscos

Reorganização pode sugerir leitura remota ou sync automático. Manter linguagem e
estados que distinguem intenção local de confirmação remota.
