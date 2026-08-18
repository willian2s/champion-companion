# 024-15 — Refatorar Dispositivo

- **Ticker:** `024`
- **Número:** `15`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar gerenciamento do Champion C001 para deixar associação, conexão,
descoberta e recuperação de erro claras. Resultado: estado atual e próxima ação
visíveis sem alterar BLE, permissões ou diagnóstico.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

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
