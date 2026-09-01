# 029-07 — Alinhar dispositivo, perfil e configurações

- **Ticker:** `029`
- **Número:** `07`
- **Status:** `pending`
- **Depende de:** `029-03`

## Objetivo e resultado esperado

Aplicar a mesma composição visual às superfícies restantes sem alterar conexão,
permissões, perfil, metas, integração Health Connect ou políticas de privacidade.

## Requisitos e critérios de aceite cobertos

- R2, R3, R4, R7 e R8 da Spec 029.
- Critérios 1, 4, 9, 10 e 11.
- Cobrir estados conectados, pendentes, permissões, integração e formulários
  alcançáveis sem executar ações destrutivas.

## Escopo incluído

- `DeviceScreen`, `ProfileScreen` e `SettingsScreen`;
- headers, cards, listas, estados, ações, formulários, mensagens e densidade;
- feedback de Health Connect e permissões como estados existentes;
- screenshots Champion sanitizados e comparação com matriz.

## Escopo excluído

- pareamento real, scan, esquecer dispositivo, exportação ou alteração de conta
  para fabricar evidência;
- mudança de permissões, manifesto, Health Connect write-only, BLE ou backup;
- novo campo de perfil, integração ou armazenamento;
- funcionalidade Google de conta, premium, coach ou analytics.

## Dependências

- fundação 029-03;
- baseline e matriz 029-01;
- `DeviceViewModel`, `ProfileViewModel`, `SettingsViewModel` e contratos atuais.

## Arquivos e símbolos prováveis

- `feature/device/DeviceScreen.kt`;
- `feature/profile/ProfileScreen.kt`;
- `feature/settings/SettingsScreen.kt`;
- `core/preferences/PreferencesModule.kt`;
- `HealthConnectSyncCoordinator`, somente como contrato preservado.

## Passos de implementação

1. Comparar as três superfícies com a matriz instalada.
2. Ajustar estrutura e componentes sem alterar intents ou efeitos.
3. Preservar mensagens de permissão, loading, erro e sucesso existentes.
4. Validar acessibilidade, formulários, foco, tema, orientação e fonte.
5. Capturar evidências sanitizadas e registrar estados não alcançados.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Smoke físico sem ações destrutivas: abrir as três superfícies, navegar estados
visíveis, voltar, testar tema, orientação, fonte ampliada e acessibilidade. Não
criar testes automatizados.

## Definição de pronto

- superfícies restantes correspondem à matriz visual sem alterar contratos;
- permissões, integração, perfil e conexão continuam explícitos e seguros;
- nenhuma credencial, dado pessoal ou estado remoto é criado;
- build, lint, smoke e evidências sanitizadas passam.

## Riscos e cuidados

- Não copiar telas de conta ou permissões da referência literalmente.
- Não expor dados de dispositivo, perfil, tokens ou identificadores em evidência.
- Não transformar ajuste visual em mudança de integração.
