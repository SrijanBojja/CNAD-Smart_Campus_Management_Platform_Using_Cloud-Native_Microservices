package com.smartcampus.student.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregate student count response")
public class StudentCountResponse {

    @Schema(description = "Total number of registered students", example = "42")
    private long count;

    @Schema(description = "Total elements alias for compatibility with paginated responses", example = "42")
    private long totalElements;

    public StudentCountResponse() {
    }

    public StudentCountResponse(long count) {
        this.count = count;
        this.totalElements = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
        this.totalElements = count;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}

