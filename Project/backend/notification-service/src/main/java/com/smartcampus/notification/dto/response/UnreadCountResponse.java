package com.smartcampus.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing unread notification count")
public class UnreadCountResponse {

    @Schema(description = "Total number of unread notifications for the user", example = "5")
    private long count;

    public UnreadCountResponse() {
    }

    public UnreadCountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
