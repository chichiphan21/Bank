package org.dacs.Common;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private double balance;

    @Column(name = "is_online")
    private boolean online;


    public User(Long id, String username, double balance) {
        this.id = id;
        this.username = username;
        this.balance = balance;
    }

    @Column
    private String otp;

    @Column
    private LocalDateTime otpCreatedAt;



}
