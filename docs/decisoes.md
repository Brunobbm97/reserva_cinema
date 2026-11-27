# Registro de decisões de arquitetura

Este arquivo registra cada decisão importante do projeto: o contexto, as alternativas
consideradas e o porquê da escolha. A ideia é que cada entrada possa virar um post de
LinkedIn ou a seção de um artigo.

Formato de cada decisão: **Contexto → Alternativas → Decisão → Por quê → Consequências**.

---

## D01. Java 21 + Spring Boot 4

- **Contexto:** precisamos de uma linguagem e framework para uma API REST com banco relacional, autenticação e regras de negócio.
- **Alternativas:** Node.js (Express/NestJS), Python (Django/FastAPI), Java com Spring Boot.
- **Decisão:** Java 21 (versão LTS) com Spring Boot 4.1.
- **Por quê:** Spring Boot é muito usado no mercado brasileiro, tem integração pronta com JPA, segurança (Spring Security) e validação. Java 21 é a versão de suporte longo mais recente com recursos modernos (records, pattern matching).
- **Consequências:** mais código "cerimonial" que em Node ou Python, mas a estrutura fica explícita, o que ajuda quem está aprendendo a entender cada camada.

## D02. PostgreSQL com JPA/Hibernate

- **Contexto:** reservas envolvem dinheiro e concorrência; precisamos de consistência forte.
- **Alternativas:** banco NoSQL (MongoDB), MySQL, PostgreSQL.
- **Decisão:** PostgreSQL acessado via JPA/Hibernate (Spring Data JPA).
- **Por quê:** dados muito relacionais (filme → sessão → sala → assento → reserva), transações ACID e recursos como índices parciais, que usamos contra overbooking (D05). O JPA evita escrever SQL repetitivo para operações simples.
- **Consequências:** é preciso entender o que o Hibernate faz por baixo (consultas geradas, carregamento LAZY) para não ter surpresas de desempenho.

## D03. Flyway para criar o banco, Hibernate só valida

- **Contexto:** o esquema do banco vai mudar ao longo das etapas (ex.: tabela de pagamentos, refresh tokens).
- **Alternativas:** deixar o Hibernate criar/alterar tabelas sozinho (`ddl-auto=update`); usar Flyway ou Liquibase com scripts versionados.
- **Decisão:** Flyway com arquivos SQL (`V1__modelo_inicial.sql`, `V2__...`), e `ddl-auto=validate`.
- **Por quê:** o `update` do Hibernate é imprevisível (não apaga colunas, não cria certas constraints) e não deixa histórico. Com Flyway, cada mudança é um arquivo SQL versionado no Git, e escrevemos as regras importantes (CHECK, UNIQUE, índices) explicitamente. O `validate` faz a aplicação falhar ao subir se as entidades Java não baterem com o banco.
- **Consequências:** toda mudança de tabela exige um novo arquivo de migração. Nunca editamos uma migração que já rodou.

## D04. Várias salas, cada uma com seu mapa de assentos

- **Contexto:** o cinema tem salas de tamanhos diferentes.
- **Alternativas:** uma capacidade numérica por sessão (ex.: "100 lugares"); assentos individuais por sala.
- **Decisão:** tabelas `rooms` e `seats` (fileira + número, ex.: C7). A sessão (`showtimes`) aponta para uma sala e herda o mapa dela.
- **Por quê:** o usuário escolhe o assento exato, e o mapa é cadastrado uma vez por sala, não por sessão. A capacidade da sessão é simplesmente a quantidade de assentos da sala.
- **Consequências:** a disponibilidade de uma sessão é calculada como "assentos da sala menos assentos com reserva ativa naquela sessão".

## D05. Proteção contra overbooking: índice único parcial no banco

- **Contexto:** duas pessoas podem tentar reservar o mesmo assento no mesmo segundo. Se a aplicação só verificar "está livre?" e depois inserir, as duas verificações podem passar e o assento é vendido duas vezes (condição de corrida).
- **Alternativas:**
  1. Verificar no código Java antes de salvar (não é seguro sob concorrência).
  2. Lock pessimista (`SELECT ... FOR UPDATE`) na sessão ou nos assentos.
  3. Lock otimista com coluna de versão.
  4. Restrição de unicidade no banco + transação.
