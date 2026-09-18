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
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ontapi.model.OntModel;
import org.spdx.tools.schema.AbstractOwlRdfConverter;
import org.spdx.tools.schema.OwlToJsonSchema;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert an RDF schema file containing SPDX property to a JSON schema file for all properties in the SPDX namespace
 * @author Gary O'Neall
 */
public class RdfSchemaToJsonSchema {

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
		ObjectNode root;
		try {
			root = new OwlToJsonSchema(model).convertToJsonSchema();
		} catch (RuntimeException e) {
			System.err.println("Error generating JSON schema: "+e.getMessage());
			return ExitCode.ERROR;
		}
		ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try (JsonGenerator jsonGenerator = jsonMapper.getFactory().createGenerator(toFile, JsonEncoding.UTF8)) {
			jsonMapper.writeTree(jsonGenerator.useDefaultPrettyPrinter(), root);
		} catch (JsonProcessingException e) {
			System.err.println("JSON error "+e.getMessage());
			return ExitCode.ERROR;
		} catch (IOException e) {
			System.err.println("I/O error: "+e.getMessage());
			return ExitCode.ERROR;
		}
		return ExitCode.SUCCESS;
	}

	public static void usage() {
		System.out.println("Usage:");
		System.out.println("RdfSchemaToJsonSchema rdfSchemaFile jsonSchemaFile");
		System.out.println("\trdfSchemaFile RDF schema file in RDF/XML format");
		System.out.println("\tjsonSchemaFile output JSON Schema file");
	}
}
