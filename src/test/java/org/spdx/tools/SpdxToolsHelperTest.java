/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.spdx.tools.SpdxToolsHelper.SerFileType;

import junit.framework.TestCase;

/**
 * Test cases for {@link SpdxToolsHelper#fileToFileType(File)}
 */
public class SpdxToolsHelperTest extends TestCase {

	static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	static final String JSONLD_TEXT = "{\"@context\": \"https://spdx.org/rdf/3.0.1/spdx-context.jsonld\","
			+ " \"name\": \"über © “quoted”\"}";
	static final String JSON_TEXT = "{\"name\": \"über © “quoted”\"}";

	Path tempDirPath;

	protected void setUp() throws Exception {
		super.setUp();
		tempDirPath = Files.createTempDirectory("spdx-tools-helper-test-");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		SpdxConverterTestV3.deleteDirAndFiles(tempDirPath);
	}

	private File write(String name, boolean bom, String text) throws IOException {
		Path path = tempDirPath.resolve(name);
		byte[] body = text.getBytes(StandardCharsets.UTF_8);
		byte[] content = new byte[(bom ? UTF8_BOM.length : 0) + body.length];
		if (bom) {
			System.arraycopy(UTF8_BOM, 0, content, 0, UTF8_BOM.length);
		}
		System.arraycopy(body, 0, content, bom ? UTF8_BOM.length : 0, body.length);
		Files.write(path, content);
		return path.toFile();
	}

	public void testJsonLdDetectedWithNonAscii() throws Exception {
		assertEquals(SerFileType.JSONLD, SpdxToolsHelper.fileToFileType(write("a.json", false, JSONLD_TEXT)));
	}

	public void testJsonLdDetectedWithUtf8Bom() throws Exception {
		assertEquals(SerFileType.JSONLD, SpdxToolsHelper.fileToFileType(write("b.json", true, JSONLD_TEXT)));
	}

	public void testPlainJsonWithNonAscii() throws Exception {
		assertEquals(SerFileType.JSON, SpdxToolsHelper.fileToFileType(write("c.json", false, JSON_TEXT)));
		assertEquals(SerFileType.JSON, SpdxToolsHelper.fileToFileType(write("d.json", true, JSON_TEXT)));
	}

	public void testUnreadableJsonFallsBackToJson() throws Exception {
		File missing = tempDirPath.resolve("missing.json").toFile();
		assertEquals(SerFileType.JSON, SpdxToolsHelper.fileToFileType(missing));
	}
}
