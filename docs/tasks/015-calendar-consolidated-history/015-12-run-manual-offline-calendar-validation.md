# 015-12 — Executar validação manual offline do calendário

- **Status:** pending
- **Objetivo:** validar fluxo integrado no Galaxy S22 sem fontes externas.
- **Resultado esperado:** evidências manuais, screenshots e builds obrigatórios.

## Documentos obrigatórios

Spec 015, overview, tasks 015-01 a 015-11, baseline atualizada durante a
implementação e instruções de privacidade.

## Dependências e entradas

- 015-11 concluída.
- Galaxy S22 disponível.
- Dados locais reais já persistidos.

## Arquivos permitidos

Este arquivo, overview, evidências e screenshots em local documental aprovado.

## Arquivos proibidos

Código, ADR, roadmap, schema, testes, alterações de dados remotos, BLE,
Health Connect e permissões.

## Escopo

Executar somente validação manual com Bluetooth desligado, relógio desconectado
e Health Connect indisponível ou sem permissão quando reproduzível. Confirmar
abertura, mês anterior/próximo, Hoje, seleção, detalhe, atividade, BPM, passos,
distância, estados vazios/erro e retorno.

## Fora do escopo

Qualquer sincronização, conexão, provider, coleta, teste automatizado ou
correção não documentada.

## Passos

1. Executar build completo.
2. Instalar debug.
3. Desligar Bluetooth e desconectar relógio.
4. Tornar Health Connect indisponível quando reproduzível.
5. Abrir calendário e navegar meses.
6. Abrir dia com e sem dados.
7. Abrir históricos e atividade relacionada.
8. Conferir peso mais recente, medidas adicionais e atividade descartada ausente.
9. Conferir meta atual, origem, última atualização, estimativa e unidade.
10. Conferir períodos 7/30/90, gráficos, listas, lacunas, zero real e dias
    futuros.
11. Conferir marcadores, legenda, seleção, Hoje, primeiro dia da semana,
    acessibilidade e dias fora do mês.
12. Conferir estados `Sem operações`, `Pendente`, `Sincronizando`,
    `Sincronizado`, `Falha` e `Parcial`, incluindo aggregate ausente, operação
    órfã e degradação parcial segura.
13. Conferir ausência de permissões/operações externas e ausência de dados
    sensíveis no Logcat.
14. Capturar screenshots visuais.
15. Registrar resultado por cenário e riscos residuais.

Os 39 cenários devem ser percorridos explicitamente: 1 abrir Calendário; 2
confirmar mês atual; 3 mês anterior; 4 mês seguinte; 5 Hoje; 6 indicadores;
7 selecionar dia; 8 abrir detalhe; 9 voltar preservando mês; 10 passos; 11 meta
atual; 12 distância estimada; 13 BPM; 14 atividades; 15 peso; 16 estados de
sync; 17 atividade descartada ausente; 18 detalhe de passos; 19 detalhe de
distância; 20 frequência cardíaca; 21 atividade; 22 voltar ao mesmo dia; 23
períodos de passos 7/30/90; 24 gráfico de passos; 25 lista de passos; 26
períodos de distância 7/30/90; 27 gráfico de distância; 28 lista de distância;
29 lacunas sem zero; 30 Bluetooth desligado; 31 Health Connect indisponível
quando possível; 32 calendário offline; 33 detalhe offline; 34 meses offline;
35 nenhuma permissão; 36 nenhum BLE; 37 Logcat sem dados sensíveis; 38
acessibilidade; 39 screenshots.

## Critérios de aceite

- Builds obrigatórios passam.
- Instalação passa.
- Calendário, detalhe e históricos abrem offline.
- Nenhum prompt de permissão aparece.
- Nenhuma operação BLE começa.
- Nenhuma operação Health Connect começa.
- Screenshots cobrem mudanças visuais.
- Atividade `DISCARDED` não aparece; atividades pendentes permanecem visíveis.
- Resumo diário mostra origem e última atualização; distância mostra estimativa e
  usa quilômetros somente na apresentação.
- Peso mais recente e medidas adicionais respeitam ordenação.
- Estados de sync e degradação parcial não são confundidos com vazio.

## Validação manual

Esta task é a validação manual no Galaxy S22 descrita acima. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar saída dos comandos, versão instalada, configuração offline,
screenshots, cenários aprovados, falhas e riscos residuais. Atualizar overview.

## Validação final

Confirmar que não houve acesso BLE ou Health Connect e que nenhum teste foi
executado. Não criar, alterar ou executar testes automatizados.

## Contexto

Fase 15 deve permanecer útil com fontes externas desligadas. Esta task comprova
o comportamento local em dispositivo físico.

## Arquivos alterados

Inicialmente nenhum; registrar somente evidências e screenshots.

## Decisões e desvios

Registrar configuração offline, cenários não reproduzíveis e desvios aprovados.

## Screenshot

Exigido para todas as superfícies visuais alteradas e cenários representativos.

## Riscos residuais

Disponibilidade variável do Health Connect pode impedir reprodução de um cenário;
registrar condição exata sem acessar provider fora do escopo.

## Atualização do overview

Marcar somente o item 015-12 após evidências completas de 015-11.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
