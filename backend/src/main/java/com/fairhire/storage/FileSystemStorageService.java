package com.fairhire.storage;

import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Local filesystem implementation of {@link StorageService}.
 * Enforces path traversal protection and assigns cryptographic/UUID generated filenames.
 */
@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    public FileSystemStorageService(@Value("${fairhire.storage.local.base-dir:./uploads/resumes}") String baseDir) {
        this.rootLocation = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize storage directory at: " + rootLocation, e);
        }
    }

    @Override
    public String store(byte[] content, String originalFilename, String sha256) throws IOException {
        if (content == null || content.length == 0) {
            throw new StorageException("Cannot store empty content");
        }

        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            String rawExt = FilenameUtils.getExtension(originalFilename);
            if (rawExt != null && !rawExt.isBlank()) {
                // Sanitize extension to alphanumeric only
                String cleanExt = rawExt.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (!cleanExt.isBlank()) {
                    extension = "." + cleanExt;
                }
            }
        }

        // Generate safe, collision-resistant filename using hash prefix + UUID
        String hashPrefix = (sha256 != null && sha256.length() >= 16) ? sha256.substring(0, 16) : "doc";
        String generatedFilename = hashPrefix + "_" + UUID.randomUUID() + extension;

        Path destinationFile = rootLocation.resolve(generatedFilename).normalize();

        // Enforce path traversal protection
        if (!destinationFile.startsWith(rootLocation)) {
            throw new StorageException("Security violation: Attempted path traversal for file: " + originalFilename);
        }

        Files.write(destinationFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return generatedFilename;
    }

    @Override
    public byte[] load(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageException("Invalid storage key: key cannot be empty");
        }

        Path file = rootLocation.resolve(storageKey).normalize();

        // Enforce path traversal protection
        if (!file.startsWith(rootLocation)) {
            throw new StorageException("Security violation: Path traversal detected for key: " + storageKey);
        }

        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new StorageException("File not found for storage key: " + storageKey);
        }

        return Files.readAllBytes(file);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        Path file = rootLocation.resolve(storageKey).normalize();
        if (!file.startsWith(rootLocation)) {
            throw new StorageException("Security violation: Path traversal detected for key: " + storageKey);
        }

        Files.deleteIfExists(file);
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }

        Path file = rootLocation.resolve(storageKey).normalize();
        if (!file.startsWith(rootLocation)) {
            return false;
        }

        return Files.exists(file) && Files.isRegularFile(file);
    }

    public Path getRootLocation() {
        return rootLocation;
    }
}
