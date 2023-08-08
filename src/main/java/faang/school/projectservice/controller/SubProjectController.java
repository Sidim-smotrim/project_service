package faang.school.projectservice.controller;

import faang.school.projectservice.dto.project.ProjectDto;
import faang.school.projectservice.dto.project.ProjectFilterDto;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subproject")
public class SubProjectController {
    private final ProjectService projectService;

    @PostMapping("/create")
    public ProjectDto createSubProject(@RequestBody ProjectDto projectDto) {
        validateSubProject(projectDto);
        return projectService.createSubProject(projectDto);
    }

    @PutMapping("/update")
    public Timestamp updateSubProject(@RequestBody ProjectDto projectDto) {
        validateSubProject(projectDto);
        return projectService.updateSubProject(projectDto);
    }

    @PostMapping("project/{projectId}/children")
    public List<ProjectDto> getProjectChildrenWithFilter(@RequestBody ProjectFilterDto filterDto, @PathVariable long projectId) {
        return projectService.getProjectChildrenWithFilter(filterDto, projectId);
    }

    private void validateSubProject(ProjectDto projectDto) {
        if (projectDto.getName() == null || projectDto.getName().isBlank()) {
            throw new DataValidationException("Project can't be created with empty name");
        }
        if (projectDto.getParentProjectId() == null) {
            throw new DataValidationException("SubProject must have parentProjectId");
        }
    }
}