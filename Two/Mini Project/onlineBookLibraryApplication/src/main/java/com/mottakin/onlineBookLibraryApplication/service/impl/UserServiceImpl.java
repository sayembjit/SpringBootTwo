package com.mottakin.onlineBookLibraryApplication.service.impl;

import com.mottakin.onlineBookLibraryApplication.constants.AppConstants;
import com.mottakin.onlineBookLibraryApplication.entity.*;
import com.mottakin.onlineBookLibraryApplication.exception.BookNotFoundException;
import com.mottakin.onlineBookLibraryApplication.model.UserDto;
import com.mottakin.onlineBookLibraryApplication.repository.*;
import com.mottakin.onlineBookLibraryApplication.service.UserService;
import com.mottakin.onlineBookLibraryApplication.utils.JWTUtils;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional

public class UserServiceImpl implements UserService, UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private BorrowReturnRepository borrowReturnRepository;
    @Autowired
    private BookReviewRepository bookReviewRepository;
    @Autowired
    private BookReserveRepository bookReserveRepository;

    public BookEntity createBook(BookEntity book) {
        return bookRepository.save(book);
    }

    public BookEntity updateBook(BookEntity updatedBook) {
        Optional<BookEntity> existingBook = bookRepository.findById(updatedBook.getId());
        if (existingBook.isPresent()) {
            BookEntity bookToUpdate = existingBook.get();
            bookToUpdate.setTitle(updatedBook.getTitle());
            return bookRepository.save(bookToUpdate);
        } else {
            throw new BookNotFoundException("Book not found with ID: " + updatedBook.getId());
        }
    }

    public void deleteBook(Long bookId) {
        Optional<BookEntity> existingBook = bookRepository.findById(bookId);
        if (existingBook.isPresent()) {
            bookRepository.deleteById(bookId);
        } else {
            throw new BookNotFoundException("Book not found with ID: " + bookId);
        }
    }

    public List<BookEntity> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public UserDto createUser(UserDto user) throws Exception {
        if (userRepository.findByEmail(user.getEmail()).isPresent())
            throw new Exception("Record already exists");

        ModelMapper modelMapper = new ModelMapper();
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        userEntity.setAddress(user.getAddress());
        userEntity.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        String publicUserId = JWTUtils.generateUserID(10);
        userEntity.setUserId(publicUserId);
        userEntity.setRole(user.getRole());
        UserEntity storedUserDetails = userRepository.save(userEntity);
        UserDto returnedValue = modelMapper.map(storedUserDetails, UserDto.class);
        String accessToken = JWTUtils.generateToken(userEntity.getEmail());
        returnedValue.setAccessToken(AppConstants.TOKEN_PREFIX + accessToken);
        return returnedValue;
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email).get();
        if (userEntity == null) throw new UsernameNotFoundException("No record found");
        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity, returnValue);
        return returnValue;
    }

    @Override
    public UserDto getUserByUserId(String userId) throws Exception {
        UserDto returnValue = new UserDto();
        UserEntity userEntity = userRepository.findByUserId(userId).orElseThrow(Exception::new);
        BeanUtils.copyProperties(userEntity, returnValue);
        return returnValue;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).get();
        if (userEntity == null) throw new UsernameNotFoundException(email);
        return new User(userEntity.getEmail(), userEntity.getPassword(),
                true, true, true, true, new ArrayList<>());
    }

    /*Book Borrow and Return Service*/

    public BorrowReturnEntity bookBorrow(Long bookId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String userId = user.get().getUserId();

        UserEntity userEntity = userRepository.findByUserId(userId).get();
        BookEntity bookEntity = bookRepository.findById(bookId).get();

        if (Objects.equals(bookEntity.getAvailable(), "BORROWED")) throw new Exception("unavailable!!!");

        ModelMapper modelMapper = new ModelMapper();
        BorrowReturnEntity borrowReturnEntity = new BorrowReturnEntity();
        borrowReturnEntity.setBookEntity(bookEntity);
        borrowReturnEntity.setUserEntity(userEntity);
        borrowReturnEntity.setBorrowDate(LocalDate.now());
        borrowReturnEntity.setDueDate(LocalDate.now().plusDays(7));
        bookEntity.setAvailable("BORROWED");


        BorrowReturnEntity storeBorrowDetails = borrowReturnRepository.save(borrowReturnEntity);

        return modelMapper.map(storeBorrowDetails, BorrowReturnEntity.class);
    }
    public BorrowReturnEntity bookReturn(Long bookId) throws Exception{
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String userId = user.get().getUserId();

        UserEntity userEntity = userRepository.findByUserId(userId).get();
        BookEntity bookEntity = bookRepository.findById(bookId).get();

        BorrowReturnEntity borrowReturnEntity = borrowReturnRepository.findByUserEntityAndBookEntityAndReturnDateIsNull(userEntity,bookEntity);
        ModelMapper modelMapper = new ModelMapper();
        borrowReturnEntity.setReturnDate(LocalDate.now());
        bookEntity.setAvailable("AVAILABLE");

        BorrowReturnEntity storeReturnDetails = borrowReturnRepository.save(borrowReturnEntity);

        return modelMapper.map(storeReturnDetails, BorrowReturnEntity.class);

    }
    public List<BookEntity> getAllBookByUser(String userId) throws Exception{
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String currentUserRole = user.get().getRole();
        String currentUserId = user.get().getUserId();
        if (!currentUserId.equals(userId) && currentUserRole.equals("USER")) {
            throw new Exception("Can not access!!!!");
        }
        List<BorrowReturnEntity> bookBorrowings = borrowReturnRepository.findAllByUserEntity(user.get());

        List<BookEntity> books = bookBorrowings.stream()
                .map(BorrowReturnEntity::getBookEntity)
                .collect(Collectors.toList());

        return books;

    }

    public List<BookEntity> getAllBorrowedBookByUser(String userId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String currentUserRole = user.get().getRole();
        String currentUserId = user.get().getUserId();
        if (!currentUserId.equals(userId) && currentUserRole.equals("USER")) {
            throw new Exception("Can not access!!!");
        }
        List<BorrowReturnEntity> bookBorrowings = borrowReturnRepository.findAllByUserEntityAndReturnDateIsNull(user.get());

        List<BookEntity> books = bookBorrowings.stream()
                .map(BorrowReturnEntity::getBookEntity)
                .collect(Collectors.toList());
        return books;
    }

    public List<BorrowReturnEntity> getUserAllHistory(String userId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String currentUserRole = user.get().getRole();
        String currentUserId = user.get().getUserId();
        if (!currentUserId.equals(userId) && currentUserRole.equals("USER")) {
            throw new Exception("Can not access!!!");
        }
        List<BorrowReturnEntity> bookBorrowings = borrowReturnRepository.findAllByUserEntity(user.get());

        List<BorrowReturnEntity> bookBorrowingInfoList = bookBorrowings.stream()
                .map(bookBorrowingEntity -> BorrowReturnEntity.builder()
                        .borrowId(bookBorrowingEntity.getBorrowId())
                        .bookEntity(bookBorrowingEntity.getBookEntity())
                        .borrowDate(bookBorrowingEntity.getBorrowDate())
                        .dueDate(bookBorrowingEntity.getDueDate())
                        .returnDate(bookBorrowingEntity.getReturnDate())
                        .build()
                )
                .collect(Collectors.toList());
        return bookBorrowingInfoList;
    }

    public BookReviewEntity createBookReview(Long bookId, BookReviewEntity bookReviewEntity) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String userId = user.get().getUserId();

        UserEntity userEntity = userRepository.findByUserId(userId).get();
        BookEntity bookEntity = bookRepository.findById(bookId).get();

        ModelMapper modelMapper = new ModelMapper();
        BookReviewEntity bookReviewEntityUpdated = new BookReviewEntity();

        bookReviewEntityUpdated.setBookEntity(bookEntity);
        bookReviewEntityUpdated.setUserEntity(userEntity);
        bookReviewEntityUpdated.setReview(bookReviewEntity.getReview());
        bookReviewEntityUpdated.setRating(bookReviewEntity.getRating());
        bookReviewEntityUpdated.setDate(LocalDate.now());

        BookReviewEntity storeReview = bookReviewRepository.save(bookReviewEntityUpdated);

        return modelMapper.map(storeReview, BookReviewEntity.class);
    }
    public List<BookReviewEntity> allBookReview(Long bookId) throws Exception {
        BookEntity bookEntity = bookRepository.findById(bookId).get();
        ModelMapper modelMapper = new ModelMapper();
        List<BookReviewEntity> bookReviews = bookReviewRepository.findAllByBookEntity(bookEntity);
        List<BookReviewEntity> bookReview = bookReviews.stream()
                .map(reviewEntity -> modelMapper.map(reviewEntity, BookReviewEntity.class))
                .collect(Collectors.toList());
        return bookReview;
    }


    public void deleteReview(Long bookId, Long reviewId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String currentUserRole = user.get().getRole();
        String currentUserId = user.get().getUserId();

        BookReviewEntity bookReview = bookReviewRepository.findByReviewId(reviewId);
        String userId = bookReview.getUserEntity().getUserId();

        if (!currentUserId.equals(userId) && currentUserRole.equals("USER")) {
            throw new Exception("You can not access this!");
        }

        if (!bookReview.getBookEntity().getId().equals(bookId)) {
            throw new Exception("Book id or Review id is wrong!");
        }

        bookReviewRepository.delete(bookReview);



    }

    public BookReviewEntity updateReview(Long bookId, Long reviewId, BookReviewEntity bookReviewEntity) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String currentUserRole = user.get().getRole();
        String currentUserId = user.get().getUserId();

        BookReviewEntity bookReview = bookReviewRepository.findByReviewId(reviewId);
        String userId = bookReview.getUserEntity().getUserId();

        if (!currentUserId.equals(userId) && currentUserRole.equals("USER")) {
            throw new Exception("You are not authorized to access this!");
        }

        if (!bookReview.getBookEntity().getId().equals(bookId)) {
            throw new Exception("Book id or Review id is wrong!");
        }

        bookReview.setRating(bookReviewEntity.getRating());
        bookReview.setReview(bookReviewEntity.getReview());
        bookReview.setDate(LocalDate.now());

        bookReviewRepository.save(bookReview);
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(bookReview, BookReviewEntity.class);
    }


    public BookReserveEntity reserveBook(Long bookId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String userId = user.get().getUserId();

        UserEntity userEntity = userRepository.findByUserId(userId).get();
        BookEntity bookEntity = bookRepository.findById(bookId).get();

        if (Objects.equals(bookEntity.getAvailable(), "AVAILABLE")) throw new Exception("This book is already available, you can borrow this!");


        ModelMapper modelMapper = new ModelMapper();
        BookReserveEntity bookReserveEntity = new BookReserveEntity();
        bookReserveEntity.setBookEntity(bookEntity);
        bookReserveEntity.setUserEntity(userEntity);
        bookReserveEntity.setReserveDate(LocalDate.now());
        bookReserveEntity.setStatus("PENDING");


        BookReserveEntity storeReserve = bookReserveRepository.save(bookReserveEntity);
        return modelMapper.map(storeReserve, BookReserveEntity.class);

    }

    public BookReserveEntity cancelReserveBook(Long bookId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        String userId = user.get().getUserId();

        UserEntity userEntity = userRepository.findByUserId(userId).get();
        BookEntity bookEntity = bookRepository.findById(bookId).get();

        ModelMapper modelMapper = new ModelMapper();
        BookReserveEntity bookCancelReserveEntity = bookReviewRepository.findByBookEntity(bookEntity);

        if (bookCancelReserveEntity.getUserEntity() != userEntity) throw new Exception("You are not authorized to cancel reservation!");


        bookCancelReserveEntity.setStatus("CANCEL");

        BookReserveEntity cancelReserve = bookReserveRepository.save(bookCancelReserveEntity);
        return modelMapper.map(cancelReserve, BookReserveEntity.class);

    }
}