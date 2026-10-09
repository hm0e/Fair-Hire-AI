package com.fairhire.services.skill;

import com.fairhire.models.Skill;
import com.fairhire.models.enums.SkillExtractionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, rule-based skill extraction engine with false-positive controls,
 * symbol-aware token boundary handling, greedy span reservation, and explainable
 * heuristic confidence scoring.
 *
 * Fully reproducible: identical resume text and skill taxonomy produce identical results.
 */
@Component
public class DeterministicSkillExtractor {

    private static final Logger log = LoggerFactory.getLogger(DeterministicSkillExtractor.class);

    public static final String SKILL_TAXONOMY_VERSION = "v1.0";
    public static final String SKILL_EXTRACTOR_VERSION = "v1.0-rule-based";
    public static final String NORMALIZATION_VERSION = "v1.0";

    /**
     * Internal representation of a compiled search pattern for a canonical skill or alias.
     */
    private record CompiledSkillRule(
            Skill skill,
            String term,
            boolean isCanonical,
            Pattern exactPattern,
            Pattern caseInsensitivePattern,
            int length,
            boolean caseSensitiveOnly
    ) implements Comparable<CompiledSkillRule> {
        @Override
        public int compareTo(CompiledSkillRule other) {
            // Priority 1: Longer terms first (greedy matching, e.g. "JavaScript" before "Java", "C++" before "C")
            int lenCmp = Integer.compare(other.length, this.length);
            if (lenCmp != 0) return lenCmp;
            // Priority 2: Canonical names before aliases
            if (this.isCanonical != other.isCanonical) {
                return this.isCanonical ? -1 : 1;
            }
            return this.term.compareTo(other.term);
        }
    }

