package com.bookstore.api.dto;

import com.bookstore.api.model.Book;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class BookResponse {

    private String isbn;
    private String title;
    private List<AuthorDto> authors;
    private int year;
    private double price;
    private String genre;

    public static BookResponse from(Book book) {
        return new BookResponse(
            book.getIsbn(),
            book.getTitle(),
            book.getAuthors().stream()
                .map(a -> new AuthorDto(
                    a.getName(),
                    a.getBirthday() != null ? a.getBirthday().toString() : null))
                .collect(Collectors.toList()),
            book.getYear(),
            book.getPrice(),
            book.getGenre()
        );
    }
}
