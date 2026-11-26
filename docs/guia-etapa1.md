# Guia da Etapa 1: estrutura do projeto e modelo de dados

Este guia mostra como baixar, abrir e rodar o projeto no IntelliJ, e em que ordem ler os
arquivos para entender o que foi feito.

---

## Parte A: colocar o projeto para rodar

### 1. Instale o que falta

| Ferramenta | Para quê | Como conferir |
|---|---|---|
| **JDK 21** | Compilar e rodar Java | `java -version` no terminal mostra 21 |
| **PostgreSQL 16** (ou mais novo) | O banco de dados | `psql --version` |
| **IntelliJ IDEA** (Community serve) | Editar e rodar o projeto | — |

O Maven não precisa ser instalado à parte: o IntelliJ já traz um embutido.

> Dica: se não quiser instalar o JDK manualmente, o IntelliJ consegue baixar na hora
> (passo 3).

### 2. Baixe o código

No terminal, na pasta onde você guarda seus projetos:

```bash
git clone https://github.com/Brunobbm97/reserva_cinema.git
```

(Prefira o `git clone` ao `.tar.gz`: assim você recebe as próximas etapas com um simples
`git pull`.)

### 3. Abra no IntelliJ

1. **File → Open...** e selecione a pasta `reserva_cinema` (a que tem o `pom.xml`).
2. Se perguntar, clique em **Trust Project**.
3. O IntelliJ reconhece o `pom.xml` e baixa as dependências sozinho. Acompanhe a barra
   de progresso no canto inferior direito. Na primeira vez demora alguns minutos.
4. Confira o JDK: **File → Project Structure → Project → SDK** deve ser **21**.
   Se não houver nenhum, escolha **Add SDK → Download JDK...** e pegue a versão 21.

Se o código aparecer cheio de vermelho, abra a aba **Maven** (lateral direita) e clique no
botão de recarregar (**Reload All Maven Projects**).

### 4. Crie o banco de dados

Abra o terminal do PostgreSQL (`psql -U postgres`, ou o **pgAdmin**, ou a aba
**Database** do IntelliJ Ultimate) e rode:

```sql
CREATE USER cinema WITH PASSWORD 'cinema';
CREATE DATABASE reserva_cinema OWNER cinema;
```

Isso cria um usuário e um banco só para este projeto. Os mesmos valores estão em
`src/main/resources/application.properties`.

### 5. Rode a aplicação

1. Abra `src/main/java/com/reservacinema/ReservaCinemaApplication.java`.
2. Clique no ▶ verde ao lado de `public class ReservaCinemaApplication` e escolha **Run**.
3. No console, procure estas linhas:

```
Migrating schema "public" to version "1 - modelo inicial"
Successfully applied 1 migration ...
Started ReservaCinemaApplication in 5.0 seconds
```

Pronto: a aplicação subiu e o Flyway criou as tabelas. Ainda não há endpoints (isso vem a
partir da Etapa 2), então abrir `http://localhost:8080` no navegador vai mostrar uma
página de erro, e isso é esperado.

Para parar, clique no ■ vermelho do console.

### 6. Veja as tabelas que foram criadas

```bash
psql -h localhost -U cinema reserva_cinema
```

```sql
\dt                 -- lista as tabelas
\d reservation_seats -- mostra colunas e índices de uma tabela
```

Você vai ver também a tabela `flyway_schema_history`: é onde o Flyway anota quais
migrações já rodou, para nunca rodar a mesma duas vezes.

### Problemas comuns

| Erro no console | Causa provável | Solução |
|---|---|---|
| `Connection refused` | PostgreSQL desligado | Inicie o serviço do PostgreSQL |
| `password authentication failed for user "cinema"` | Usuário não criado ou senha diferente | Refaça o passo 4 |
| `database "reserva_cinema" does not exist` | Banco não criado | Refaça o passo 4 |
| `Schema-validation: missing column ...` | Entidade Java diferente da tabela | Confira se a entidade e o SQL da migração têm o mesmo nome de coluna |
| `release version 21 not supported` | JDK errado | Passo 3, item 4 |

---

## Parte B: entender o que foi feito

Leia os arquivos nesta ordem. Cada um depende do anterior.

### 1. `pom.xml`: as dependências

É a "lista de compras" do projeto. O Maven lê esse arquivo e baixa as bibliotecas:

