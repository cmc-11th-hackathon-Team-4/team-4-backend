package com.cmc.hackathon4.domain.testitem.repository;

import com.cmc.hackathon4.domain.testitem.entity.TestItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestItemRepository extends JpaRepository<TestItem, Long> {
}
