package kg.musabaev.megalabnews.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.musabaev.megalabnews.Application;
import kg.musabaev.megalabnews.dto.NewPostRequest;
import kg.musabaev.megalabnews.repository.PostRepo;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {Application.class, PostController.class}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostControllerTest {

	private final String apiPrefix = "/api/v1/posts";
	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private PostRepo repo;

	@Test
	@Sql("/initDbForPostControllerTest.sql")
	@Order(0)
	void shouldReturn2Records() throws Exception {
		mvc.perform(get(apiPrefix))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.page.content").isArray())
				.andExpect(jsonPath("$.page.content", hasSize(2)))
				.andExpect(jsonPath("$.page.totalElements", is(2)));
	}

	@Test
	@Order(1)
	void shouldBeStatus201_whenSuchTitleNotExists() throws Exception {
		final var newPost = new NewPostRequest(
				"spring 1",
				"desc",
				"content",
				List.of("java", "persistence")
		);
		mvc.perform(post(apiPrefix)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(objectMapper.writeValueAsString(newPost)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.id", is(3)))
				.andExpect(jsonPath("$.title", is(newPost.title())))
				.andExpect(jsonPath("$.description", is(newPost.description())))
				.andExpect(jsonPath("$.content", is(newPost.content())))
				.andExpect(jsonPath("$.tags", is(newPost.tags())))
				.andExpect(jsonPath("$.createdDate", is(LocalDate.now().toString())));
	}

	@Test
	@Order(2)
	void shouldBeStatus409_whenSuchTitleAlreadyExists() throws Exception {
		final var newPost = new NewPostRequest(
				"spring 1",
				"desc",
				"content",
				List.of("java", "persistence")
		);
		mvc.perform(post(apiPrefix)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(objectMapper.writeValueAsString(newPost)))
				.andDo(print())
				.andExpect(status().isConflict());
	}

	@Test
	@Order(3)
	void shouldBeStatus400_whenConstraintViolation() throws Exception {
		final var newPost = new NewPostRequest(
				null,
				null,
				null,
				null
		);
		mvc.perform(post(apiPrefix)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(objectMapper.writeValueAsString(newPost)))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(4)
	void shouldReturn3Records() throws Exception {
		mvc.perform(get(apiPrefix))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.page.content").isArray())
				.andExpect(jsonPath("$.page.content", hasSize(3)))
				.andExpect(jsonPath("$.page.totalElements", is(3)));
	}

	@Test
	@Order(5)
	void shouldBeStatus200_whenPostById3Exists() throws Exception {
		mvc.perform(get(apiPrefix + "/3")
						.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.title", is("spring 1")))
				.andExpect(jsonPath("$.description", is("desc")))
				.andExpect(jsonPath("$.createdDate", is(LocalDate.now().toString())))
				.andExpect(jsonPath("$.tags", hasItems("java", "persistence")));
	}

	@Test
	@Order(5)
	void shouldBeStatus404_whenPostById4NotExists() throws Exception {
		mvc.perform(get(apiPrefix + "/4")
						.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	@Order(6)
	void shouldBeStatus404_whenDeletingPostById4() throws Exception {
		mvc.perform(delete(apiPrefix + "/4"))
				.andDo(print())
				.andExpect(status().isNotFound());
		assertThat(repo.findAll(), hasSize(3));
	}

	@Test
	@Order(7)
	void shouldBeStatus200_whenDeletingPostById3() throws Exception {
		mvc.perform(delete(apiPrefix + "/3"))
				.andDo(print())
				.andExpect(status().isOk());
		assertThat(repo.existsById(3L), is(false));
		assertThat(repo.findAll(), hasSize(2)); // фетчится без тега если что
	}
}