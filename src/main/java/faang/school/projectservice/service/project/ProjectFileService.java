package faang.school.projectservice.service.project;

import com.amazonaws.services.s3.model.S3Object;
import faang.school.projectservice.dto.resource.GetResourceDto;
import faang.school.projectservice.dto.resource.ResourceDto;
import faang.school.projectservice.dto.resource.UpdateResourceDto;
import faang.school.projectservice.exception.InvalidCurrentUserException;
import faang.school.projectservice.exception.StorageSpaceExceededException;
import faang.school.projectservice.jpa.ResourceRepository;
import faang.school.projectservice.mapper.ResourceMapper;
import faang.school.projectservice.model.TeamMember;
import faang.school.projectservice.model.project.Project;
import faang.school.projectservice.model.resource.Resource;
import faang.school.projectservice.model.resource.ResourceStatus;
import faang.school.projectservice.model.resource.ResourceType;
import faang.school.projectservice.repository.ProjectRepository;
import faang.school.projectservice.util.FileService;
import faang.school.projectservice.validator.FileValidator;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectFileService {
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final FileService fileService;
    private final ResourceMapper resourceMapper;
    private final FileValidator fileValidator;

    @Transactional
    public ResourceDto uploadFile(MultipartFile multipartFile, long projectId, long userId) {
        Resource resource;
        String fileKey;
        try {
            Project project = projectRepository.getProjectById(projectId);
            TeamMember teamMember = findTeamMember(project, userId);
            fileValidator.validateFreeStorageCapacity(project, BigInteger.valueOf(multipartFile.getSize()));

            fileKey = generateFileKey(multipartFile, projectId);
            resource = fillUpResource(multipartFile, project, teamMember, fileKey);

            updateProjectStorage(resource);
            resourceRepository.save(resource);
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Could not update due to concurrent modifications. Please try again.");
        }

        fileService.upload(multipartFile, fileKey);
        return resourceMapper.toDto(resource);
    }

    @Transactional
    public UpdateResourceDto updateFile(MultipartFile multipartFile, long resourceId, long userId) {
        Resource resource;
        String fileKey;
        try {
            resource = resourceRepository.getReferenceById(resourceId);
            TeamMember updatedBy = findTeamMember(resource.getProject(), userId);
            fileValidator.validateFileOnUpdate(resource.getName(), multipartFile.getOriginalFilename());
            fileValidator.validateIfUserCanChangeFile(resource, userId);
            BigInteger storageCapacityOnUpdate = storageCapacityOnUpdate(
                    resource, BigInteger.valueOf(multipartFile.getSize()));

            fileKey = generateFileKey(multipartFile, resource.getProject().getId());
            resource.getProject().setStorageSize(storageCapacityOnUpdate);

            resource.setUpdatedBy(updatedBy);
            resource.setKey(fileKey);
            resource.setSize(BigInteger.valueOf(multipartFile.getSize()));
            resourceRepository.save(resource);

            updateProjectStorage(resource);
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Could not update due to concurrent modifications. Please try again.");
        }

        fileService.delete(resource.getKey());
        fileService.upload(multipartFile, fileKey);
        return resourceMapper.toUpdateDto(resource);
    }

    @Transactional
    public void deleteFile(long resourceId, long userId) {
        Resource resource;
        try {
            resource = resourceRepository.getReferenceById(resourceId);
            TeamMember updatedBy = findTeamMember(resource.getProject(), userId);
            fileValidator.validateIfUserCanChangeFile(resource, userId);
            fileValidator.validateResourceOnDelete(resource);

            resource.setStatus(ResourceStatus.DELETED);
            resource.setUpdatedBy(updatedBy);
            updateProjectStorage(resource);

            resourceRepository.save(resource);
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Could not update due to concurrent modifications. Please try again.");
        }

        fileService.delete(resource.getKey());
    }

    @Transactional(readOnly = true)
    public GetResourceDto getFile(long resourceId, long userId) {
        Resource resource = resourceRepository.getReferenceById(resourceId);
        findTeamMember(resource.getProject(), userId);
        S3Object file = fileService.getFile(resource.getKey());

        return GetResourceDto.builder()
                .name(resource.getName())
                .type(file.getObjectMetadata().getContentType())
                .inputStream(file.getObjectContent())
                .size(resource.getSize().longValue())
                .build();
    }

    private String generateFileKey(MultipartFile multipartFile, long projectId) {
        String fileName = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize();

        return String.format("p%d_%s_%s", projectId, size, fileName);
    }

    private TeamMember findTeamMember(Project project, long userId) {
        Optional<TeamMember> matchingMember = project.getTeams().stream()
                .flatMap(team -> team.getTeamMembers().stream())
                .filter(teamMember -> teamMember.getUserId() == userId)
                .findAny();

        if (matchingMember.isEmpty()) {
            String errorMessage = String.format(
                    "The user with id: %d is not on the project", userId);
            throw new InvalidCurrentUserException(errorMessage);
        } else {
            return matchingMember.get();
        }
    }

    private BigInteger storageCapacityOnUpdate(Resource resource, BigInteger fileSize) {
        BigInteger storageSize = resource.getProject().getStorageSize();
        BigInteger resourceSize = resource.getSize();
        BigInteger storageCapacity = storageSize.add(resourceSize);

        if (fileSize.compareTo(storageCapacity) > 0) {
            String errorMessage = String.format(
                    "Impossible to update %s, project %d storage has not enough space",
                    resource.getName(), resource.getProject().getId());
            throw new StorageSpaceExceededException(errorMessage);
        }

        return storageCapacity;
    }

    private void updateProjectStorage(Resource resource) {
        Project project = resource.getProject();
        BigInteger storageSize = project.getStorageSize();
        BigInteger resourceSize = resource.getSize();

        if (resource.getStatus().equals(ResourceStatus.DELETED)) {
            project.setStorageSize(storageSize.add(resourceSize));
        } else {
            project.setStorageSize(storageSize.subtract(resourceSize));
        }

        projectRepository.save(project);
    }

    private Resource fillUpResource(MultipartFile multipartFile, Project project, TeamMember teamMember, String fileKey) {
        return Resource.builder()
                .name(multipartFile.getOriginalFilename())
                .key(fileKey)
                .size(BigInteger.valueOf(multipartFile.getSize()))
                .type(ResourceType.getResourceType(multipartFile.getContentType()))
                .status(ResourceStatus.ACTIVE)
                .createdBy(teamMember)
                .project(project)
                .build();
    }
}
