/*
 * Copyright (c) 2013 Villu Ruusmann
 *
 * This file is part of JPMML-Evaluator
 *
 * JPMML-Evaluator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-Evaluator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.evaluator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.PMML;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;

abstract
public class Example {

	abstract
	public void execute() throws Exception;

	static
	public void execute(Class<? extends Example> clazz, String... args) throws Exception {
		Example example = clazz.newInstance();

		JCommander commander = new JCommander(example);
		commander.setProgramName(clazz.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			commander.usage();

			System.exit(-1);
		}

		example.execute();
	}

	static
	public PMML readPMML(File file) throws Exception {

		try(InputStream is = new FileInputStream(file)){
			Source source = ImportFilter.apply(new InputSource(is));

			return JAXBUtil.unmarshalPMML(source);
		}
	}

	static
	public void writePMML(PMML pmml, File file) throws Exception {

		try(OutputStream os = new FileOutputStream(file)){
			Result result = new StreamResult(os);

			JAXBUtil.marshalPMML(pmml, result);
		}
	}

	static
	public CsvUtil.Table readTable(File file, String separator) throws IOException {

		try(InputStream is = new FileInputStream(file)){
			return CsvUtil.readTable(is, separator);
		}
	}

	static
	public void writeTable(CsvUtil.Table table, File file) throws IOException {

		try(OutputStream os = new FileOutputStream(file)){
			CsvUtil.writeTable(table, os);
		}
	}
}