- **Decisão:** opção 4. A tabela `reservation_seats` tem um índice único em `(showtime_id, seat_id)` **apenas para linhas ativas** (`WHERE active`). A reserva é salva dentro de uma transação; se o banco recusar por duplicidade, a API responde 409 Conflict ("assento já reservado").
- **Por quê:** é a opção mais simples de entender e a mais confiável: quem garante a regra é o banco, que é o único ponto que enxerga todas as requisições ao mesmo tempo. Locks funcionam, mas exigem entender níveis de isolamento e podem causar esperas e deadlocks.
- **Por que parcial (`WHERE active`):** ao cancelar uma reserva, marcamos o assento como `active = false` em vez de apagar a linha. Assim o assento volta a ficar livre e o histórico continua existindo para os relatórios.
- **Consequências:** `showtime_id` fica repetido em `reservation_seats` (já existe em `reservations`). É uma duplicação proposital, porque um índice só pode usar colunas da própria tabela.

## D06. Preço fixo por sessão e dinheiro em BigDecimal

- **Contexto:** precisamos cobrar pelas reservas.
- **Alternativas:** preço por tipo de assento (VIP, comum), meia-entrada, preço fixo por sessão.
- **Decisão:** preço fixo por sessão, em `NUMERIC(10,2)` no banco e `BigDecimal` no Java. A reserva guarda o `total_amount` calculado no momento da compra.
- **Por quê:** é o modelo mais simples para começar. `double` tem erros de arredondamento (0.1 + 0.2 = 0.30000000000000004), inaceitáveis para dinheiro. Guardar o total na reserva protege o histórico se o preço da sessão mudar depois.
- **Consequências:** preços diferenciados ficam para uma evolução futura (seria uma nova coluna ou tabela de categorias de assento).

## D07. Horários em UTC (`Instant` / `TIMESTAMPTZ`)

- **Contexto:** sessões têm data e hora, e servidor, banco e cliente podem estar em fusos diferentes.
- **Alternativas:** `LocalDateTime` (sem fuso), `Instant` (ponto exato no tempo, UTC).
- **Decisão:** `Instant` no Java, `TIMESTAMPTZ` no PostgreSQL, Hibernate configurado em UTC.
- **Por quê:** evita bugs clássicos de horário de verão e fuso. A conversão para o horário local fica para quem exibe (o cliente).
- **Consequências:** ao listar "sessões de um dia", precisamos converter o dia local para um intervalo em UTC.

## D08. Organização por funcionalidade, com subpastas por camada, e código em inglês

- **Contexto:** precisamos decidir como dividir as pastas e em que idioma nomear o código.
- **Alternativas:**
  1. Pacotes só por camada (`controller/`, `service/`, `repository/` na raiz), misturando todas as funcionalidades.
  2. Pacotes só por funcionalidade (`movie/`, com tudo solto dentro).
  3. Por funcionalidade e, dentro de cada uma, subpastas por camada.
- **Decisão:** opção 3. Cada funcionalidade (`user`, `movie`, `room`, `showtime`, `reservation`) tem `entity/`, `repository/`, `service/` e, mais adiante, `controller/` e `dto/`. Código em inglês, comentários e documentação em português.
- **Por quê:** tudo de "filmes" fica junto (fácil de achar), e dentro dela as camadas ficam separadas (fácil de saber onde cada coisa mora). Na primeira versão as classes ficavam soltas dentro da funcionalidade; mudamos ao perceber que, com services, controllers e DTOs, a pasta ficaria cheia demais. Inglês é o padrão do mercado e combina com o próprio Java e Spring (`findBy...`, `save`).
- **Consequências:** nomes de pacote um pouco mais longos (`com.reservacinema.movie.repository`).

## D09. Services com interface + implementação (`MovieService` / `MovieServiceImpl`)

