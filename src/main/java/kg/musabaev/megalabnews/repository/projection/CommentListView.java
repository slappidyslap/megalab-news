package kg.musabaev.megalabnews.repository.projection;

import java.time.LocalDate;

public interface CommentListView {
	Long getId();
	UserInfo getAuthor(); // todo spring security
	String getContent();
	LocalDate getCreatedDate();

	interface UserInfo {
		Long getId();
		String getName();
		String getSurname();
		String getUsername();
		String getUserPictureUrl();
	}
}
