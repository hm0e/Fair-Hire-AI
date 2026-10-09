package com.fairhire.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class MatchRequestDto {

    @JsonProperty("resume_ids")
    @JsonAlias({"resumeIds", "resume_ids"})
    private List<Long> resumeIds;

    @JsonProperty("department")
    private String department;

    @JsonProperty("resume_departments")
    @JsonAlias({"resumeDepartments", "resume_departments"})
    private Map<Long, String> resumeDepartments;

    @JsonProperty("screening_mode")
    @JsonAlias({"screeningMode", "screening_mode"})
    private String screeningMode;

    @JsonProperty("matching_method")
    @JsonAlias({"matchingMethod", "matching_method"})
    private String matchingMethod;

    public MatchRequestDto() {}

    public MatchRequestDto(List<Long> resumeIds) {
        this.resumeIds = resumeIds;
    }

    public List<Long> getResumeIds() {
        return resumeIds;
    }

    public void setResumeIds(List<Long> resumeIds) {
        this.resumeIds = resumeIds;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Map<Long, String> getResumeDepartments() {
        return resumeDepartments;
    }

    public void setResumeDepartments(Map<Long, String> resumeDepartments) {
        this.resumeDepartments = resumeDepartments;
    }

    public String getScreeningMode() {
        return screeningMode;
    }

    public void setScreeningMode(String screeningMode) {
        this.screeningMode = screeningMode;
    }

    public String getMatchingMethod() {
        return matchingMethod;
    }

    public void setMatchingMethod(String matchingMethod) {
        this.matchingMethod = matchingMethod;
    }
}
