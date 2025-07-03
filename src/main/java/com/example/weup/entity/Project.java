package com.example.weup.entity;

import com.example.weup.dto.request.ProjectEditRequestDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", updatable = false, nullable = false)
    private Long projectId;

    @Column(name = "project_name", nullable = false, length = 50)
    private String projectName;

    @Column(nullable = false)
    @Builder.Default
    private String description = "프로젝트 입니다.";

    @Column(nullable = false)
    private String projectImage;

    @Column(nullable = false)
    @Builder.Default
    private boolean status = true;

    @Column(name = "project_created_time", nullable = false)
    @Builder.Default
    private LocalDate projectCreatedTime = LocalDate.now();

    @Column(name = "project_ended_time")
    private LocalDate projectEndedTime;

    @Column(name = "is_revealed_number", nullable = false)
    @Builder.Default
    private boolean isRevealedNumber = false;

    @Column
    private String roomName;

    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<Member> members = new ArrayList<>();


    public void editProjectInfo(ProjectEditRequestDTO dto) {
        this.projectName = dto.getProjectName();
        this.status = dto.isStatus();
        this.isRevealedNumber = dto.isRevealedNumber();
    }

    public void editProjectImage(String image) {
        this.projectImage = image;
    }

    public void editProjectDescription(String description) {
        this.description = description;
    }

}