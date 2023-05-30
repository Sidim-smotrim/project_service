package faang.school.projectservice.service;

import faang.school.projectservice.dto.StageDto;
import faang.school.projectservice.dto.StageRolesDto;
import faang.school.projectservice.dto.filter.StageFilterDto;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.mapper.StageMapper;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.Stage;
import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.TeamRole;
import faang.school.projectservice.repository.ProjectRepository;
import faang.school.projectservice.repository.StageRepository;
import faang.school.projectservice.repository.StageRolesRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StageService {
    private final StageRepository stageRepository;
    private final StageRolesRepository stageRolesRepository;
    private final ProjectRepository projectRepository;
    private final StageMapper mapper;

    @Transactional
    public StageDto createStage(StageDto stageDto) {
        validateStageDto(stageDto);
        Stage stage = mapper.toEntity(stageDto);
        Project project = projectRepository.findById(stageDto.getProjectId()).get();
        stage.setProject(project);
        return mapper.toDto(stageRepository.save(stage));
    }

    @Transactional(readOnly = true)
    public List<StageDto> findStageByFilter(StageFilterDto filter) {
        List<Project> projects = projectRepository.findAll();
        List<Stage> stages = new ArrayList<>();
        List<Project> filteredProjects = projects.stream()
                .filter(f -> f.getStatus() == filter.getProjectStatus())
                .skip((long) filter.getSize() * filter.getPage())
                .toList();
        filteredProjects.forEach(i -> stages.addAll(i.getStages()));
        return stages.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StageDto findStageById(Long stageId) {
        return mapper.toDto(stageRepository.findById(stageId).orElseThrow(
                () -> new EntityNotFoundException(String.format("Stage not found by id: %s", stageId))
        ));
    }

    @Transactional
    public void removeStageById(Long stageId) {
        stageRepository.delete(stageRepository.findById(stageId).orElseThrow(
                () -> new EntityNotFoundException(String.format("Stage not found by id: %s", stageId))
        ));
    }

    @Transactional(readOnly = true)
    public List<StageDto> findAll() {
        return stageRepository.findAll().stream().map(mapper::toDto).toList();
    }

    private void validateStageDto(StageDto stageDto) {
        Project project = projectRepository.findById(stageDto.getProjectId()).orElseThrow(
                () -> new EntityNotFoundException(String.format("Project not found by id : %s", stageDto.getProjectId())));
        List<TeamMember> teamMembers = project.getTeam().getTeamMembers();
        List<StageRolesDto> stageRolesDtos = stageDto.getStageRolesDtos();

        //сначала сравниваем размеры
        int teamSize = teamMembers.size();
        int stageRolesSize = stageRolesDtos.size();
        if (teamSize < stageRolesSize) {
            throw new DataValidationException(String
                    .format("The size of the planned stage %s cannot be larger then %s", teamSize, stageRolesSize));
        }
        //проверяем, есть ли в команде достаточное количество ролей
        Map<TeamRole, Integer> teamsMap = new EnumMap<>(TeamRole.class);
        for (StageRolesDto stageRolesDto : stageRolesDtos) {
            TeamRole teamRole = stageRolesDto.getTeamRole();
            teamsMap.put(teamRole, teamsMap.getOrDefault(teamRole, 0) + 1);
        }
        for (TeamMember teamMember : teamMembers) {
            List<TeamRole> teamRoles = teamMember.getRoles();
            for (TeamRole teamRole : teamRoles) {
                int count = teamsMap.get(teamRole);
                if (count == 0) {
                    break;
                }
                count--;
                teamsMap.put(teamRole, count);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<TeamRole, Integer> entry : teamsMap.entrySet()) {
            if (entry.getValue() > 0) {
                builder.append(String.format("The team is missing %s more %sS", entry.getValue(), entry.getKey().toString()));
                builder.append("\n");
            }
        }
        String exceptionString = builder.toString();
        if (exceptionString.length() > 0) {
            throw new DataValidationException(exceptionString);
        }
    }
}
