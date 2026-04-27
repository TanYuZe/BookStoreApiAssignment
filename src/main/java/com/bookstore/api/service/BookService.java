package com.bookstore.api.service;

import com.bookstore.api.dto.AuthorDto;
import com.bookstore.api.dto.BookRequest;
import com.bookstore.api.exception.BookNotFoundException;
import com.bookstore.api.exception.DuplicateIsbnException;
import com.bookstore.api.model.Author;
import com.bookstore.api.model.Book;
import com.bookstore.api.repository.AuthorRepository;
import com.bookstore.api.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Transactional
    public Book addBook(BookRequest request) {
        if (bookRepository.existsById(request.getIsbn())) {
            throw new DuplicateIsbnException("Book with ISBN " + request.getIsbn() + " already exists");
        }
        return bookRepository.save(buildBook(new Book(), request));
    }

    @Transactional
    public Book updateBook(String isbn, BookRequest request) {
        Book book = bookRepository.findById(isbn)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));
        book.getAuthors().clear();
        buildBook(book, request);
        book.setIsbn(isbn); // path ISBN is authoritative — ignore body isbn
        return bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Book> searchBooks(String title, String authorName) {
        return bookRepository.searchBooks(title, authorName);
    }

    @Transactional
    public void deleteBook(String isbn) {
        if (!bookRepository.existsById(isbn)) {
            throw new BookNotFoundException("Book not found with ISBN: " + isbn);
        }
        bookRepository.deleteById(isbn);
    }

    private Book buildBook(Book book, BookRequest request) {
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setYear(request.getYear());
        book.setPrice(request.getPrice());
        book.setGenre(request.getGenre());

        Set<Author> authors = new HashSet<>();
        for (AuthorDto dto : request.getAuthors()) {
            LocalDate birthday;
            try {
                birthday = (dto.getBirthday() != null && !dto.getBirthday().isBlank())
                    ? LocalDate.parse(dto.getBirthday())
                    : null;
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid birthday value '" + dto.getBirthday() + "': must be YYYY-MM-DD");
            }

            Author author = (birthday != null)
                ? authorRepository.findByNameAndBirthday(dto.getName(), birthday)
                    .orElse(new Author(dto.getName(), birthday))
                : new Author(dto.getName(), null);

            authors.add(author);
        }
        book.setAuthors(authors);
        return book;
    }
}
