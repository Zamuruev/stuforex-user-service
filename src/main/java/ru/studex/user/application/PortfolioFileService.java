package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.PortfolioFileEntity;
import ru.studex.user.infrastructure.persistence.PortfolioFileRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioFileService {

    private final PortfolioFileRepository fileRepository;
    private final FileStorageService fileStorageService;

    public List<PortfolioFileEntity> getFiles(String userId) {
        return fileRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId));
    }

    @Transactional
    public PortfolioFileEntity upload(String userId, MultipartFile file, String category, String description) {
        String url = fileStorageService.storePortfolioFile(userId, file);
        return fileRepository.save(PortfolioFileEntity.builder()
                .userId(UUID.fromString(userId))
                .fileUrl(url)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
                .fileType(file.getContentType())
                .category(category != null ? category : "OTHER")
                .description(description)
                .build());
    }

    @Transactional
    public void delete(String userId, String fileId) {
        PortfolioFileEntity entity = fileRepository.findById(UUID.fromString(fileId))
                .orElseThrow(() -> new ApiException("Файл не найден", 404));
        if (!entity.getUserId().toString().equals(userId))
            throw new ApiException("Нет прав на удаление", 403);
        fileStorageService.delete(entity.getFileUrl());
        fileRepository.delete(entity);
    }

    @Transactional
    public PortfolioFileEntity updateDescription(String userId, String fileId, String description) {
        PortfolioFileEntity entity = fileRepository.findById(UUID.fromString(fileId))
                .orElseThrow(() -> new ApiException("Файл не найден", 404));
        if (!entity.getUserId().toString().equals(userId))
            throw new ApiException("Нет прав на редактирование", 403);
        entity.setDescription(description);
        return fileRepository.save(entity);
    }
}
