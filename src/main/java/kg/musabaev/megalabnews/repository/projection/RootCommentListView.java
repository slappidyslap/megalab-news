package kg.musabaev.megalabnews.repository.projection;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public interface RootCommentListView {
	Long getId();

	@JsonProperty("commentatorId")
	Long getCommentator(); // todo spring security

	String getContent();

	LocalDate getCreatedDate();
}