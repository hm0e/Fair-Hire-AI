package com.fairhire.controllers;

import com.fairhire.models.Skill;
import com.fairhire.repositories.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for inspecting the controlled skill taxonomy catalog.
 */
@RestController
@RequestMapping({"/api/v1/skills", "/api/skills"})
@CrossOrigin(origins = "*")
public class SkillController {

    private final SkillRepository skillRepository;

    public SkillController(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /**
     * Retrieve all active canonical skills in the taxonomy catalog.
     */
    @GetMapping
    public ResponseEntity<List<Skill>> getAllActiveSkills() {
        return ResponseEntity.ok(skillRepository.findByIsActiveTrue());
    }

    /**
     * Retrieve a canonical skill by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSkillById(@PathVariable Long id) {
        return skillRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Skill with ID " + id + " not found."
                )));
    }
}
