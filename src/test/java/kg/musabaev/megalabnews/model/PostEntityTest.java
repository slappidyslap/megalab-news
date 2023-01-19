package kg.musabaev.megalabnews.model;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;

@Disabled
@DataJpaTest
@ActiveProfiles("test")
@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostEntityTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void shouldSetCurrentDate_whenPersistPost() {
		Post transientPost = new Post();
		transientPost.setTitle("title");
		transientPost.setContent("it is content");
		Post persistedPost = entityManager.persistAndFlush(transientPost);
		assertThat(LocalDate.now(), is(persistedPost.getCreatedDate()));
	}

	@Test
	void shouldBeSameHashcode() {
		Set<Post> posts = new HashSet<>();

		Post transientPost = new Post();
		transientPost.setTitle("title");
		transientPost.setContent("content");

		posts.add(transientPost);

		Post persistedPost = entityManager.persistAndFlush(transientPost);

		assertThat(persistedPost, in(posts));
		assertThat(persistedPost.hashCode(), is(transientPost.hashCode()));
		log.info("persisted post's hashcode: {}", persistedPost.hashCode());
		log.info("transient post's hashcode: {}", transientPost.hashCode());
	}
}
