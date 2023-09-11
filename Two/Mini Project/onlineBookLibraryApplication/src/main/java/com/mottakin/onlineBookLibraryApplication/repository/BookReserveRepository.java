package com.mottakin.onlineBookLibraryApplication.repository;

import com.mottakin.onlineBookLibraryApplication.entity.BookReserveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookReserveRepository extends JpaRepository<BookReserveEntity, Long> {
}
