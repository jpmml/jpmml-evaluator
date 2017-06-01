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
package org.jpmml.evaluator.visitors;

import java.util.List;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.Header;
import org.dmg.pmml.PMML;
import org.dmg.pmml.clustering.ClusteringModel;
import org.jpmml.evaluator.UnsupportedFeatureException;
import org.jpmml.schema.Version;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UnsupportedFeatureInspectorTest {

	@Test
	public void inspect(){
		ClusteringModel clusteringModel = new ClusteringModel()
			.setModelClass(ClusteringModel.ModelClass.DISTRIBUTION_BASED);

		PMML pmml = new PMML(Version.PMML_4_3.getVersion(), new Header(), new DataDictionary())
			.addModels(clusteringModel);

		UnsupportedFeatureInspector inspector = new UnsupportedFeatureInspector();

		try {
			inspector.applyTo(pmml);

			fail();
		} catch(UnsupportedFeatureException ufe){
			List<UnsupportedFeatureException> exceptions = inspector.getExceptions();

			assertEquals(1, exceptions.size());

			assertEquals("ClusteringModel@modelClass=distributionBased", ufe.getMessage());
		}
	}
}