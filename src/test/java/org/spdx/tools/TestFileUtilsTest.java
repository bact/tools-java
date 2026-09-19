/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link TestFileUtils}
 */
public class TestFileUtilsTest {

	private Path root;

	@Before
	public void createRoot() throws IOException {
		root = Files.createTempDirectory("spdx-test-file-utils-");
	}

	@After
	public void deleteRoot() {
		TestFileUtils.deleteDirAndFiles(root);
	}

	private static Path writeFile(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		return Files.write(file, "content".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void deletesTreeWithoutFollowingSymbolicLinks() throws IOException {
		Path outside = writeFile(root.resolve("outside").resolve("keep.txt"));
		Path tree = root.resolve("tree");
		writeFile(tree.resolve("a.txt"));
		writeFile(tree.resolve("sub").resolve("deep").resolve("b.txt"));
		try {
			Files.createSymbolicLink(tree.resolve("link"), root.resolve("outside"));
		} catch (IOException | UnsupportedOperationException e) {
			// symbolic links may need a privilege (Windows) or not be supported
			assumeNoException(e);
		}
		TestFileUtils.deleteDirAndFiles(tree);
		assertFalse(Files.exists(tree, LinkOption.NOFOLLOW_LINKS));
		assertTrue("the link target must not be deleted", Files.exists(outside));
	}
}
