package com.traveleasy.bus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "role", nullable = false, length = 50)
    private String role; // "TRAVELLER", "OPERATOR", "ADMIN"

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(name = "company_name", length = 255)
    private String companyName;
}
