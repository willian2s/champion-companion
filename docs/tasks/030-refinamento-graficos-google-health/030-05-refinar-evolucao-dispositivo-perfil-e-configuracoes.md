# 030-05 — Refinar Evolução, Dispositivo, Perfil e Configurações

- **Ticker:** `030`
- **Número:** `05`
- **Status:** `pending`
- **Depends on:** `030-04`

## Objetivo e resultado esperado

Aplicar linguagem visual integral às superfícies de Evolução, Dispositivo,
Perfil e Configurações, alinhando resumo, controles, estados, ações e gráficos
ao Google Health/Fitbit sem alterar contratos.

## Requisitos e critérios cobertos

- R1–R6; todos os critérios da Spec 030 nas superfícies desta subtarefa.

## Escopo incluído

- `EvolutionScreen`: período, filtro, resumo, tendências, comparação, recordes,
  estados e navegação para atividade;
- `DeviceScreen`: pré-requisitos, associação, busca, conexão, manutenção,
  retry, diagnóstico e exportação sanitizada;
- `ProfileScreen`: altura, meta, passo, calibração, perfis RUNNING/WALKING,
  histórico, edição, correção e confirmações;
- `SettingsScreen`: disponibilidade, permissões, toggle, fila, sincronização
  foreground e mensagens Health Connect;
- cards, listas, diálogos, estados, acessibilidade, temas e orientação.

## Escopo excluído

- fórmulas, elegibilidade, recordes, persistência, BLE, permissões ou Health
  Connect;
- novos períodos, métricas, destinos, sync automático ou leitura remota;
- mudança dos perfis independentes, DataStore ou snapshots históricos;
- PoC debug, GPS, rota, localização e treino phone.

## Dependências

- `030-04` deve estar concluída.

## Arquivos e símbolos prováveis

- `feature/evolution/*`;
- `feature/device/*`;
- `feature/profile/*`;
- `feature/settings/*`;
- `core/designsystem/*` e `core/designsystem/chart/*`.

## Passos de implementação

1. Mapear cada tela, estado e ação para a hierarquia visual compartilhada.
2. Aplicar períodos existentes de Evolução: 7/30/90, sem ano novo.
3. Clarificar origem, disponibilidade, pendência, fila e confirmação sem ocultar
   informação operacional.
4. Preservar perfis, snapshots, permissões, BLE e Health Connect write-only.
5. Validar filtros, diálogos, navegação contextual, fonte, orientação e temas.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Não criar, alterar ou executar testes automatizados. Registrar evidência
sanitizada e declarar `N/A` quando necessário.

## Definição de pronto

- as quatro superfícies usam linguagem visual integral coerente;
- filtros, ações, estados e contratos permanecem funcionais;
- nenhum dado é criado ou reescrito para aparência;
- smoke, acessibilidade, screenshot e riscos estão registrados.

## Riscos e cuidados

- não confundir status local com confirmação de provider remoto;
- não reescrever snapshots ou perfis históricos;
- não usar cor como único sinal de permissão, erro ou comparação;
- preservar a separação RUNNING/WALKING e restrições de CYCLING/TREADMILL.
