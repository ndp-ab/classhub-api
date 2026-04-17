package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.FundExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface FundExpenseRepository extends JpaRepository<FundExpense, Long> {
    List<FundExpense> findByClassroomId(Long classroomId);
}
