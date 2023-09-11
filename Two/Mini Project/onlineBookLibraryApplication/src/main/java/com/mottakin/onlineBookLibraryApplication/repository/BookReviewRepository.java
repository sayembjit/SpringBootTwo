package com.mottakin.onlineBookLibraryApplication.repository;

import com.mottakin.onlineBookLibraryApplication.entity.BookEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BookReserveEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BookReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReviewEntity, Long> {
    List<BookReviewEntity> findAllByBookEntity(BookEntity bookEntity);

    BookReviewEntity findByReviewId(Long reviewId);

    BookReserveEntity findByBookEntity(BookEntity bookEntity);
}
