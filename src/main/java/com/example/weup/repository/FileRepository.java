package com.example.weup.repository;

import com.example.weup.entity.Board;
import com.example.weup.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findAllByBoard(Board board);

    boolean existsByBoard(Board board);
}
