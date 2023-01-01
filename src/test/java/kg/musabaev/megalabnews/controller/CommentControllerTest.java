package kg.musabaev.megalabnews.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.musabaev.megalabnews.Application;
import kg.musabaev.megalabnews.dto.NewCommentRequest;
import kg.musabaev.megalabnews.dto.UpdateCommentRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {Application.class, CommentController.class}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentControllerTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;

	final String apiPrefix = "/api/v1/posts";

	@Test
	@Order(0)
	@Sql("/init-db-for-comment-controller-test.sql")
	void shouldReturnTreeOfComments() throws Exception {
		mvc.perform(get(apiPrefix + "/1/comments"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].id", is(1)))
				.andExpect(jsonPath("$.content[0].content", is("1 c - 1 p")))
				.andExpect(jsonPath("$.content[1].id", is(2)))
				.andExpect(jsonPath("$.content[1].content", is("2 c - 1 p")));

		mvc.perform(get(apiPrefix + "/2/comments"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].id", is(3)))
				.andExpect(jsonPath("$.content[0].content", is("1 c - 2 p")))
				.andExpect(jsonPath("$.content[1].id", is(4)))
				.andExpect(jsonPath("$.content[1].content", is("2 c - 2 p")));

		mvc.perform(get(apiPrefix + "/1/comments/1"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id", is(5)))
				.andExpect(jsonPath("$.content[0].content", is("1 c - 1 c - 1 p")));

		mvc.perform(get(apiPrefix + "/1/comments/2"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());

		mvc.perform(get(apiPrefix + "/2/comments/3"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());

		mvc.perform(get(apiPrefix + "/2/comments/4"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id", is(6)))
				.andExpect(jsonPath("$.content[0].content", is("1 c - 2 c - 2 p")));
	}

	@Test
	@Order(1)
	void shouldBeStatus400_whenPrePersistConstraintViolation() throws Exception {
		var newCommentDto = new NewCommentRequest(
				null,
				null
		);

		mvc.perform(post(apiPrefix + "/1/comments")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newCommentDto)))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(2)
	void shouldBeStatus201_whenSavingCommentAsRoot() throws Exception {
		var newCommentDto = new NewCommentRequest(
				null,
				"3 c - 1 p"
		);

		mvc.perform(post(apiPrefix + "/1/comments")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newCommentDto)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(7)))
				.andExpect(jsonPath("$.postId", is(1)))
				.andExpect(jsonPath("$.parentId", nullValue()))
				.andExpect(jsonPath("$.commentatorId", is(0)))
				.andExpect(jsonPath("$.content", is(newCommentDto.content())))
				.andExpect(jsonPath("$.createdDate", is(LocalDate.now().toString())));

		mvc.perform(get(apiPrefix + "/1/comments"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(3)));
	}

	@Test
	@Order(3)
	void shouldBeStatus201_whenSavingCommentAsChild() throws Exception {
		var newCommentDto = new NewCommentRequest(
				3L,
				"1 c - 1 c - 2 p"
		);

		mvc.perform(post(apiPrefix + "/2/comments")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newCommentDto)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(8)))
				.andExpect(jsonPath("$.postId", is(2)))
				.andExpect(jsonPath("$.parentId", is(3)))
				.andExpect(jsonPath("$.content", is(newCommentDto.content())));

		mvc.perform(get(apiPrefix + "/2/comments/3"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)));

		mvc.perform(get(apiPrefix + "/2/comments/4"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	@Order(4)
	void shouldBeStatus404_whenCommentNotFoundInPost() throws Exception {
		mvc.perform(get(apiPrefix + "/1/comments/3"))
				.andExpect(status().isNotFound());

		mvc.perform(patch(apiPrefix + "/1/comments/4")
						.content(objectMapper.writeValueAsString(new UpdateCommentRequest("test")))
						.contentType(APPLICATION_JSON))
				.andExpect(status().isNotFound());

		mvc.perform(delete(apiPrefix + "/1/comments/6"))
				.andExpect(status().isNotFound());
	}

	@Test
	@Order(5)
	void shouldBeStatus400_whenPreUpdatingConstraintViolation() throws Exception {
		mvc.perform(patch(apiPrefix + "/1/comments/5")
						.content(objectMapper.writeValueAsString(new UpdateCommentRequest("test")))
						.contentType(APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(5)))
				.andExpect(jsonPath("$.postId", is(1)))
				.andExpect(jsonPath("$.content", is("test")));
	}

	@Test
	@Order(6)
	void shouldBeStatus200_whenDeleteRootComment() throws Exception {
		mvc.perform(delete(apiPrefix + "/1/comments/2"))
				.andDo(print())
				.andExpect(status().isOk());

		mvc.perform(get(apiPrefix + "/1/comments"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2))); // #shouldBeStatus201_whenSavingCommentAsRoot
	}

	@Test
	@Order(7)
	void shouldBeStatus200_whenDeleteChildComment() throws Exception {
		mvc.perform(delete(apiPrefix + "/2/comments/6"))
				.andDo(print())
				.andExpect(status().isOk());

		mvc.perform(get(apiPrefix + "/2/comments/4"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(0)));
	}
}
