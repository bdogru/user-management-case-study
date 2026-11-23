package io.github.bdogru.usermanagement.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import com.jayway.jsonpath.JsonPath;
import io.github.bdogru.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

	private static final Locale TURKISH = Locale.of("tr", "TR");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository repository;

	@BeforeEach
	void deleteAllUsers() {
		repository.deleteAll();
	}

	@Test
	void createStoresTheEmailInLowerCaseWhateverTheDefaultLocale() throws Exception {
		Locale original = Locale.getDefault();
		Locale.setDefault(TURKISH); // a default-locale toLowerCase() turns "I" into a dotless "ı" here
		try {
			mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Ivy Smith\",\"email\":\"Ivy.Smith@Example.COM\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/api/users/\\d+")))
				.andExpect(jsonPath("$.name").value("Ivy Smith"))
				.andExpect(jsonPath("$.email").value("ivy.smith@example.com"))
				.andExpect(jsonPath("$.version").value(0));
		}
		finally {
			Locale.setDefault(original);
		}
	}

	@Test
	void listReturnsAllUsers() throws Exception {
		create("Ivy Smith", "ivy@example.com");
		create("Bob Brown", "bob@example.com");

		mvc.perform(get("/api/users"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].email", containsInAnyOrder("ivy@example.com", "bob@example.com")));
	}

	@Test
	void invalidInputGivesEnglishFieldErrorsWhateverTheLocale() throws Exception {
		Locale original = Locale.getDefault();
		Locale.setDefault(TURKISH);
		try {
			mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).header("Accept-Language", "tr-TR")
					.content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("email: must be a well-formed email address, name: must not be blank"));
		}
		finally {
			Locale.setDefault(original);
		}
	}

	@Test
	void duplicateEmailIsAConflictEvenWhenOnlyTheCaseDiffers() throws Exception {
		create("Ivy Smith", "ivy@example.com");

		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Other\",\"email\":\"IVY@example.com\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A user with this email already exists"));
	}

	@Test
	void unknownIdIsNotFound() throws Exception {
		mvc.perform(get("/api/users/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("User 999 not found"));
		mvc.perform(put("/api/users/999").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"X\",\"version\":0}"))
			.andExpect(status().isNotFound());
		mvc.perform(delete("/api/users/999"))
			.andExpect(status().isNotFound());
	}

	@Test
	void updateChangesTheNameAndIncrementsTheVersion() throws Exception {
		long id = create("Ivy Smith", "ivy@example.com");

		mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ivy Jones\",\"version\":0}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Ivy Jones"))
			.andExpect(jsonPath("$.email").value("ivy@example.com"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void updateCannotChangeTheEmail() throws Exception {
		long id = create("Ivy Smith", "ivy@example.com");

		mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ivy Jones\",\"email\":\"new@example.com\",\"version\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Unknown property: email"));

		mvc.perform(get("/api/users/" + id))
			.andExpect(jsonPath("$.email").value("ivy@example.com"))
			.andExpect(jsonPath("$.name").value("Ivy Smith"));
	}

	@Test
	void updateWithAStaleVersionIsAConflictAndChangesNothing() throws Exception {
		long id = create("Ivy Smith", "ivy@example.com");
		mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ivy Jones\",\"version\":0}"))
			.andExpect(status().isOk());

		mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ivy Brown\",\"version\":0}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("The user was changed by someone else. Reload and try again."));

		mvc.perform(get("/api/users/" + id))
			.andExpect(jsonPath("$.name").value("Ivy Jones"))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void deleteRemovesTheUser() throws Exception {
		long id = create("Ivy Smith", "ivy@example.com");

		mvc.perform(delete("/api/users/" + id))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/users/" + id))
			.andExpect(status().isNotFound());
	}

	private long create(String name, String email) throws Exception {
		String body = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

}
