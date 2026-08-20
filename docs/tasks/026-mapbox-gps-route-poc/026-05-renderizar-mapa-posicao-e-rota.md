# 026-05 — Renderizar mapa, posição e rota em tempo real

- **Ticker:** `026`
- **Número:** `05`
- **Status:** pending

## Objetivo

Renderizar Mapbox Standard na UI Compose e transformar os pontos GPS aceitos em uma rota visual atualizada progressivamente.

## Resultado esperado

Durante um ciclo ativo, a posição corrente e a linha da rota evoluem no mapa usando a mesma fonte de GPS da PoC, sem segunda fonte concorrente de localização.

## Dependências

- `026-04` concluída;
- Mapbox configurado e token público disponível;
- estado da PoC já fornece ponto corrente e lista de pontos aceitos.

## Escopo incluído

- `MapboxMap` Compose;
- Mapbox Standard;
- conversão `WorkoutRoutePoint` → `Point`;
- `LineString`;
- `GeoJsonSource`;
- `LineLayer`;
- representação da posição corrente;
- atualização incremental da geometria;
- estilo legível da rota.

## Escopo excluído

- Mapbox Location Component como fonte independente de verdade;
- Directions;
- Map Matching;
- geocoding;
- persistência;
- câmera final/follow completo, tratados em `026-06`.

## Fonte única de verdade

Obrigatório:

```text
GpsMapPocLocationSource
        ├── ponto corrente → posição no mapa
        └── pontos aceitos → LineString
```

Não usar simultaneamente outro provider de localização do Mapbox para definir a posição exibida.

## Regras de rota

- zero pontos: sem linha;
- um ponto: posição visível, sem LineString inválida;
- dois ou mais: desenhar `LineString`;
- somente pontos aceitos pelo filtro entram na source;
- ordem da geometria segue sequência aceita;
- atualização não pode bloquear a UI a cada fix.

## Estilo

- `line-cap` arredondado;
- `line-join` arredondado;
- largura adequada ao telefone;
- cor coerente com identidade Champion quando houver contraste suficiente;
- mapa-base permanece legível nos temas aplicáveis.

## Privacidade

- não logar `Point`, `LineString`, GeoJSON, bounds ou coordenadas;
- não salvar GeoJSON em arquivo;
- não exportar screenshot de localização real para o repositório.

## Passos de implementação

1. Renderizar Mapbox Standard no shell da PoC.
2. Tratar erro de token/configuração sem expor segredo.
3. Criar source GeoJSON derivada do estado aceito.
4. Adicionar `LineLayer`.
5. Adicionar representação de posição corrente usando o mesmo estado.
6. Atualizar geometria ao receber novos pontos.
7. Validar zero/um/múltiplos pontos.
8. Confirmar fluidez básica no aparelho.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke físico mínimo:

- mapa carrega;
- primeiro ponto posiciona a visualização;
- segundo ponto permite linha;
- novos pontos fazem a linha crescer;
- pan/zoom continuam responsivos;
- Logcat não contém coordenadas.

## Definição de pronto

- Mapbox Standard renderiza no aparelho;
- rota é visível com dois ou mais pontos;
- linha cresce somente com pontos aceitos;
- posição e rota usam a mesma fonte;
- mapa permanece interativo durante updates;
- nenhum serviço adicional Mapbox foi introduzido.
