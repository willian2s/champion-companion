# 026-05 — Renderizar mapa, posição e rota em tempo real

- **Ticker:** `026`
- **Número:** `05`
- **Status:** pending

## Objetivo

Conectar o estado GPS da PoC ao MapLibre Compose, usando OpenFreeMap como mapa-base e desenhando apenas os pontos aceitos como rota dinâmica.

## Resultado esperado

Durante tracking real, o mapa mostra a posição corrente e uma linha que cresce conforme novos pontos aceitos entram no estado, sem segunda fonte independente de localização.

## Requisitos e critérios cobertos

- Spec 026: critérios 6 a 10, 12, 17 a 19;
- OpenFreeMap como mapa-base;
- posição e rota oriundas da mesma fonte GPS;
- GeoJSON/LineString + LineLayer;
- mapa responsivo durante updates.

## Escopo incluído

- `MaplibreMap` na tela técnica;
- `BaseStyle.Uri` ou API equivalente da versão fixada;
- style `Positron`;
- conversão de pontos aceitos para geometria GeoJSON;
- source/layer da rota;
- marcador da posição atual;
- atualização incremental observável;
- contraste e legibilidade da linha;
- estado de erro do mapa-base separado de erro GPS.

## Escopo excluído

- location component como segunda fonte GPS;
- Directions;
- map matching;
- geocoding;
- offline;
- self-hosting;
- persistência de GeoJSON.

## Dependências

- `026-04` concluída;
- MapLibre Compose configurado;
- OpenFreeMap carregando;
- lista de pontos aceitos disponível no estado.

## Regra de fonte única

```text
GpsMapPocLocationSource
        ├── currentPoint → posição atual
        └── acceptedPoints → rota
```

Não iniciar rastreamento de localização próprio do SDK de mapa para alimentar o marcador.

## Geometria

Com zero pontos:

- sem linha.

Com um ponto:

- posição atual/início, sem LineString inválida.

Com dois ou mais:

```text
acceptedPoints
      ↓
GeoJSON coordinates
      ↓
LineString
      ↓
GeoJsonSource
      ↓
LineLayer
```

Respeitar ordem GeoJSON:

```text
longitude, latitude
```

A lista interna do domínio pode continuar usando nomes latitude/longitude; somente a adaptação para geometria deve inverter para a ordem exigida pelo GeoJSON.

## Estilo da rota

Direção inicial:

- cap arredondado;
- join arredondado;
- largura legível em smartphone;
- cor de acento do Champion quando compatível com API/tokens existentes;
- contraste suficiente no `Positron`;
- nenhuma coordenada embutida em style JSON persistido.

## Posição atual

O marcador deve:

- representar o fix observado pela mesma fonte usada na rota;
- continuar visível quando houver somente um ponto;
- não expor precisão falsa;
- não depender de conta ou API de localização externa.

## OpenFreeMap e atribuição

- não ocultar atribuição obrigatória;
- não substituir o style por endpoint não documentado sem registrar desvio;
- não implementar downloads em massa;
- falha de rede deve produzir estado de mapa indisponível sem derrubar o fluxo GPS.

## Passos de implementação

1. Renderizar OpenFreeMap via MapLibre Compose.
2. Confirmar atribuição visível.
3. Adaptar ponto corrente para marcador.
4. Adaptar pontos aceitos para GeoJSON.
5. Criar source e LineLayer declarativos conforme API da versão fixada.
6. Atualizar geometria quando a lista aceita muda.
7. Não redesenhar com pontos rejeitados.
8. Observar custo de recomposição durante smoke físico curto.
9. Separar erro de mapa e erro de GPS.
10. Registrar screenshot sanitizado sem rota/localização pessoal.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke físico deve confirmar mapa, marcador e crescimento da linha. Evidência versionada não pode conter localização pessoal.

## Definition of Done

- OpenFreeMap aparece via MapLibre Compose;
- atribuição está preservada;
- posição atual deriva da fonte GPS da PoC;
- rota usa somente pontos aceitos;
- LineString cresce durante tracking;
- mapa permanece interativo;
- falha do mapa-base não corrompe estado GPS;
- nenhum GeoJSON/rota é persistido;
- builds/lint passam.

## Riscos e cuidados

- recriar geometria inteira a cada segundo é aceitável para PoC curta, mas registrar qualquer jank observado para a spec de produção;
- não antecipar otimização de rotas longas sem evidência;
- verificar API real da versão 0.13.x antes de copiar snippets de documentação antiga;
- não remover attribution para ganhar espaço visual.
