package ru.studex.user.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.studex.user.api.exception.ApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> DOC_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public String storeAvatar(String userId, MultipartFile file) {
        if (!IMAGE_TYPES.contains(file.getContentType()))
            throw new ApiException("Разрешены только изображения (JPEG, PNG, WEBP, GIF)", 400);

        String ext = getExtension(file.getOriginalFilename());
        String filename = "avatar_" + userId + "_" + UUID.randomUUID() + ext;
        return store(file, "avatars", filename);
    }

    public String storeCover(String userId, MultipartFile file) {
        if (!IMAGE_TYPES.contains(file.getContentType()))
            throw new ApiException("Разрешены только изображения (JPEG, PNG, WEBP, GIF)", 400);
        if (file.getSize() > 10 * 1024 * 1024)
            throw new ApiException("Размер обложки не должен превышать 10 МБ", 400);

        String ext = getExtension(file.getOriginalFilename());
        String filename = "cover_" + userId + "_" + UUID.randomUUID() + ext;
        return store(file, "covers", filename);
    }

    public String storePortfolioFile(String userId, MultipartFile file) {
        if (!DOC_TYPES.contains(file.getContentType()))
            throw new ApiException("Разрешены PDF, изображения и документы Word", 400);
        if (file.getSize() > 20 * 1024 * 1024)
            throw new ApiException("Размер файла не должен превышать 20 МБ", 400);

        String ext = getExtension(file.getOriginalFilename());
        String filename = "pf_" + userId + "_" + UUID.randomUUID() + ext;
        return store(file, "portfolio", filename);
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path path = Paths.get(uploadDir).resolve(relativePath.replaceFirst("^/uploads/", "")).normalize();
            Files.deleteIfExists(path);
        } catch (IOException ignored) {}
    }

    private String store(MultipartFile file, String subdir, String filename) {
        try {
            Path dir = Paths.get(uploadDir, subdir);
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            file.transferTo(dest);
            return "/uploads/" + subdir + "/" + filename;
        } catch (IOException e) {
            throw new ApiException("Не удалось сохранить файл: " + e.getMessage(), 500);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
