# Task 000-03 - Inventariar Aplicacao

## Objetivo

Documentar estrutura geral, funcionalidades, telas, estados, permissoes e backup atual do MotionLog como referencia funcional.

## Contexto

O inventario funcional orienta paridade futura sem copiar componentes React Native ou dependencias Expo.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`

## Dependencias

- `000-01`
- `000-02`

## Arquivos Ou Areas De Entrada

- Estrutura de diretorios de `../motionlog-app`
- Telas, navegacao, hooks, services, repositories, stores, modelos, permissoes e backup do MotionLog

## Arquivos Exatos De Saida

- `docs/baseline/feature-inventory.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Funcionalidades, comportamento atual, telas, arquivos principais, dependencias, persistencia, integracoes, estado funcional e observacoes para Android nativo.
- Classificar estado como `Funcional`, `Parcial`, `Experimental`, `Diagnostico` ou `Nao confirmado`.
- Cobrir requisitos originais 1-8, 30-33 e 100.

## Fora Do Escopo

- Contratos detalhados de parsers.
- Banco detalhado.
- Mapeamento completo de Health Connect.
- Implementacao Android.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Ler baseline atual.
2. Mapear estrutura de diretorios sem alterar arquivos.
3. Identificar telas e navegacao.
4. Identificar hooks, services, repositories e stores.
5. Identificar permissoes e comportamento em loading/erro/vazio.
6. Registrar backup/restauracao atual apenas como referencia.
7. Registrar lacunas em `gaps-and-open-questions.md` se ja existir; senao listar no proprio inventario para consolidacao por `000-14`.
8. Atualizar checklist do overview.

## Criterios De Aceite

- Inventario cobre funcionalidades principais.
- Cada funcionalidade tem estado funcional.
- Caminhos consultados estao registrados.
- Backup tratado apenas como referencia.

## Validacao Final

- Confirmar que nenhuma dependencia Expo ou componente React Native foi copiado.
- Confirmar que `motionlog-app` nao foi alterado.