    /**
     * Extract canonical skills from parsed resume text using active taxonomy skills.
     *
     * @param rawText      Parsed and normalized resume text
     * @param activeSkills Active taxonomy skills from database
     * @return List of deduplicated canonical skill extraction candidates
     */
    public List<ExtractedSkillCandidate> extractSkills(String rawText, List<Skill> activeSkills) {
        if (rawText == null || rawText.isBlank() || activeSkills == null || activeSkills.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Build and compile extraction rules sorted by length descending (longest first)
        List<CompiledSkillRule> rules = buildCompiledRules(activeSkills);

        BitSet occupiedBits = new BitSet(rawText.length());
        List<ExtractedSkillCandidate> rawCandidates = new ArrayList<>();

        // 2. Scan text using rules in priority order
        for (CompiledSkillRule rule : rules) {
            // First attempt exact case match
            Matcher exactMatcher = rule.exactPattern.matcher(rawText);
            while (exactMatcher.find()) {
                int start = exactMatcher.start();
                int end = exactMatcher.end();
                String matchedStr = rawText.substring(start, end);

                if (isSpanAvailable(occupiedBits, start, end)) {
                    occupiedBits.set(start, end);
                    SkillExtractionMethod method = rule.isCanonical
                            ? SkillExtractionMethod.EXACT_CANONICAL_MATCH
                            : SkillExtractionMethod.EXACT_ALIAS_MATCH;
                    String snippet = extractContextSnippet(rawText, start, end);

                    rawCandidates.add(new ExtractedSkillCandidate(
                            rule.skill,
                            rule.skill.getName(),
                            matchedStr,
                            snippet,
                            method.getDefaultConfidence(),
                            method,
                            start,
                            end
                    ));
                }
            }

            // If not case-sensitive only, attempt case-insensitive match for remaining spans
            if (!rule.caseSensitiveOnly) {
                Matcher ciMatcher = rule.caseInsensitivePattern.matcher(rawText);
                while (ciMatcher.find()) {
                    int start = ciMatcher.start();
                    int end = ciMatcher.end();
                    String matchedStr = rawText.substring(start, end);

                    if (isSpanAvailable(occupiedBits, start, end)) {
                        occupiedBits.set(start, end);
                        SkillExtractionMethod method = rule.isCanonical
                                ? SkillExtractionMethod.CASE_INSENSITIVE_CANONICAL_MATCH
                                : SkillExtractionMethod.CASE_INSENSITIVE_ALIAS_MATCH;
                        String snippet = extractContextSnippet(rawText, start, end);

                        rawCandidates.add(new ExtractedSkillCandidate(
                                rule.skill,
                                rule.skill.getName(),
                                matchedStr,
                                snippet,
                                method.getDefaultConfidence(),
                                method,
                                start,
                                end
                        ));
                    }
                }
            }
        }

        // 3. Deduplicate by canonical skill ID: retain candidate with highest confidence & richest context
        Map<Long, ExtractedSkillCandidate> deduplicated = new LinkedHashMap<>();
        for (ExtractedSkillCandidate candidate : rawCandidates) {
            Long skillId = candidate.skill().getId();
            if (!deduplicated.containsKey(skillId)) {
                deduplicated.put(skillId, candidate);
            } else {
                ExtractedSkillCandidate existing = deduplicated.get(skillId);
                if (candidate.compareTo(existing) < 0) { // higher confidence or earlier in document
                    deduplicated.put(skillId, candidate);
                }
            }
        }

        List<ExtractedSkillCandidate> result = new ArrayList<>(deduplicated.values());
        // Sort final output deterministically by canonical skill name
        result.sort(Comparator.comparing(ExtractedSkillCandidate::canonicalName));
        return result;
    }

    private boolean isSpanAvailable(BitSet occupiedBits, int start, int end) {
        if (start < 0 || end <= start) {
            return false;
        }
        int next = occupiedBits.nextSetBit(start);
        return next == -1 || next >= end;
    }

    /**
     * Builds regex patterns for all skills and their aliases, with boundary rules for symbol-heavy terms.
     */
    private List<CompiledSkillRule> buildCompiledRules(List<Skill> activeSkills) {
        List<CompiledSkillRule> rules = new ArrayList<>();

        for (Skill skill : activeSkills) {
            // Canonical term
            addRuleIfValid(rules, skill, skill.getName(), true);

            // Synonyms / Aliases
            if (skill.getSynonyms() != null) {
                for (String alias : skill.getSynonyms()) {
                    if (alias != null && !alias.isBlank()) {
                        addRuleIfValid(rules, skill, alias.trim(), false);
                    }
                }
            }
        }

        Collections.sort(rules);
        return rules;
    }

    /**
     * Technical skills whose terms are common natural-language English words or single letters
     * (e.g., "C", "Go") that must only match exact casing to prevent false positives
     * on standard prose (e.g., "go ahead", "let it go").
     */
    private static final Set<String> CASE_SENSITIVE_ONLY_TERMS = Set.of(
            "C",
            "GO"
    );

    private boolean isCaseSensitiveOnly(String term) {
        if (term.length() == 1) {
            return true;
        }
        return CASE_SENSITIVE_ONLY_TERMS.contains(term.toUpperCase(Locale.ROOT));
    }

    private void addRuleIfValid(List<CompiledSkillRule> rules, Skill skill, String term, boolean isCanonical) {
        String trimmed = term.trim();
        if (trimmed.isEmpty()) return;

        // Determine if term is short or ambiguous (e.g. "C", "Go") which must only match exact casing
        boolean caseSensitiveOnly = isCaseSensitiveOnly(trimmed);

        String regex = buildBoundaryRegex(trimmed);
        Pattern exactPattern = Pattern.compile(regex);
        Pattern ciPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        rules.add(new CompiledSkillRule(
                skill,
                trimmed,
                isCanonical,
                exactPattern,
                ciPattern,
                trimmed.length(),
                caseSensitiveOnly
        ));
    }

    /**
     * Constructs a regex pattern with token boundaries tailored to programming language symbols.
     * Prevents "Java" from matching "JavaScript", "C" from matching "C++", and "React" from matching "Reaction".
     */
    public static String buildBoundaryRegex(String term) {
        String lower = term.toLowerCase(Locale.ROOT);

        // Special handling for symbol-rich terms
        if (term.equals("C++")) {
            return "(?<![a-zA-Z0-9+#])C\\+\\+(?![a-zA-Z0-9+#])";
        }
        if (term.equals("C#") || lower.equals("c#")) {
            return "(?<![a-zA-Z0-9+#])C#(?![a-zA-Z0-9+#])";
        }
        if (term.equals("C")) {
            // Single character C: Must not be preceded by alphanumeric, +, #, /, dot, hyphen, underscore
            // and must not be followed by alphanumeric, +, #, /, hyphen, underscore OR a dot followed by alphanumeric
            return "(?<![a-zA-Z0-9+#/._-])C(?![a-zA-Z0-9+#/_\\-]|\\.[a-zA-Z0-9])";
        }
        if (term.equalsIgnoreCase(".NET")) {
            return "(?<![a-zA-Z0-9.])" + Pattern.quote(term) + "(?![a-zA-Z0-9])";
        }
        if (term.equalsIgnoreCase("Node.js")) {
            return "(?<![a-zA-Z0-9])" + Pattern.quote(term) + "(?![a-zA-Z0-9])";
        }
        if (term.equalsIgnoreCase("Vue.js")) {
            return "(?<![a-zA-Z0-9])" + Pattern.quote(term) + "(?![a-zA-Z0-9])";
        }
        if (term.equalsIgnoreCase("React.js")) {
            return "(?<![a-zA-Z0-9])" + Pattern.quote(term) + "(?![a-zA-Z0-9])";
        }

        // General term handling:
        // Handle whitespace inside term (e.g. "Spring Boot" can match "Spring   Boot" or "Spring\nBoot")
        String escaped = Pattern.quote(term);
        if (term.contains(" ")) {
            String[] parts = term.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append("\\s+");
                sb.append(Pattern.quote(parts[i]));
            }
            escaped = sb.toString();
        }

        // Left boundary: non-alphanumeric preceding
        // Right boundary: non-alphanumeric following (also ensure not followed by + or # to protect C++ / C#)
        return "(?<![a-zA-Z0-9])" + escaped + "(?![a-zA-Z0-9+#])";
    }

