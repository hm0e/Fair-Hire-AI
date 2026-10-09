package com.fairhire.services.skill;

import com.fairhire.models.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("benchmark")
@DisplayName("Phase 3B: Deterministic Skill Extractor Scaling & Profiling Benchmark")
public class SkillExtractionPerformanceBenchmarkTest {

    private DeterministicSkillExtractor extractor;
    private List<Skill> fullTaxonomy;

    private static final String BASE_PARAGRAPH =
            "Experienced senior developer proficient in Python, Java, Docker, Kubernetes, AWS, PostgreSQL, and Linux. Developed high-throughput microservices.\n";

    @BeforeEach
    void setUp() {
        extractor = new DeterministicSkillExtractor();
        fullTaxonomy = buildFullTaxonomy();
    }

    public static List<Skill> buildFullTaxonomy() {
        List<Skill> skills = new ArrayList<>();
        long id = 1;
        skills.add(createSkill(id++, "Java", "PROGRAMMING_LANGUAGE", List.of("Java SE", "Java EE", "Core Java", "Java programming", "Java 8", "Java 11", "Java 17", "Java 21")));
        skills.add(createSkill(id++, "Python", "PROGRAMMING_LANGUAGE", List.of("Python 3", "Python programming", "Python3", "CPython")));
        skills.add(createSkill(id++, "JavaScript", "PROGRAMMING_LANGUAGE", List.of("JS", "ECMAScript", "ES6", "ES2015", "Vanilla JS")));
        skills.add(createSkill(id++, "TypeScript", "PROGRAMMING_LANGUAGE", List.of("TS")));
        skills.add(createSkill(id++, "C++", "PROGRAMMING_LANGUAGE", List.of("C/C++", "C plus plus", "Cpp")));
        skills.add(createSkill(id++, "C#", "PROGRAMMING_LANGUAGE", List.of("C Sharp", "CSharp", "C-Sharp")));
        skills.add(createSkill(id++, "C", "PROGRAMMING_LANGUAGE", List.of("C programming", "ANSI C")));
        skills.add(createSkill(id++, "Go", "PROGRAMMING_LANGUAGE", List.of("Golang")));
        skills.add(createSkill(id++, "Rust", "PROGRAMMING_LANGUAGE", List.of("Rust lang")));
        skills.add(createSkill(id++, "SQL", "PROGRAMMING_LANGUAGE", List.of("Structured Query Language", "ANSI SQL")));
        skills.add(createSkill(id++, ".NET", "FRAMEWORK", List.of(".NET Core", ".NET Framework", "dotnet", "dot net")));
        skills.add(createSkill(id++, "Spring Boot", "FRAMEWORK", List.of("SpringBoot", "Spring Boot Framework", "Spring Framework")));
        skills.add(createSkill(id++, "React", "FRAMEWORK", List.of("React.js", "ReactJS", "React Native")));
        skills.add(createSkill(id++, "Node.js", "FRAMEWORK", List.of("NodeJS", "Node.JS")));
        skills.add(createSkill(id++, "Angular", "FRAMEWORK", List.of("AngularJS", "Angular 2+", "Angular 14")));
        skills.add(createSkill(id++, "Vue.js", "FRAMEWORK", List.of("Vue", "VueJS", "Vue 3")));
        skills.add(createSkill(id++, "Django", "FRAMEWORK", List.of("Django Framework", "Django REST Framework")));
        skills.add(createSkill(id++, "FastAPI", "FRAMEWORK", List.of("Fast API")));
        skills.add(createSkill(id++, "Docker", "CLOUD_DEVOPS", List.of("Docker containers", "Docker Compose", "Containerization")));
        skills.add(createSkill(id++, "Kubernetes", "CLOUD_DEVOPS", List.of("K8s", "Kube")));
        skills.add(createSkill(id++, "AWS", "CLOUD_DEVOPS", List.of("Amazon Web Services", "Amazon AWS")));
        skills.add(createSkill(id++, "Azure", "CLOUD_DEVOPS", List.of("Microsoft Azure")));
        skills.add(createSkill(id++, "GCP", "CLOUD_DEVOPS", List.of("Google Cloud Platform", "Google Cloud")));
        skills.add(createSkill(id++, "PostgreSQL", "DATABASE", List.of("Postgres", "PostgreSQL DB")));
        skills.add(createSkill(id++, "MySQL", "DATABASE", List.of("MySQL Server")));
        skills.add(createSkill(id++, "MongoDB", "DATABASE", List.of("Mongo", "MongoDB NoSQL")));
        skills.add(createSkill(id++, "Redis", "DATABASE", List.of("Redis cache")));
        skills.add(createSkill(id++, "Git", "TOOL", List.of("GitHub", "GitLab", "Git version control")));
        skills.add(createSkill(id++, "Linux", "TOOL", List.of("Unix", "Ubuntu", "Linux OS")));
        skills.add(createSkill(id++, "REST API", "ARCHITECTURE", List.of("REST", "RESTful", "RESTful APIs", "REST APIs", "RESTful Web Services")));
        skills.add(createSkill(id++, "GraphQL", "ARCHITECTURE", List.of("GraphQL API")));
        return skills;
    }

