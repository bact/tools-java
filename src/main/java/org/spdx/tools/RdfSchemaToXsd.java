/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
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
package org.spdx.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.spdx.tools.schema.OwlToXsd;
import org.spdx.tools.schema.SchemaException;

/**
 * Convert an RDF OWL document to an XML Schema
 * @author Gary O'Neall
 */
public class RdfSchemaToXsd {

	/**
	 * @param args arg[0] RDF Schema file path; arg[1] output file path
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err
					.println("Invalid number of arguments");
			usage();
			return;
		}
		File fromFile = new File(args[0]);
		if (!fromFile.exists()) {
			System.err
				.println("Input file "+args[0]+" does not exist.");
			usage();
			return;
		}
		File toFile = new File(args[1]);
		if (toFile.exists()) {
			System.err.println("Output file "+args[1]+" already exists.");
			usage();
			return;
		}
		OntModel model = null;
		try (InputStream is = new FileInputStream(fromFile)) {
			model = OntModelFactory.createModel(OntSpecification.OWL2_DL_MEM);
			model.read(is, "RDF/XML");
		} catch (FileNotFoundException e) {
			System.err.println("File not found for "+fromFile.getName());
			return;
		} catch (IOException e) {
			System.err.println("Error closing input file stream: "+e.getMessage());
		}
		try {
			OwlToXsd owlToXsd = new OwlToXsd(model);
			XmlSchema xmlSchema = owlToXsd.convertToXsd();
			try (OutputStream os = new FileOutputStream(toFile)) {
				xmlSchema.write(os);
			} catch (IOException e) {
				System.err.println("I/O error: "+e.getMessage());
				return;
			}
		} catch (XmlSchemaSerializerException e1) {
			System.err.println("Error generating XSD schema: "+e1.getMessage());
		} catch (SchemaException e1) {
			System.err.println("Error generating XSD schema: "+e1.getMessage());
		}
		

	}

	public static void usage() {
		System.out.println("Usage:");
		System.out.println("RdfSchemaToXsd rdfSchemaFile xsdFile");
		System.out.println("\trdfSchemaFile RDF schema file in RDF/XML format");
		System.out.println("\txsdFile output XML Schema");
	}

}
