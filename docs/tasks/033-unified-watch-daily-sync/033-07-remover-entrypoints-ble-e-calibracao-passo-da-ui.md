# 033-07 — Remover entrypoints BLE e calibração por passo da UI

- **Status:** in_progress
- **Depende de:** 033-05, 033-06 fisicamente validada

## Objetivo

Alinhar a experiência do usuário ao novo ownership sem apagar contratos legacy.

## Ownership de entrada

Contrato visual adotado durante a execução desta task:

```text
Home pull-to-refresh
  -> sincronizar relógio / atualizar dados locais

Home botão Sync
  -> sincronização explícita global com Health Connect
  -> reutiliza SettingsIntent.SyncNow

Configurações -> Gerenciar relógio
  -> DeviceScreen
  -> associação/conexão/manutenção BLE
```

O cabeçalho da Home deixa de usar o relógio como atalho de gerenciamento e passa
a expor a ação remota explícita. Saúde também deixa de expor atalho visual de
dispositivo. Ver ADR-032.

## Saúde

Remover da UI/contrato de produto:

- medir frequência cardíaca manualmente;
- buscar D1;
- cancel/retry/progresso BLE desses fluxos;
- atalho visual de gerenciamento do relógio.

Preservar histórico manual/D1 antigo como leitura.

### Estado da execução

- [x] detalhe de frequência cardíaca já estava somente leitura após o redesign 031;
- [x] atalho visual do relógio removido do cabeçalho Saúde;
- [ ] limpar contratos/state machines de navegação que só protegem medição manual/D1 legado quando não houver mais consumidor de UI.

## Exercícios

Remover da UI de produto:

- buscar treinos no relógio;
- cancel/retry/progresso de importação própria.

Preservar review, referência de distância, treadmill, reprocessamento de manutenção
e sync explícito.

### Estado da execução

- [ ] remover `Importar treinos` do menu;
- [ ] remover progresso/snackbar/cancel/retry da importação própria;
- [ ] manter status Health Connect, revisão e sync explícito do workout.

## Perfil

Remover affordances de:

- step length manual;
- sugestão por altura;
- perfis/histórico/correção de calibração por passo.

Preservar internamente legacy/fallback. Calibração contextual de distância D4
continua no exercício.

### Estado da execução

- [ ] remover card de comprimento de passo;
- [ ] remover diálogos de perfis aprendidos e histórico/correção de calibração por passo;
- [ ] preservar altura, meta de passos e dados históricos internos.

## Home e Configurações

- [x] substituir ação de relógio do cabeçalho da Home por `Sincronizar com Health Connect`;
- [x] reutilizar o mesmo sync global já validado em Configurações;
- [x] exibir progresso foreground e feedback terminal na Home;
- [x] adicionar `Configurações -> Gerenciar relógio` abrindo o `DeviceScreen` existente;
- [x] preservar pull-to-refresh como única leitura normal do relógio;
- [x] preservar CTA contextual de associação quando a Home não possui dispositivo configurado.

## Guard rails

- não apagar dados históricos;
- não redesenhar superfícies além do necessário;
- preservar baseline visual pós-031;
- não criar segundo pipeline de Health Connect;
- não transformar watch refresh em auto-sync remoto;
- screenshot no PR por ser mudança visual.

## Validação pendente

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Também validar manualmente:

- pull-to-refresh continua atualizando somente relógio/local;
- botão Sync da Home executa o mesmo sync global de Configurações;
- snackbar/progresso da Home refletem o gesto remoto;
- `Configurações -> Gerenciar relógio` abre a tela de dispositivo;
- Saúde não mostra atalho de relógio;
- screenshot da Home e Configurações para revisão visual.
