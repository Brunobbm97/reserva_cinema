# Guia da Etapa 2: cadastro, login com JWT e papéis

Nesta etapa a API ganhou "porta de entrada": cadastro, login, renovação de tokens, logout,
e a separação entre o que um USER e um ADMIN podem fazer.

---

## Parte A: atualizar e rodar

1. Na pasta do projeto: `git pull` (ou **Git → Pull** no IntelliJ).
2. Na aba **Maven** do IntelliJ, clique em **Reload All Maven Projects** (entraram duas
   dependências novas: Spring Security e JJWT).
3. Rode a `ReservaCinemaApplication` como antes. No console, procure:

```
Migrating schema "public" to version "2 - refresh tokens"
Started ReservaCinemaApplication in ...
Admin inicial criado: admin@cinema.com
```

O Flyway viu que o banco estava na versão 1 e aplicou só a `V2`. O `AdminSeeder` criou o
admin (só na primeira vez; nas próximas ele percebe que já existe e não faz nada).

---

## Parte B: o fluxo, em uma imagem

```
 CADASTRO / LOGIN
 Cliente ── POST /api/auth/login {email, senha} ──▶ AuthController ──▶ AuthServiceImpl
                                                                       │ confere senha (BCrypt)
                                                                       │ gera access token (JWT, 15 min)
                                                                       │ gera refresh token (aleatório, 7 dias)
 Cliente ◀── {accessToken, refreshToken} ─────────────────────────────┘ salva o HASH do refresh no banco

 REQUISIÇÃO PROTEGIDA
 Cliente ── GET /api/users/me  +  header "Authorization: Bearer <accessToken>"
            │
            ▼
   JwtAuthenticationFilter: confere a assinatura e a validade do token
            │ ok → coloca o usuário no SecurityContext ("está logado")
            ▼
   SecurityConfig: a rota exige login? exige ADMIN? → 401 / 403 se não
            ▼
   UserController.me(@AuthenticationPrincipal usuário) ──▶ resposta

 ACCESS TOKEN EXPIROU (401)
 Cliente ── POST /api/auth/refresh {refreshToken} ──▶ revoga o antigo, devolve um par novo
```

---

## Parte C: o que cada classe faz

### Pacote `auth/` (nova funcionalidade)

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `AuthController` | Rotas públicas `/api/auth/*`. Só recebe, valida (`@Valid`) e repassa ao service | `register`, `login`, `refresh`, `logout` |
| `AuthService` / `AuthServiceImpl` | As regras de autenticação | `register`: cria USER com senha em BCrypt. `login`: mesma mensagem para e-mail ou senha errados. `refresh`: rotação + detecção de reuso. `issueTokens`: gera o par de tokens |
| `JwtService` / `JwtServiceImpl` | Cria e lê o JWT | `generateAccessToken`: coloca id, e-mail e papel no token e assina. `parseAccessToken`: confere assinatura e validade |
| `JwtAuthenticationFilter` | Roda antes de todo controller e "loga" o usuário a partir do header | `doFilterInternal` |
| `AuthenticatedUser` | O usuário logado (id, e-mail, papel), extraído do token | — |
| `JwtProperties` | Lê `app.jwt.*` do `application.properties` | — |
| `SecurityErrorWriter` | Escreve os erros 401/403 em JSON, já que os filtros rodam antes do `GlobalExceptionHandler` | `write` |
| `RefreshToken` (entidade) | Uma sessão: hash do token, validade, se foi revogado | `isUsable`, `revoke` |
| `RefreshTokenRepository` | Acesso à tabela `refresh_tokens` | `findByTokenHash`; `revokeAllByUserId` (um `UPDATE` escrito à mão com `@Query`) |
| `dto/*` | O formato do que entra e sai | `RegisterRequest` tem as validações do cadastro |

### Pacote `user/` (ganhou service e controller)

| Classe | O que faz | Métodos que valem a atenção |
|---|---|---|
| `UserController` | `GET /api/users/me` (qualquer logado), `GET /api/admin/users` e `PATCH /api/admin/users/{id}/role` (só ADMIN) | Repare que o usuário vem do token (`@AuthenticationPrincipal`), nunca de um parâmetro |
| `UserService` / `UserServiceImpl` | Regras sobre usuários | `changeRole`: impede o admin de tirar o próprio ADMIN; atualiza sem chamar `save()` (dirty checking) |
| `UserResponse` | O que a API mostra de um usuário (sem o hash da senha) | `from(user)` converte a entidade |

