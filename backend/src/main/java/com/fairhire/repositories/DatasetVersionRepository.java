package com.fairhire.repositories;

import com.fairhire.models.DatasetVersion;
import com.fairhire.models.enums.DatasetLifecycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {
    Optional<DatasetVersion> findByVersionTag(String versionTag);
    List<DatasetVersion> findByLifecycleStatus(DatasetLifecycleStatus status);
}
