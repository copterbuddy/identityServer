package com.example.iden2.repository;

import java.util.Optional;

import com.example.iden2.model.Clients;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientsRepository extends JpaRepository<Clients, Long> {

    public Optional<Clients> findById(Long id);

    // public List<Clients> findByClientidEquals(String clientid);

    public Optional<Clients> findByClientid(String clientid);
}
