package com.mottakin.onlineBookLibraryApplication.service;

import com.mottakin.onlineBookLibraryApplication.entity.BookEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BookReserveEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BookReviewEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BorrowReturnEntity;
import com.mottakin.onlineBookLibraryApplication.model.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(UserDto user) throws Exception;
    UserDto getUser(String email);

    UserDto getUserByUserId(String id) throws Exception;

    /*Book Borrow and Return Service*/
    public BorrowReturnEntity bookBorrow(Long bookId) throws Exception ;

    public BorrowReturnEntity bookReturn(Long bookId) throws Exception;

    public List<BookEntity> getAllBookByUser(String userId) throws Exception;
    public List<BookEntity> getAllBorrowedBookByUser(String userId) throws Exception;
    public List<BorrowReturnEntity> getUserAllHistory(String userId) throws Exception;
    public BookReviewEntity createBookReview(Long bookId, BookReviewEntity bookReviewDto) throws Exception;
    public List<BookReviewEntity> allBookReview(Long bookId) throws Exception;
    public void deleteReview(Long bookId, Long reviewId) throws Exception;
    public BookReviewEntity updateReview(Long bookId, Long reviewId, BookReviewEntity bookReviewEntity) throws Exception;
    public BookReserveEntity cancelReserveBook(Long bookId) throws Exception;
    public BookReserveEntity reserveBook(Long bookId) throws Exception;
}
