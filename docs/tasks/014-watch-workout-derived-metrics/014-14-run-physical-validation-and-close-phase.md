# 014-14 — Executar validação física e encerrar fase

- **Ticker:** `014`
- **Número:** `14`
- **Status:** pending

## Objetivo e resultado esperado

Executar gate físico completo com Champion C001, Galaxy S22 e Health Connect
real; reconciliar documentação factual e encerrar Fase 014 somente com evidência
suficiente. Resultado: status final explícito, sem reabrir Fase 013.

## Contexto

Fase reúne parser, perfil, derivação, UI, reprocessamento e projeção remota.
Validação precisa confirmar cadeia inteira e cenários fail-closed.

## Requisitos cobertos

- Todos os requisitos e critérios da spec;
- fechamento documental e preservação histórica.

## Dependências

- `014-01` a `014-13` concluídas.

## Arquivos permitidos

- `docs/integration/014-*` para evidências sanitizadas;
- spec e overview da Fase 014;
- ADR-018, roadmap, baselines e protocolo para estado factual final;
- screenshots em local adotado pelo repositório;
- correções documentais estritamente factuais encontradas no gate.

## Arquivos proibidos

- código de produção, Gradle, manifesto, migrations e schemas;
- correções funcionais escondidas dentro do gate;
- spec/overview/status da Fase 013 para reabertura;
- payload BLE, dados de saúde e identificadores pessoais em evidências.

## Escopo

- importação explícita D3/D4;
- decodificação, duração, perfil, snapshot, métricas, gráficos e splits;
- reprocessamento explícito;
- sync granular, retry e reconciliação legada;
- status final e riscos residuais.

## Fora do escopo

- implementar correção descoberta; abrir follow-up quando bloqueadora;
- GPS, rota, mapa, treino phone, BLE live ou WorkManager;
- testes automatizados;
- remover infraestrutura histórica.

## Passos de implementação

1. Confirmar checkout/base e versões de app/dispositivo/provider sem registrar
   identificadores sensíveis.
2. Importar atividades de referência e comparar sequência/soma de passos.
3. Validar quatro casos temporais disponíveis ou registrar limitação factual.
4. Configurar passo manual e sugestão aceita; validar ausência sem perfil.
5. Conferir snapshot, métricas, acumulado, splits, BPM e screenshots.
6. Reprocessar legado explicitamente e confirmar preservação.
7. Sincronizar em Health Connect, repetir retry e reconciliar agregado legado.
8. Registrar divergências, riscos e gates `PASS`, `FAIL` ou `N/A` justificado.
9. Atualizar documentos e fechar somente se critérios estiverem atendidos.
10. Atualizar overview por último.

## Critérios de aceite

- Sequência de referência soma `3.737` passos e raw permanece preservado.
- Duração de `1.637 s` produz último intervalo de 17 s.
- Valor pessoal não vira constante global; edição não muda snapshot antigo.
- UI usa distância canônica em todas as superfícies.
- Splits e BPM correspondem à linha do tempo.
- Provider não contém agregado e série simultaneamente após reconciliação.
- Retry não duplica records.
- Fase 013 permanece fechada e legado preservado.
- Qualquer bloqueio impede status `completed` e fica explícito.

## Validação manual

- Galaxy S22 + Champion C001 real;
- app debug e, quando seguro, release;
- Health Connect real com permissões concedidas/revogadas;
- reinício do app, desconexão do relógio após importação e repetição de sync.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- relatório `docs/integration/014-*` sanitizado;
- screenshots de perfil, detalhe, gráficos/splits e provider;
- matriz de gates, versões e riscos residuais;
- resultados dos comandos permitidos.

## Definição de pronto

Gate físico documentado e revisado; spec/overview/roadmap/baselines refletem
resultado real. Item `014-14` e progresso só mudam para concluído quando todos os
14 itens estiverem aceitos.

## Riscos e cuidados

- Cenário físico indisponível não pode receber `PASS` presumido.
- Evidência deve ser sanitizada e não conter BPM, passos individuais, payload ou
  IDs pessoais quando publicada.
- Não corrigir produção nesta subtarefa; bloquear fechamento e abrir task.

## Rollback

Reverter apenas afirmações documentais incorretas. Evidências válidas permanecem
históricas; não reverter dados remotos sem plano explícito e identidade própria.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md` por último. Marcar `014-14`, definir progresso
`14/14` e alterar status geral somente após todos os critérios e revisão final.
