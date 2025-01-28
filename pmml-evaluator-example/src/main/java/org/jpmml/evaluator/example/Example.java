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
package org.jpmml.evaluator.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.TableReader;
import org.jpmml.evaluator.TableWriter;
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
		Constructor<? extends Example> constructor = clazz.getDeclaredConstructor();

		Example example = constructor.newInstance();

		JCommander commander = new JCommander(example);
		commander.setProgramName(clazz.getName());
		commander.setParameterDescriptionComparator(new ParameterOrderComparator());

		IUsageFormatter usageFormatter = new DefaultUsageFormatter(commander);

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			usageFormatter.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(example.help){
			StringBuilder sb = new StringBuilder();

			usageFormatter.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		example.execute();
	}

	static
	public PMML readPMML(File file) throws Exception {
		return readPMML(file, false);
	}

	static
	public PMML readPMML(File file, boolean acceptServiceJar) throws Exception {

		if(acceptServiceJar){

			if(isServiceJar(file, PMML.class)){
				URL url = (file.toURI()).toURL();

				return PMMLUtil.load(url);
			}
		}

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
	public Table readTable(File file, String separator) throws IOException {
		CsvReader.CsvReaderBuilder csvReaderBuilder = CsvReader.builder()
			.ignoreDifferentFieldCount(false)
			.fieldSeparator(separator.charAt(0));

		TableReader tableReader = new TableReader(csvReaderBuilder);

		try(InputStream is = new FileInputStream(file)){
			return tableReader.read(is);
		}
	}

	static
	public void writeTable(Table table, File file, String separator) throws IOException {
		CsvWriter.CsvWriterBuilder csvWriterBuilder = CsvWriter.builder()
			.lineDelimiter(LineDelimiter.PLATFORM)
			.fieldSeparator(separator.charAt(0));

		TableWriter tableWriter = new TableWriter(csvWriterBuilder);

		try(OutputStream os = new FileOutputStream(file)){
			tableWriter.write(table, os);
		}
	}

	static
	public Object newInstance(String name) throws ReflectiveOperationException {
		Class<?> clazz = Class.forName(name);

		Method newInstanceMethod = clazz.getDeclaredMethod("newInstance");

		return newInstanceMethod.invoke(null);
	}

	static
	public Function<String, String> createCellParser(Collection<String> missingValues){
		Function<String, String> function = new Function<String, String>(){

			@Override
			public String apply(String string){

				if(missingValues != null && missingValues.contains(string)){
					return null;
				}

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
		};

		return function;
	}

	static
	public Function<Object, String> createCellFormatter(String missingValue){
		Function<Object, String> function = new Function<Object, String>(){

			@Override
			public String apply(Object object){
				object = EvaluatorUtil.decode(object);

				if(object == null){
					return missingValue;
				}

				return object.toString();
			}
 		};

 		return function;
	}

	static
	private boolean isServiceJar(File file, Class<?> clazz){

		try(ZipFile zipFile = new ZipFile(file)){
			ZipEntry serviceZipEntry = zipFile.getEntry("META-INF/services/" + clazz.getName());

			return (serviceZipEntry != null);
		} catch(IOException ioe){
			return false;
		}
	}
}