    private static Skill createSkill(Long id, String name, String category, List<String> synonyms) {
        Skill s = new Skill(name, category, synonyms);
        s.setId(id);
        return s;
    }

    private String generateText(int targetKb) {
        int targetBytes = targetKb * 1024;
        int repeats = Math.max(1, targetBytes / BASE_PARAGRAPH.length());
        StringBuilder sb = new StringBuilder(repeats * BASE_PARAGRAPH.length());
        for (int i = 0; i < repeats; i++) {
            sb.append(BASE_PARAGRAPH);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("Run 8-Tier Scaling Benchmark with 5 Iterations (10KB to 5MB)")
    void runBenchmark() {
        int[] tiersKb = {10, 50, 100, 250, 500, 1000, 2000, 5000};
        int iterations = 5;

        System.out.println("=========================================================================================");
        System.out.println("  FAIRHIRE AI — DETERMINISTIC SKILL EXTRACTOR 8-TIER BENCHMARK (5 ITERATIONS EACH)       ");
        System.out.println("=========================================================================================");
        System.out.printf("%-10s %-12s %-10s %-10s %-10s %-10s %-10s %-10s%n",
                "Size (KB)", "Bytes", "Min (ms)", "Median(ms)", "Mean (ms)", "P95 (ms)", "Max (ms)", "Extracted");
        System.out.println("-----------------------------------------------------------------------------------------");

        // Warm up JIT
        String warmupText = generateText(50);
        for (int w = 0; w < 3; w++) {
            extractor.extractSkills(warmupText, fullTaxonomy);
        }

        for (int kb : tiersKb) {
            String text = generateText(kb);
            int byteLen = text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            double[] times = new double[iterations];
            int skillCount = 0;

            for (int i = 0; i < iterations; i++) {
                long t0 = System.nanoTime();
                List<ExtractedSkillCandidate> result = extractor.extractSkills(text, fullTaxonomy);
                long t1 = System.nanoTime();
                times[i] = (t1 - t0) / 1_000_000.0;
                skillCount = result.size();
            }

            Arrays.sort(times);
            double min = times[0];
            double max = times[iterations - 1];
            double median = times[iterations / 2];
            double mean = Arrays.stream(times).average().orElse(0.0);
            // p95 for 5 samples (index 4)
            double p95 = times[(int) Math.ceil(0.95 * iterations) - 1];

            System.out.printf("%-10d %-12d %-10.2f %-10.2f %-10.2f %-10.2f %-10.2f %-10d%n",
                    kb, byteLen, min, median, mean, p95, max, skillCount);

            assertThat(skillCount).isEqualTo(7);
        }
        System.out.println("=========================================================================================\n");
    }
}
