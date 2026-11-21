package com.cmc.hackathon4.domain.testitem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestItemRepository extends JpaRepository<TestItem, Long> {
}
