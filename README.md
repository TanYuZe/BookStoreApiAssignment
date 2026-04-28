# BookStore API Assignment

A Spring Boot REST API for managing a bookstore's inventory. Built as an assignment to practice Spring Security, JPA, and layered architecture.

## What it does

The API lets you create, update, search, and delete books. Each book has one or more authors. Deleting books is restricted to admin users — regular users can do everything else.

## Stack

- Java 17 + Spring Boot 3.2.0
- PostgreSQL for storage, H2 for tests
- Spring Security with HTTP Basic Auth
- Maven

## Getting started

You'll need Java 17, Maven, and a running PostgreSQL instance.

1. Create a database called `bookstoredb` (or whatever you want, just update the config).

2. Open [src/main/resources/application.properties](src/main/resources/application.properties) and set your database credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/bookstoredb
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```

3. Run it:
   ```bash
   mvn spring-boot:run
   ```

On first startup it will create the tables and seed a few sample books and users automatically.

## Endpoints

Base path: `/v1/api/books`

| Method | Path | What it does |
|--------|------|-------------|
| GET | `/v1/api/books` | Get all books |
| POST | `/v1/api/books` | Add a book |
| PUT | `/v1/api/books/{isbn}` | Update a book |
| GET | `/v1/api/books/search` | Search by title and/or author name |
| DELETE | `/v1/api/books/{isbn}` | Delete a book (admin only) |

All endpoints require authentication.

### Adding a book

```bash
curl -u user:password123 -X POST http://localhost:8080/v1/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0-7432-7356-5",
    "title": "The Great Gatsby",
    "year": 1925,
    "price": 12.99,
    "genre": "Fiction",
    "authors": [
      { "name": "F. Scott Fitzgerald", "birthday": "1896-09-24" }
    ]
  }'
```

### Searching

```bash
curl -u user:password123 "http://localhost:8080/v1/api/books/search?author=George+Orwell"
```

Both `title` and `author` are optional — leave one out and it matches everything. Search is case-insensitive but needs an exact full-name match (no partial/substring).

## Authentication

Two users are created on startup:

| Username | Password | Role |
|----------|----------|------|
| `user` | `password123` | USER |
| `admin` | `admin123` | ADMIN |

Only `admin` can delete books.

## Running the tests

No PostgreSQL needed — tests use H2 in memory.

```bash
mvn test
```

There are 35 tests split across integration tests (MockMvc), service unit tests (Mockito), and repository tests (DataJpaTest).

## Project layout

```
src/main/java/com/bookstore/api/
├── controller/       # REST layer
├── service/          # Business logic
├── model/            # JPA entities (Book, Author, User)
├── dto/              # Request/response objects
├── repository/       # Spring Data repos + custom queries
├── exception/        # GlobalExceptionHandler + custom exceptions
└── config/           # Security config + data seeding
```

## A few things worth noting

- Books use ISBN as the primary key, not a generated ID.
- When adding a book, if an author with the same name and birthday already exists in the database, it reuses them instead of creating a duplicate.
- The search endpoint does exact matching on the full title/author name, not a contains/like search.
