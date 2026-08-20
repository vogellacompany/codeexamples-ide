package com.vogella.json.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * Validates a JSON document against a JSON Schema (Draft 2020-12) using
 * networknt/json-schema-validator.
 */
public class JsonSchemaValidationExample {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonSchemaFactory FACTORY = JsonSchemaFactory
			.getInstance(SpecVersion.VersionFlag.V202012);

	/**
	 * Validates the JSON read from {@code data} against the schema read from
	 * {@code schema}.
	 *
	 * @return the validation errors, empty if the document is valid
	 */
	public Set<ValidationMessage> validate(InputStream schema, InputStream data) {
		try {
			JsonSchema jsonSchema = FACTORY.getSchema(schema);
			JsonNode node = MAPPER.readTree(data);
			return jsonSchema.validate(node);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read JSON schema or data", e);
		}
	}

	public static void main(String[] args) {
		JsonSchemaValidationExample example = new JsonSchemaValidationExample();
		try (InputStream schema = resource("schema.json");
				InputStream data = resource("data.json")) {
			Set<ValidationMessage> errors = example.validate(schema, data);
			if (errors.isEmpty()) {
				System.out.println("data.json is valid against schema.json");
			} else {
				System.out.println("data.json is invalid:");
				errors.forEach(error -> System.out.println("  " + error.getMessage()));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static InputStream resource(String name) {
		InputStream in = JsonSchemaValidationExample.class.getResourceAsStream(name);
		if (in == null) {
			throw new IllegalStateException("Missing classpath resource: " + name);
		}
		return in;
	}
}
