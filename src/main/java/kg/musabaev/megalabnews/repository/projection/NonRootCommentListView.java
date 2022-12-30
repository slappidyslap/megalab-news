package kg.musabaev.megalabnews.repository.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDate;

public interface NonRootCommentListView {
	Long getId();

	@JsonProperty("commentatorId")
	Long getCommentator(); // todo spring security

	String getContent();

	LocalDate getCreatedDate();

	@JsonProperty("parentId")
	ParentComment getParent();

	interface ParentComment {

		@JsonValue
		Long getId();
	}
}
