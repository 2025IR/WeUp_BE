package com.example.weup.repository;

import com.example.weup.entity.AccountSocial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSocialRepository extends JpaRepository<AccountSocial, Long> {

    boolean existsByEmail(String email);
}
