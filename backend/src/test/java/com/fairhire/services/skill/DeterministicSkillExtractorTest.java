package com.fairhire.services.skill;

import com.fairhire.models.Skill;
import com.fairhire.models.enums.SkillExtractionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 3B: Deterministic Skill Extractor Tests")
class DeterministicSkillExtractorTest {

    private DeterministicSkillExtractor extractor;
    private List<Skill> testTaxonomy;

    @BeforeEach
    void setUp() {
        extractor = new DeterministicSkillExtractor();

        Skill java = new Skill("Java", "PROGRAMMING_LANGUAGE", List.of("Java SE", "Core Java", "Java programming", "Java 17"));
        java.setId(1L);

        Skill python = new Skill("Python", "PROGRAMMING_LANGUAGE", List.of("Python 3", "Python programming", "Python3"));
        python.setId(2L);

        Skill springBoot = new Skill("Spring Boot", "FRAMEWORK", List.of("SpringBoot", "Spring Boot Framework"));
        springBoot.setId(3L);

        Skill react = new Skill("React", "FRAMEWORK", List.of("React.js", "ReactJS", "React Native"));
        react.setId(4L);

        Skill docker = new Skill("Docker", "CLOUD_DEVOPS", List.of("Docker containers", "Docker Compose"));
        docker.setId(5L);

        Skill aws = new Skill("AWS", "CLOUD_DEVOPS", List.of("Amazon Web Services", "Amazon AWS"));
        aws.setId(6L);

        Skill js = new Skill("JavaScript", "PROGRAMMING_LANGUAGE", List.of("JS", "ECMAScript", "ES6"));
        js.setId(7L);

        Skill cplusplus = new Skill("C++", "PROGRAMMING_LANGUAGE", List.of("C/C++", "C plus plus", "Cpp"));
        cplusplus.setId(8L);

        Skill csharp = new Skill("C#", "PROGRAMMING_LANGUAGE", List.of("C Sharp", "CSharp", "C-Sharp"));
        csharp.setId(9L);

        Skill c = new Skill("C", "PROGRAMMING_LANGUAGE", List.of("C programming", "ANSI C"));
        c.setId(10L);

        Skill dotnet = new Skill(".NET", "FRAMEWORK", List.of(".NET Core", "dotnet"));
        dotnet.setId(11L);

        Skill nodejs = new Skill("Node.js", "FRAMEWORK", List.of("NodeJS", "Node.JS"));
        nodejs.setId(12L);

        Skill go = new Skill("Go", "PROGRAMMING_LANGUAGE", List.of("Golang", "Go programming"));
        go.setId(13L);

        testTaxonomy = List.of(java, python, springBoot, react, docker, aws, js, cplusplus, csharp, c, dotnet, nodejs, go);
    }

