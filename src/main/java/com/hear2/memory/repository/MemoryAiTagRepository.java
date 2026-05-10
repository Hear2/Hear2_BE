package com.hear2.memory.repository;

import com.hear2.memory.entity.MemoryAiTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryAiTagRepository extends JpaRepository<MemoryAiTag, Long> {

    List<MemoryAiTag> findByMemoryIdOrderByTagNameAsc(Long memoryId);
}
