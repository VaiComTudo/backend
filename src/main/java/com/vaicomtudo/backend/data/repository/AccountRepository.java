package com.vaicomtudo.backend.data.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vaicomtudo.backend.data.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

}
