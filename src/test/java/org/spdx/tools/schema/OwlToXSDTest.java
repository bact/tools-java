/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ontapi.model.OntModel;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

import junit.framework.TestCase;

/**
 * Tests generation of XML Schema Definitions (XSD) from SPDX OWL ontologies.
 */
public class OwlToXSDTest extends TestCase {

	static final String OWL_FILE_PATH = "testResources" + File.separator + "spdx-2-2-revision-8-ontology.owl.xml";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConvertToXsd() throws IOException, XmlSchemaSerializerException, SchemaException {
		OwlToXsd otx = null;
		try (InputStream is = new FileInputStream(new File(OWL_FILE_PATH))) {
			OntModel model = AbstractOwlRdfConverter.createOntModel();
			model.read(is, "RDF/XML");
			otx = new OwlToXsd(model);
		}
		XmlSchema result = otx.convertToXsd();

		assertNotNull(result);
		assertNotNull(result.getElementByName("Document"));
		String expectedIRI = "http://spdx.org/rdf/terms";
		assertEquals(expectedIRI, result.getTargetNamespace());

		// every base type is defined in the schema (owl:Thing is not)
		int extensions = 0;
		for (XmlSchemaObject item : result.getItems()) {
			if (item instanceof XmlSchemaComplexType) {
				XmlSchemaContentModel model = ((XmlSchemaComplexType) item).getContentModel();
				if (model instanceof XmlSchemaComplexContent
						&& ((XmlSchemaComplexContent) model).getContent() instanceof XmlSchemaComplexContentExtension) {
					XmlSchemaComplexContentExtension extension =
							(XmlSchemaComplexContentExtension) ((XmlSchemaComplexContent) model).getContent();
					extensions++;
					assertNotNull("undefined base type " + extension.getBaseTypeName(),
							result.getTypeByName(extension.getBaseTypeName()));
				}
			}
		}
		assertTrue(extensions > 0);
	}

	public void testOntologyWithoutHeaderIsRejected() {
		OntModel model = AbstractOwlRdfConverter.createOntModel();
		model.read(new java.io.StringReader("<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'"
				+ " xmlns:owl='http://www.w3.org/2002/07/owl#'><owl:Class rdf:about='http://example.com/A'/></rdf:RDF>"),
				null, "RDF/XML");
		try {
			new OwlToXsd(model);
			fail("expected an exception for a model without an ontology");
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("No ontologies"));
		}
	}

}
