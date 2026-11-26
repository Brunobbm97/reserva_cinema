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
