package com.traveleasy.bus.repository;

import com.traveleasy.bus.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    @Query("SELECT v FROM EmailVerification v WHERE LOWER(v.email) = LOWER(:email) " +
           "AND v.otpCode = :otpCode AND v.purpose = :purpose AND v.isUsed = false " +
           "AND v.expiresAt > :now ORDER BY v.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findValidOtp(
            @Param("email") String email,
            @Param("otpCode") String otpCode,
            @Param("purpose") String purpose,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT v FROM EmailVerification v WHERE LOWER(v.email) = LOWER(:email) " +
           "AND v.purpose = :purpose AND v.isUsed = false AND v.expiresAt > :now " +
           "ORDER BY v.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findLatestActiveOtp(
            @Param("email") String email,
            @Param("purpose") String purpose,
            @Param("now") LocalDateTime now
    );
}
