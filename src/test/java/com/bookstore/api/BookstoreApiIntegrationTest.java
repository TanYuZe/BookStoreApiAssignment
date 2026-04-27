package com.bookstore.api;

import com.bookstore.api.dto.AuthorDto;
import com.bookstore.api.dto.BookRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookstoreApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────
    // Add Book
    // ──────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("Add book → 201 Created")
    void addBook() throws Exception {
        mockMvc.perform(post("/v1/api/books")
                .with(httpBasic("user", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest("978-3-16-148410-0", "Clean Code",
                        List.of(new AuthorDto("Robert C. Martin", "1952-12-05")),
                        2008, 35.99, "Software Engineering"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isbn").value("978-3-16-148410-0"))
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.authors[0].name").value("Robert C. Martin"))
            .andExpect(jsonPath("$.price").value(35.99));
    }

    @Test @Order(2)
    @DisplayName("Add duplicate ISBN → 409 Conflict")
    void addDuplicateIsbn() throws Exception {
        mockMvc.perform(post("/v1/api/books")
                .with(httpBasic("user", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest("978-3-16-148410-0", "Duplicate Book",
                        List.of(new AuthorDto("Some Author", null)), 2020, 10.0, "Fiction"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test @Order(3)
    @DisplayName("Add book without auth → 401")
    void addBookUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest("978-0-00-000000-0", "No Auth Book",
                        List.of(new AuthorDto("Author", null)), 2020, 5.0, "Fiction"))))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(4)
    @DisplayName("Add book with wrong credentials → 401")
    void addBookBadCredentials() throws Exception {
        mockMvc.perform(post("/v1/api/books")
                .with(httpBasic("user", "wrongpassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest("978-0-00-000000-0", "Bad Creds Book",
                        List.of(new AuthorDto("Author", null)), 2020, 5.0, "Fiction"))))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(5)
    @DisplayName("Add book with missing fields → 400 Bad Request")
    void addBookValidationFail() throws Exception {
        String invalidJson = """
            { "isbn": "", "title": "", "authors": [], "year": 0, "price": -1.0, "genre": "" }
            """;
        mockMvc.perform(post("/v1/api/books")
                .with(httpBasic("user", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Validation failed")));
    }

    // ──────────────────────────────────────────────────────────────
    // Update Book
    // ──────────────────────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("Update existing book → 200 OK")
    void updateBook() throws Exception {
        mockMvc.perform(put("/v1/api/books/978-3-16-148410-0")
                .with(httpBasic("user", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest("978-3-16-148410-0", "Clean Code (2nd Ed.)",
                        List.of(new AuthorDto("Robert C. Martin", "1952-12-05")),
                        2020, 39.99, "Software Engineering"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Clean Code (2nd Ed.)"))
            .andExpect(jsonPath("$.price").value(39.99));
    }

    @Test @Order(7)
    @DisplayName("Update non-existent book → 404")
    void updateNonExistentBook() throws Exception {
        mockMvc.perform(put("/v1/api/books/000-0-00-000000-0")
                .with(httpBasic("user", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildRequest("000-0-00-000000-0", "Ghost Book",
                        List.of(new AuthorDto("Nobody", null)), 2000, 5.0, "Mystery"))))
            .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────────
    // Search Books
    // ──────────────────────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("Search by title → returns matching book")
    void searchByTitle() throws Exception {
        mockMvc.perform(get("/v1/api/books/search")
                .with(httpBasic("user", "password123"))
                .param("title", "1984"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].isbn").value("978-0-452-28423-4"));
    }

    @Test @Order(9)
    @DisplayName("Search by author name → returns matching book")
    void searchByAuthor() throws Exception {
        mockMvc.perform(get("/v1/api/books/search")
                .with(httpBasic("user", "password123"))
                .param("author", "George Orwell"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("1984"));
    }

    @Test @Order(10)
    @DisplayName("Search by title AND author → exact match on both")
    void searchByTitleAndAuthor() throws Exception {
        mockMvc.perform(get("/v1/api/books/search")
                .with(httpBasic("user", "password123"))
                .param("title", "1984")
                .param("author", "George Orwell"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test @Order(11)
    @DisplayName("Search title + wrong author → empty result")
    void searchNoMatch() throws Exception {
        mockMvc.perform(get("/v1/api/books/search")
                .with(httpBasic("user", "password123"))
                .param("title", "1984")
                .param("author", "J.R.R. Tolkien"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test @Order(12)
    @DisplayName("Search without parameters → 400 Bad Request")
    void searchNoParams() throws Exception {
        mockMvc.perform(get("/v1/api/books/search")
                .with(httpBasic("user", "password123")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("At least one search parameter")));
    }

    // ──────────────────────────────────────────────────────────────
    // Delete Book
    // ──────────────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("Delete book as ROLE_USER → 403 Forbidden")
    void deleteBookAsUser() throws Exception {
        mockMvc.perform(delete("/v1/api/books/978-3-16-148410-0")
                .with(httpBasic("user", "password123")))
            .andExpect(status().isForbidden());
    }

    @Test @Order(14)
    @DisplayName("Delete book as ROLE_ADMIN → 204 No Content")
    void deleteBookAsAdmin() throws Exception {
        mockMvc.perform(delete("/v1/api/books/978-3-16-148410-0")
                .with(httpBasic("admin", "admin123")))
            .andExpect(status().isNoContent());
    }

    @Test @Order(15)
    @DisplayName("Delete already-deleted book → 404 Not Found")
    void deleteNonExistentBook() throws Exception {
        mockMvc.perform(delete("/v1/api/books/978-3-16-148410-0")
                .with(httpBasic("admin", "admin123")))
            .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────

    private BookRequest buildRequest(String isbn, String title, List<AuthorDto> authors,
                                     int year, double price, String genre) {
        BookRequest req = new BookRequest();
        req.setIsbn(isbn);
        req.setTitle(title);
        req.setAuthors(authors);
        req.setYear(year);
        req.setPrice(price);
        req.setGenre(genre);
        return req;
    }
}
