package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {

    Optional<Bank> findByBin(String bin);

    List<Bank> findByActiveTrueOrderByShortNameAsc();
}
