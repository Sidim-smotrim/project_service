package faang.school.projectservice.service.moment;

import faang.school.projectservice.dto.moment.MomentDto;
import faang.school.projectservice.dto.project.ProjectDto;
import faang.school.projectservice.exception.DataValidException;
import faang.school.projectservice.mapper.moment.MomentMapper;
import faang.school.projectservice.model.Moment;
import faang.school.projectservice.model.Project;
import faang.school.projectservice.model.ProjectStatus;
import faang.school.projectservice.model.Team;
import faang.school.projectservice.repository.ProjectRepository;
import faang.school.projectservice.repository.MomentRepository;
import faang.school.projectservice.model.TeamMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.IntStream;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MomentServiceTest {

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Spy
    private MomentMapper momentMapper = MomentMapper.INSTANCE;

    @InjectMocks
    private MomentService momentService;

    @Test
    public void testCreate_Successful() {
        MomentDto momentDto = createMomentDto();

        when(momentRepository.save(any(Moment.class))).thenReturn(createMoment());
        when(projectRepository.getProjectById(any(Long.class))).thenReturn(createProject());

        MomentDto result = momentService.create(momentDto);

        verify(momentRepository, times(1)).save(any(Moment.class));
        assertEquals(momentDto.getName(), result.getName());
    }

    @Test
    public void testCreate_ProjectClosed() {
        MomentDto momentDto = createMomentDto();
        momentDto.getProjects().get(0).setStatus(ProjectStatus.COMPLETED);

        assertThrows(DataValidException.class, () -> momentService.create(momentDto));
        verify(momentRepository, never()).save(any(Moment.class));
    }

    @Test
    public void testCreate_ProjectTeamMembersInvalid() {
        MomentDto momentDto = createMomentDto();
        momentDto.setUserIds(List.of(1L, 2L, 3L, 8L, 7L));

        when(projectRepository.getProjectById(anyLong())).thenReturn(createProject());

        assertThrows(DataValidException.class, () -> momentService.create(momentDto));
        verify(momentRepository, never()).save(any(Moment.class));
    }

    @Test
    public void testCreateMoment_InvalidId() {
        MomentDto invalidMomentDto = createMomentDto();
        invalidMomentDto.setId(0L);
        assertThrows(DataValidException.class, () -> momentService.create(invalidMomentDto));
    }

    @Test
    public void testCreateMoment_InvalidName() {
        MomentDto invalidMomentDto = createMomentDto();
        invalidMomentDto.setName("   ");
        assertThrows(DataValidException.class, () -> momentService.create(invalidMomentDto));
    }

    @Test
    public void testCreateMoment_InvalidProjects() {
        MomentDto invalidMomentDto = createMomentDto();
        invalidMomentDto.setProjects(List.of());
        assertThrows(DataValidException.class, () -> momentService.create(invalidMomentDto));
    }

    @Test
    public void testCreateMoment_InvalidCreator() {
        MomentDto invalidMomentDto = createMomentDto();
        invalidMomentDto.setCreatedBy(0L);
        assertThrows(DataValidException.class, () -> momentService.create(invalidMomentDto));
    }

    @Test
    void updateMoment_ValidMomentDto() {
        MomentDto momentDto = createMomentDto();
        momentDto.setName("Moment 2");
        momentDto.setDate(LocalDate.of(2020, 3, 1).atStartOfDay());
        momentDto.setProjects(List.of(new ProjectDto(1L, ProjectStatus.CREATED)));

        when(momentRepository.findById(momentDto.getId())).thenReturn(Optional.of(createMoment()));
        when(projectRepository.getProjectById(any(Long.class))).thenReturn(createProject());

        MomentDto updatedMoment = momentService.update(momentDto);

        assertEquals(momentDto.getName(), updatedMoment.getName());
        assertEquals(momentDto.getDate(), updatedMoment.getDate());
        assertEquals(momentDto.getProjects(), updatedMoment.getProjects());
        assertEquals(momentDto.getDescription(), updatedMoment.getDescription());
        assertEquals(momentDto.getUserIds(), updatedMoment.getUserIds());
    }

    @Test
    void updateMoment_MomentNotFound() {
        when(projectRepository.getProjectById(any(Long.class))).thenReturn(createProject());
        when(momentRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> momentService.update(createMomentDto()));
        assertEquals("Moment not found. Id: 1", exception.getMessage());
    }


    private MomentDto createMomentDto() {
        MomentDto momentDto = new MomentDto();
        momentDto.setId(1L);
        momentDto.setName("Test Moment");
        momentDto.setDescription("Test Description");
        momentDto.setDate(LocalDateTime.now());
        momentDto.setProjects(Collections.singletonList(createProjectDto()));
        momentDto.setUserIds(createUserIds());
        momentDto.setCreatedBy(1L);
        return momentDto;
    }

    private Moment createMoment() {
        Moment moment = new Moment();
        moment.setId(1L);
        moment.setName("Test Moment");
        moment.setDescription("Test Description");
        moment.setDate(LocalDateTime.now());
        moment.setProject(Collections.singletonList(createProject()));
        moment.setCreatedBy(1L);
        return moment;
    }

    private ProjectDto createProjectDto() {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId(1L);
        projectDto.setStatus(ProjectStatus.IN_PROGRESS);
        return projectDto;
    }

    private Project createProject() {
        Project project = new Project();
        project.setId(1L);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        Team team = new Team();
        team.setTeamMembers(createTeamMembers());
        project.setTeam(team);
        return project;
    }

    private List<TeamMember> createTeamMembers() {
        List<Long> userIds = createUserIds();
        List<TeamMember> teamMembers = new ArrayList<>();
        for (Long userId : userIds) {
            TeamMember teamMember = new TeamMember();
            teamMember.setUserId(userId);
            teamMembers.add(teamMember);
        }
        return teamMembers;
    }

    private List<Long> createUserIds() {
        return IntStream.rangeClosed(1, 5).mapToLong(i -> i).boxed().toList();
    }
}
