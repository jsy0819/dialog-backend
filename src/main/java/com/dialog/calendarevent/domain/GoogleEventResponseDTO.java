package com.dialog.calendarevent.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoogleEventResponseDTO {

	private String id;

	private String summary;

	private String status;

	private String created;

	private String htmlLink;

	private EventDateTimeDTO start;
	
	private EventDateTimeDTO end;

    @JsonProperty("items")
	private List<GoogleEventResponseDTO> items;

	public List<GoogleEventResponseDTO> getItems() {
        return items;
    }
}
