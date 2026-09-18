/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;

/**
 * Test cases for {@link SpdxViewer}
 */
public class SpdxViewerTest extends TestCase {

	static final String TEST_FILE = "testResources" + File.separator + "SPDXJSONExample-v2.3.spdx.json";

	public void testViewerOutput() {
		PrintStream original = System.out;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int result;
		try {
			System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
			result = SpdxViewer.run(new String[] {TEST_FILE});
			System.out.println("MARKER-AFTER-RUN");
		} finally {
			System.setOut(original);
		}
		assertEquals(ExitCode.SUCCESS, result);
		String output = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
		assertTrue(output.contains("Document Name: SPDX-Tools-v2.0"));
		// non-ASCII text in the document is written in the encoding of System.out
		assertTrue(output.contains("© Copyright 2007 Hewlett-Packard"));
		// the viewer must not close System.out
		assertTrue(output.contains("MARKER-AFTER-RUN"));
	}

	public void testFileNotFound() {
		assertEquals(ExitCode.ERROR,
				SpdxViewer.run(new String[] {"testResources" + File.separator + "doesNotExist.json"}));
	}

	public void testUsageError() {
		assertEquals(ExitCode.USAGE_ERROR, SpdxViewer.run(new String[] {}));
	}
}
