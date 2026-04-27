package com.bookstore.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BookRequest {

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotBlank(message = "Title is required")
    private String title;

    @NotEmpty(message = "At least one author is required")
    @Valid
    private List<@NotNull AuthorDto> authors;

    @Min(value = 1000, message = "Year must be a valid 4-digit year")
    @Max(value = 9999, message = "Year must be a valid 4-digit year")
    private int year;

    @Positive(message = "Price must be positive")
    private double price;

    @NotBlank(message = "Genre is required")
    private String genre;
}
