package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.FundExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundExpenseRepository extends JpaRepository<FundExpense, Long> {
    List<FundExpense> findByClassroomId(Long classroomId);
}
