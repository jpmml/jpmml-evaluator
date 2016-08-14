/*
 * Copyright (c) 2015 Villu Ruusmann
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import com.google.common.io.ByteStreams;
import org.dmg.pmml.PMML;
import org.jpmml.model.visitors.LocatorTransformer;

abstract
public class IntegrationTestBatch extends ArchiveBatch {

	private Evaluator evaluator = null;


	public IntegrationTestBatch(String name, String dataset){
		super(name, dataset);
	}

	abstract
	public IntegrationTest getIntegrationTest();

	@Override
	public InputStream open(String path){
		IntegrationTest integrationTest = getIntegrationTest();

		Class<? extends IntegrationTest> clazz = integrationTest.getClass();

		return clazz.getResourceAsStream(path);
	}

	@Override
	public PMML getPMML() throws Exception {
		PMML pmml = super.getPMML();

		LocatorTransformer locatorTransformer = new LocatorTransformer();
		locatorTransformer.applyTo(pmml);

		return pmml;
	}

	@Override
	public Evaluator getEvaluator() throws Exception {

		if(this.evaluator == null){
			Evaluator evaluator =  super.getEvaluator();

			ensureSerializability(evaluator);

			this.evaluator = evaluator;
		}

		return this.evaluator;
	}

	@Override
	public void close() throws Exception {

		if(this.evaluator != null){

			try {
				ensureSerializability(this.evaluator);
			} finally {
				this.evaluator = null;
			}
		}
	}

	static
	private void ensureSerializability(Evaluator evaluator) throws IOException {

		try(ObjectOutputStream oos = new ObjectOutputStream(ByteStreams.nullOutputStream())){
			oos.writeObject(evaluator);
		}
	}
}
