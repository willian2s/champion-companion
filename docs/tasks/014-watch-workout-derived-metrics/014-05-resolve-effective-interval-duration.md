# 014-05 — Resolver duração efetiva dos intervalos

- **Ticker:** `014`
- **Número:** `05`
- **Status:** pending

## Objetivo e resultado esperado

Aplicar política temporal da ADR-018 aos intervalos existentes, incluindo último
intervalo parcial e falha fechada. Resultado: janelas positivas, ordenadas e sem
síntese, prontas para derivação.

## Contexto

Parser atual atribui 60 segundos a todo intervalo e não usa o restante da duração
total. `resolvedEndedAt` também não é propagado explicitamente às métricas.

## Requisitos cobertos

- R4;
- integridade temporal de R5, R7 e R8.

## Dependências

- `014-04` concluída.

## Arquivos permitidos

- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- contratos temporais em `core/model`;
- mappers/repositories de intervalos estritamente necessários;
- documentação de protocolo e overview.

Símbolos prováveis: `resolvedDurationSeconds`, `resolvedEndedAt`,
`WorkoutMetrics.endedAt`, resolvedor temporal e persistência de duração efetiva.

## Arquivos proibidos

- UI, perfil, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- sessões phone e rota histórica.

## Escopo

- quatro relações entre `T`, `N` e 60 segundos;
- cauda sem intervalo;
- índices ausentes/duplicados;
- propagação do fim temporal efetivo.

## Fora do escopo

- corrigir timestamps de origem sem evidência;
- sintetizar intervalo ausente;
- calcular distância, ritmo ou split;
- testes automatizados.

## Passos de implementação

1. Centralizar política temporal em função determinística.
2. Aplicar 60 segundos aos intervalos anteriores e restante positivo ao último.
3. Marcar série inelegível quando `T <= (N−1)×60`.
4. Preservar cauda quando `T > N×60` sem criar bucket.
5. Persistir duração efetiva e motivo de inelegibilidade.
6. Propagar `endedAt` resolvido sem inflar duração ativa.
7. Atualizar protocolo e overview.

## Critérios de aceite

- `1.637 s` produz 27 intervalos de 60 s e último de 17 s.
- `T=N×60` mantém todos com 60 s.
- Caso curto falha fechado sem truncar fatos.
- Caso longo preserva sessão e não inventa intervalo.
- Lacunas não são preenchidas.

## Validação manual

- Exercitar tabela dos quatro casos com dados sanitizados em debug/local.
- Conferir início/fim do último intervalo e janela da sessão.
- Verificar reabertura do detalhe após persistência.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- matriz `T/N → durações/elegibilidade`;
- registro sanitizado do exemplo de 1.637 s;
- builds e smoke local.

## Definição de pronto

Política temporal persistida e consumível, sem intervalos sintéticos, com item
`014-05` marcado.

## Riscos e cuidados

- `N=0` mantém sessão sem série.
- Amostra BPM na fronteira deve usar convenção temporal consistente.
- Não converter cauda em passos ou distância zero inventados.

## Rollback

Desativar resolvedor e leitura de duração efetiva, preservando campos adicionados
e dados raw; não apagar intervalos.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-05` e registrar matriz validada.
