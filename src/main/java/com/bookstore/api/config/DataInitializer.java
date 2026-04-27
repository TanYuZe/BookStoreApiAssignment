package com.bookstore.api.config;

import com.bookstore.api.model.Author;
import com.bookstore.api.model.Book;
import com.bookstore.api.model.User;
import com.bookstore.api.repository.BookRepository;
import com.bookstore.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;
        userRepository.save(new User("user",  passwordEncoder.encode("password123"), "ROLE_USER"));
        userRepository.save(new User("admin", passwordEncoder.encode("admin123"),    "ROLE_ADMIN"));
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) return;

        Book lotr = new Book();
        lotr.setIsbn("978-0-618-00222-3");
        lotr.setTitle("The Lord of the Rings");
        lotr.setYear(1954);
        lotr.setPrice(29.99);
        lotr.setGenre("Fantasy");
        lotr.setAuthors(Set.of(new Author("J.R.R. Tolkien", LocalDate.of(1892, 1, 3))));

        Book orwell = new Book();
        orwell.setIsbn("978-0-452-28423-4");
        orwell.setTitle("1984");
        orwell.setYear(1949);
        orwell.setPrice(14.99);
        orwell.setGenre("Dystopian Fiction");
        orwell.setAuthors(Set.of(new Author("George Orwell", LocalDate.of(1903, 6, 25))));

        Book huxley = new Book();
        huxley.setIsbn("978-0-06-085052-4");
        huxley.setTitle("Brave New World");
        huxley.setYear(1932);
        huxley.setPrice(13.99);
        huxley.setGenre("Dystopian Fiction");
        huxley.setAuthors(Set.of(new Author("Aldous Huxley", LocalDate.of(1894, 7, 26))));

        bookRepository.saveAll(List.of(lotr, orwell, huxley));
    }
}