### Pacotes `config/` e `common/`

| Classe | O que faz |
|---|---|
| `SecurityConfig` | O "mapa de permissões": `/api/auth/**` é público, `GET` de filmes e sessões será público, `/api/admin/**` exige ADMIN, o resto exige login. Também define o BCrypt e encaixa o filtro de JWT |
| `AdminSeeder` | Cria o admin inicial ao subir a aplicação, se ele não existir |
| `ApiException` e filhas | Cada tipo de erro já sabe seu status HTTP (404, 409, 401, 400) |
| `GlobalExceptionHandler` | Transforma qualquer exceção no JSON padrão `ApiError` |

### Três conceitos novos que aparecem no código

- **Injeção de dependência pelo construtor:** os services recebem o que precisam
  (repositórios, `PasswordEncoder`...) no construtor. Quem cria e entrega esses objetos é o
  Spring. Você nunca escreve `new AuthServiceImpl(...)`.
- **`@Transactional`:** tudo dentro do método acontece numa transação. Se der erro no meio, o
  banco desfaz tudo. Veja o caso especial `noRollbackFor` no `refresh`.
- **`record`:** classe imutável e enxuta. Usamos para DTOs, que são só "envelopes" de dados.

---

## Parte D: testar no Insomnia ou Postman

Crie as requisições abaixo. Em todas as que têm corpo, use **Body → JSON**.

**1. Cadastrar um usuário**
`POST http://localhost:8080/api/auth/register`
```json
{ "name": "Maria", "email": "maria@teste.com", "password": "senha1234" }
```
Resposta `201` com `accessToken` e `refreshToken`. Copie os dois.

**2. Ver "quem sou eu"**
`GET http://localhost:8080/api/users/me`
Na aba **Auth**, escolha **Bearer Token** e cole o `accessToken`.
Resposta `200` com seus dados. Teste também **sem** o token: `401`.

**3. Tentar uma rota de admin com usuário comum**
`GET http://localhost:8080/api/admin/users` com o token da Maria → `403`.

**4. Logar como admin**
`POST http://localhost:8080/api/auth/login`
```json
{ "email": "admin@cinema.com", "password": "admin123" }
```
Use o `accessToken` do admin no `GET /api/admin/users` → `200` com a lista.

**5. Promover a Maria a ADMIN** (com o token do admin)
`PATCH http://localhost:8080/api/admin/users/{id da Maria}/role`
```json
{ "role": "ADMIN" }
```

**6. Renovar os tokens**
`POST http://localhost:8080/api/auth/refresh`
```json
{ "refreshToken": "<cole o refreshToken>" }
```
Funciona uma vez. Mande **o mesmo** refresh token de novo: `401 "Refresh token revogado"`.
Isso é a rotação (decisão D12).

**7. Logout**
`POST http://localhost:8080/api/auth/logout` com `{ "refreshToken": "..." }` → `204`.

**Experimentos que valem a pena:**
- Cadastre com e-mail inválido e senha curta e leia a lista `details` do erro `400`.
- Cole o `accessToken` em [jwt.io](https://jwt.io) e veja o conteúdo: `sub` (id), `email`,
  `role`, `iat` e `exp`. Note que qualquer um consegue **ler** o JWT; o que ninguém consegue é
  **alterar** sem a chave secreta. Por isso nunca colocamos dados sensíveis nele.
- Mude `app.jwt.access-token-minutes` para `1`, reinicie, espere 1 minuto e use o token: `401`.

---

## Parte E: ver no pgAdmin

- Tabela `users`: botão direito → **View/Edit Data → All Rows**. Repare na coluna
  `password_hash`: começa com `$2a$10$...` (o `10` é o "custo" do BCrypt). Cadastre dois
  usuários com a mesma senha e compare: os hashes são diferentes por causa do sal.
- Tabela `refresh_tokens`: cada login cria uma linha. Faça um refresh e veja a linha antiga
  com `revoked = true` e uma nova criada. O valor em `token_hash` **não** é o token que você
  recebeu, é o SHA-256 dele.
- Tabela `flyway_schema_history`: agora tem duas linhas (V1 e V2).