- **Contexto:** a lógica de negócio (cadastrar filme, reservar assento) vai morar nos services. Precisamos decidir se cada service é uma classe só ou uma interface com uma implementação.
- **Alternativas:** classe concreta única (`MovieService` com `@Service`); interface `MovieService` + classe `MovieServiceImpl`.
- **Decisão:** interface + implementação.
- **Por quê:** a interface descreve **o que** o service oferece (o "contrato"), e a `Impl` descreve **como** faz. O controller depende só do contrato, o que deixa claro o que é público e facilita trocar a implementação (ex.: um pagamento simulado por um real, na Etapa 5).
- **Consequências:** um arquivo a mais por service. Muitos projetos modernos usam só a classe concreta quando existe uma única implementação; aqui escolhemos a interface pela clareza e pelo aprendizado do conceito.

---

# Etapa 2: autenticação e papéis

## D10. Autenticação com JWT: access token curto + refresh token longo

- **Contexto:** a API precisa saber quem está fazendo cada requisição e se essa pessoa é ADMIN ou USER.
- **Alternativas:**
  1. Sessão no servidor com cookie (o modelo clássico de sites).
  2. Um único JWT de longa duração.
  3. Access token JWT curto (15 min) + refresh token longo (7 dias).
- **Decisão:** opção 3.
- **Por quê:** o JWT é **autocontido**: o servidor confere a assinatura e já sabe o id e o papel do usuário, sem consultar o banco a cada requisição, e sem guardar sessão (API *stateless*). O ponto fraco do JWT é que ele não pode ser "desligado" antes de expirar. Por isso ele dura pouco (15 min). Para o usuário não precisar logar a cada 15 minutos, existe o refresh token, que fica no banco e **pode** ser revogado.
- **Consequências:** depois do logout, o access token ainda vale até expirar (no máximo 15 min). É a troca aceita em troca de não consultar o banco em toda requisição. O cliente precisa lidar com o 401 e chamar `/api/auth/refresh`.

## D11. Biblioteca JJWT + filtro próprio, em vez do "resource server" do Spring

- **Contexto:** precisamos gerar e validar JWT dentro do Spring Security.
- **Alternativas:** `spring-boot-starter-oauth2-resource-server` (o Spring valida o token quase sozinho, com configuração); a biblioteca JJWT com um filtro escrito por nós.
- **Decisão:** JJWT + `JwtAuthenticationFilter` (um `OncePerRequestFilter`).
- **Por quê:** o objetivo do projeto é aprender. Com o filtro próprio, cada passo fica visível: ler o header, validar o token, registrar o usuário no `SecurityContext`. O resource server é ótimo, mas esconde esses passos.
- **Consequências:** mais código nosso para manter. Migrar para o resource server no futuro é possível sem mudar os controllers, porque eles só recebem o `AuthenticatedUser`.

## D12. Refresh token aleatório, salvo como hash, com rotação e detecção de reuso

- **Contexto:** o refresh token vale 7 dias; se vazar, é perigoso.
- **Alternativas:** refresh token também em JWT; token aleatório guardado em texto puro; token aleatório guardado como hash.
- **Decisão:** 32 bytes aleatórios (`SecureRandom`), guardando no banco só o SHA-256. A cada uso, o token antigo é revogado e um novo é emitido (**rotação**). Se um token já revogado for usado de novo, **todas** as sessões do usuário são revogadas.
- **Por quê:** não precisa ser JWT, porque sempre consultamos o banco para ele. Guardar o hash faz com que um vazamento do banco não entregue tokens usáveis (mesma ideia das senhas). A rotação com detecção de reuso pega o caso "alguém copiou meu token": quando o dono legítimo e o atacante usam o mesmo token, o segundo uso derruba tudo.
- **Consequências:** a tabela `refresh_tokens` cresce; no futuro vale uma limpeza periódica de tokens expirados.

## D13. Senhas com BCrypt

