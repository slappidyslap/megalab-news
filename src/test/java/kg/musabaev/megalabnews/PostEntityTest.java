package kg.musabaev.megalabnews;

import kg.musabaev.megalabnews.model.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DataJpaTest
@ActiveProfiles("test")
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
}
