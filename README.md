# PlacePay — PDV Android (Maquineta)

App de **Ponto de Venda (PDV)** para maquinetas Android: vendas, pagamentos, caixa,
produtos, clientes, comprovantes e integração com um backend SaaS. Feito para rodar
em maquineta (tela pequena, botões grandes) e **funcionar mesmo sem internet**.

> Novo no projeto? Leia este README primeiro e depois `CLAUDE.md` (convenções) e
> `HANDOFF.md` (guia de entrada). O `ROADMAP.md` tem o histórico completo de decisões.

---

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Clean Architecture** (data / domain / presentation) + **MVVM**
- **Room** (banco local — offline-first)
- **Retrofit / OkHttp / Gson** (integração com o SaaS)
- **Hilt** (injeção de dependência)
- **Coroutines / Flow**
- **CameraX + ML Kit** (leitura de código de barras)

## Estrutura do código

Tudo em `app/src/main/java/com/example/pdvmaquineta/`:

- **`data/`** — Room (entities, dao, migrations), repositórios, sincronização com o SaaS (`data/sync`) e injeção de dependência (`data/di`).
- **`domain/`** — modelos, casos de uso (use cases) e regras de autorização (`AuthorizationPolicy` / `Permission`).
- **`presentation/`** — telas Compose + ViewModels, separadas por área: `sale` (venda), `cash` (caixa), `customer` (clientes), `settings` (configurações), etc.

## Como rodar

1. Tenha o **Android Studio** instalado (versão recente).
2. Clone o repositório:
   ```
   git clone https://github.com/Floball2026/PlacePay.git
   ```
3. Abra a pasta no Android Studio e **espere o Gradle Sync** terminar (a primeira vez demora alguns minutos).
4. `local.properties` **não vem no clone** (está no `.gitignore`). O Android Studio normalmente cria sozinho ao abrir; se não criar, faça um apontando `sdk.dir` para o seu Android SDK.
5. Escolha um **emulador** (ou uma maquineta/aparelho conectado) e clique em **Run ▶**.
6. **Login inicial de teste:** usuário `admin`, senha `000000` (o admin é recriado automaticamente quando não existe nenhum admin ativo; para resetar, limpe os dados do app).

## Integração com o SaaS (backend)

O app sincroniza com o SaaS **Asset-Integrator** (base: `https://asset-integrator-labsyuca.replit.app/api`).
Fatias já integradas: **carga de produtos (PLU)**, **envio de vendas** (fila outbox), **heartbeat**,
**operadores**, **clientes**, **config** e **checagem de versão**. O terminal precisa ser **ativado**
em Configurações antes de sincronizar.

## Configuração local (feita pelo gestor no terminal)

A tela de **Configurações** (acessível a **admin** e **supervisor**) guarda a config do negócio
**no próprio aparelho** — não é alterada remotamente. Inclui: pedir CPF (lembrete, não obrigatório),
desconto máximo, formas de pagamento aceitas, exigir supervisor para correção/cancelamento e cashback.

## Convenções importantes (leia antes de mexer)

- **Offline-first:** toda escrita crítica (venda, pagamento, caixa) grava **local primeiro** e sincroniza depois. Nenhuma tela crítica pode travar se a API estiver fora do ar.
- **Migrações do Room:** são escritas à mão e **testadas em aparelho físico**. **Nunca** usar `fallbackToDestructiveMigration` (apagaria vendas/caixa reais). Ver `HANDOFF.md`.
- **Autorização centralizada** em `AuthorizationPolicy` / `Permission` — não espalhar regra de perfil (operador/supervisor/admin) pela UI.
- **Pagamento real** só atrás de interface com **mock** — não plugar SDK real da adquirente sem confirmação.
- **Dinheiro sempre em centavos** (`Long`).

## Fluxo de trabalho (Git)

- **`main` = versão estável.** Trabalhe em uma **branch** e abra um **Pull Request** para juntar.
- Ciclo de cada mudança: **Pull → alterar → testar no Android Studio → Commit → Push**.
- Não suba código que não compila ou não roda.

## Documentação complementar

- **`CLAUDE.md`** — convenções e stack completos.
- **`HANDOFF.md`** — guia de entrada + regra de migrações.
- **`ROADMAP.md`** — histórico de fases, decisões e pendências conscientes.
- **`docs/`** — documentos adicionais.
