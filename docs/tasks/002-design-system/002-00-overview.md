# Task 002-00 — Overview Design System

## Status

Em andamento. `002-01`, `002-02`, `002-03` e `002-04` concluídas em 2026-07-20.

## Objetivo

Controlar entrega da Fase 02: identidade Material 3 própria, componentes reutilizáveis e recursos de marca mínimos.

## Leitura obrigatória

- `docs/core/current-project-baseline.md`
- `docs/specs/002-design-system.md`
- `docs/specs/_inputs/phase-02-design-direction.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`

## Regras

- Usar somente APIs estáveis `androidx.compose.material3`; não introduzir Material 2.
- Identidade nova azul-petróleo, seed `#006782`; não copiar MotionLog.
- Componentes não recebem DAOs, GATT, `HealthConnectClient`, exceções internas ou modelos de domínio.
- Não implementar BLE, Health Connect, persistência, notificações ativas ou sincronização real.
- Não criar preferência/persistência de tema.
- Não alterar roadmap, ADR, documentos concluídos, MotionLog, commits ou push.
- Cada task atualiza somente próprio `Status`; `002-09` consolida este checklist.

## Ordem e paralelismo

1. `002-01` após leitura desta task.
2. `002-02` após `002-01`.
3. `002-03` após `002-02`.
4. `002-04`, `002-05` e `002-06` em paralelo após `002-03`, sem editar mesmos arquivos.
5. `002-07` após `002-04`–`002-06`.
6. `002-08` após `002-02`; pode executar em paralelo com `002-03`–`002-07`.
7. `002-09` última, após todas.

## Checklist

- [x] `002-01` Fundação visual auditada; exclusão MotionLog registrada.
- [x] `002-02` Tema, paletas, tipografia, spacing, shapes e elevação definidos.
- [x] `002-03` Estados visuais e tokens semânticos definidos.
- [x] `002-04` Componentes de ação, conexão e status criados.
- [ ] `002-05` Cards, métricas e gráfico criados.
- [ ] `002-06` Feedback e banners criados.
- [ ] `002-07` Shell usa design system, sem cores locais.
- [ ] `002-08` Launcher, splash e ícone monocromático criados.
- [ ] `002-09` Validação final concluída.

## Aceite da fase

- Tema central Material 3 tem esquemas próprios claro/escuro, Material You opcional e fallback.
- Tokens, componentes e recursos cumprem contratos públicos da spec.
- Previews, acessibilidade, testes e validação física possuem evidência em `002-09`.
