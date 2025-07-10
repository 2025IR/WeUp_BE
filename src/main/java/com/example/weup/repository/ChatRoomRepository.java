package com.example.weup.repository;

import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByProject(Project project);
}
