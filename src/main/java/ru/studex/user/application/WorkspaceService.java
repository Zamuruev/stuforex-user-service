package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.infrastructure.persistence.WorkspaceStateEntity;
import ru.studex.user.infrastructure.persistence.WorkspaceStateRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceStateRepository repo;

    public String getState(String userId) {
        return repo.findByUserId(UUID.fromString(userId))
                .map(WorkspaceStateEntity::getStateJson)
                .orElse("{}");
    }

    @Transactional
    public void saveState(String userId, String stateJson) {
        UUID uid = UUID.fromString(userId);
        WorkspaceStateEntity entity = repo.findByUserId(uid)
                .orElseGet(() -> WorkspaceStateEntity.builder().userId(uid).build());
        entity.setStateJson(stateJson);
        repo.save(entity);
    }
}
