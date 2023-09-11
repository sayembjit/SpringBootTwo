package com.mottakin.onlineBookLibraryApplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottakin.onlineBookLibraryApplication.constants.AppConstants;
import com.mottakin.onlineBookLibraryApplication.entity.BookEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BookReserveEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BookReviewEntity;
import com.mottakin.onlineBookLibraryApplication.entity.BorrowReturnEntity;
import com.mottakin.onlineBookLibraryApplication.exception.BookNotFoundException;
import com.mottakin.onlineBookLibraryApplication.model.UserDto;
import com.mottakin.onlineBookLibraryApplication.model.UserLoginRequestModel;
import com.mottakin.onlineBookLibraryApplication.repository.BorrowReturnRepository;
import com.mottakin.onlineBookLibraryApplication.service.impl.UserServiceImpl;
import com.mottakin.onlineBookLibraryApplication.utils.JWTUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
/*@RequestMapping("/users")*/

public class UserController {
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private AuthenticationManager authenticationManager;
    @GetMapping("/hello")
    public String hello(){
        return "Hello";
    }

    @GetMapping("/hello2")
    public String hello2(){
        return "Hello2";
    }

    @GetMapping("/welcome")
    public String welcome(){
        return "Welcome!!! This endpoint is not secure";
    }
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserByUserId(@PathVariable String userId) {
        try {
            UserDto userDto = userServiceImpl.getUserByUserId(userId);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*Registration Method for the users*/

    @PostMapping("/user/register")
    public ResponseEntity<?> register (@RequestBody UserDto userDto) {
        try {
            UserDto createdUser = userServiceImpl.createUser(userDto);
            String accessToken = JWTUtils.generateToken(createdUser.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("user", createdUser);
            response.put(AppConstants.HEADER_STRING, AppConstants.TOKEN_PREFIX + accessToken);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }
    }

    /*Login Method for our users*/

    @PostMapping("/user/login")
    public void login(@RequestBody UserLoginRequestModel userLoginRequestModel, HttpServletResponse response) throws IOException {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginRequestModel.getEmail(), userLoginRequestModel.getPassword()));
            if (authentication.isAuthenticated()) {
                UserDto userDto = userServiceImpl.getUser((userLoginRequestModel.getEmail()));
                String accessToken = JWTUtils.generateToken(userDto.getEmail());

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("userId", userDto.getUserId());
                responseBody.put("email", userDto.getEmail());
                responseBody.put(AppConstants.HEADER_STRING, AppConstants.TOKEN_PREFIX + accessToken);

                response.setContentType("application/json");
                response.getWriter().write(new ObjectMapper().writeValueAsString(responseBody));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Authentication failed");
                errorResponse.put("message", "Invalid email or password");
                response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
            }
        } catch (UsernameNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found");
            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        }
    }
    @PostMapping("/create")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> createBook(@RequestBody BookEntity book) {
        try {
            BookEntity createdBook = userServiceImpl.createBook(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
        } catch (BookNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred.");
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> updateBook(@RequestBody BookEntity book) {
        try {
            BookEntity updatedBook = userServiceImpl.updateBook(book);
            return ResponseEntity.ok(updatedBook);
        } catch (BookNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred.");
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            userServiceImpl.deleteBook(id);
            return ResponseEntity.ok("Book deleted successfully.");
        } catch (BookNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred.");
        }
    }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('admin', 'USER')")
    public ResponseEntity<?> getAllBooks() {
        try {
            List<BookEntity> books = userServiceImpl.getAllBooks();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred.");
        }
    }

    /*Book Borrow Return */
    @PostMapping("/borrow/{bookId}")
    public ResponseEntity<?> borrowBook(@PathVariable Long bookId){
        try{
            BorrowReturnEntity success = userServiceImpl.bookBorrow(bookId);
            return ResponseEntity.ok(success);}
        catch (Exception e) {

            return ResponseEntity.badRequest().body("Book is not available for borrowing.");
        }

    }

    @PostMapping("/return/{bookId}")
    public ResponseEntity<?> returnBook(@PathVariable Long bookId) {
        try{
            BorrowReturnEntity success = userServiceImpl.bookReturn(bookId);
            return ResponseEntity.ok(success);}
        catch (Exception e) {

            return ResponseEntity.badRequest().body("Book is not available for borrowing.");
        }
    }

    @GetMapping("/{userId}/books")
    public ResponseEntity<?> getALLBooksByUser(@PathVariable String userId) {
        try {
            List<BookEntity> allBookByUser = userServiceImpl.getAllBookByUser(userId);
            return new ResponseEntity<>(allBookByUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/{userId}/borrowed-books")
    public ResponseEntity<?> getBorrowedBooks(@PathVariable String userId) {
        try {
            List<BookEntity> allBookByUser = userServiceImpl.getAllBorrowedBookByUser(userId);
            return new ResponseEntity<>(allBookByUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/{userId}/history")
    public ResponseEntity<?> userHistory(@PathVariable String userId) {
        try {
            List<BorrowReturnEntity> userAllHistory = userServiceImpl.getUserAllHistory(userId);
            return new ResponseEntity<>(userAllHistory, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{bookId}/reviews/create")
    public ResponseEntity<?> createReview(@PathVariable Long bookId, @RequestBody BookReviewEntity bookReviewEntity) {
        try {
            BookReviewEntity newReview =  userServiceImpl.createBookReview(bookId, bookReviewEntity);
            return new ResponseEntity<>(newReview, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/{bookId}/reviews")
    public ResponseEntity<?> allReview(@PathVariable Long bookId) {
        try {
            List <BookReviewEntity> newReview =  userServiceImpl.allBookReview(bookId);
            return new ResponseEntity<>(newReview, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @DeleteMapping("/{bookId}/reviews/{reviewId}/delete")
    public ResponseEntity<?> deleteReview (@PathVariable Long bookId, @PathVariable Long reviewId) {
        try {
            userServiceImpl.deleteReview(bookId, reviewId);
            return new  ResponseEntity<>("Review Deleted!", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @PutMapping("/{bookId}/reviews/{reviewId}/update")
    public ResponseEntity<?> updateReview (@PathVariable Long bookId, @PathVariable Long reviewId, @RequestBody BookReviewEntity bookReviewEntity) {
        try {
            BookReviewEntity updatedReview =  userServiceImpl.updateReview(bookId, reviewId, bookReviewEntity);
            return new  ResponseEntity<>(updatedReview, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping("/{bookId}/reserve")
    public ResponseEntity<?> reserveBook (@PathVariable Long bookId) {
        try {
            BookReserveEntity updatedReview =  userServiceImpl.reserveBook(bookId);
            return new  ResponseEntity<>(updatedReview, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{bookId}/cancel-reservation")
    public ResponseEntity<?> cancelReserveBook (@PathVariable Long bookId) {
        try {
            BookReserveEntity cancelReview =  userServiceImpl.cancelReserveBook(bookId);
            return new  ResponseEntity<>(cancelReview, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
