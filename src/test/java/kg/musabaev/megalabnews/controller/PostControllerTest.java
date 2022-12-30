package kg.musabaev.megalabnews.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kg.musabaev.megalabnews.Application;
import kg.musabaev.megalabnews.dto.NewOrUpdatePostRequest;
import kg.musabaev.megalabnews.repository.PostRepo;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {Application.class, PostController.class}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class PostControllerTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	PostRepo repo;

	final String apiPrefix = "/api/v1/posts";
	final static String rootFolderName = "test-storage";
	final static String postImageFolderName = "post-image";
	static Path storage;

	@BeforeAll
	static void beforeAll() {
		storage = Path.of(rootFolderName, postImageFolderName);
		boolean isDirsCreated = storage.toFile().mkdirs();
		if (isDirsCreated) log.info("{} созданы", storage);
	}

	@AfterAll
	static void afterAll() throws IOException {
		FileSystemUtils.deleteRecursively(Path.of(rootFolderName));
	}

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

		var testFile = new MockMultipartFile(
				"image",
				"test1.jpeg",
				"image/jpeg",
				"it is test bro".getBytes());

		MvcResult result = mvc.perform(multipart(apiPrefix + "/images")
						.file(testFile))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(endsWith("test1.jpeg")))
				.andReturn();
		String imageUrl = result.getResponse().getContentAsString();

		final var newPost = new NewOrUpdatePostRequest(
				"spring 1",
				"desc",
				"content",
				Set.of("java", "persistence"),
				imageUrl
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
				.andExpect(jsonPath("$.tags", hasItems(newPost.tags().toArray())))
				.andExpect(jsonPath("$.createdDate", is(LocalDate.now().toString())))
				.andExpect(jsonPath("$.imageUrl", is(imageUrl)));
	}

	@Test
	@Order(2)
	void shouldBeStatus409_whenSuchTitleAlreadyExists() throws Exception {
		final var newPost = new NewOrUpdatePostRequest(
				"spring 1",
				"desc",
				"content",
				Set.of("java", "persistence"),
				null
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
		final var newPost = new NewOrUpdatePostRequest(
				null,
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
		MvcResult result = mvc.perform(get(apiPrefix + "/3")
						.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.title", is("spring 1")))
				.andExpect(jsonPath("$.description", is("desc")))
				.andExpect(jsonPath("$.createdDate", is(LocalDate.now().toString())))
				.andExpect(jsonPath("$.tags", hasItems("java", "persistence")))
				.andReturn();

		String imageUrl = getImageUrlFromJson(result);
		assertThat(imageUrl, is(notNullValue()));

		mvc.perform(request(HttpMethod.GET, imageUrl))
				.andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
				.andExpect(status().isOk());
	}


	@Test
	@Order(6)
	void shouldBeStatus404_whenPostById4NotExists() throws Exception {
		mvc.perform(get(apiPrefix + "/4")
						.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	@Order(7)
	void shouldBeStatus404_whenDeletingPostById4() throws Exception {
		mvc.perform(delete(apiPrefix + "/4"))
				.andDo(print())
				.andExpect(status().isNotFound());
		assertThat(repo.findAll(), hasSize(3));
	}

	@Test
	@Order(8)
	void shouldBeStatus200AndShouldCascadeDeleteImage_whenDeletingPostById3() throws Exception {
		MvcResult result = mvc.perform(get(apiPrefix + "/3")
				.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andReturn();
		String imageUrl = getImageUrlFromJson(result);
		assertThat(imageUrl, is(notNullValue()));

		mvc.perform(delete(apiPrefix + "/3"))
				.andDo(print())
				.andExpect(status().isOk());
		assertThat(repo.existsById(3L), is(false));
		assertThat(repo.findAll(), hasSize(2)); // фетчится без тега если что


		mvc.perform(request(HttpMethod.GET, imageUrl))
				.andExpect(status().isNotFound());
	}

	@Test
	@Order(9)
	void shouldBeStatus400_whenUploadNotValidFormat() throws Exception {
		var testFile = new MockMultipartFile(
				"image",
				"conditionally_image.json",
				MediaType.APPLICATION_JSON_VALUE,
				"{}".getBytes());

		mvc.perform(multipart(apiPrefix + "/images")
						.file(testFile))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	private String getImageUrlFromJson(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
		String stringifyJson = result.getResponse().getContentAsString();
		Map<String, Object> map = objectMapper.readValue(stringifyJson, new TypeReference<>() {});
		return  (String) map.get("imageUrl");
	}
}