- `spring-boot-starter-webmvc`: para criar a API REST (vamos usar a partir da Etapa 2).
- `spring-boot-starter-data-jpa`: JPA/Hibernate, que transforma classes Java em linhas de tabela.
- `spring-boot-starter-validation`: validação de dados de entrada.
- `spring-boot-starter-flyway` + `flyway-database-postgresql`: migrações do banco.
- `postgresql`: o driver que conversa com o PostgreSQL.

### 2. `application.properties`: a configuração

- `spring.datasource.*`: onde está o banco. A sintaxe `${DB_URL:jdbc:...}` significa "use
  a variável de ambiente `DB_URL` se existir, senão use este valor padrão".
- `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate **não** cria tabelas, só confere se
  as classes Java batem com o banco. Quem cria é o Flyway (decisão D03).
- `spring.jpa.open-in-view=false`: desliga um comportamento do Spring que esconde problemas
  de desempenho. Vamos ver o efeito disso na Etapa 3.

### 3. `db/migration/V1__modelo_inicial.sql`: o coração da etapa

Leia com calma, os comentários explicam cada tabela. Pontos principais:

- `BIGSERIAL PRIMARY KEY`: id numérico gerado automaticamente (1, 2, 3...).
- `REFERENCES tabela (id)`: chave estrangeira; o banco não deixa apontar para algo que não existe.
- `CHECK (...)`: regras simples que o banco garante (ex.: preço não pode ser negativo).
- `UNIQUE`: não pode repetir (ex.: dois usuários com o mesmo e-mail).
- O índice `uq_active_seat_per_showtime` no fim: é a proteção contra overbooking (decisão D05).

O nome do arquivo importa: `V1__` (V maiúsculo, número, **dois** sublinhados). A próxima
mudança no banco será `V2__alguma_coisa.sql`. **Nunca edite um arquivo de migração que
já rodou**: crie um novo.

### 4. As entidades (as classes com `@Entity`)

Cada uma representa uma tabela. Leia nesta ordem:

1. `user/Role.java` e `user/User.java`: o mais simples. Repare em `@Enumerated(EnumType.STRING)`.
2. `movie/Genre.java` e `movie/Movie.java`: veja o `@ManyToMany` com `@JoinTable`, que liga à tabela `movie_genres`.
3. `room/Room.java` e `room/Seat.java`: `@OneToMany` de um lado e `@ManyToOne` do outro. O método `addSeat` cuida dos dois lados.
4. `showtime/Showtime.java`: o horário de fim é calculado a partir da duração do filme.
5. `reservation/Reservation.java` e `reservation/ReservationSeat.java`: veja os métodos `addSeat`, `confirm` e `cancel`. As regras ficam dentro da entidade.

Anotações que aparecem em todas:

| Anotação | Significado |
|---|---|
| `@Entity` / `@Table(name = "...")` | Esta classe é a tabela tal |
| `@Id` + `@GeneratedValue(IDENTITY)` | Chave primária gerada pelo banco |
| `@Column(name = "...", nullable = false)` | Mapeia o campo para uma coluna |
| `@ManyToOne(fetch = LAZY)` | "Muitos destes apontam para um daquele"; LAZY = só busca quando usar |
| `@OneToMany(mappedBy = "...")` | O outro lado da relação, sem coluna própria |

Por que o construtor vazio é `protected`? O JPA precisa dele para criar objetos ao ler do
banco, mas nós queremos que o código use o construtor com parâmetros, que já deixa o
objeto válido.

### 5. Os repositórios (`...Repository.java`)

São interfaces que estendem `JpaRepository`. O Spring cria a implementação sozinho e você
ganha `save`, `findById`, `findAll`, `delete`... de graça. Métodos como
`findByEmail(String email)` viram consultas SQL a partir do **nome** do método.

### 6. `docs/decisoes.md`

O porquê de cada escolha, com as alternativas. É a base para os posts e artigos.

---

## Exercícios para fixar

1. Rode a aplicação, depois mude o nome de uma coluna em uma entidade (ex.: `title` →
   `titulo` no `@Column`) e rode de novo. Leia o erro do `validate`. Desfaça a mudança.
2. No `psql`, tente inserir dois usuários com o mesmo e-mail e veja o banco recusar.
3. Desenhe no papel o diagrama das tabelas e as setas das chaves estrangeiras, comparando
   com o diagrama do README.
