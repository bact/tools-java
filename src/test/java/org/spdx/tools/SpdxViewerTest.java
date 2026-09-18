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
		assertTrue(output.contains("\u00A9 Copyright 2007 Hewlett-Packard"));
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

	private static String captureConsoleWriter(String[] chunks) throws Exception {
		PrintStream original = System.out;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
			SpdxViewer.ConsoleWriter writer = new SpdxViewer.ConsoleWriter();
			for (String chunk : chunks) {
				writer.write(chunk.toCharArray(), 0, chunk.length());
			}
			writer.flush();
		} finally {
			System.setOut(original);
		}
		return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
	}

	public void testConsoleWriterSurrogatePairSplitAtFlushBoundary() throws Exception {
		String padding = "a".repeat(SpdxViewer.ConsoleWriter.FLUSH_THRESHOLD - 1);
		// the high surrogate ends the first flushed chunk, its low surrogate starts the next
		String output = captureConsoleWriter(new String[] {padding + "\uD83D", "\uDE00 end"});
		assertEquals(padding + "\uD83D\uDE00 end", output);
	}

	public void testConsoleWriterBuffersUntilFlush() throws Exception {
		PrintStream original = System.out;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
			SpdxViewer.ConsoleWriter writer = new SpdxViewer.ConsoleWriter();
			writer.write("small".toCharArray(), 0, 5);
			assertEquals(0, bytes.size());
			writer.close();
			assertEquals("small", new String(bytes.toByteArray(), StandardCharsets.UTF_8));
		} finally {
			System.setOut(original);
		}
	}

	public void testConsoleWriterLargeOutputIsComplete() throws Exception {
		StringBuilder expected = new StringBuilder();
		String[] chunks = new String[3000];
		for (int i = 0; i < chunks.length; i++) {
			chunks[i] = "line " + i + " \u00A9 \uD83D\uDE00\n";
			expected.append(chunks[i]);
		}
		assertEquals(expected.toString(), captureConsoleWriter(chunks));
	}
}
