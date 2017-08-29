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
import java.lang.reflect.Method;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Function;
import org.dmg.pmml.PMML;
import org.jpmml.model.PMMLUtil;

abstract
public class Example {

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true
	)
	@ParameterOrder (
		value = Integer.MAX_VALUE
	)
	private boolean help = false;


	abstract
	public void execute() throws Exception;

	static
	public void execute(Class<? extends Example> clazz, String... args) throws Exception {
		Example example = clazz.newInstance();

		JCommander commander = new JCommander(example);
		commander.setProgramName(clazz.getName());
		commander.setParameterDescriptionComparator(new ParameterOrderComparator());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			commander.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(example.help){
			StringBuilder sb = new StringBuilder();

			commander.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		example.execute();
	}

	static
	public PMML readPMML(File file) throws Exception {

		try(InputStream is = new FileInputStream(file)){
			return PMMLUtil.unmarshal(is);
		}
	}

	static
	public void writePMML(PMML pmml, File file) throws Exception {

		try(OutputStream os = new FileOutputStream(file)){
			PMMLUtil.marshal(pmml, os);
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

	static
	public Object newInstance(Class<?> clazz) throws ReflectiveOperationException {
		Method newInstanceMethod = clazz.getDeclaredMethod("newInstance");

		return newInstanceMethod.invoke(null);
	}

	public static final Function<String, String> CSV_PARSER = new Function<String, String>(){

		@Override
		public String apply(String string){

			if(("").equals(string) || ("N/A").equals(string) || ("NA").equals(string)){
				return null;
			}

			// Remove leading and trailing quotation marks
			string = stripQuotes(string, '\"');
			string = stripQuotes(string, '\"');

			// Standardize European-style decimal marks (',') to US-style decimal marks ('.')
			if(string.indexOf(',') > -1){
				String usString = string.replace(',', '.');

				try {
					Double.parseDouble(usString);

					string = usString;
				} catch(NumberFormatException nfe){
					// Ignored
				}
			}

			return string;
		}

		private String stripQuotes(String string, char quoteChar){

			if(string.length() > 1 && ((string.charAt(0) == quoteChar) && (string.charAt(string.length() - 1) == quoteChar))){
				return string.substring(1, string.length() - 1);
			}

			return string;
		}
	};

	public static final Function<Object, String> CSV_FORMATTER = new Function<Object, String>(){

		@Override
		public String apply(Object object){
			object = EvaluatorUtil.decode(object);

			if(object == null){
				return "N/A";
			}

			return object.toString();
		}
	};
}