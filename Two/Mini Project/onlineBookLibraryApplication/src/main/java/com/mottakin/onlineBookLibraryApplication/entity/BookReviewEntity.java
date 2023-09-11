package com.mottakin.onlineBookLibraryApplication.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "reviews")
public class BookReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne
    @JoinColumn(name = "bookId")
    private BookEntity bookEntity;

    @ManyToOne
    @JoinColumn(name = "userId")
    private UserEntity userEntity;

    private Long rating;

    private String review;

    private LocalDate date;
}
