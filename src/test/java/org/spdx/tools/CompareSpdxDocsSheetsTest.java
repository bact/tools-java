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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import junit.framework.TestCase;

/**
 * Test cases for the comparison spreadsheet written by {@link CompareSpdxDocs},
 * using small SPDX 2.3 JSON documents generated in a temporary directory
 */
public class CompareSpdxDocsSheetsTest extends TestCase {

	static final ObjectMapper MAPPER = new ObjectMapper();
	static final String CHECKSUM_SHA1 = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12";

	Path tempDirPath;

	protected void setUp() throws Exception {
		super.setUp();
		tempDirPath = Files.createTempDirectory("spdx-compare-sheets-test-");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		TestFileUtils.deleteDirAndFiles(tempDirPath);
	}

	/**
	 * @param index makes the document namespace unique
	 * @param licenseId license ID of the extracted license and of the concluded license of the file ./a.c
	 * @param extractedText extracted text of that license
	 */
	private ObjectNode createDoc(int index, String licenseId, String extractedText) {
		ObjectNode doc = MAPPER.createObjectNode();
		doc.put("SPDXID", "SPDXRef-DOCUMENT");
		doc.put("spdxVersion", "SPDX-2.3");
		doc.put("name", "doc" + index);
		doc.put("dataLicense", "CC0-1.0");
		doc.put("documentNamespace", "http://example.com/spdxdocs/doc" + index);
		ObjectNode creationInfo = doc.putObject("creationInfo");
		creationInfo.put("created", "2020-01-01T00:00:00Z");
		creationInfo.putArray("creators").add("Tool: test");
		ObjectNode extracted = doc.putArray("hasExtractedLicensingInfos").addObject();
		extracted.put("licenseId", licenseId);
		extracted.put("extractedText", extractedText);
		ObjectNode file = doc.putArray("files").addObject();
		file.put("SPDXID", "SPDXRef-File");
		file.put("fileName", "./a.c");
		ObjectNode checksum = file.putArray("checksums").addObject();
		checksum.put("algorithm", "SHA1");
		checksum.put("checksumValue", CHECKSUM_SHA1);
		file.put("licenseConcluded", licenseId);
		file.putArray("licenseInfoInFiles").add(licenseId);
		file.put("copyrightText", "NONE");
		addDescribes(doc, "SPDXRef-File");
		return doc;
	}

	private void addDescribes(ObjectNode doc, String elementId) {
		ArrayNode relationships = doc.has("relationships") ? (ArrayNode) doc.get("relationships")
				: doc.putArray("relationships");
		ObjectNode relationship = relationships.addObject();
		relationship.put("spdxElementId", "SPDXRef-DOCUMENT");
		relationship.put("relationshipType", "DESCRIBES");
		relationship.put("relatedSpdxElement", elementId);
	}

	private String write(String fileName, ObjectNode doc) throws IOException {
		Path path = tempDirPath.resolve(fileName);
		Files.write(path, MAPPER.writeValueAsBytes(doc));
		return path.toString();
	}

	private String[] compareArgs(String outputName, List<String> docPaths) {
		List<String> args = new ArrayList<>();
		args.add(tempDirPath.resolve(outputName).toString());
		args.addAll(docPaths);
		return args.toArray(new String[0]);
	}

