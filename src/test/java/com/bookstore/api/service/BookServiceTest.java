package com.bookstore.api.service;

import com.bookstore.api.dto.AuthorDto;
import com.bookstore.api.dto.BookRequest;
import com.bookstore.api.exception.BookNotFoundException;
import com.bookstore.api.exception.DuplicateIsbnException;
import com.bookstore.api.model.Author;
import com.bookstore.api.model.Book;
import com.bookstore.api.repository.AuthorRepository;
import com.bookstore.api.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @InjectMocks BookService bookService;

    private BookRequest request;

    @BeforeEach
    void setUp() {
        request = new BookRequest();
        request.setIsbn("978-0-13-468599-1");
        request.setTitle("Clean Code");
        request.setAuthors(List.of(new AuthorDto("Robert Martin", "1952-12-05")));
        request.setYear(2008);
        request.setPrice(35.99);
        request.setGenre("Software Engineering");
    }

    // ── addBook ──────────────────────────────────────────────────────

    @Test
    @DisplayName("addBook: new ISBN with new author → saved and returned")
    void addBook_success_newAuthor() {
        when(bookRepository.existsById("978-0-13-468599-1")).thenReturn(false);
        when(authorRepository.findByNameAndBirthday(eq("Robert Martin"), any())).thenReturn(Optional.empty());

        Book saved = new Book();
        saved.setIsbn("978-0-13-468599-1");
        saved.setTitle("Clean Code");
        saved.setYear(2008);
        saved.setPrice(35.99);
        saved.setGenre("Software Engineering");
        saved.setAuthors(Set.of(new Author("Robert Martin", LocalDate.of(1952, 12, 5))));
        when(bookRepository.save(any())).thenReturn(saved);

        Book result = bookService.addBook(request);

        assertThat(result.getIsbn()).isEqualTo("978-0-13-468599-1");
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("addBook: new ISBN with existing author → existing author reused")
    void addBook_reuseExistingAuthor() {
        Author existing = new Author("Robert Martin", LocalDate.of(1952, 12, 5));
        when(bookRepository.existsById(any())).thenReturn(false);
        when(authorRepository.findByNameAndBirthday(eq("Robert Martin"), eq(LocalDate.of(1952, 12, 5))))
            .thenReturn(Optional.of(existing));

        Book saved = new Book();
        saved.setIsbn("978-0-13-468599-1");
        saved.setTitle("Clean Code");
        saved.setYear(2008);
        saved.setPrice(35.99);
        saved.setGenre("Software Engineering");
        saved.setAuthors(Set.of(existing));
        when(bookRepository.save(any())).thenReturn(saved);

        bookService.addBook(request);

        verify(bookRepository).save(argThat(b -> b.getAuthors().contains(existing)));
    }

    @Test
    @DisplayName("addBook: author without birthday → no findByNameAndBirthday lookup")
    void addBook_authorWithNoBirthday() {
        request.setAuthors(List.of(new AuthorDto("Anonymous", null)));
        when(bookRepository.existsById(any())).thenReturn(false);
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookService.addBook(request);

        verify(authorRepository, never()).findByNameAndBirthday(any(), any());
    }

    @Test
    @DisplayName("addBook: duplicate ISBN → DuplicateIsbnException, no save")
    void addBook_duplicateIsbn_throwsException() {
        when(bookRepository.existsById("978-0-13-468599-1")).thenReturn(true);

        assertThatThrownBy(() -> bookService.addBook(request))
            .isInstanceOf(DuplicateIsbnException.class)
            .hasMessageContaining("978-0-13-468599-1");

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("addBook: malformed birthday → IllegalArgumentException, no save")
    void addBook_malformedBirthday_throwsException() {
        request.setAuthors(List.of(new AuthorDto("Bad Date Author", "not-a-date")));
        when(bookRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> bookService.addBook(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not-a-date");

        verify(bookRepository, never()).save(any());
    }

    // ── updateBook ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateBook: existing ISBN → fields updated and returned")
    void updateBook_success() {
        Book existing = new Book();
        existing.setIsbn("978-0-13-468599-1");
        existing.setTitle("Old Title");
        existing.setYear(2000);
        existing.setPrice(10.0);
        existing.setGenre("Old Genre");

        when(bookRepository.findById("978-0-13-468599-1")).thenReturn(Optional.of(existing));
        when(authorRepository.findByNameAndBirthday(any(), any())).thenReturn(Optional.empty());
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.updateBook("978-0-13-468599-1", request);

        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getPrice()).isEqualTo(35.99);
        assertThat(result.getYear()).isEqualTo(2008);
    }

    @Test
    @DisplayName("updateBook: path ISBN overrides body ISBN")
    void updateBook_pathIsbnIsAuthoritative() {
        request.setIsbn("978-9-99-999999-9"); // body has a different isbn

        Book existing = new Book();
        existing.setIsbn("978-0-13-468599-1");
        existing.setTitle("Original");
        existing.setYear(2000);
        existing.setPrice(10.0);
        existing.setGenre("Genre");

        when(bookRepository.findById("978-0-13-468599-1")).thenReturn(Optional.of(existing));
        when(authorRepository.findByNameAndBirthday(any(), any())).thenReturn(Optional.empty());
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.updateBook("978-0-13-468599-1", request);

        assertThat(result.getIsbn()).isEqualTo("978-0-13-468599-1");
    }

    @Test
    @DisplayName("updateBook: non-existent ISBN → BookNotFoundException, no save")
    void updateBook_notFound_throwsException() {
        when(bookRepository.findById("978-0-13-468599-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook("978-0-13-468599-1", request))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("978-0-13-468599-1");

        verify(bookRepository, never()).save(any());
    }

    // ── searchBooks ──────────────────────────────────────────────────

    @Test
    @DisplayName("searchBooks: delegates to repository with correct params")
    void searchBooks_delegatesToRepository() {
        Book book = new Book();
        book.setIsbn("978-0-13-468599-1");
        book.setTitle("Clean Code");
        when(bookRepository.searchBooks("Clean Code", null)).thenReturn(List.of(book));

        List<Book> results = bookService.searchBooks("Clean Code", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).searchBooks("Clean Code", null);
    }

    @Test
    @DisplayName("searchBooks: no match → empty list")
    void searchBooks_noMatch_returnsEmptyList() {
        when(bookRepository.searchBooks(any(), any())).thenReturn(List.of());

        List<Book> results = bookService.searchBooks("Unknown", null);

        assertThat(results).isEmpty();
    }

    // ── deleteBook ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBook: existing ISBN → deleteById called")
    void deleteBook_success() {
        when(bookRepository.existsById("978-0-13-468599-1")).thenReturn(true);

        bookService.deleteBook("978-0-13-468599-1");

        verify(bookRepository).deleteById("978-0-13-468599-1");
    }

    @Test
    @DisplayName("deleteBook: non-existent ISBN → BookNotFoundException, no deleteById")
    void deleteBook_notFound_throwsException() {
        when(bookRepository.existsById("978-0-13-468599-1")).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook("978-0-13-468599-1"))
            .isInstanceOf(BookNotFoundException.class)
            .hasMessageContaining("978-0-13-468599-1");

        verify(bookRepository, never()).deleteById(any());
    }
}
