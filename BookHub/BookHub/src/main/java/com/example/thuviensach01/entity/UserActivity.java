package com.example.thuviensach01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_activity", indexes = {
        @Index(name = "idx_activity_username", columnList = "username"),
        @Index(name = "idx_activity_createdAt", columnList = "createdAt")
})
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // tên người dùng (nếu ẩn danh thì để null)
    @Column(length = 64)
    private String username;

    // hành động: LOGIN / LOGOUT / REGISTER / CREATE_BOOK / UPDATE_BOOK / DELETE_BOOK / CHANGE_PASSWORD ...
    @Column(nullable = false, length = 40)
    private String action;

    // mô tả thêm (tuỳ)
    @Column(length = 500)
    private String details;

    // thông tin request (không bắt buộc)
    @Column(length = 64)
    private String ip;

    @Column(length = 200)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }


    public static UserActivity of(String username, String action, String details, String ip, String ua) {
        return UserActivity.builder()
                .username(username)
                .action(action)
                .details(details)
                .ip(ip)
                .userAgent(ua)
                .createdAt(Instant.now())
                .build();
    }
}