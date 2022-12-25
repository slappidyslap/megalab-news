package kg.musabaev.megalabnews.repository.projection;

import java.time.LocalDate;
import java.util.Set;

public interface PostWithoutContent {
	Long getId();

	String getTitle();

	Set<String> getTags();

	String getDescription();

	LocalDate getCreatedDate();

	String getCoverPath();
}