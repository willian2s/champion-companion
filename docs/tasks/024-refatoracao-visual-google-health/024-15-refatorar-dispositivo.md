# 024-15 — Refatorar Dispositivo

- **Ticker:** `024`
- **Número:** `15`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar gerenciamento do Champion C001 para deixar associação, conexão,
descoberta e recuperação de erro claras. Resultado: estado atual e próxima ação
visíveis sem alterar BLE, permissões ou diagnóstico.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, prerequisites, dispositivo associado, conexão e scan;
- candidatos, seleção, substituir, esquecer, retry e disconnect;
- dialogs, diagnóstico seguro e exportação sanitizada;
- estados disponíveis, loading e erro.

## Escopo excluído

- BLE core, auto-connect, association repository ou permissões;
- PoC técnica debug e payloads;
- mudança no efeito de esquecer histórico.

## Dependências

- `024-14` concluída;
- Champion C001 pode permanecer desconectado; estados não seguros ficam `N/A`.

## Arquivos e símbolos prováveis

- `feature/device/DeviceScreen.kt`;
- `DeviceScreen`, `DeviceManagementScreen` e Composables/dialogs locais;
- `DeviceManagementDebugPoc.kt` somente como fronteira excluída.

## Passos de implementação

1. Mapear estados e ações BLE sem acioná-los ainda.
2. Destacar dispositivo associado, conexão e próxima ação segura.
3. Separar descoberta, manutenção e diagnóstico.
4. Preservar confirmações de esquecer/substituir e identificadores seguros.
5. Validar permissões/Bluetooth/scan apenas quando não houver risco operacional.
6. Confirmar que esquecer não remove histórico e diagnóstico permanece
   sanitizado.
7. Registrar estados `N/A`, capturas e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: abrir tela, rolar, ações não destrutivas, abrir/cancelar dialogs e voltar;
scan/conexão somente se seguro; fonte `1,3x`, landscape e temas.

## Definição de pronto

- associação, conexão e próxima ação são claras;
- todos os estados/callbacks BLE permanecem;
- dialogs e diagnóstico preservam segurança;
- PoC debug não foi expandida;
- comandos, capturas sanitizadas e overview registrados.

## Riscos e cuidados

- operações BLE competem com importações/medições;
- não esquecer ou substituir aparelho por evidência;
- identificadores e diagnósticos exigem sanitização;
- estados raros não devem ser fabricados.

## Registro de execução

### Status

`completed`; tela refatorada após revisão do excesso de status e informações
duplicadas. Associação, estado atual, descoberta, conexão, manutenção e
diagnóstico ficaram visualmente separados sem alteração dos fluxos BLE.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`;
- `024-15-captura-real-sanitizada-dispositivo.png`;
- `024-00-overview.md`.

### Decisões e desvios

- O estado de conexão passou a aparecer uma única vez no card `Estado atual`,
  junto da próxima ação; o card `Dispositivo associado` mantém identidade,
  identificador seguro, firmware e sincronização, sem repetir conexão ou
  timestamps técnicos.
- Descoberta, conexão, manutenção e diagnóstico receberam cards próprios com
  acentos nominais `primary`, `secondary` e `tertiary`, conforme tema, sem
  alterar tema raiz.
- Candidatos selecionados não repetem `Selecionado` e `Pronto para conectar`;
  o chip e a ação principal preservam essa informação.
- Diagnóstico continua sanitizado e a confirmação de esquecer continua
  informando que histórico e dados salvos permanecem intactos.
- Nenhuma alteração em ViewModel, contratos, BLE, permissões, persistência,
  navegação, Health Connect ou PoC debug.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou |
| `./gradlew app:compileDebugKotlin` | Primeira tentativa falhou por import removido de `Instant`; import restaurado e compilação repetida com sucesso |
| `./gradlew app:compileDebugKotlin` | Passou após correção |
| `./gradlew app:lintDebug` | Passou |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou |
| Revisão independente final | Passou; estado real de conexão, duplicação de descoberta e checklist SDD aprovados |
| Smoke ADB portrait | Passou; header, estado único, associação compacta, descoberta e conexão visíveis |
| Smoke ADB rolagem | Passou; conexão, manutenção, diagnóstico e PoC debug separados e acessíveis |
| Smoke ADB diagnóstico | Passou; abrir e fechar diagnóstico seguro sem exposição de payload |
| Smoke ADB esquecer | Passou; abrir/cancelar confirmação; nenhum esquecimento executado |
| Smoke ADB fonte `1,3x`, landscape e tema claro/escuro | Passou; bounds acessíveis sem clipping observado; aparelho restaurado para fonte `1,0x`, portrait e tema escuro |
| Captura sanitizada | Passou; barra de status e timestamps pessoais removidos de `024-15-captura-real-sanitizada-dispositivo.png` |

Não foram criados, alterados ou executados testes automatizados.

### Evidências

- Hierarquia ADB inicial confirmou `Estado atual` com uma única ocorrência de
  `Conectado`, seguido por associação compacta sem badge de conexão duplicado.
- Hierarquia após rolagem confirmou cards separados `Conexão`, `Manutenção`,
  `Diagnóstico seguro` e PoC técnica de debug.
- Dialogs de diagnóstico e esquecimento foram abertos e cancelados; histórico
  permaneceu protegido por confirmação explícita.
- Captura sanitizada registra header, estado atual e associação sem timestamps
  ou dados pessoais.

### Riscos residuais

- Estados transitórios BLE, scan real, retry, erro e ausência de associado não
  foram fabricados; permanecem `N/A` nesta validação.
- Operações de conectar, desconectar, esquecer, substituir e exportar não foram
  executadas no aparelho para evitar alteração de associação ou dados.
- Revisão independente final passou sem bloqueadores.
