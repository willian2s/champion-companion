# 013-06 — Implementar prontidão GPS, qualidade GNSS e filtro de pontos

- **Ticker:** `013`
- **Número:** `06`
- **Status:** pending

## Objetivo e resultado esperado

Criar fronteira de localização precisa, classificação de readiness e filtro determinístico de pontos, sem UI ativa nem persistência de rota em produção. Resultado: stream tipado pronto para preparação/captura.

## Contexto

Projeto não possui runtime de localização. Prontidão deve usar fix/idade/precisão como fonte primária e GNSS apenas como complemento. Mock e pontos ruins são proibidos.

## Documentos obrigatórios

Spec 013; ADR-014; `013-03/04/05`; documentação Android `LocationManager`, `Location`, `GnssStatus` e permissões da API alvo.

## Dependências

`013-05` concluída.

## Arquivos permitidos

Novos `core/location/**` ou caminho real aprovado; contratos em `core/model`; bindings Hilt; logging seguro; task/overview.

## Arquivos e símbolos prováveis

Novos `WorkoutLocationSource`, `GpsReadinessEvaluator`, `GnssQualitySnapshot`, `RoutePointFilter`, `RoutePointRejection` e `WorkoutRouteAccumulator`.

## Arquivos proibidos

UI final, FGS, repositories Room salvo adaptação mínima já aprovada, BLE, Health Connect e testes.

## Requisitos cobertos

Localização precisa; estados GPS; thresholds; GnssStatus opcional; mock; filtro; segmentos; categorias seguras de logging.

## Escopo

- adapter Android de permissão/serviços/fix;
- high accuracy durante preparação/sessão;
- readiness forte/bom/fraco/inutilizável;
- texto semântico com precisão/satélites;
- filtro puro com motivos categorizados;
- cálculo geodésico entre pontos aceitos e regras de gap/segmento.

## Fora do escopo

Persistir aquecimento, tela, countdown, serviço, mapa ou enviar HC.

## Passos de implementação

1. Implementar fonte com lifecycle explícito e cancelamento.
2. Detectar permissão negada/aproximada, provider desligado e fix mock.
3. Classificar fix recente `<=10 s`; forte `<=10 m`; bom `<=20 m`; fraco `<=50 m`; demais indisponível.
4. Complementar satélites/C/N0 quando API fornecer, sem bloquear readiness pela ausência.
5. Implementar filtro de ordem, idade, precisão, coordenadas, velocidade humana, duplicata, mock e ruído.
6. Emitir accepted/rejected com categoria, nunca coordenada em logs.
7. Definir gap prolongado ajustável e criação de segmento após retomada/perda/recuperação.
8. Garantir cleanup e rejeição de callbacks por geração antiga.
9. Atualizar overview.

## Critérios de aceite

- iniciar fica conceitualmente elegível só em bom/forte;
- precisão não é chamada de força de rádio;
- ausência de GNSS detalhado não impede avaliação por fix;
- ponto rejeitado não entra em distância;
- pausa/lacuna não conecta segmentos;
- logs expõem só contagens/categorias/faixa.

## Validação manual

No Galaxy S22, observar estados com permissão negada/aproximada, localização desligada, indoor/outdoor, fix envelhecido e mock quando reproduzível. Comparar texto com precisão real sem registrar coordenadas.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Matriz estado→entrada, logs sanitizados, captura de estados sem coordenadas e saídas Gradle.

## Screenshots

Somente se houver superfície diagnóstica temporária aprovada; ocultar coordenadas e dados pessoais.

## Definição de pronto

Fonte/readiness/filtro compilam, têm cleanup e desbloqueiam preparação; overview atualizado.

## Riscos e cuidados

APIs/valores variam por aparelho. Thresholds são direção inicial ajustável após validação física. Não armazenar ponto de aquecimento.

## Rollback

Remover binding/runtime e manter contratos inertes; nenhuma migration nova.

Não criar, alterar ou executar testes automatizados.
