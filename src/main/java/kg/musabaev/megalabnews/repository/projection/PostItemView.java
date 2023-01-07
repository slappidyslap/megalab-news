package kg.musabaev.megalabnews.repository.projection;

import java.time.LocalDate;
import java.util.Set;

public interface PostItemView {
	Long getId();
	String getTitle();
	String getDescription();
	LocalDate getCreatedDate();
	Set<String> getTags();
	String getContent();
	String getImageUrl();
	UserInfo getAuthor();

	interface UserInfo {
		Long getId();
		String getName();
		String getSurname();
		String getUsername();
		String getUserPictureUrl();
	}
}