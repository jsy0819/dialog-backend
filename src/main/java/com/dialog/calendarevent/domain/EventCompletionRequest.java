package com.dialog.calendarevent.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventCompletionRequest {
	@JsonProperty("isCompleted")
	private boolean isCompleted;
}
