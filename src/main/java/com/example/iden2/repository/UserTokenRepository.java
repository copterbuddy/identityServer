package com.example.iden2.repository;

import java.util.Optional;

import com.example.iden2.model.UserToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    public Optional<UserToken> findByToken(String userToken);
}
