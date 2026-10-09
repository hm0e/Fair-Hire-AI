package com.fairhire.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MatchBatchResponseDto {

    @JsonProperty("job_id")
    private Long jobId;

    @JsonProperty("algorithm_version")
    private String algorithmVersion;

    @JsonProperty("matching_method")
    private String matchingMethod;

    @JsonProperty("total_evaluated")
    private Integer totalEvaluated;

    @JsonProperty("results")
    private List<MatchResponseDto> results;

    public MatchBatchResponseDto() {}

    public MatchBatchResponseDto(Long jobId, String algorithmVersion, String matchingMethod,
                                 Integer totalEvaluated, List<MatchResponseDto> results) {
        this.jobId = jobId;
        this.algorithmVersion = algorithmVersion;
        this.matchingMethod = matchingMethod;
        this.totalEvaluated = totalEvaluated;
        this.results = results;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }

    public String getMatchingMethod() { return matchingMethod; }
    public void setMatchingMethod(String matchingMethod) { this.matchingMethod = matchingMethod; }

    public Integer getTotalEvaluated() { return totalEvaluated; }
    public void setTotalEvaluated(Integer totalEvaluated) { this.totalEvaluated = totalEvaluated; }

    public List<MatchResponseDto> getResults() { return results; }
    public void setResults(List<MatchResponseDto> results) { this.results = results; }
}
