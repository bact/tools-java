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
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Test cases for the RdfSchemaTo* command line tools
 */
public class RdfSchemaToolsTest extends TestCase {

	static final String ONTOLOGY = "testResources" + File.separator + "spdx-2-2-revision-8-ontology.owl.xml";

	/** output file name -> tool run method, invoked as run(inputFile, outputFile) */
	static final Map<String, Function<String[], Integer>> TOOLS = new LinkedHashMap<>();
	static {
		TOOLS.put("schema.xsd", RdfSchemaToXsd::run);
		TOOLS.put("schema.json", RdfSchemaToJsonSchema::run);
		TOOLS.put("context.json", RdfSchemaToJsonContext::run);
	}

	Path tempDirPath;

	protected void setUp() throws Exception {
		super.setUp();
		tempDirPath = Files.createTempDirectory("spdx-rdf-schema-test-");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		TestFileUtils.deleteDirAndFiles(tempDirPath);
	}

	public void testGeneratesOutputAndReleasesFile() throws Exception {
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve(tool.getKey());
			assertEquals(tool.getKey(), ExitCode.SUCCESS,
					tool.getValue().apply(new String[] {ONTOLOGY, output.toString()}).intValue());
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
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve(tool.getKey());
			Path missing = tempDirPath.resolve("doesNotExist.owl.xml");
			assertEquals(ExitCode.ERROR, tool.getValue().apply(new String[] {missing.toString(), output.toString()}).intValue());
			assertFalse(Files.exists(output));
		}
	}

	public void testUnwritableOutputDoesNotThrow() {
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			Path unwritable = tempDirPath.resolve("no-such-dir").resolve(tool.getKey());
			assertEquals(ExitCode.ERROR, tool.getValue().apply(new String[] {ONTOLOGY, unwritable.toString()}).intValue());
			assertFalse(Files.exists(unwritable));
		}
	}

	public void testExistingOutputIsNotOverwritten() throws Exception {
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve(tool.getKey());
			Files.write(output, "keep".getBytes(StandardCharsets.UTF_8));
			assertEquals(ExitCode.ERROR, tool.getValue().apply(new String[] {ONTOLOGY, output.toString()}).intValue());
			assertEquals("keep", new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
		}
	}

	public void testUsageError() {
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			assertEquals(ExitCode.USAGE_ERROR, tool.getValue().apply(new String[] {}).intValue());
			assertEquals(ExitCode.USAGE_ERROR, tool.getValue().apply(new String[] {ONTOLOGY}).intValue());
			assertEquals(ExitCode.USAGE_ERROR, tool.getValue().apply(new String[] {"a", "b", "c"}).intValue());
		}
	}

	public void testMalformedOntologyIsAnError() throws Exception {
		Path input = tempDirPath.resolve("bad.owl.xml");
		Files.write(input, "not rdf".getBytes(StandardCharsets.UTF_8));
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve("bad-" + tool.getKey());
			assertEquals(tool.getKey(), ExitCode.ERROR,
					tool.getValue().apply(new String[] {input.toString(), output.toString()}).intValue());
			assertFalse(Files.exists(output));
		}
	}

	public void testOntologyWithoutHeaderIsAnError() throws Exception {
		Path input = tempDirPath.resolve("noheader.owl.xml");
		Files.write(input, ("<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'"
				+ " xmlns:owl='http://www.w3.org/2002/07/owl#'><owl:Class rdf:about='http://example.com/A'/></rdf:RDF>")
				.getBytes(StandardCharsets.UTF_8));
		for (Map.Entry<String, Function<String[], Integer>> tool : TOOLS.entrySet()) {
			Path output = tempDirPath.resolve("noheader-" + tool.getKey());
			assertEquals(tool.getKey(), ExitCode.ERROR,
					tool.getValue().apply(new String[] {input.toString(), output.toString()}).intValue());
			assertFalse(Files.exists(output));
		}
	}
}
