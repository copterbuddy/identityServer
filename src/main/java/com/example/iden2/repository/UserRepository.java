package com.example.iden2.repository;

import java.util.List;
import java.util.Optional;

import com.example.iden2.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    public Optional<User> findById(Long id);

    public Optional<User> findByUsernameAndPassword(String username, String password);

    public List<User> findByClientid(String clientid);

    public List<User> findAll();

    public List<User> findByUserid(String userid);

    // public List<User> findByUseridPasswordStatusTrue(String userid);
}
