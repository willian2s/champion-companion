# Task 000-12 - Criar Source Map

## Objetivo

Criar mapa entre codigo atual do MotionLog e destinos conceituais no Champion Companion.

## Contexto

O source map orienta futuras tasks de implementacao sem definir nomes finais de classes Kotlin.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-inventory.md`
- documentos em `docs/protocol/` ja criados

## Dependencias

- `000-03`
- `000-04`
- `000-05`, `000-06`, `000-07` recomendadas
- `000-10`, `000-11` recomendadas

## Arquivos Ou Areas De Entrada

- Artefatos de baseline/protocolo ja criados
- Caminhos consultados em `../motionlog-app`

## Arquivos Exatos De Saida

- `docs/baseline/source-map.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

Tabela com:

- Funcionalidade
- Origem no `motionlog-app`
- Responsabilidade atual
- Destino conceitual no `champion-companion`
- Documentos relacionados
- Status da analise
- Duvidas

Destinos conceituais permitidos:

- `core/bluetooth`
- `core/database`
- `core/healthconnect`
- `core/location`
- `core/notifications`
- `feature/home`
- `feature/heart`
- `feature/exercises`
- `feature/activitydetail`
- `feature/device`
- `feature/profile`
- `feature/diagnostics`

## Fora Do Escopo

- Inventar nomes definitivos de classes Kotlin.
- Implementar codigo.
- Refatorar documentos de origem.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Ler inventario e docs de protocolo.
2. Extrair caminhos do MotionLog registrados.
3. Mapear cada funcionalidade para destino conceitual permitido.
4. Registrar duvidas sem inventar destinos.
5. Atualizar checklist do overview.

## Criterios De Aceite

- Source map cobre funcionalidades principais.
- Todos os destinos sao conceituais.
- Caminhos de origem estao registrados.

## Validacao Final

- Confirmar que nenhum nome definitivo de classe Kotlin foi inventado.
- Confirmar que `motionlog-app` nao foi alterado.
