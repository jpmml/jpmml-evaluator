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
package org.jpmml.evaluator.tree;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.dmg.pmml.tree.Node;
import org.jpmml.evaluator.DoubleValue;
import org.jpmml.evaluator.ValueMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeScoreDistributionTest {

	@Test
	public void getProbability(){
		Node node = new Node()
			.setScore("ham");

		final
		BiMap<String, Node> entityRegistry = ImmutableBiMap.of("1", node);

		NodeScoreDistribution<Double> classification = new NodeScoreDistribution<Double>(new ValueMap<String, Double>(), node){

			@Override
			public BiMap<String, Node> getEntityRegistry(){
				return entityRegistry;
			}
		};

		classification.put("ham", new DoubleValue(0.75d));
		classification.put("spam", new DoubleValue(0.25d));

		assertEquals(ImmutableSet.of("ham", "spam"), classification.getCategories());

		assertEquals((Double)0.75d, classification.getProbability("ham"));
		assertEquals((Double)0.25d, classification.getProbability("spam"));
	}
}