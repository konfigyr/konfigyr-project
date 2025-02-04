package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class RepresentationMarshallingTest {

	final ObjectMapper mapper = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	@Test
	@DisplayName("should serialize and deserialize links")
	void links() throws Exception {
		var links = List.of(
				Link.of("https://localhost/search?q={param}", LinkRelation.SEARCH),
				Link.of("https://localhost/search?q={param}&page={page}", LinkRelation.SEARCH, "POST"),
				Link.of("https://localhost")
		);

		String json = mapper.writeValueAsString(links);

		assertThat(json).isEqualTo("[{\"rel\":\"search\",\"href\":\"https://localhost/search?q={param}\",\"method\":\"GET\"}," +
				"{\"rel\":\"search\",\"href\":\"https://localhost/search?q={param}&page={page}\",\"method\":\"POST\"}," +
				"{\"rel\":\"self\",\"href\":\"https://localhost\",\"method\":\"GET\"}]");

		assertThatObject(mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, Link.class)))
				.isInstanceOf(List.class)
				.isEqualTo(links);
	}

	@Test
	@DisplayName("should serialize and deserialize representation model")
	void representationModel() throws Exception {
		RepresentationModel<?> model = new RepresentationModel<>(Link.of("https://localhost"));
		String json = mapper.writeValueAsString(model);

		assertThat(json).isEqualTo("{\"links\":[{\"rel\":\"self\",\"href\":\"https://localhost\",\"method\":\"GET\"}]}");

		assertThat(mapper.readValue(json, RepresentationModel.class))
				.isEqualTo(model);
	}

	@Test
	@DisplayName("should serialize entity model")
	void entityModel() throws Exception {
		RepresentationModel<?> model = Person.create(Link.of("https://localhost"));

		assertThat(mapper.writeValueAsString(model))
				.isEqualTo("{\"firstName\":\"John\",\"lastName\":\"Doe\"," +
						"\"links\":[{\"rel\":\"self\",\"href\":\"https://localhost\",\"method\":\"GET\"}]}");
	}

	@Test
	@DisplayName("should serialize collection model")
	void collectionModel() throws Exception {
		RepresentationModel<?> model = CollectionModel.of(List.of(Person.create()), Link.of("https://localhost"));

		assertThat(mapper.writeValueAsString(model))
				.isEqualTo("{\"links\":[{\"rel\":\"self\",\"href\":\"https://localhost\",\"method\":\"GET\"}]," +
						"\"data\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}]}");
	}

	@Test
	@DisplayName("should serialize page model")
	void pagedModel() throws Exception {
		Page<EntityModel<Person>> page = new PageImpl<>(List.of(Person.create(Link.of("https://localhost/1"))));
		RepresentationModel<?> model = PagedModel.of(page, Link.of("https://localhost"));

		assertThat(mapper.writeValueAsString(model))
				.isEqualTo("{\"links\":[{\"rel\":\"self\",\"href\":\"https://localhost\",\"method\":\"GET\"}]," +
						"\"data\":[{\"firstName\":\"John\",\"lastName\":\"Doe\",\"links\":[{\"rel\":\"self\",\"href\":\"https://localhost/1\",\"method\":\"GET\"}]}]," +
						"\"metadata\":{\"size\":1,\"number\":0,\"total\":1,\"pages\":1}}");
	}

	record Person(@JsonProperty String firstName, @JsonProperty String lastName) {
		static EntityModel<Person> create(Link... links) {
			return EntityModel.of(new Person("John", "Doe"), links);
		}
	}

}
