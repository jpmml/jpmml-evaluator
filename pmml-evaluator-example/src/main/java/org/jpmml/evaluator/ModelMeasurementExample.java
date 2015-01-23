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
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.Source;

import com.beust.jcommander.Parameter;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.github.jamm.MemoryMeter;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;

public class ModelMeasurementExample extends Example {

	@Parameter (
		names = {"--model"},
		description = "PMML file",
		required = true
	)
	private File model = null;

	@Parameter (
		names = {"--visitor-classes"},
		description = "List of Visitor class names"
	)
	private List<String> visitorClasses = null;


	static
	public void main(String... args) throws Exception {
		execute(ModelMeasurementExample.class, args);
	}

	@Override
	public void execute() throws Exception {
		PMML pmml;

		InputStream is = new FileInputStream(this.model);

		try {
			Source source = ImportFilter.apply(new InputSource(is));

			long begin = System.currentTimeMillis();

			pmml = JAXBUtil.unmarshalPMML(source);

			long end = System.currentTimeMillis();

			System.out.println("Loaded the PMML object in " + (end - begin) + " ms.");
		} finally {
			is.close();
		}

		printSummary(pmml);

		List<String> visitorClasses = (this.visitorClasses != null ? this.visitorClasses : Collections.<String>emptyList());
		for(String visitorClass : visitorClasses){
			Class<?> clazz = Class.forName(visitorClass);

			long begin = System.currentTimeMillis();

			Visitor visitor = (Visitor)clazz.newInstance();
			visitor.applyTo(pmml);

			long end = System.currentTimeMillis();

			System.out.println("Applied Visitor class " + clazz.getName() + " in " + (end - begin) + " ms.");

			printSummary(pmml);
		}
	}

	private void printSummary(PMML pmml){
		MemoryMeter meter = new MemoryMeter();

		long size = meter.measure(pmml);
		System.out.println("The size of the PMML object: " + size + " bytes");

		long deepSize = meter.measureDeep(pmml);
		System.out.println("The size of the PMML object with child objects: " + deepSize + " bytes");

		long numberOfChildren = meter.countChildren(pmml);
		System.out.println("The number of child object references: " + numberOfChildren);
	}
}