/**
 * SPDX-FileCopyrightText: Copyright (c) 2013 Source Auditor Inc.
 * SPDX-FileCopyrightText: Copyright (c) 2013 Black Duck Software Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.tools.compare;

import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxCreatorInformation;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Worksheet containing creator level information
 * Column 1 describes if the creator is the same or different
 * Columns 2 through N are for creators in each of the documents
 * @author Gary O'Neall
 */
public class CreatorSheet extends AbstractSheet {
	private static final int COL_WIDTH = 50;
	/**
	 * @param workbook
	 * @param sheetName
	 */
	public CreatorSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	/* (non-Javadoc)
	 * @see org.spdx.spdxspreadsheet.AbstractSheet#verify()
	 */
	@Override
	public String verify() {
		// Nothing to verify
		return null;
	}

	/**
	 * @param wb
	 * @param sheetName
	 */
	public static void create(Workbook wb, String sheetName) {
		int sheetNum = wb.getSheetIndex(sheetName);
		if (sheetNum >= 0) {
			wb.removeSheetAt(sheetNum);
		}
		Sheet sheet = wb.createSheet(sheetName);
		CellStyle headerStyle = AbstractSheet.createHeaderStyle(wb);
		CellStyle defaultStyle = AbstractSheet.createLeftWrapStyle(wb);
		Row row = sheet.createRow(0);
		for (int i = 0; i < MultiDocumentSpreadsheet.MAX_DOCUMENTS; i++) {
			sheet.setColumnWidth(i, COL_WIDTH*256);
			sheet.setDefaultColumnStyle(i, defaultStyle);
			Cell cell = row.createCell(i);
			cell.setCellStyle(headerStyle);
		}
	}

	/**
	 * @param comparer
	 * @param docNames
	 * @throws InvalidSPDXAnalysisException
	 */
	public void importCompareResults(SpdxComparer comparer, List<String> docNames) throws SpdxCompareException, InvalidSPDXAnalysisException {
		if (comparer.getNumSpdxDocs() != docNames.size()) {
			throw(new SpdxCompareException("Number of document names does not match the number of SPDX documents"));
		}
		this.clear();
		Row header = sheet.getRow(0);
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			Cell headerCell = header.getCell(i);
			headerCell.setCellValue(docNames.get(i));
			SpdxCreatorInformation creationInfo = comparer.getSpdxDoc(i).getCreationInfo();
			if (creationInfo != null) {
				String[] creators = creationInfo.getCreators().toArray(new String[creationInfo.getCreators().size()]);
				Arrays.sort(creators);
				for (int j = 0; j < creators.length; j++) {
					Cell creatorCell = null;
					while (j+1 > this.getNumDataRows()) {
						this.addRow();
					}
					creatorCell = sheet.getRow(j+1).createCell(i);
					creatorCell.setCellValue(creators[j]);
				}
			}
		}
	}

}
