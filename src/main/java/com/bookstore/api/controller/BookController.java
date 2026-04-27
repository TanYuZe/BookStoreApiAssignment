package com.bookstore.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.api.dto.BookRequest;
import com.bookstore.api.dto.BookResponse;
import com.bookstore.api.model.Book;
import com.bookstore.api.service.BookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    
    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks()
            .stream()
            .map(BookResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(books);
    }

    @PostMapping
    public ResponseEntity<BookResponse> addBook(@Valid @RequestBody BookRequest request) {
        Book saved = bookService.addBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(saved));
    }

    
    @PutMapping("/{isbn}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable String isbn,
                                                   @Valid @RequestBody BookRequest request) {
        Book updated = bookService.updateBook(isbn, request);
        return ResponseEntity.ok(BookResponse.from(updated));
    }

   
    @GetMapping("/search")
    public ResponseEntity<List<BookResponse>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author) {

        boolean noTitle  = title  == null || title.isBlank();
        boolean noAuthor = author == null || author.isBlank();
        if (noTitle && noAuthor) {
            throw new IllegalArgumentException("At least one search parameter (title or author) must be provided");
        }
        title  = noTitle  ? null : title.strip();
        author = noAuthor ? null : author.strip();

        List<BookResponse> results = bookService.searchBooks(title, author)
            .stream()
            .map(BookResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    
    @DeleteMapping("/{isbn}")
    public ResponseEntity<Void> deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
        return ResponseEntity.noContent().build();
    }
}