    @Test
    @DisplayName("Should extract basic canonical skills exactly")
    void testBasicSkillExtraction() {
        String text = "Senior Developer skilled in Java, Python, Spring Boot, React, Docker, and AWS.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> canonicalNames = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(canonicalNames).containsExactlyInAnyOrder("Java", "Python", "Spring Boot", "React", "Docker", "AWS");

        ExtractedSkillCandidate javaSkill = skills.stream().filter(s -> s.canonicalName().equals("Java")).findFirst().orElseThrow();
        assertThat(javaSkill.confidence()).isEqualByComparingTo("1.000");
        assertThat(javaSkill.method()).isEqualTo(SkillExtractionMethod.EXACT_CANONICAL_MATCH);
        assertThat(javaSkill.matchedText()).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should resolve aliases to canonical skills")
    void testAliasNormalization() {
        String text = "Extensive experience with Java SE, SpringBoot, and React.js for web development.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> canonicalNames = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(canonicalNames).containsExactlyInAnyOrder("Java", "Spring Boot", "React");

        ExtractedSkillCandidate springBootCandidate = skills.stream()
                .filter(s -> s.canonicalName().equals("Spring Boot"))
                .findFirst()
                .orElseThrow();
        assertThat(springBootCandidate.matchedText()).isEqualTo("SpringBoot");
        assertThat(springBootCandidate.method()).isEqualTo(SkillExtractionMethod.EXACT_ALIAS_MATCH);
        assertThat(springBootCandidate.confidence()).isEqualByComparingTo("0.950");

        ExtractedSkillCandidate reactCandidate = skills.stream()
                .filter(s -> s.canonicalName().equals("React"))
                .findFirst()
                .orElseThrow();
        assertThat(reactCandidate.matchedText()).isEqualTo("React.js");
        assertThat(reactCandidate.method()).isEqualTo(SkillExtractionMethod.EXACT_ALIAS_MATCH);
    }

    @Test
    @DisplayName("Should prevent false positives: Java must NOT match JavaScript")
    void testFalsePositive_JavaVsJavaScript() {
        String text = "Expert in JavaScript and ECMAScript. No backend Java knowledge.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> canonicalNames = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        // Since text says "No backend Java knowledge", Java SHOULD match the second mention, but NOT inside JavaScript
        assertThat(canonicalNames).contains("JavaScript", "Java");

        // Verify with text containing ONLY JavaScript
        String jsOnly = "Built complex single page apps using JavaScript and JS libraries.";
        List<ExtractedSkillCandidate> jsOnlySkills = extractor.extractSkills(jsOnly, testTaxonomy);
        List<String> jsOnlyNames = jsOnlySkills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(jsOnlyNames).containsExactly("JavaScript");
        assertThat(jsOnlyNames).doesNotContain("Java");
    }

    @Test
    @DisplayName("Should prevent false positives: C must NOT match C++ or C#")
    void testFalsePositive_CVsCPlusPlusAndCSharp() {
        String text = "Strong knowledge of C++ and C# enterprise frameworks.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).contains("C++", "C#");
        assertThat(names).doesNotContain("C");
    }

    @Test
    @DisplayName("Should correctly match isolated C when present")
    void testIsolatedCProgrammingMatch() {
        String text = "Proficient in languages: C, Python, and Go.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).contains("C", "Python", "Go");
    }

    @Test
    @DisplayName("Should prevent false positives: React must NOT match Reaction or Reactor")
    void testFalsePositive_ReactVsUnrelatedWords() {
        String text = "Researched chemical reaction rates and nuclear reactor safety mechanisms.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).doesNotContain("React");
    }

    @Test
    @DisplayName("Should handle symbol terms like .NET and Node.js")
    void testSymbolTerms_DotNetAndNodeJs() {
        String text = "Architected backend microservices using .NET Core and Node.js runtime.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).contains(".NET", "Node.js");
    }

    @Test
    @DisplayName("Should deduplicate repeated mentions into single canonical skill with highest confidence")
    void testDuplicateHandling() {
        String text = "Skills include Java, Core Java, and java programming with Java 17 features.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        assertThat(skills).hasSize(1);
        ExtractedSkillCandidate singleJava = skills.get(0);
        assertThat(singleJava.canonicalName()).isEqualTo("Java");
        // Exact canonical match "Java" has highest confidence 1.000
        assertThat(singleJava.confidence()).isEqualByComparingTo("1.000");
        assertThat(singleJava.method()).isEqualTo(SkillExtractionMethod.EXACT_CANONICAL_MATCH);
    }

    @Test
    @DisplayName("Should extract meaningful context snippet containing the matched skill")
    void testContextSnippetExtraction() {
        String text = "Summary:\nOver 5 years of experience.\nDeveloped high-throughput REST APIs using SpringBoot for banking.\nEducation: B.S.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        ExtractedSkillCandidate springBoot = skills.stream()
                .filter(s -> s.canonicalName().equals("Spring Boot"))
                .findFirst()
                .orElseThrow();

        assertThat(springBoot.contextSnippet()).isNotEmpty();
        assertThat(springBoot.contextSnippet()).contains("SpringBoot");
        assertThat(springBoot.contextSnippet().length()).isLessThanOrEqualTo(280);
    }

    @Test
    @DisplayName("Should be 100% deterministic and reproducible across multiple runs")
    void testReproducibility() {
        String resume = """
                Senior Software Engineer
                Expertise in Python, Docker, Kubernetes, AWS, and PostgreSQL.
                Built machine learning pipelines using Python 3 and deployed containers to AWS via Docker Compose.
                """;

        List<ExtractedSkillCandidate> run1 = extractor.extractSkills(resume, testTaxonomy);
        List<ExtractedSkillCandidate> run2 = extractor.extractSkills(resume, testTaxonomy);
        List<ExtractedSkillCandidate> run3 = extractor.extractSkills(resume, testTaxonomy);

        assertThat(run1).hasSameSizeAs(run2);
        assertThat(run2).hasSameSizeAs(run3);

        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).canonicalName()).isEqualTo(run2.get(i).canonicalName());
            assertThat(run1.get(i).matchedText()).isEqualTo(run2.get(i).matchedText());
            assertThat(run1.get(i).confidence()).isEqualByComparingTo(run2.get(i).confidence());
            assertThat(run1.get(i).method()).isEqualTo(run2.get(i).method());
            assertThat(run1.get(i).contextSnippet()).isEqualTo(run2.get(i).contextSnippet());
        }
    }

    // ========================================================================
    // B-01 Regression Tests: Single-Letter "C" Boundaries & Sentence Endings
    // ========================================================================

    @Test
    @DisplayName("B-01: Should extract C when appearing at sentence ending with a period")
    void testCAtSentenceEnding() {
        String text = "Candidate has deep practical expertise in C.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).containsExactly("C");

        ExtractedSkillCandidate cSkill = skills.get(0);
        assertThat(cSkill.matchedText()).isEqualTo("C");
        assertThat(cSkill.confidence()).isEqualByComparingTo("1.000");
    }

    @Test
    @DisplayName("B-01: Should extract C followed by period and next sentence, while extracting subsequent skills")
    void testCInMultiSentenceText() {
        String text = "Candidate has deep practical expertise in C. Later transitioned to Python and Java.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).containsExactlyInAnyOrder("C", "Python", "Java");
    }

    @Test
    @DisplayName("B-01: Should match C in parentheses followed by sentence-ending punctuation")
    void testCParenthesesAndPunctuation() {
        String text = "Primary programming languages: (C).";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).containsExactly("C");
    }

    @Test
    @DisplayName("B-01: Should NOT extract C from acronyms or dot-separated titles like C.E.O. or C.Developer")
    void testCDoesNotMatchAcronymsOrDotSeparatedIdentifiers() {
        String text = "Served as C.E.O. of the startup. Worked closely with C.Developer teams.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).doesNotContain("C");
    }

    @Test
    @DisplayName("B-01: Should NOT extract single C when part of C++ or C#")
    void testCDoesNotMatchCPlusPlusOrCSharp() {
        String text = "Senior developer in C++ and C# enterprise stacks.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).containsExactlyInAnyOrder("C++", "C#");
        assertThat(names).doesNotContain("C");
    }

    // ========================================================================
    // B-02 Regression Tests: Programming Language "Go" vs English Verb "go"
    // ========================================================================

    @Test
    @DisplayName("B-02: Should NOT extract Go from common English prose containing lowercase 'go'")
    void testGoFalsePositivesPreventedInProse() {
        List<String> proseSamples = List.of(
                "Always ready to go the extra mile.",
                "Decided to let it go.",
                "We must go ahead with the roadmap.",
                "Ongoing initiatives are going to succeed."
        );

        for (String sample : proseSamples) {
            List<ExtractedSkillCandidate> skills = extractor.extractSkills(sample, testTaxonomy);
            List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
            assertThat(names)
                    .as("Failed on sample: '%s'", sample)
                    .doesNotContain("Go");
        }
    }

    @Test
    @DisplayName("B-02: Should extract Go when exactly cased or using canonical alias Golang")
    void testGoTruePositivesMatched() {
        String text1 = "Proficient in Go, Python, and Java.";
        List<ExtractedSkillCandidate> skills1 = extractor.extractSkills(text1, testTaxonomy);
        assertThat(skills1.stream().map(ExtractedSkillCandidate::canonicalName).toList())
                .contains("Go");

        String text2 = "Built scalable backend services using Golang and Docker.";
        List<ExtractedSkillCandidate> skills2 = extractor.extractSkills(text2, testTaxonomy);
        assertThat(skills2.stream().map(ExtractedSkillCandidate::canonicalName).toList())
                .contains("Go", "Docker");

        String text3 = "Strong expertise in Go programming and microservice architectures.";
        List<ExtractedSkillCandidate> skills3 = extractor.extractSkills(text3, testTaxonomy);
        assertThat(skills3.stream().map(ExtractedSkillCandidate::canonicalName).toList())
                .contains("Go");
    }

    // ========================================================================
    // B-03 Regression Tests: "Node" Alias False Positive Control
    // ========================================================================

    @Test
    @DisplayName("B-03: Should NOT extract Node.js from infrastructure or DOM node mentions")
    void testNodeFalsePositivesPrevented() {
        List<String> nodeProse = List.of(
                "Architected high-throughput worker node in Kubernetes cluster.",
                "Inspected cluster node and DOM node elements.",
                "Managed cluster worker nodes across multiple regions."
        );

        for (String sample : nodeProse) {
            List<ExtractedSkillCandidate> skills = extractor.extractSkills(sample, testTaxonomy);
            List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
            assertThat(names)
                    .as("Failed on sample: '%s'", sample)
                    .doesNotContain("Node.js");
        }
    }

    @Test
    @DisplayName("B-03: Should extract Node.js for canonical and valid technical aliases")
    void testNodeTruePositivesMatched() {
        String text = "Full-stack developer experienced with Node.js, NodeJS, and Node.JS frameworks.";
        List<ExtractedSkillCandidate> skills = extractor.extractSkills(text, testTaxonomy);

        List<String> names = skills.stream().map(ExtractedSkillCandidate::canonicalName).toList();
        assertThat(names).containsExactly("Node.js");
        assertThat(skills.get(0).canonicalName()).isEqualTo("Node.js");
    }
}

