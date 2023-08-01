package faang.school.projectservice.dto.project;

import faang.school.projectservice.model.ProjectStatus;
import faang.school.projectservice.model.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDto {
    private Long id;
    @NotNull(message = "Project description should not be null")
    @Size(max = 4096, message = "Project's description length can't be more than 4096 symbols")
    private String description;
    @NotNull(message = "Project name should not be null")
    @NotBlank(message = "Project can't be created with empty name")
    @Size(max = 128, message = "Project's name length can't be more than 128 symbols")
    private String name;
    private long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ProjectVisibility visibility;
    private ProjectStatus status;
}
