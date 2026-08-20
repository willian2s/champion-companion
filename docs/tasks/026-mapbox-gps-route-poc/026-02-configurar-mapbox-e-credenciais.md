# 026-02 — Configurar Mapbox, credenciais e isolamento Gradle

- **Ticker:** `026`
- **Número:** `02`
- **Status:** pending

## Objetivo

Adicionar Mapbox Maps SDK e extensão Compose de forma controlada, sem versionar segredos e sem criar entrypoint de produção para a PoC.

## Resultado esperado

Build debug consegue resolver e empacotar Mapbox; release continua compilável e sem superfície da PoC; nenhum token secreto entra no repositório.

## Dependências

- `026-01` concluída;
- ADR experimental registrada;
- credenciais Mapbox disponíveis localmente para validação.

## Escopo incluído

- repositório Maven Mapbox;
- versão `11.26.0` inicialmente prevista, confirmada no momento da execução;
- artefatos `-ndk27` quando compatíveis;
- aliases no version catalog;
- dependências necessárias à variante debug;
- leitura segura de `MAPBOX_DOWNLOADS_TOKEN`;
- leitura/injeção segura de `MAPBOX_ACCESS_TOKEN`;
- falha clara quando credencial requerida estiver ausente.

## Escopo excluído

- tela da PoC;
- GPS;
- rota;
- Activity;
- persistência;
- Map Matching, Directions ou geocoding.

## Arquivos prováveis

- `settings.gradle.kts`;
- `gradle/libs.versions.toml`;
- `app/build.gradle.kts`;
- `.gitignore` somente se tecnicamente necessário;
- documentação local de setup sem valores de token.

## Regras de credenciais

### Downloads token

- nome esperado: `MAPBOX_DOWNLOADS_TOKEN`;
- secreto;
- scope mínimo `Downloads:Read`;
- preferir `~/.gradle/gradle.properties`, variável de ambiente ou secret de CI;
- nunca expor em `BuildConfig`, recurso Android, Logcat ou documentação versionada.

### Public access token

- nome local sugerido: `MAPBOX_ACCESS_TOKEN`;
- token público dedicado ao Champion Companion;
- somente scopes públicos necessários;
- nunca usar token secreto dentro do APK;
- nunca registrar o valor.

## Isolamento esperado

```text
debug   → pode conter dependência/entrypoint da PoC
release → sem entrypoint da PoC
```

Preferir `debugImplementation` quando a resolução de dependências e o source set permitirem.

## Passos de implementação

1. Confirmar versão Mapbox prevista e requisitos Maven atuais.
2. Configurar repositório autenticado sem segredo versionado.
3. Adicionar aliases do SDK e Compose.
4. Isolar dependências para debug quando possível.
5. Configurar acesso ao public token para runtime debug.
6. Garantir mensagem de configuração segura quando token local faltar.
7. Confirmar que release não passa a expor superfície experimental.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Também verificar no diff e busca textual que nenhum valor real de token foi adicionado.

## Definição de pronto

- dependências Mapbox resolvem;
- debug compila;
- release compila;
- nenhum segredo está versionado;
- credenciais possuem nomes e origem documentados;
- isolamento definido na spec foi preservado;
- task registra qualquer desvio de versão/artefato antes de concluir.

## Riscos

- autenticação Maven afetar configuração global do build;
- segredo vazar em erro ou propriedade gerada;
- dependência debug ainda influenciar resolução release;
- versão/artefato previsto ter mudado desde a criação da spec.
