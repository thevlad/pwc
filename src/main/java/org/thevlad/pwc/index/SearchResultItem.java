package org.thevlad.pwc.index;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {

	private String title;
	private String link;
	private String context;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMM dd,yyyy")
	private ZonedDateTime date;
	

}
