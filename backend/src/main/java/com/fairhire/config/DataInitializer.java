package com.fairhire.config;

import com.fairhire.models.*;
import com.fairhire.repositories.*;
import com.fairhire.services.JobService;
import com.fairhire.services.ResumeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final JobService jobService;
    private final ResumeService resumeService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, SkillRepository skillRepository,
                           JobRepository jobRepository, JobService jobService,
                           ResumeService resumeService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.resumeService = resumeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Users
        if (userRepository.count() == 0) {
            userRepository.save(new User("Lead Recruiter", "recruiter@fairhire.ai", passwordEncoder.encode("password123"), "recruiter"));
            userRepository.save(new User("Job Seeker", "candidate@fairhire.ai", passwordEncoder.encode("password123"), "candidate"));
            userRepository.save(new User("Research Admin", "admin@fairhire.ai", passwordEncoder.encode("password123"), "admin"));
        }

        // 2. Seed Baseline Skills
        if (skillRepository.count() == 0) {
            List<Skill> defaultSkills = List.of(
                    new Skill("Java", "PROGRAMMING_LANGUAGE", List.of("Java SE", "Java EE", "Core Java", "Java programming", "Java 8", "Java 11", "Java 17", "Java 21")),
                    new Skill("Python", "PROGRAMMING_LANGUAGE", List.of("Python 3", "Python programming", "Python3", "CPython")),
                    new Skill("JavaScript", "PROGRAMMING_LANGUAGE", List.of("JS", "ECMAScript", "ES6", "ES2015", "Vanilla JS")),
                    new Skill("TypeScript", "PROGRAMMING_LANGUAGE", List.of("TS")),
                    new Skill("C++", "PROGRAMMING_LANGUAGE", List.of("C/C++", "C plus plus", "Cpp")),
                    new Skill("C#", "PROGRAMMING_LANGUAGE", List.of("C Sharp", "CSharp", "C-Sharp")),
                    new Skill("C", "PROGRAMMING_LANGUAGE", List.of("C programming", "ANSI C")),
                    new Skill("Go", "PROGRAMMING_LANGUAGE", List.of("Golang")),
                    new Skill("Rust", "PROGRAMMING_LANGUAGE", List.of("Rust lang")),
                    new Skill("SQL", "PROGRAMMING_LANGUAGE", List.of("Structured Query Language", "ANSI SQL")),
                    new Skill(".NET", "FRAMEWORK", List.of(".NET Core", ".NET Framework", "dotnet", "dot net")),
                    new Skill("Spring Boot", "FRAMEWORK", List.of("SpringBoot", "Spring Boot Framework", "Spring Framework")),
                    new Skill("React", "FRAMEWORK", List.of("React.js", "ReactJS", "React Native")),
                    new Skill("Node.js", "FRAMEWORK", List.of("NodeJS", "Node.JS")),
                    new Skill("Angular", "FRAMEWORK", List.of("AngularJS", "Angular 2+", "Angular 14")),
                    new Skill("Vue.js", "FRAMEWORK", List.of("Vue", "VueJS", "Vue 3")),
                    new Skill("Django", "FRAMEWORK", List.of("Django Framework", "Django REST Framework")),
                    new Skill("FastAPI", "FRAMEWORK", List.of("Fast API")),
                    new Skill("Docker", "CLOUD_DEVOPS", List.of("Docker containers", "Docker Compose", "Containerization")),
                    new Skill("Kubernetes", "CLOUD_DEVOPS", List.of("K8s", "Kube")),
                    new Skill("AWS", "CLOUD_DEVOPS", List.of("Amazon Web Services", "Amazon AWS")),
                    new Skill("Azure", "CLOUD_DEVOPS", List.of("Microsoft Azure")),
                    new Skill("GCP", "CLOUD_DEVOPS", List.of("Google Cloud Platform", "Google Cloud")),
                    new Skill("PostgreSQL", "DATABASE", List.of("Postgres", "PostgreSQL DB")),
                    new Skill("MySQL", "DATABASE", List.of("MySQL Server")),
                    new Skill("MongoDB", "DATABASE", List.of("Mongo", "MongoDB NoSQL")),
                    new Skill("Redis", "DATABASE", List.of("Redis cache")),
                    new Skill("Git", "TOOL", List.of("GitHub", "GitLab", "Git version control")),
                    new Skill("Linux", "TOOL", List.of("Unix", "Ubuntu", "Linux OS")),
                    new Skill("REST API", "ARCHITECTURE", List.of("REST", "RESTful", "RESTful APIs", "REST APIs", "RESTful Web Services")),
                    new Skill("GraphQL", "ARCHITECTURE", List.of("GraphQL API"))
            );
            skillRepository.saveAll(defaultSkills);
        }

        // 3. Seed Sample Job
        if (jobRepository.count() == 0) {
            jobService.createJob(
                    "Senior Backend Python Developer",
                    "We are seeking an aggressive and competitive backend developer to lead our Python, Django, REST API, and SQL services.",
                    "Python, SQL, REST APIs, Docker, Git",
                    1L
            );
            jobService.createJob(
                    "Full Stack Engineer (Neutral Draft)",
                    "Looking for a collaborative software engineer with experience in React, Python, REST APIs, and database design.",
                    "React, Python, REST APIs, SQL",
                    1L
            );
        }

        // 4. Seed Baseline Resumes
        if (resumeService.getAllResumes().isEmpty()) {
            resumeService.parseAndSaveResume(
                    "Priya Sharma\nSoftware Engineer with 3 years of experience in Python, Django, REST APIs, SQL, and Docker. Led backend microservices.",
                    null,
                    "Priya Sharma",
                    "priya.sharma@example.com"
            );
            resumeService.parseAndSaveResume(
                    "Rahul Verma\nBackend Developer with 4 years of experience in Python, Flask, PostgreSQL, Docker, AWS, and Git version control.",
                    null,
                    "Rahul Verma",
                    "rahul.verma@example.com"
            );
            resumeService.parseAndSaveResume(
                    "Ananya Iyer\nFull-Stack Developer skilled in React, JavaScript, Python, REST API development, and SQL databases.",
                    null,
                    "Ananya Iyer",
                    "ananya.iyer@example.com"
            );
        }
    }
}
