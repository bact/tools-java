/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ontapi.model.OntModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import junit.framework.TestCase;

/**
 * Tests generation of JSON-LD contexts from SPDX OWL ontologies.
 */
public class OwlToJsonContextTest extends TestCase {

	static final String OWL_FILE_PATH = "testResources" + File.separator 
			+ "spdx-2-2-revision-8-ontology.owl.xml";

	public void testConvertToContext() throws IOException {
		OwlToJsonContext otjc = null;
		try (InputStream is = new FileInputStream(new File(OWL_FILE_PATH))) {
			OntModel model = AbstractOwlRdfConverter.createOntModel();
			model.read(is, "RDF/XML");
			otjc = new OwlToJsonContext(model);
		}
		ObjectNode result = otjc.convertToContext();

		assertNotNull(result);
		assertTrue(result.has("@context"));
		ObjectNode context = (ObjectNode) result.get("@context");

		assertTrue(context.size() > 0);
		assertTrue(context.has("Document"));
		// the properties of the ontology, not only the fixed entries
		assertTrue("too few entries: " + context.size(), context.size() > 80);
		JsonNode annotations = context.get("annotations");
		assertEquals("spdx:annotations", annotations.get("@id").asText());
		assertEquals("spdx:Annotation", annotations.get("@type").asText());
		assertEquals("@set", annotations.get("@container").asText());
		assertEquals("xs:hexBinary", context.get("checksumValue").get("@type").asText());
		// rdfs:comment is an annotation property, its type is set explicitly
		assertEquals("xs:string", context.get("comment").get("@type").asText());
	}
}
