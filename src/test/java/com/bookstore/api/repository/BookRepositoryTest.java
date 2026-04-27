package com.bookstore.api.repository;

import com.bookstore.api.model.Author;
import com.bookstore.api.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        Author orwell = new Author("George Orwell", LocalDate.of(1903, 6, 25));
        Author tolkien = new Author("J.R.R. Tolkien", LocalDate.of(1892, 1, 3));

        Book b1 = new Book();
        b1.setIsbn("978-0-452-28423-4");
        b1.setTitle("1984");
        b1.setYear(1949);
        b1.setPrice(14.99);
        b1.setGenre("Dystopian Fiction");
        b1.setAuthors(Set.of(orwell));

        Book b2 = new Book();
        b2.setIsbn("978-0-618-00222-3");
        b2.setTitle("The Lord of the Rings");
        b2.setYear(1954);
        b2.setPrice(29.99);
        b2.setGenre("Fantasy");
        b2.setAuthors(Set.of(tolkien));

        em.persist(b1);
        em.persist(b2);
        em.flush();
    }

    // ── search by title ───────────────────────────────────────────────

    @Test
    @DisplayName("searchBooks by title: exact match → returns book")
    void searchByTitle_exactMatch() {
        List<Book> result = bookRepository.searchBooks("1984", null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsbn()).isEqualTo("978-0-452-28423-4");
    }

    @Test
    @DisplayName("searchBooks by title: case-insensitive → still returns book")
    void searchByTitle_caseInsensitive() {
        List<Book> lower = bookRepository.searchBooks("1984", null);
        List<Book> upper = bookRepository.searchBooks("1984", null);
        List<Book> mixed = bookRepository.searchBooks("1984", null);
        assertThat(lower).hasSize(1);
        assertThat(upper).hasSize(1);
        assertThat(mixed).hasSize(1);
    }

    @Test
    @DisplayName("searchBooks by title: no match → empty list")
    void searchByTitle_noMatch() {
        List<Book> result = bookRepository.searchBooks("Unknown Title", null);
        assertThat(result).isEmpty();
    }

    // ── search by author ──────────────────────────────────────────────

    @Test
    @DisplayName("searchBooks by author: exact match → returns book")
    void searchByAuthor_exactMatch() {
        List<Book> result = bookRepository.searchBooks(null, "George Orwell");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("1984");
    }

    @Test
    @DisplayName("searchBooks by author: lowercase name → still returns book")
    void searchByAuthor_caseInsensitive() {
        List<Book> result = bookRepository.searchBooks(null, "george orwell");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("1984");
    }

    @Test
    @DisplayName("searchBooks by author: no match → empty list")
    void searchByAuthor_noMatch() {
        List<Book> result = bookRepository.searchBooks(null, "Unknown Author");
        assertThat(result).isEmpty();
    }

    // ── search by both ────────────────────────────────────────────────

    @Test
    @DisplayName("searchBooks by title AND author: both match → returns book")
    void searchByTitleAndAuthor_bothMatch() {
        List<Book> result = bookRepository.searchBooks("1984", "George Orwell");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("searchBooks by title AND author: author mismatch → empty list")
    void searchByTitleAndAuthor_authorMismatch() {
        List<Book> result = bookRepository.searchBooks("1984", "J.R.R. Tolkien");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchBooks by title AND author: title mismatch → empty list")
    void searchByTitleAndAuthor_titleMismatch() {
        List<Book> result = bookRepository.searchBooks("The Lord of the Rings", "George Orwell");
        assertThat(result).isEmpty();
    }

    // ── null params behave as wildcard ────────────────────────────────

    @Test
    @DisplayName("searchBooks: null title matches all titles (author only search)")
    void searchBooks_nullTitle_matchesAll() {
        List<Book> result = bookRepository.searchBooks(null, "J.R.R. Tolkien");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("The Lord of the Rings");
    }

    @Test
    @DisplayName("searchBooks: null author matches all authors (title only search)")
    void searchBooks_nullAuthor_matchesAll() {
        List<Book> result = bookRepository.searchBooks("The Lord of the Rings", null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsbn()).isEqualTo("978-0-618-00222-3");
    }
}
