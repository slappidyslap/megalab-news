package kg.musabaev.megalabnews.repository.projection;

import java.time.LocalDate;

public interface PostListView {
	Long getId();

	String getTitle();

	String getDescription();

	LocalDate getCreatedDate();

	String getImageUrl();
}