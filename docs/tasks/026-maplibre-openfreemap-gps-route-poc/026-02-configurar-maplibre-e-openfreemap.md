# 026-02 — Configurar MapLibre e OpenFreeMap

- **Ticker:** `026`
- **Número:** `02`
- **Status:** pending

## Objetivo

Adicionar a dependência estável do MapLibre Compose e configurar o mapa-base OpenFreeMap sem conta, token, API key, Maven privado ou forma de pagamento.

## Resultado esperado

O build debug consegue renderizar um mapa-base OpenFreeMap via MapLibre Compose, enquanto a variante release continua compilando normalmente e nenhum segredo novo entra no projeto.

## Requisitos e critérios cobertos

- Spec 026: critérios 6 a 10 e 31 a 32;
- MapLibre Compose vindo do Maven Central;
- OpenFreeMap `Positron` como style inicial;
- atribuição cartográfica preservada;
- ausência de credenciais de mapa.

## Escopo incluído

- confirmar versão estável vigente do MapLibre Compose antes de editar Gradle;
- adicionar entrada no version catalog;
- adicionar dependência no app;
- confirmar se `debugImplementation` é tecnicamente compatível com a organização dos source sets;
- utilizar `implementation` somente se necessário e documentar o motivo;
- confirmar Maven Central já disponível antes de alterar `settings.gradle.kts`;
- validar style público do OpenFreeMap;
- criar composição mínima de smoke somente se necessária para confirmar integração técnica.

## Escopo excluído

- tokens;
- secrets;
- `local.properties` para mapa;
- Maven privado;
- billing;
- GPS;
- rota;
- câmera de tracking;
- Room;
- Health Connect.

## Dependências

- `026-01` concluída;
- ADR da PoC vigente;
- documentação oficial do MapLibre Compose e OpenFreeMap revalidada no dia da execução.

## Versão inicial planejada

Planejamento de 2026-08-20:

```text
org.maplibre.compose:maplibre-compose:0.13.0
```

A task deve usar a release estável vigente confirmada no momento da implementação. Snapshot só pode entrar por nova decisão explícita.

## Style inicial

```text
https://tiles.openfreemap.org/styles/positron
```

Fallback permitido para investigação:

```text
https://tiles.openfreemap.org/styles/liberty
```

Se o fallback virar escolha efetiva, registrar desvio e motivo.

## Arquivos prováveis

- `gradle/libs.versions.toml`;
- `app/build.gradle.kts`;
- `settings.gradle.kts` somente se realmente necessário;
- arquivos debug mínimos de smoke, se indispensáveis;
- overview 026.

## Passos de implementação

1. Confirmar release estável e coordenadas Maven oficiais.
2. Confirmar termos atuais do OpenFreeMap e que continuam sem cadastro/API key/pagamento.
3. Confirmar Maven Central no repositório atual.
4. Adicionar dependência MapLibre Compose com menor impacto possível.
5. Configurar `Positron` como style URI do mapa-base.
6. Não desabilitar attribution/ornamentos necessários à atribuição.
7. Não adicionar qualquer mecanismo de token ou secret.
8. Validar configuração de dependências e builds.
9. Atualizar overview com arquivos, versão real, comandos e desvios.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Se houver superfície visual mínima executável nesta task:

```bash
./gradlew installDebug
```

## Definition of Done

- MapLibre Compose está resolvido por Maven Central;
- OpenFreeMap é configurado sem credenciais;
- style inicial está documentado;
- atribuição não foi removida;
- nenhum token, API key ou billing foi adicionado;
- `assembleDebug` passa;
- `assembleRelease` passa;
- lint passa;
- overview registra versão e estratégia Gradle realmente usadas.

## Riscos e cuidados

- API Compose pode diferir entre releases; implementar conforme versão fixada, não conforme snapshot;
- MapLibre Compose ainda não cobre toda a superfície do SDK nativo; confirmar somente APIs necessárias à PoC;
- OpenFreeMap não oferece SLA;
- não introduzir repositórios adicionais sem necessidade comprovada;
- se a dependência em `debugImplementation` inviabilizar referências do source set, preferir solução simples e documentada a hacks de Gradle.
