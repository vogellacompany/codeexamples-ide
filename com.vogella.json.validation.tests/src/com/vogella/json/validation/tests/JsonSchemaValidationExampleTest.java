package com.vogella.json.validation.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.networknt.schema.ValidationMessage;
import com.vogella.json.validation.JsonSchemaValidationExample;

class JsonSchemaValidationExampleTest {

	private final JsonSchemaValidationExample example = new JsonSchemaValidationExample();

	@Test
	void validDocumentHasNoErrors() throws Exception {
		try (InputStream schema = schema(); InputStream data = resource("data.json")) {
			Set<ValidationMessage> errors = example.validate(schema, data);
			assertTrue(errors.isEmpty(), () -> "Expected no errors but got: " + errors);
		}
	}

	@Test
	void invalidDocumentReportsErrors() throws Exception {
		// Missing required "name", negative "age", and an undeclared property.
		String invalid = """
				{
				  "age": -1,
				  "unexpected": true
				}
				""";
		try (InputStream schema = schema();
				InputStream data = new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))) {
			Set<ValidationMessage> errors = example.validate(schema, data);
			assertFalse(errors.isEmpty(), "Expected validation errors for an invalid document");
		}
	}

	private static InputStream schema() {
		return resource("schema.json");
	}

	private static InputStream resource(String name) {
		InputStream in = JsonSchemaValidationExample.class.getResourceAsStream(name);
		if (in == null) {
			throw new IllegalStateException("Missing classpath resource: " + name);
		}
		return in;
	}
}