- **Contexto:** nunca se guarda senha em texto puro.
- **Alternativas:** SHA-256 (rápido), BCrypt, Argon2.
- **Decisão:** BCrypt (`BCryptPasswordEncoder`).
- **Por quê:** hashes rápidos como SHA-256 permitem testar bilhões de senhas por segundo. BCrypt é **propositalmente lento** e usa um "sal" aleatório por senha, então duas senhas iguais geram hashes diferentes. Argon2 é ainda mais moderno, mas BCrypt é o padrão do Spring e mais que suficiente aqui.
- **Consequências:** BCrypt só usa os primeiros 72 bytes da senha; por isso limitamos a senha a 72 caracteres na validação.

## D14. DTOs (records) na entrada e na saída da API

- **Contexto:** o que a API recebe e devolve.
- **Alternativas:** usar as entidades JPA direto nos controllers; criar DTOs.
- **Decisão:** DTOs como `record` (`RegisterRequest`, `TokenResponse`, `UserResponse`...).
- **Por quê:** devolver a entidade `User` exporia o hash da senha. Receber a entidade permitiria que alguém mandasse `"role": "ADMIN"` no cadastro. O DTO define exatamente o que entra e o que sai, e carrega as validações (`@NotBlank`, `@Email`).
- **Consequências:** um pouco mais de código (conversões como `UserResponse.from(user)`), mas a API fica desacoplada do banco.

## D15. Admin inicial via configuração, cadastro público sempre USER

- **Contexto:** a especificação pede um admin inicial e que só admins promovam outros usuários.
- **Alternativas:** inserir o admin numa migração SQL; criar via código ao subir, lendo e-mail e senha da configuração.
- **Decisão:** `AdminSeeder` (um `ApplicationRunner`) cria o admin se ele não existir, com dados de `ADMIN_EMAIL` / `ADMIN_PASSWORD` (com padrão só para desenvolvimento). O `/api/auth/register` sempre cria USER. Um admin não pode tirar o próprio papel de ADMIN.
- **Por quê:** a senha precisa passar pelo BCrypt (difícil de fazer numa migração SQL) e não deve ficar fixa no código versionado. A trava de "auto-rebaixamento" evita que o último admin se tranque fora do sistema.
- **Consequências:** em produção é obrigatório definir `ADMIN_PASSWORD` e `JWT_SECRET`.

## D16. Erros padronizados com hierarquia de exceções

- **Contexto:** cada erro precisa virar um status HTTP correto com uma mensagem clara.
- **Alternativas:** tratar erros com `try/catch` em cada controller; lançar `RuntimeException` genérica; ter exceções próprias por tipo de erro.
- **Decisão:** `ApiException` com subclasses (`NotFoundException` → 404, `ConflictException` → 409, `UnauthorizedException` → 401, `BusinessException` → 400) e um `@RestControllerAdvice` que transforma todas no mesmo JSON (`ApiError`). Erros inesperados viram 500 sem detalhes internos.
- **Por quê:** o service só diz *o que* deu errado (`throw new ConflictException("E-mail já cadastrado")`); o formato da resposta fica num lugar só.
- **Consequências:** erros de segurança (401/403) acontecem nos filtros, antes dos controllers, então precisaram de um escritor próprio (`SecurityErrorWriter`) no mesmo formato.

---

# Etapa 3: filmes, salas e sessões

## D17. Sala criada junto com o seu mapa de assentos

- **Contexto:** cada sala tem o próprio mapa (fileiras e números). Era preciso decidir como o admin cadastra isso.
- **Alternativas:** cadastrar a sala e depois cada assento em requisições separadas; informar só uma capacidade numérica; enviar a sala com a descrição das fileiras de uma vez.
- **Decisão:** o `POST /api/admin/rooms` recebe o nome e uma lista de fileiras (`{label, seats}`). O service gera os assentos (A1..A10, B1..B10...) numa transação só.
- **Por quê:** é uma operação atômica — ou a sala nasce completa, ou não nasce. Evita salas pela metade. Uma checagem em memória (`Set`) impede mandar o mesmo lugar duas vezes antes de chegar ao banco.
- **Consequências:** o mapa é definido na criação. Editar o mapa de uma sala já usada fica para depois, porque mexeria em sessões e reservas existentes.

