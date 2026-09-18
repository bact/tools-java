/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.spdx.core.DefaultModelStore;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.ModelRegistry;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxModelInfoV2_X;
import org.spdx.library.model.v2.SpdxPackageVerificationCode;
import org.spdx.library.model.v3_0_1.SpdxModelInfoV3_0;
import org.spdx.storage.simple.InMemSpdxStore;

import junit.framework.TestCase;

public class GenerateVerificationCodeTest extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String FILES = TEST_DIR + File.separator + "sourcefiles";

	protected void setUp() throws Exception {
		super.setUp();
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV3_0());
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV2_X());
		DefaultModelStore.initialize(new InMemSpdxStore(), "http://default/namespace", new ModelCopyManager());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGenerateVerificationCode() throws OnlineToolException, InvalidSPDXAnalysisException {
		String skippedRegex = "Ver.*";
		SpdxPackageVerificationCode result = GenerateVerificationCode.generateVerificationCode(FILES, skippedRegex);
		assertFalse(result.getValue().isEmpty());
		assertEquals(1, result.getExcludedFileNames().size());
		assertTrue(result.getExcludedFileNames().contains("./VerificationSheet.java"));
		SpdxPackageVerificationCode result2 = GenerateVerificationCode.generateVerificationCode(FILES, null);
		assertFalse(result.equivalent(result2));
		assertEquals(0, result2.getExcludedFileNames().size());
	}

	private Path createTree() throws IOException {
		Path root = Files.createTempDirectory("spdx-verification-code-test-");
		Files.write(root.resolve("a.txt"), "a".getBytes(StandardCharsets.UTF_8));
		Files.createDirectories(root.resolve("sub").resolve("deep"));
		Files.write(root.resolve("sub").resolve("b.txt"), "b".getBytes(StandardCharsets.UTF_8));
		Files.write(root.resolve("sub").resolve("deep").resolve("c.txt"), "c".getBytes(StandardCharsets.UTF_8));
		return root;
	}

	public void testSkippedRegexUsesForwardSlashSeparators() throws Exception {
		Path root = createTree();
		try {
			SpdxPackageVerificationCode result =
					GenerateVerificationCode.generateVerificationCode(root.toString(), "sub/.*");
			assertEquals(2, result.getExcludedFileNames().size());
			assertTrue(result.getExcludedFileNames().contains("./sub/b.txt"));
			assertTrue(result.getExcludedFileNames().contains("./sub/deep/c.txt"));
			result = GenerateVerificationCode.generateVerificationCode(root.toString(), "sub/deep/c\\.txt");
			assertEquals(1, result.getExcludedFileNames().size());
			assertTrue(result.getExcludedFileNames().contains("./sub/deep/c.txt"));
			result = GenerateVerificationCode.generateVerificationCode(root.toString(), "a\\.txt");
			assertEquals(1, result.getExcludedFileNames().size());
			assertTrue(result.getExcludedFileNames().contains("./a.txt"));
		} finally {
			TestFileUtils.deleteDirAndFiles(root);
		}
	}

	public void testInvalidRegexIsAUsageError() throws Exception {
		Path root = createTree();
		try {
			assertEquals(ExitCode.USAGE_ERROR, GenerateVerificationCode.run(new String[] {root.toString(), "["}));
			assertEquals(ExitCode.ERROR, GenerateVerificationCode.run(new String[] {root.resolve("missing").toString()}));
			assertEquals(ExitCode.SUCCESS, GenerateVerificationCode.run(new String[] {root.toString()}));
		} finally {
			TestFileUtils.deleteDirAndFiles(root);
		}
	}
}
