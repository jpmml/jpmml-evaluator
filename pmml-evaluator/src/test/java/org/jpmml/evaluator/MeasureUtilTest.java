/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.dmg.pmml.BinarySimilarity;
import org.dmg.pmml.ComparisonMeasure;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Jaccard;
import org.dmg.pmml.SimpleMatching;
import org.dmg.pmml.Tanimoto;
import org.dmg.pmml.clustering.ClusteringField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MeasureUtilTest {

	@Test
	public void evaluateSimilarity(){
		ComparisonMeasure comparisonMeasure = new ComparisonMeasure(ComparisonMeasure.Kind.SIMILARITY);

		List<ClusteringField> clusteringFields = createClusteringFields("one", "two", "three", "four");

		BitSet flags = createFlags(Arrays.asList(0, 0, 1, 1));
		BitSet referenceFlags = createFlags(Arrays.asList(0, 1, 0, 1));

		comparisonMeasure.setMeasure(new SimpleMatching());

		assertEquals((2d / 4d), MeasureUtil.evaluateSimilarity(comparisonMeasure, clusteringFields, flags, referenceFlags), 1e-8);

		comparisonMeasure.setMeasure(new Jaccard());

		assertEquals((1d / 3d), MeasureUtil.evaluateSimilarity(comparisonMeasure, clusteringFields, flags, referenceFlags), 1e-8);

		comparisonMeasure.setMeasure(new Tanimoto());

		assertEquals((2d / (1d + 2 * 2d + 1d)), MeasureUtil.evaluateSimilarity(comparisonMeasure, clusteringFields, flags, referenceFlags), 1e-8);

		comparisonMeasure.setMeasure(new BinarySimilarity(0.5d, 0.5d, 0.5d, 0.5d, 1d, 1d, 1d, 1d));

		assertEquals((2d / 4d), MeasureUtil.evaluateSimilarity(comparisonMeasure, clusteringFields, flags, referenceFlags), 1e-8);
	}

	static
	private List<ClusteringField> createClusteringFields(String... names){
		List<ClusteringField> result = new ArrayList<>(names.length);

		for(String name : names){
			ClusteringField clusteringField = new ClusteringField(FieldName.create(name));

			result.add(clusteringField);
		}

		return result;
	}

	static
	private BitSet createFlags(List<Integer> values){
		return MeasureUtil.toBitSet(FieldValueUtil.createAll(null, null, values));
	}
}