	private List<Row> dataRows(String outputName, String sheetName) throws IOException {
		try (Workbook workbook = WorkbookFactory.create(tempDirPath.resolve(outputName).toFile())) {
			Sheet sheet = workbook.getSheet(sheetName);
			assertNotNull("missing sheet " + sheetName, sheet);
			List<Row> rows = new ArrayList<>();
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				if (sheet.getRow(i) != null) {
					rows.add(sheet.getRow(i));
				}
			}
			return rows;
		}
	}

	private static String text(Row row, int col) {
		Cell cell = row.getCell(col);
		return cell == null ? "" : cell.getStringCellValue();
	}

	public void testMaximumNumberOfDocuments() throws Exception {
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < CompareSpdxDocs.MAX_ARGS - 1; i++) {
			paths.add(write("doc" + i + ".spdx.json", createDoc(i, "LicenseRef-L", "Some license text")));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("max.xlsx", paths)));
		assertTrue(Files.exists(tempDirPath.resolve("max.xlsx")));
	}

	public void testTooManyDocuments() throws Exception {
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < CompareSpdxDocs.MAX_ARGS; i++) {
			paths.add("doc" + i + ".spdx.json");
		}
		assertEquals(ExitCode.USAGE_ERROR, CompareSpdxDocs.run(compareArgs("many.xlsx", paths)));
	}

	public void testSingleDocumentIsAnErrorAndLeavesNoOutput() throws Exception {
		String path = write("one.spdx.json", createDoc(0, "LicenseRef-L", "Some license text"));
		assertEquals(ExitCode.ERROR, CompareSpdxDocs.run(compareArgs("one.xlsx", Arrays.asList(path))));
		assertFalse(Files.exists(tempDirPath.resolve("one.xlsx")));
	}

	public void testExtractedLicenseTextLongerThanCellLimit() throws Exception {
		StringBuilder longText = new StringBuilder();
		while (longText.length() < 40000) {
			longText.append("word ");
		}
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			paths.add(write("doc" + i + ".spdx.json", createDoc(i, "LicenseRef-L" + i, longText.toString())));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("long.xlsx", paths)));
		List<Row> rows = dataRows("long.xlsx", "Extracted Licenses");
		assertFalse(rows.isEmpty());
		String cellText = text(rows.get(0), 0);
		assertTrue(cellText.length() <= 32767);
		assertTrue(cellText.endsWith("[more...]"));
	}

	public void testFileLicensesMappedAcrossThreeDocuments() throws Exception {
		// each document names the same license differently: the extracted text is what makes them equal
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			paths.add(write("doc" + i + ".spdx.json", createDoc(i, "LicenseRef-L" + i, "Some license text")));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("three.xlsx", paths)));
		for (String sheetName : new String[] {"File Concluded", "File Found Licenses"}) {
			List<Row> rows = dataRows("three.xlsx", sheetName);
			assertEquals(sheetName, 1, rows.size());
			assertEquals(sheetName, "Equal", text(rows.get(0), 1));
		}
	}

	private ObjectNode createDocWithPackage(int index, Consumer<ObjectNode> packageCustomizer) {
		ObjectNode doc = createDoc(index, "LicenseRef-L", "Some license text");
		ObjectNode pkg = doc.putArray("packages").addObject();
		pkg.put("SPDXID", "SPDXRef-Package");
		pkg.put("name", "pkg");
		pkg.put("downloadLocation", "NOASSERTION");
		pkg.put("filesAnalyzed", false);
		pkg.put("licenseConcluded", "NOASSERTION");
		pkg.put("licenseDeclared", "NOASSERTION");
		pkg.put("copyrightText", "NONE");
		packageCustomizer.accept(pkg);
		addDescribes(doc, "SPDXRef-Package");
		return doc;
	}

	private String equalsCellOfRow(List<Row> rows, String fieldName) {
		for (Row row : rows) {
			if (fieldName.equals(text(row, 0))) {
				return text(row, 1);
			}
		}
		fail("no row for " + fieldName);
		return null;
	}

	public void testPackageAttributionDifferenceIsReported() throws Exception {
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			final int docIndex = i;
			paths.add(write("doc" + i + ".spdx.json", createDocWithPackage(i,
					pkg -> pkg.putArray("attributionTexts").add("attribution " + docIndex))));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("attr.xlsx", paths)));
		List<Row> rows = dataRows("attr.xlsx", "Package");
		assertEquals("Diff", equalsCellOfRow(rows, "Attributions"));
		assertEquals("Equal", equalsCellOfRow(rows, "Annotations"));
	}

	public void testDuplicateDocumentNamespacesAreComparedAndReported() throws Exception {
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			final int docIndex = i;
			ObjectNode doc = createDocWithPackage(0,
					pkg -> pkg.putArray("attributionTexts").add("attribution " + docIndex));
			paths.add(write("doc" + i + ".spdx.json", doc));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("dup.xlsx", paths)));
		// documents that share a namespace are still told apart
		assertEquals("Diff", equalsCellOfRow(dataRows("dup.xlsx", "Package"), "Attributions"));
		boolean reported = false;
		for (Row row : dataRows("dup.xlsx", "Verification Errors")) {
			for (Cell cell : row) {
				if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
						&& cell.getStringCellValue().contains("Duplicate Document URI")) {
					reported = true;
				}
			}
		}
		assertTrue("duplicate namespace not reported", reported);
	}

	private List<String> writeDocsWithFileMd5(String... md5Values) throws IOException {
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < md5Values.length; i++) {
			ObjectNode doc = createDoc(i, "LicenseRef-L", "Some license text");
			ObjectNode checksum = ((ObjectNode) doc.get("files").get(0)).withArray("checksums").addObject();
			checksum.put("algorithm", "MD5");
			checksum.put("checksumValue", md5Values[i]);
			paths.add(write("doc" + i + ".spdx.json", doc));
		}
		return paths;
	}

	public void testFileChecksumsOtherThanSha1AreCompared() throws Exception {
		List<String> paths = writeDocsWithFileMd5("d41d8cd98f00b204e9800998ecf8427e",
				"0cc175b9c0f1b6a831c399e269772661");
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("md5.xlsx", paths)));
		List<Row> rows = dataRows("md5.xlsx", "File Checksum");
		assertEquals(1, rows.size());
		assertEquals("Different", text(rows.get(0), 1));
		assertTrue(text(rows.get(0), 2).contains("MD5 d41d8cd98f00b204e9800998ecf8427e"));
		assertTrue(text(rows.get(0), 2).contains("SHA1 " + CHECKSUM_SHA1));
	}

	public void testEqualFileChecksums() throws Exception {
		List<String> paths = writeDocsWithFileMd5("d41d8cd98f00b204e9800998ecf8427e",
				"d41d8cd98f00b204e9800998ecf8427e");
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("md5same.xlsx", paths)));
		assertEquals("Equal", text(dataRows("md5same.xlsx", "File Checksum").get(0), 1));
	}

	public void testPackageEqualAttributions() throws Exception {
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			paths.add(write("doc" + i + ".spdx.json", createDocWithPackage(i,
					pkg -> pkg.putArray("attributionTexts").add("same attribution"))));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("sameattr.xlsx", paths)));
		assertEquals("Equal", equalsCellOfRow(dataRows("sameattr.xlsx", "Package"), "Attributions"));
	}

	public void testSnippetsAreListedInNameOrder() throws Exception {
		String[] names = {"snippet-f", "snippet-b", "snippet-e", "snippet-a", "snippet-d", "snippet-c"};
		List<String> paths = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			ObjectNode doc = createDoc(i, "LicenseRef-L", "Some license text");
			ArrayNode snippets = doc.putArray("snippets");
			for (String name : names) {
				ObjectNode snippet = snippets.addObject();
				snippet.put("SPDXID", "SPDXRef-" + name);
				snippet.put("name", name);
				snippet.put("snippetFromFile", "SPDXRef-File");
				ObjectNode range = snippet.putArray("ranges").addObject();
				range.putObject("startPointer").put("offset", 1).put("reference", "SPDXRef-File");
				range.putObject("endPointer").put("offset", 2).put("reference", "SPDXRef-File");
				snippet.put("licenseConcluded", "NOASSERTION");
				snippet.put("copyrightText", "NONE");
			}
			paths.add(write("doc" + i + ".spdx.json", doc));
		}
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("snippets.xlsx", paths)));
		List<String> listed = new ArrayList<>();
		for (Row row : dataRows("snippets.xlsx", "Snippets")) {
			if ("Snippet Name".equals(text(row, 0))) {
				listed.add(text(row, 2));
			}
		}
		List<String> expected = new ArrayList<>(Arrays.asList(names));
		expected.sort(null);
		assertEquals(expected, listed);
	}

	public void testMalformedFileInDirectoryIsSkipped() throws Exception {
		Path dir = Files.createDirectory(tempDirPath.resolve("docs"));
		for (int i = 0; i < 2; i++) {
			Files.write(dir.resolve("good" + i + ".spdx.json"),
					MAPPER.writeValueAsBytes(createDoc(i, "LicenseRef-L", "Some license text")));
		}
		Files.write(dir.resolve("bad.rdf.xml"), "not rdf".getBytes(StandardCharsets.UTF_8));
		assertEquals(ExitCode.SUCCESS, CompareSpdxDocs.run(compareArgs("dir.xlsx", Arrays.asList(dir.toString()))));
	}

	public void testMalformedFileArgumentIsAnError() throws Exception {
		Path bad = tempDirPath.resolve("bad.rdf.xml");
		Files.write(bad, "not rdf".getBytes(StandardCharsets.UTF_8));
		assertEquals(ExitCode.ERROR, CompareSpdxDocs.run(compareArgs("bad.xlsx", Arrays.asList(bad.toString()))));
	}

	public void testNormalizeDocNamesSingleDocument() {
		assertEquals(Arrays.asList("doc.spdx.json"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("dir/sub/doc.spdx.json")));
		assertEquals(Arrays.asList("doc.spdx.json"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("doc.spdx.json")));
	}

	public void testNormalizeDocNamesIdenticalPaths() {
		assertEquals(Arrays.asList("doc.json", "doc.json"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("dir/doc.json", "dir/doc.json")));
	}

	public void testNormalizeDocNamesRemovesCommonDirectory() {
		assertEquals(Arrays.asList("a/x.json", "b/y.json"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("/tmp/run/a/x.json", "/tmp/run/b/y.json")));
		assertEquals(Arrays.asList("a/x.json", "b/y.json"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("C:\\run\\a\\x.json", "C:\\run\\b\\y.json")));
	}

	public void testNormalizeDocNamesKeepsWholeFileNames() {
		assertEquals(Arrays.asList("doc-1.json", "doc-2.json"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("doc-1.json", "doc-2.json")));
		assertEquals(Arrays.asList("doc1.spdx.json", "doc2.spdx"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("dir/doc1.spdx.json", "dir/doc2.spdx")));
	}

	public void testNormalizeDocNamesOnePathIsPrefixOfAnother() {
		assertEquals(Arrays.asList("b", "bc"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("a/b", "a/bc")));
		assertEquals(Arrays.asList("bc", "b"),
				CompareSpdxDocs.normalizeDocNames(Arrays.asList("a/bc", "a/b")));
	}

	public void testNormalizeDocNamesNoDocuments() {
		assertTrue(CompareSpdxDocs.normalizeDocNames(new ArrayList<String>()).isEmpty());
	}
}
