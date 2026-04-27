package com.bookstore.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookstore.api.model.Book;

public interface BookRepository extends JpaRepository<Book, String> {

    @Query("""
        SELECT DISTINCT b FROM Book b JOIN b.authors a
        WHERE (:title IS NULL OR LOWER(b.title) = LOWER(CAST(:title AS string)))
          AND (:authorName IS NULL OR LOWER(a.name) = LOWER(CAST(:authorName AS string)))
        """)
    List<Book> searchBooks(@Param("title") String title, @Param("authorName") String authorName);
}
