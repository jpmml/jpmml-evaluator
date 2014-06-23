/*
 * Copyright (c) 2014 Villu Ruusmann
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

import java.io.InputStream;

import javax.xml.transform.Source;

import org.dmg.pmml.PMML;
import org.jpmml.manager.ModelManager;
import org.jpmml.manager.ModelManagerFactory;
import org.jpmml.manager.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;

public class PMMLUtil {

	private PMMLUtil(){
	}

	static
	public PMML loadPMML(InputStream is) throws Exception {
		Source source = ImportFilter.apply(new InputSource(is));

		return JAXBUtil.unmarshalPMML(source);
	}

	static
	public PMMLManager createPMMLManager(InputStream is) throws Exception {
		PMML pmml = loadPMML(is);

		return new PMMLManager(pmml);
	}

	static
	public ModelEvaluator<?> createModelEvaluator(InputStream is) throws Exception {
		return (ModelEvaluator<?>)createModelManager(is, ModelEvaluatorFactory.getInstance());
	}

	static
	public ModelManager<?> createModelManager(InputStream is, ModelManagerFactory modelManagerFactory) throws Exception {
		PMMLManager manager = createPMMLManager(is);

		return manager.getModelManager(null, modelManagerFactory);
	}
}