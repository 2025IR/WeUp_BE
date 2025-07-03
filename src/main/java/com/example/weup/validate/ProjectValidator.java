package com.example.weup.validate;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Project;
import com.example.weup.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectValidator {

    private final ProjectRepository projectRepository;

    public Project validateActiveProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        if (!project.isStatus()) {
            throw new GeneralException(ErrorInfo.ENDED_PROJECT);
        }

        return project;
    }
}
