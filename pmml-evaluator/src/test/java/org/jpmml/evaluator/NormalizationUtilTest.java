/*
 * Copyright (c) 2011 University of Tartu
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

import org.dmg.pmml.*;

import org.junit.*;

import static org.junit.Assert.*;

public class NormalizationUtilTest {

	private NormContinuous norm;

	@Before
	public void setUp(){
		norm = new NormContinuous(new FieldName("x"));
		norm.getLinearNorms().add(new LinearNorm(0.01, 0.0));
		norm.getLinearNorms().add(new LinearNorm(3.07897, 0.5));
		norm.getLinearNorms().add(new LinearNorm(11.44, 1.0));
	}

	@Test
	public void testNormalize(){
		assertEquals(0.00000, NormalizationUtil.normalize(norm, 0.01), 1e-5);
		assertEquals(0.19583, NormalizationUtil.normalize(norm, 1.212), 1e-5);
		assertEquals(0.50000, NormalizationUtil.normalize(norm, 3.07897), 1e-5);
		assertEquals(0.70458, NormalizationUtil.normalize(norm, 6.5), 1e-5);
		assertEquals(1.00000, NormalizationUtil.normalize(norm, 11.44), 1e-5);
	}

	@Test
	public void testNormalizeOutliers(){
		// as is method
		assertEquals(-0.16455, NormalizationUtil.normalize(norm, -1.0), 1e-5);
		assertEquals( 1.04544, NormalizationUtil.normalize(norm, 12.2), 1e-5);

		// as missing values method
		norm.setOutliers(OutlierTreatmentMethodType.AS_MISSING_VALUES);
		norm.setMapMissingTo(0.5);
		assertEquals(0.5, NormalizationUtil.normalize(norm, -1.0), 1e-5);
		assertEquals(0.5, NormalizationUtil.normalize(norm, 12.2), 1e-5);

		// as extreme values method
		norm.setOutliers(OutlierTreatmentMethodType.AS_EXTREME_VALUES);
		assertEquals(0.0, NormalizationUtil.normalize(norm, -1.0), 1e-5);
		assertEquals(1.0, NormalizationUtil.normalize(norm, 12.2), 1e-5);
	}

	@Test
	public void testDenormalize(){
		assertEquals(0.010, NormalizationUtil.denormalize(norm, 0.0), 1e-5);
		assertEquals(0.300, NormalizationUtil.denormalize(norm, 0.047247), 1e-5);
		assertEquals(7.123, NormalizationUtil.denormalize(norm, 0.741838), 1e-5);
		assertEquals(11.44, NormalizationUtil.denormalize(norm, 1.0), 1e-5);
	}
}