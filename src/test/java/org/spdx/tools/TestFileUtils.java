/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * File helpers shared by the tests
 */
final class TestFileUtils {

	private TestFileUtils() {
	}

	/**
	 * Deletes a file, or a directory with its contents. Symbolic links are deleted, not followed.
	 * Failures are reported to stderr, not thrown.
	 * @param dirOrFile path to delete; null or a missing path is ignored
	 */
	static void deleteDirAndFiles(Path dirOrFile) {
		if (Objects.isNull(dirOrFile)) {
			return;
		}
		if (!Files.exists(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		if (Files.isDirectory(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> files = Files.newDirectoryStream(dirOrFile)) {
				for (Path file : files) {
					deleteDirAndFiles(file);
				}
			} catch (IOException e) {
				System.err.println("IO error deleting directory or file " + e.getMessage());
			}
		}
		try {
			Files.delete(dirOrFile);
		} catch (IOException e) {
			System.err.println("IO error deleting directory or file " + e.getMessage());
		}
	}
}
