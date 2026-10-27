package com.malik.reservation.user.entity;

import com.malik.reservation.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_email", nullable = false, unique = true, length = 254)
    private String normalizedEmail;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;


    public static AppUser create(String name, String normalizedEmail, String passwordHash, UserRole role) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setNormalizedEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }
}
