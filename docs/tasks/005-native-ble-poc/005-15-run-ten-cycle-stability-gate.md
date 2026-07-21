# Task 005-15 — Executar Gate De Dez Ciclos Consecutivos

## Status

Planejada.

## Objetivo

Executar Gate A com dez ciclos completos e consecutivos no Galaxy S22 com Champion C001 usando mesma configuração de duração.

## Contexto

Esta é a prova final da fase. Qualquer falha reinicia contagem do gate.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-14-run-negative-physical-scenarios.md`

## Dependências

- `005-14` concluída.

## Pré-Condições

- Galaxy S22 conectado por ADB;
- Champion C001 disponível e carregado;
- build debug instalado;
- Bluetooth habilitado;
- configuração final de duração definida e fixa.

## Entradas

- tela técnica;
- relatório sanitizado por ciclo.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-15-run-ten-cycle-stability-gate.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- concluir task com menos de dez ciclos consecutivos;
- alterar roadmap e ADRs.

## Escopo

- rodar dez ciclos consecutivos completos;
- registrar evidência de cada ciclo;
- confirmar cleanup e ausência de restart obrigatório.

## Fora Do Escopo

- correção de código grande nesta task;
- feature final de produto.

## Passos

1. Confirmar configuração fixa do ciclo.
2. Executar ciclo 1 a 10 sem reiniciar app.
3. Em cada ciclo, confirmar: permissões, scan, conexão, `TransportReady`, A1, FEA1, D0/F002, valor final técnico, `D0 00`, disconnect e cleanup.
4. Exportar relatório sanitizado de cada ciclo.
5. Se qualquer ciclo falhar, registrar causa e reiniciar sequência após correção necessária.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não ocultar retry automático;
- não considerar ciclo vencedor sem cleanup confirmado;
- não marcar como concluída sem dez relatórios consecutivos válidos.

## Critérios De Aceite

- dez ciclos completos e consecutivos;
- nenhuma operação GATT concorrente;
- nenhuma fila presa;
- nenhum timeout residual;
- nenhum callback antigo contaminando novo ciclo;
- nenhum crash, ANR ou restart obrigatório.

## Testes

- validação física manual.

## Comandos De Validação

```bash
adb devices
git status --short
```

## Evidências Esperadas

- dez relatórios sanitizados;
- registro resumido de duração e resultado de cada ciclo;
- confirmação de Gate A aprovado.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar explicitamente aprovação ou bloqueio do Gate A.
