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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.IntStream;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    public void testGetMomentById_ValidId() {
        long momentId = 1L;
        Moment moment = createMoment();

        when(momentRepository.findById(momentId)).thenReturn(Optional.of(moment));

        MomentDto expectedDto = new MomentDto();
        expectedDto.setId(momentId);

        when(momentMapper.toDto(moment)).thenReturn(expectedDto);

        MomentDto result = momentService.getMomentById(momentId);

        assertNotNull(result);
        assertEquals(momentId, result.getId());
    }

    @Test
    public void testGetMomentById_InvalidId() {
        long momentId = 1L;

        when(momentRepository.findById(momentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> momentService.getMomentById(momentId));
    }

    @Test
    public void testGetAllMoments_ReturnsListOfMomentDtos() {
        Moment moment1 = new Moment();
        moment1.setId(1L);
        Moment moment2 = new Moment();
        moment2.setId(2L);

        List<Moment> moments = List.of(moment1, moment2);

        when(momentRepository.findAll()).thenReturn(moments);

        List<MomentDto> result = momentService.getAllMoments();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
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
