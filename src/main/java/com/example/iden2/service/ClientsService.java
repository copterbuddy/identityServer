package com.example.iden2.service;

import java.util.Optional;

import javax.transaction.Transactional;

import com.example.iden2.model.Clients;
import com.example.iden2.repository.ClientsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class ClientsService {

    @Autowired
    private ClientsRepository clientsRepository;

    public Clients findByClientsId(String clientId) {
        var clientsId = clientsRepository.findByClientid(clientId).stream().findFirst().orElse(null);
        return clientsId;
    }

    public Optional<Clients> findById(Long id) {
        Optional<Clients> clients = clientsRepository.findById(id);
        return clients;
    }
}
