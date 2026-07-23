# Task 009-02 - Formalizar D1 local-first, ADR e roadmap

## Status

Pendente.

## Objetivo

Formalizar por documentacao duradoura que Fase 09 e owner de D1 local + fila `PENDING`, enquanto Fase 12 e owner de Health Connect real.

## Contexto

`docs/specs/_inputs/phase-09-watch-heart-rate-history-direction.md` corrige explicitamente dependencia circular do roadmap. Antes de qualquer implementacao relevante, projeto precisa registrar essa fronteira por ADR, roadmap e documentos correlatos para evitar que Fase 09 puxe `HealthConnectClient`, permissoes, provider ou estados `SYNCED` antes do momento certo.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/_inputs/phase-09-watch-heart-rate-history-direction.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-01-audit-d1-protocol-history-and-health-connect-boundary.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/protocol/history-d1.md`
- `docs/baseline/health-connect-mapping.md`
- ADRs vigentes em `docs/decisions/`

## Dependencias

- `009-01`

## Entradas

- achados factuais de `009-01`;
- roadmap ainda desatualizado;
- regra de produto que todos D1 validos devem ser enviados futuramente, sem selecao individual.

## Arquivos Exatos Permitidos

- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-02-formalize-d1-local-first-adr-and-roadmap.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/protocol/history-d1.md`
- `docs/decisions/adr-010-d1-local-first-and-health-connect-phase-boundary.md`

## Arquivos Proibidos

- qualquer `.kt`
- `docs/specs/_inputs/phase-09-watch-heart-rate-history-direction.md`
- tasks concluidas de fases anteriores

## Escopo

- criar ADR com proximo numero disponivel;
- atualizar roadmap da Fase 09 e Fase 12 sem renumerar fases;
- alinhar protocolo D1 quando a fronteira impactar linguagem documental;
- registrar matriz de dependencias entre D1 local e Health Connect futuro;
- manter regra de enviar todos D1 validos futuramente;
- avaliar se metadata generica de importacao exige ADR adicional ou pode ficar coberta por ADR principal.

## Fora Do Escopo

- implementar `HealthConnectClient`;
- pedir permissao Health Connect;
- criar mapeadores de provider;
- escrever provider;
- alterar codigo BLE ou UI.

## Passos

1. Confirmar proximo numero de ADR disponivel.
2. Criar ADR registrando: Fase 09 local-first, fila `PENDING`, fonte visual local, falha futura do provider nao desfaz dados locais, Fase 12 como owner do provider real.
3. Atualizar roadmap: remover da Fase 09 disponibilidade/permissao/escrita/lotes reais; explicitar em Fase 12 processamento de todos D1 pendentes.
4. Ajustar `docs/protocol/history-d1.md` se necessario para refletir fronteira documental correta.
5. Atualizar spec e overview da Fase 09 no mesmo trabalho.

## Criterios De Aceite

- ADR criada em `adr-010-d1-local-first-and-health-connect-phase-boundary.md`;
- roadmap alinhado sem renumerar fases;
- Fase 09 mantida como local-first;
- Fase 12 explicitada como owner de Health Connect real;
- regra de enviar todos D1 validos no futuro preservada;
- nenhum codigo alterado.

## Testes Pequenos Aplicaveis

- nenhum. Task documental/arquitetural.

## Validacao Minima

- revisao de diff documental;
- `git diff -- docs/decisions docs/roadmap docs/protocol/history-d1.md docs/specs/009-watch-heart-rate-history-d1.md docs/tasks/009-watch-heart-rate-history-d1/`.

## Evidencias

- ADR criada;
- roadmap ajustado;
- protocolo alinhado quando aplicavel.

## Atualizacao Do Overview

- marcar `009-02` como concluida;
- atualizar dependencias e riscos restantes;
- registrar explicitamente que `009-03` depende de contratos documentados aqui.

## Validacao Final

- confirmar que task permaneceu estritamente documental e nao implementou cliente Health Connect.
