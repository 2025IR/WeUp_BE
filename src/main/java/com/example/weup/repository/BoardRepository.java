package com.example.weup.repository;

import com.example.weup.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    @Query("""
    SELECT b FROM Board b
    LEFT JOIN FETCH b.tag t
    LEFT JOIN FETCH b.member m
    LEFT JOIN FETCH m.user u
    WHERE b.project.projectId = :projectId
      AND (:tag IS NULL OR t.tagName = :tag)
      AND (:search IS NULL OR b.title LIKE %:search% OR b.contents LIKE %:search%)
    """)
    Page<Board> findByProjectIdAndFilters(Long projectId, String tag, String search, Pageable pageable);

}