## D18. Upload de pôster no disco local, com nome gerado e validação de tipo

- **Contexto:** o filme tem um pôster (imagem). Precisamos receber e servir esse arquivo.
- **Alternativas:** guardar a imagem no banco (coluna binária); usar um serviço na nuvem (S3); gravar no disco local.
- **Decisão:** disco local (pasta `uploads/posters`), guardando no banco só o caminho. A imagem é servida pela URL pública `/files/**`. O arquivo recebe um nome novo (UUID) e só aceitamos JPEG/PNG/WebP.
- **Por quê:** é o mais simples para aprender e combina com "armazenamento local está bom por enquanto". Guardar imagem no banco incha o banco e piora o desempenho. Trocar por S3 depois é fácil, porque isolamos tudo atrás da interface `PosterStorageService`.
- **Segurança (importante):** nunca usamos o nome enviado pelo usuário no disco. Um nome como `../../app.properties` poderia escrever fora da pasta (ataque de *path traversal*). Por isso geramos o nome e ainda conferimos que o caminho final continua dentro da pasta raiz. Validar o tipo evita subir um arquivo executável disfarçado.
- **Consequências:** os arquivos não entram no Git (pasta `uploads/` ignorada) e não são copiados junto no deploy; em produção, a pasta precisa persistir.

## D19. Não expor entidades: tudo entra e sai por DTO

- **Contexto:** continuação da decisão D14, agora para filmes, salas e sessões.
- **Decisão:** cada recurso tem seus DTOs de entrada e saída (`MovieRequest`/`MovieResponse`, `CreateRoomRequest`/`RoomResponse`, `ShowtimeRequest`/`ShowtimeResponse`). Gêneros são enviados como nomes, não como ids.
- **Por quê:** além de proteger o que entra e sai, os DTOs evitam um problema clássico do JPA: serializar uma entidade com relações `LAZY` fora da transação quebra (a famosa `LazyInitializationException`). Convertendo para DTO dentro do service, montamos exatamente os dados que a resposta precisa.
- **Consequências:** o service faz as conversões (`MovieResponse.from(...)`), e o cliente não lida com ids de gênero.

## D20. Sem sobreposição de sessões na mesma sala

- **Contexto:** não faz sentido duas sessões ao mesmo tempo na mesma sala.
- **Alternativas:** não checar; checar no código ao criar; uma constraint de exclusão no banco (`EXCLUDE USING gist`).
- **Decisão:** checar no service com uma consulta que conta sessões sobrepostas na mesma sala, dentro da transação de criação.
- **Por quê:** a constraint de exclusão do PostgreSQL é poderosa, mas avançada e específica demais para este momento de aprendizado. A checagem por consulta é fácil de ler e cobre o uso real (um admin cadastrando sessões). O horário de término é calculado a partir da duração do filme, então a sobreposição considera o filme inteiro.
- **Consequências:** em teoria, dois admins criando sessões no mesmo instante poderiam furar a checagem (é a mesma condição de corrida da reserva). Para o volume de um admin cadastrando a grade, é aceitável; a proteção forte de concorrência fica para as reservas (etapa 4), onde ela importa de verdade.

## D21. Datas e fuso na listagem por dia

- **Contexto:** "sessões do dia 1º de dezembro" depende do fuso de quem pergunta.
- **Decisão:** a API guarda tudo em UTC (`Instant`), mas interpreta o parâmetro `?date=` no fuso de Brasília: o dia local vira o intervalo `[00:00 local, 00:00 do dia seguinte)` convertido para UTC.
- **Por quê:** o usuário pensa no horário local. Guardar em UTC e converter só na consulta evita bugs de fuso e de horário de verão.
- **Consequências:** o fuso está fixo no código por enquanto; se o cinema tivesse salas em fusos diferentes, isso viraria configuração.

---

# Etapa 4: reservas com proteção contra overbooking

## D22. A reserva é feita numa transação e o conflito do banco vira 409

