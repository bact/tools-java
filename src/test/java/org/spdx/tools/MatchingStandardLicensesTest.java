/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.spdx.core.DefaultModelStore;
import org.spdx.core.ModelRegistry;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxModelInfoV2_X;
import org.spdx.library.model.v3_0_1.SpdxModelInfoV3_0;
import org.spdx.storage.simple.InMemSpdxStore;

import junit.framework.TestCase;

/**
 * Test cases for MatchingStandardLicenses
 *
 * @author Arthit Suriyawongkul
 */
public class MatchingStandardLicensesTest extends TestCase {

	static final String TEST_DIR = "testResources";

	protected void setUp() throws Exception {
		super.setUp();
		// Force local JAR-cached listed licenses,
		// so tests don't access network - avoids slow/flaky runs.
		System.setProperty("org.spdx.useJARLicenseInfoOnly", "true");
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV3_0());
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV2_X());
		DefaultModelStore.initialize(new InMemSpdxStore(), "http://default/namespace",
				new ModelCopyManager());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMatch() throws Exception {
		File licenseFile = File.createTempFile("apache20", ".txt");
		licenseFile.deleteOnExit();
		Files.write(licenseFile.toPath(),
				"Apache License Version 2.0, January 2004".getBytes(StandardCharsets.UTF_8));
		int result = MatchingStandardLicenses.run(new String[] {licenseFile.getAbsolutePath()});
		assertEquals(ExitCode.SUCCESS, result);
	}

	public void testFileNotFound() {
		int result = MatchingStandardLicenses
				.run(new String[] {TEST_DIR + File.separator + "doesNotExist.txt"});
		assertEquals(ExitCode.ERROR, result);
	}

	public void testUsageError() {
		int result = MatchingStandardLicenses.run(new String[] {});
		assertEquals(ExitCode.USAGE_ERROR, result);

		result = MatchingStandardLicenses.run(new String[] {"a.txt", "b.txt"});
		assertEquals(ExitCode.USAGE_ERROR, result);

		result = MatchingStandardLicenses.run(null);
		assertEquals(ExitCode.USAGE_ERROR, result);
	}

	private static File writeTemp(byte[] content) throws Exception {
		File file = File.createTempFile("readall", ".txt");
		file.deleteOnExit();
		Files.write(file.toPath(), content);
		return file;
	}

	private static byte[] concat(byte[] first, byte[] second) {
		byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	static final String NON_ASCII_TEXT = "© Copyright 2007 über “quoted”";

	public void testReadAllUtf8() throws Exception {
		File file = writeTemp(NON_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
		assertEquals(NON_ASCII_TEXT, MatchingStandardLicenses.readAll(file));
	}

	public void testReadAllUtf8Bom() throws Exception {
		byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
		File file = writeTemp(concat(bom, NON_ASCII_TEXT.getBytes(StandardCharsets.UTF_8)));
		assertEquals(NON_ASCII_TEXT, MatchingStandardLicenses.readAll(file));
	}

	public void testReadAllUtf16Bom() throws Exception {
		byte[] leBom = {(byte) 0xFF, (byte) 0xFE};
		File le = writeTemp(concat(leBom, NON_ASCII_TEXT.getBytes(StandardCharsets.UTF_16LE)));
		assertEquals(NON_ASCII_TEXT, MatchingStandardLicenses.readAll(le));
		byte[] beBom = {(byte) 0xFE, (byte) 0xFF};
		File be = writeTemp(concat(beBom, NON_ASCII_TEXT.getBytes(StandardCharsets.UTF_16BE)));
		assertEquals(NON_ASCII_TEXT, MatchingStandardLicenses.readAll(be));
	}

	public void testReadAllLegacyEncodingFallsBackToDefaultCharset() throws Exception {
		// 0xA9 alone is not valid UTF-8
		byte[] legacy = {'(', 'c', ')', ' ', (byte) 0xA9, ' ', 'x'};
		File file = writeTemp(legacy);
		assertEquals(new String(legacy, Charset.defaultCharset()), MatchingStandardLicenses.readAll(file));
	}

	public void testReadAllEmptyAndBomOnly() throws Exception {
		assertEquals("", MatchingStandardLicenses.readAll(writeTemp(new byte[0])));
		assertEquals("", MatchingStandardLicenses.readAll(
				writeTemp(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF})));
	}
}
