# 026-06 — Implementar câmera, stop, rota final e reset

- **Ticker:** `026`
- **Número:** `06`
- **Status:** pending

## Objetivo

Completar a experiência interativa da PoC com acompanhamento da câmera, recentralização, parada segura, enquadramento da rota concluída e reset integral entre ciclos.

## Resultado esperado

Durante tracking, a câmera acompanha o usuário sem impedir pan/zoom manual; após `Parar`, a rota permanece visível e é enquadrada; `Limpar` permite novo ciclo sem contaminação.

## Dependências

- `026-05` concluída;
- mapa, posição e rota em tempo real funcionando.

## Escopo incluído

- estado `followUser`;
- centralização no primeiro ponto;
- zoom inicial aproximado de atividade urbana;
- suspensão de follow após gesto manual;
- ação `Recentralizar`;
- comportamento de `Parar`;
- cálculo de bounds em memória;
- fit da rota concluída;
- reset de estado e geometria;
- segundo ciclo no mesmo processo.

## Escopo excluído

- rotação automática por bearing;
- câmera estilo navegação;
- animações agressivas por fix;
- persistência;
- pausa/retomada de treino real;
- background.

## Regras de câmera durante tracking

No primeiro ponto aceito:

```text
center = primeiro ponto
zoom ≈ 16
followUser = true
```

Durante tracking:

- acompanhar somente enquanto `followUser == true`;
- evitar animação completa a cada atualização de 1 s;
- gesto manual suspende follow;
- `Recentralizar` restaura follow e centraliza no ponto corrente;
- não forçar bearing/tilt nesta PoC.

## Regras de parada

Ao tocar `Parar`:

- nenhum novo fix entra na rota;
- pontos e métricas permanecem em memória;
- mapa continua interativo;
- follow deixa de ser obrigatório;
- com dois ou mais pontos, enquadrar bounds completos com padding;
- com um ponto, manter centro e zoom coerentes;
- com zero pontos, preservar câmera.

## Regras de reset

`Limpar` deve:

- remover rota e posição final associada ao ciclo;
- zerar contadores e distância;
- limpar erro técnico anterior;
- invalidar route ID e geração anteriores;
- retornar a `Ready` ou estado de readiness atual;
- impedir callback antigo de contaminar novo ciclo.

## Passos de implementação

1. Adicionar estado de follow.
2. Detectar gesto manual suportado pela integração Compose escolhida.
3. Implementar recentralização.
4. Implementar transição `Tracking` → `Stopped`.
5. Calcular bounds somente em memória.
6. Aplicar câmera final.
7. Implementar reset completo.
8. Repetir dois ciclos consecutivos no mesmo processo.

## Privacidade

Bounds calculados são dados de localização e não podem ser logados ou persistidos.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke físico:

- iniciar e receber rota;
- mover/zoom manualmente;
- confirmar suspensão de follow;
- recentralizar;
- parar;
- confirmar enquadramento final;
- limpar;
- iniciar novo ciclo sem reiniciar app.

## Definição de pronto

- follow não impede interação manual;
- recentralização funciona;
- stop congela a rota;
- rota final permanece visível;
- bounds finais funcionam para múltiplos pontos;
- zero/um ponto possuem fallback seguro;
- reset elimina estado anterior;
- segundo ciclo é independente do primeiro.
