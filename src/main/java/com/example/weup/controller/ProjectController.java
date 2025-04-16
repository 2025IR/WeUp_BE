package com.example.weup.controller;

import com.example.weup.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

}