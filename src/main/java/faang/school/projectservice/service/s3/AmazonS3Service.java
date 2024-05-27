package faang.school.projectservice.service.s3;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface AmazonS3Service {

    String uploadFile(String path, MultipartFile file);

    InputStream downloadFile(String key);

    void deleteFile(String key);
}
