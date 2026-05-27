package com.aiprreview.repository;

import com.aiprreview.entity.RepositoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends MongoRepository<RepositoryEntity, String> {

    List<RepositoryEntity> findByUserId(String userId);

    List<RepositoryEntity> findByUserIdAndIsActive(String userId, Boolean isActive);

    Optional<RepositoryEntity> findByUserIdAndFullName(String userId, String fullName);

    List<RepositoryEntity> findByFullName(String fullName);

    Optional<RepositoryEntity> findByIdAndUserId(String id, String userId);

    Boolean existsByUserIdAndFullName(String userId, String fullName);

    Long countByUserId(String userId);

    List<RepositoryEntity> findByUserIdAndNameContainingIgnoreCase(String userId, String name);

    void deleteByIdAndUserId(String id, String userId);
}
