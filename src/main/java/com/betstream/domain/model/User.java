package com.betstream.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String role;           // USER, ADMIN
    private Boolean active;
    private Boolean emailVerified;
    @CreatedDate
    private LocalDateTime createdAt;
}
