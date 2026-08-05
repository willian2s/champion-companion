# 014-09 — Calcular splits e BPM temporal

- **Ticker:** `014`
- **Número:** `09`
- **Status:** pending

## Objetivo e resultado esperado

Gerar splits cumulativos de 1 km com tempo interpolado linearmente e BPM
ponderado por sobreposição temporal das amostras reais.

Resultado: splits determinísticos, com fronteiras estáveis e ausentes quando a
série não for elegível.

## Dependências

- `014-08` concluída.

## Arquivos permitidos

- novos contratos/políticas de split em `core/model`;
- read models/mappers de atividade;
- persistência de snapshot, se prevista em `014-02/03`;
- overview.

## Arquivos proibidos

- parser, perfil, Compose e Health Connect;
- Gradle, manifesto, migrations e schemas;
- BPM sintético ou média global importada.

## Escopo

### Splits

- limites em `1.000 m`, `2.000 m`, `3.000 m` e seguintes;
- cruzamento interno usa interpolação linear pela fração da distância;
- janelas contíguas e semiabertas `[start, end)`;
- split parcial final, quando apresentado, é identificado separadamente;
- série inelegível ou não monotônica não produz split.

### BPM temporal

Cada amostra real `i` representa sample-and-hold em:

```text
[sampledAt_i, sampledAt_i+1)
```

A última amostra é limitada pelo fim da sessão. Para cada split:

1. recortar cada intervalo de validade pela janela do split;
2. multiplicar BPM pela duração da sobreposição;
3. dividir pela soma das durações cobertas;
4. manter BPM ausente quando a cobertura for zero.

Amostra exatamente no início pertence ao split; amostra exatamente no fim
pertence ao próximo, evitando dupla contagem.

## Fora do escopo

- suavização, zonas de BPM ou estimativa de amostra ausente;
- UI/gráficos;
- record Health Connect por split;
- testes automatizados.

## Passos de implementação

1. Percorrer o acumulado canônico em ordem temporal.
2. Emitir cada cruzamento de múltiplo de 1.000 m.
3. Interpolar instante pela fração linear no intervalo cruzado.
4. Formar janelas semiabertas de split.
5. Construir intervalos sample-and-hold para BPM real.
6. Agregar por duração de sobreposição.
7. Representar ausência quando não houver cobertura.
8. Falhar fechado para série inelegível ou não monotônica.
9. Atualizar overview.

## Critérios de aceite

- Limites ocorrem exatamente nos múltiplos de 1 km.
- Cruzamento interno usa interpolação reproduzível.
- Janelas não se sobrepõem nem deixam dupla contagem na fronteira.
- BPM não usa média simples sem tempo, média global ou amostra fora da janela.
- Sem cobertura, BPM permanece ausente.
- Zero distância não cria split.
- Série implausível bloqueada pela política não produz split funcional.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela de cruzamentos/frações/instantes;
- janelas semiabertas;
- intervalos BPM considerados e durações de sobreposição;
- casos de fronteira e ausência;
- builds e smoke do read model.

## Definição de pronto

Splits e BPM temporal determinísticos estão disponíveis para UI, com item
`014-09` marcado.

## Rollback

Ocultar splits e retornar lista vazia/ausência, preservando a série canônica.