- **Contexto:** o coração do sistema. Duas pessoas podem escolher o mesmo assento quase ao mesmo tempo. Precisamos garantir que só uma consiga, sem travar o sistema.
- **Como funciona:** ao reservar, abrimos uma transação, criamos a reserva com os assentos e mandamos gravar (`saveAndFlush`). O índice único parcial `uq_active_seat_per_showtime` (criado lá na Etapa 1, decisão D05) faz o banco recusar o segundo `INSERT` do mesmo assento. Capturamos essa falha (`DataIntegrityViolationException`) e respondemos `409 Conflict` com uma mensagem clara.
- **Por que `saveAndFlush` e não só `save`:** por padrão, o Hibernate só envia os comandos ao banco no fim da transação (no commit), que acontece **depois** do nosso método. Se não forçarmos o envio, a violação explodiria no commit, fora do nosso `try/catch`, e viraria um erro `500`. O `flush` antecipa o `INSERT` para dentro do `try`, onde conseguimos tratar. Isto apareceu de verdade no teste: antes do ajuste, o conflito dava `500`; depois, dá `409`.
- **Por que confiar no banco e não checar antes:** poderíamos consultar "o assento está livre?" e depois inserir. Mas entre a consulta e o `INSERT`, outra requisição pode inserir primeiro (condição de corrida). O banco é o único ponto que vê todas as requisições em série, então é ele quem decide. Validamos isto com um teste real: 8 requisições simultâneas no mesmo assento resultaram em **exatamente um** `201` e sete `409`, com **uma** linha ativa no banco.
- **Consequências:** a solução é simples e robusta, sem locks explícitos. O custo é tratar a exceção, mas o código fica claro.

## D23. O usuário vem do token, nunca do corpo (proteção contra IDOR)

- **Contexto:** o projeto antigo (`reservation_movie`) recebia o `userId` no corpo da requisição. Isso deixaria qualquer um reservar, ver ou cancelar em nome de outra pessoa só trocando o número.
- **Decisão:** em todas as rotas de reserva, o usuário é lido do access token (`@AuthenticationPrincipal`). O corpo só traz a sessão e os assentos. Ao buscar uma reserva, filtramos por dono (`findByIdAndUserId`).
- **Por quê:** é a defesa contra **IDOR** (Insecure Direct Object Reference), uma das falhas mais comuns em APIs: acessar o recurso de outro usuário trocando o id na URL. Quando a reserva não é do usuário, respondemos `404` (e não `403`), para nem confirmar que ela existe.
- **Consequências:** o cliente não precisa (nem pode) informar quem é; quem manda é o token.

## D24. Reserva nasce PENDING_PAYMENT e já segura o assento

- **Contexto:** a confirmação depende do pagamento, que é a próxima etapa.
- **Decisão:** a reserva é criada com status `PENDING_PAYMENT`. Mesmo pendente, o assento já conta como ocupado (o índice único não olha o status, só o `active`). O pagamento (Etapa 5) muda para `CONFIRMED`.
- **Por quê:** o assento precisa ficar reservado no instante do clique, senão duas pessoas pagariam pelo mesmo lugar. Segurar primeiro e confirmar no pagamento é como a maioria dos sistemas de bilhete funciona.
- **Consequência / dívida técnica:** uma reserva pendente que nunca é paga segura o assento para sempre. A solução usual é uma expiração (ex.: liberar após 10 minutos sem pagamento). Deixamos isso anotado para tratar junto com o pagamento ou na rodada de melhorias.

## D25. Cancelar desativa os assentos, não apaga a reserva

- **Contexto:** o usuário pode cancelar uma reserva futura.
- **Decisão:** `cancel()` muda o status para `CANCELLED` e marca cada assento como `active = false`. Só é permitido cancelar enquanto a sessão não começou, e só o dono cancela.
- **Por quê:** desativar (em vez de apagar) libera o assento para outra pessoa — o índice único só considera os ativos — e mantém o histórico para os relatórios do admin (Etapa 6). Validamos: após o cancelamento, o assento volta a aparecer livre no mapa e pode ser reservado de novo.
- **Consequências:** a tabela `reservation_seats` guarda linhas inativas; é intencional, é o histórico.
