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

import org.apache.jena.ontapi.model.OntModel;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.spdx.tools.schema.AbstractOwlRdfConverter;
import org.spdx.tools.schema.OwlToXsd;
import org.spdx.tools.schema.SchemaException;

/**
 * Convert an RDF OWL document to an XML Schema
 * @author Gary O'Neall
 */
public class RdfSchemaToXsd {

	/**
	 * Main entry point, terminates the JVM with the exit status of {@link #run(String[])}
	 * @param args arg[0] RDF Schema file path; arg[1] output file path
	 */
	public static void main(String[] args) {
		System.exit(run(args));
	}

	/**
	 * Runs the command logic and reports results to standard out/error,
	 * without terminating the JVM - allows the logic to be unit tested.
	 * @param args arg[0] RDF Schema file path; arg[1] output file path
	 * @return process exit status, see {@link ExitCode}
	 */
	static int run(String[] args) {
		if (args == null || args.length != 2) {
			System.err.println("Invalid number of arguments");
			usage();
			return ExitCode.USAGE_ERROR;
		}
		File fromFile = new File(args[0]);
		if (!fromFile.exists()) {
			System.err.println("Input file "+args[0]+" does not exist.");
			usage();
			return ExitCode.ERROR;
		}
		File toFile = new File(args[1]);
		if (toFile.exists()) {
			System.err.println("Output file "+args[1]+" already exists.");
			usage();
			return ExitCode.ERROR;
		}
		OntModel model = null;
		try (InputStream is = new FileInputStream(fromFile)) {
			model = AbstractOwlRdfConverter.createOntModel();
			model.read(is, "RDF/XML");
		} catch (FileNotFoundException e) {
			System.err.println("File not found for "+fromFile.getName());
			return ExitCode.ERROR;
		} catch (IOException e) {
			System.err.println("Error closing input file stream: "+e.getMessage());
		} catch (RuntimeException e) {
			System.err.println("Unable to read ontology from file "+fromFile.getName()+": "+e.getMessage());
			return ExitCode.ERROR;
		}
		XmlSchema xmlSchema;
		try {
			xmlSchema = new OwlToXsd(model).convertToXsd();
		} catch (XmlSchemaSerializerException | SchemaException | RuntimeException e) {
			System.err.println("Error generating XSD schema: "+e.getMessage());
			return ExitCode.ERROR;
		}
		try (OutputStream os = new FileOutputStream(toFile)) {
			xmlSchema.write(os);
		} catch (IOException e) {
			System.err.println("I/O error: "+e.getMessage());
			return ExitCode.ERROR;
		}
		return ExitCode.SUCCESS;
	}

	public static void usage() {
		System.out.println("Usage:");
		System.out.println("RdfSchemaToXsd rdfSchemaFile xsdFile");
		System.out.println("\trdfSchemaFile RDF schema file in RDF/XML format");
		System.out.println("\txsdFile output XML Schema");
	}

}
