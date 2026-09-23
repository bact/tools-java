/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spdx.spreadsheetstore.SpreadsheetException;

import junit.framework.TestCase;

/**
 * Test cases for the helpers of the comparison sheets
 */
public class CompareSheetsTest extends TestCase {

	private static class Element {
		final Optional<String> name;
		final String id;

		Element(String name, String id) {
			this.name = Optional.ofNullable(name);
			this.id = id;
		}
	}

	private static int compare(Element a, Element b) {
		return DocumentRelationshipSheet.compareElements(a.name, a.id, b.name, b.id);
	}

	public void testElementOrderIsATotalOrder() {
		// named and unnamed elements, with equal names, equal IDs and different IDs for the same name
		List<Element> elements = new ArrayList<>();
		for (String name : new String[] {null, "a", "b"}) {
			for (String id : new String[] {"SPDXRef-1", "SPDXRef-2", "SPDXRef-3"}) {
				elements.add(new Element(name, id));
			}
		}
		for (Element a : elements) {
			assertEquals(0, compare(a, a));
			for (Element b : elements) {
				assertEquals(Integer.signum(compare(a, b)), -Integer.signum(compare(b, a)));
				for (Element c : elements) {
					if (compare(a, b) <= 0 && compare(b, c) <= 0) {
						assertTrue(compare(a, c) <= 0);
					}
				}
			}
		}
	}

	public void testElementOrder() {
		assertTrue(compare(new Element(null, "SPDXRef-9"), new Element("a", "SPDXRef-1")) < 0);
		assertTrue(compare(new Element("a", "SPDXRef-9"), new Element("b", "SPDXRef-1")) < 0);
		assertTrue(compare(new Element(null, "SPDXRef-1"), new Element(null, "SPDXRef-2")) < 0);
		// elements with the same name are aligned in the same row, whatever their IDs
		assertEquals(0, compare(new Element("a", "SPDXRef-1"), new Element("a", "SPDXRef-2")));
	}

	public void testVerificationSheetRowCount() throws SpreadsheetException {
		try (Workbook workbook = new XSSFWorkbook()) {
			VerificationSheet.create(workbook, "Verification");
			VerificationSheet sheet = new VerificationSheet(workbook, "Verification");
			assertEquals(0, sheet.getNumDataRows());
			List<List<String>> errors = new ArrayList<>();
			errors.add(Arrays.asList("first error", "second error", "third error"));
			errors.add(Arrays.asList("only error"));
			sheet.importVerificationErrors(errors, Arrays.asList("doc1", "doc2"));
			assertEquals(3, sheet.getNumDataRows());
			assertEquals("third error", sheet.getSheet().getRow(3).getCell(0).getStringCellValue());
			assertEquals("only error", sheet.getSheet().getRow(1).getCell(1).getStringCellValue());
			sheet.resizeRows();
			sheet.clear();
			assertEquals(0, sheet.getNumDataRows());
			assertNull(sheet.getSheet().getRow(1));
		} catch (java.io.IOException e) {
			throw new SpreadsheetException("unable to close the workbook: " + e.getMessage());
		}
	}
}
