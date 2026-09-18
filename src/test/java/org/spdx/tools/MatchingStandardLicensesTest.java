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

	static final String NON_ASCII_TEXT = "\u00A9 Copyright 2007 \u00FCber \u201Cquoted\u201D";

	/** U+1F600, outside the BMP: two chars in UTF-16 and four bytes in UTF-8 and UTF-32 */
	static final String SUPPLEMENTARY_TEXT = "x \uD83D\uDE00 y";

	private static byte[] bytes(int... values) {
		byte[] result = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (byte) values[i];
		}
		return result;
	}

	private static final byte[] UTF8_BOM = bytes(0xEF, 0xBB, 0xBF);
	private static final byte[] UTF16LE_BOM = bytes(0xFF, 0xFE);
	private static final byte[] UTF16BE_BOM = bytes(0xFE, 0xFF);
	private static final byte[] UTF32LE_BOM = bytes(0xFF, 0xFE, 0x00, 0x00);
	private static final byte[] UTF32BE_BOM = bytes(0x00, 0x00, 0xFE, 0xFF);

	private static void assertReadAll(String expected, byte[] bom, String text, Charset charset)
			throws Exception {
		File file = writeTemp(concat(bom, text.getBytes(charset)));
		assertEquals(charset.name() + " with BOM", expected, MatchingStandardLicenses.readAll(file));
	}

	public void testReadAllUtf8() throws Exception {
		File file = writeTemp(NON_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
		assertEquals(NON_ASCII_TEXT, MatchingStandardLicenses.readAll(file));
	}

	public void testReadAllUtf8Bom() throws Exception {
		assertReadAll(NON_ASCII_TEXT, UTF8_BOM, NON_ASCII_TEXT, StandardCharsets.UTF_8);
	}

	public void testReadAllUtf16Bom() throws Exception {
		assertReadAll(NON_ASCII_TEXT, UTF16LE_BOM, NON_ASCII_TEXT, StandardCharsets.UTF_16LE);
		assertReadAll(NON_ASCII_TEXT, UTF16BE_BOM, NON_ASCII_TEXT, StandardCharsets.UTF_16BE);
		assertReadAll(SUPPLEMENTARY_TEXT, UTF16LE_BOM, SUPPLEMENTARY_TEXT, StandardCharsets.UTF_16LE);
		assertReadAll(SUPPLEMENTARY_TEXT, UTF16BE_BOM, SUPPLEMENTARY_TEXT, StandardCharsets.UTF_16BE);
	}

	public void testReadAllUtf32Bom() throws Exception {
		Charset utf32le = Charset.forName("UTF-32LE");
		Charset utf32be = Charset.forName("UTF-32BE");
		assertReadAll(NON_ASCII_TEXT, UTF32LE_BOM, NON_ASCII_TEXT, utf32le);
		assertReadAll(NON_ASCII_TEXT, UTF32BE_BOM, NON_ASCII_TEXT, utf32be);
		assertReadAll(SUPPLEMENTARY_TEXT, UTF32LE_BOM, SUPPLEMENTARY_TEXT, utf32le);
		assertReadAll(SUPPLEMENTARY_TEXT, UTF32BE_BOM, SUPPLEMENTARY_TEXT, utf32be);
	}

	public void testReadAllUtf16LeIsNotTakenForUtf32Le() throws Exception {
		// FF FE 41 00 starts like the UTF-32LE BOM (FF FE 00 00) only up to the second byte
		assertReadAll("Apache", UTF16LE_BOM, "Apache", StandardCharsets.UTF_16LE);
	}

	public void testReadAllBomlessUtf8StartingWithBomLikeChars() throws Exception {
		// U+00FF U+00FE encoded as UTF-8 is C3 BF C3 BE, not a BOM
		String text = "\u00FF\u00FE text";
		assertEquals(text, MatchingStandardLicenses.readAll(
				writeTemp(text.getBytes(StandardCharsets.UTF_8))));
	}

	public void testReadAllLegacyEncodingUsesFallbackCharset() throws Exception {
		// 0xA9 alone is not valid UTF-8; it is U+00A9 in ISO-8859-1 and windows-1252
		byte[] legacy = {'(', 'c', ')', ' ', (byte) 0xA9, ' ', 'x'};
		File file = writeTemp(legacy);
		assertEquals("(c) \u00A9 x", MatchingStandardLicenses.readAll(file, StandardCharsets.ISO_8859_1));
		assertEquals("(c) \u00A9 x", MatchingStandardLicenses.readAll(file, Charset.forName("windows-1252")));
	}

	public void testReadAllValidUtf8IgnoresFallbackCharset() throws Exception {
		File file = writeTemp(NON_ASCII_TEXT.getBytes(StandardCharsets.UTF_8));
		assertEquals(NON_ASCII_TEXT, MatchingStandardLicenses.readAll(file, StandardCharsets.ISO_8859_1));
	}

	public void testReadAllDefaultFallbackIsPlatformCharset() throws Exception {
		byte[] legacy = {'(', 'c', ')', ' ', (byte) 0xA9, ' ', 'x'};
		File file = writeTemp(legacy);
		assertEquals(MatchingStandardLicenses.readAll(file, Charset.defaultCharset()),
				MatchingStandardLicenses.readAll(file));
	}

	public void testReadAllEmptyAndBomOnly() throws Exception {
		assertEquals("", MatchingStandardLicenses.readAll(writeTemp(new byte[0])));
		for (byte[] bom : new byte[][] {UTF8_BOM, UTF16LE_BOM, UTF16BE_BOM, UTF32LE_BOM, UTF32BE_BOM}) {
			assertEquals("", MatchingStandardLicenses.readAll(writeTemp(bom)));
		}
	}

	public void testReadAllTruncatedBomPrefixIsNotABom() throws Exception {
		// a single 0xFF (or 0xEF 0xBB) is not a complete BOM and not valid UTF-8: decoded by the fallback
		assertEquals("\u00FF", MatchingStandardLicenses.readAll(
				writeTemp(bytes(0xFF)), StandardCharsets.ISO_8859_1));
		assertEquals("\u00EF\u00BB", MatchingStandardLicenses.readAll(
				writeTemp(bytes(0xEF, 0xBB)), StandardCharsets.ISO_8859_1));
	}
}
