/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Test cases for the RdfSchemaTo* command line tools
 */
public class RdfSchemaToolsTest extends TestCase {

	static final String ONTOLOGY = "testResources" + File.separator + "spdx-2-2-revision-8-ontology.owl.xml";

	/** output file name -> tool main method, invoked as main(inputFile, outputFile) */
	static final Map<String, Consumer<String[]>> TOOLS = new LinkedHashMap<>();
	static {
		TOOLS.put("schema.xsd", RdfSchemaToXsd::main);
		TOOLS.put("schema.json", RdfSchemaToJsonSchema::main);
		TOOLS.put("context.json", RdfSchemaToJsonContext::main);
	}

	Path tempDirPath;

	protected void setUp() throws Exception {
		super.setUp();
		tempDirPath = Files.createTempDirectory("spdx-rdf-schema-test-");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		SpdxConverterTestV3.deleteDirAndFiles(tempDirPath);
	}

	public void testGeneratesOutputAndReleasesFile() throws Exception {
		for (Map.Entry<String, Consumer<String[]>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve(tool.getKey());
			tool.getValue().accept(new String[] {ONTOLOGY, output.toString()});
			assertTrue(tool.getKey() + " output missing", Files.exists(output));
			String text = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
			assertFalse(tool.getKey() + " output empty", text.isEmpty());
			if (tool.getKey().endsWith(".xsd")) {
				assertTrue(text.contains("schema"));
			} else {
				JsonNode root = new ObjectMapper().readTree(text);
				assertTrue(root.isObject() && root.size() > 0);
			}
			// a leaked file handle would prevent deletion on Windows
			assertTrue(Files.deleteIfExists(output));
		}
	}

	public void testMissingInputCreatesNoOutput() {
		for (Map.Entry<String, Consumer<String[]>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve(tool.getKey());
			Path missing = tempDirPath.resolve("doesNotExist.owl.xml");
			tool.getValue().accept(new String[] {missing.toString(), output.toString()});
			assertFalse(Files.exists(output));
		}
	}

	public void testUnwritableOutputDoesNotThrow() {
		for (Map.Entry<String, Consumer<String[]>> tool : TOOLS.entrySet()) {
			Path unwritable = tempDirPath.resolve("no-such-dir").resolve(tool.getKey());
			tool.getValue().accept(new String[] {ONTOLOGY, unwritable.toString()});
			assertFalse(Files.exists(unwritable));
		}
	}

	public void testExistingOutputIsNotOverwritten() throws Exception {
		for (Map.Entry<String, Consumer<String[]>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve(tool.getKey());
			Files.write(output, "keep".getBytes(StandardCharsets.UTF_8));
			tool.getValue().accept(new String[] {ONTOLOGY, output.toString()});
			assertEquals("keep", new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
		}
	}
}