    /**
     * Extracts a clean contextual sentence or clause snippet surrounding the match location.
     * Bounded to a maximum of 280 characters to fit comfortably in the 300-char column.
     */
    public static String extractContextSnippet(String fullText, int matchStart, int matchEnd) {
        if (fullText == null || fullText.isEmpty()) return "";

        int totalLen = fullText.length();

        // 1. Scan backward for sentence or line beginning
        int snippetStart = Math.max(0, matchStart - 70);
        for (int i = matchStart - 1; i >= snippetStart; i--) {
            char c = fullText.charAt(i);
            if (c == '\n' || c == '\r' || (c == '.' && i + 1 < matchStart && Character.isWhitespace(fullText.charAt(i + 1)))) {
                snippetStart = i + 1;
                break;
            }
        }

        // 2. Scan forward for sentence or line ending
        int snippetEnd = Math.min(totalLen, matchEnd + 70);
        for (int i = matchEnd; i < snippetEnd; i++) {
            char c = fullText.charAt(i);
            if (c == '\n' || c == '\r' || (c == '.' && (i + 1 == totalLen || Character.isWhitespace(fullText.charAt(i + 1))))) {
                snippetEnd = (c == '.') ? i + 1 : i;
                break;
            }
        }

        String rawSnippet = fullText.substring(snippetStart, snippetEnd).trim();
        // Collapse multiple whitespace/newlines into single spaces
        String cleanSnippet = rawSnippet.replaceAll("\\s+", " ").trim();

        // If snippet is still too long, truncate with ellipsis
        if (cleanSnippet.length() > 280) {
            cleanSnippet = cleanSnippet.substring(0, 277) + "...";
        }

        return cleanSnippet;
    }
}
