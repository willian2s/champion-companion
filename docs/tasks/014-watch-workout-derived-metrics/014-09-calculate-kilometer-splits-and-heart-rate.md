# 014-09 — Calcular splits de quilômetro e frequência cardíaca

- **Ticker:** `014`
- **Número:** `09`
- **Status:** pending

## Objetivo e resultado esperado

Gerar splits cumulativos de 1 km com tempo interpolado linearmente e BPM
calculado por sobreposição temporal de amostras reais. Resultado: splits
determinísticos, ausentes quando série não é elegível.

## Contexto

Série canônica de `014-08` fornece janelas e distâncias por intervalo. Amostras
BPM atuais têm timestamps reais por atividade.

## Requisitos cobertos

- R7 e fronteiras temporais de R4/R5.

## Dependências

- `014-08` concluída.

## Arquivos permitidos

- novos contratos/políticas de split em `core/model`;
- read models/mappers de atividade;
- persistência de snapshot, se contrato de `014-02/03` exigir;
- overview.

Símbolos prováveis: `WorkoutSplit`, calculador de limites, interpolador temporal e
agregador de `WorkoutHeartRateSample`.

## Arquivos proibidos

- parser, perfil, Compose e Health Connect;
- Gradle, manifesto, migrations e schemas;
- BPM sintético ou médias importadas como amostras.

## Escopo

- limites 1.000 m incrementais;
- split completo e parcial final claramente distinguido, se exibido;
- interpolação dentro do intervalo;
- BPM por sobreposição temporal.

## Fora do escopo

- suavização, zonas de BPM ou estimativa de amostra ausente;
- UI/gráficos;
- record Health Connect por split;
- testes automatizados.

## Passos de implementação

1. Percorrer acumulado canônico em ordem temporal.
2. Emitir cada cruzamento de múltiplo de 1.000 m.
3. Interpolar instante pela fração linear no intervalo cruzado.
4. Formar janelas de split contíguas.
5. Agregar somente amostras reais com sobreposição temporal definida.
6. Representar BPM ausente quando janela não contiver amostra.
7. Falhar fechado para série inelegível ou não monotônica.
8. Atualizar overview.

## Critérios de aceite

- Limites ocorrem exatamente em 1.000, 2.000, 3.000 m etc.
- Cruzamento interno usa interpolação linear reproduzível.
- BPM não usa média global nem amostra fora da janela.
- Zero distância não cria split.
- Série incompleta/inelegível não cria split enganoso.

## Validação manual

- Conferir atividade com limite dentro de intervalo.
- Conferir múltiplos limites, último parcial e ausência de BPM.
- Comparar timestamps e BPM com linha do tempo sanitizada.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela de cruzamentos/frações/instantes;
- janelas e amostras BPM consideradas;
- builds e smoke do read model.

## Definição de pronto

Splits determinísticos e BPM temporal disponíveis para UI, com item `014-09`
marcado.

## Riscos e cuidados

- Definir fronteiras semiabertas para não contar amostra duas vezes.
- Erros de ponto flutuante exigem comparação estável sem alterar distância.
- Não revelar BPM/timestamps individuais em evidência pública.

## Rollback

Ocultar splits e retornar lista vazia/ausência; preservar série canônica.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-09` e registrar convenção de
fronteiras temporais.
