/*
 * Copyright (c) 2022 Villu Ruusmann
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
package org.jpmml.evaluator.support_vector_machine;

import java.util.List;

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.support_vector_machine.PMMLAttributes;
import org.dmg.pmml.support_vector_machine.SupportVectorMachine;
import org.dmg.pmml.support_vector_machine.SupportVectorMachineModel;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.ReflectionUtil;
import org.jpmml.model.visitors.AbstractVisitor;

/**
 * <p>
 * A PMML transformer that ensures the availability of correct {@link SupportVectorMachineModel#getClassificationMethod()} attribute.
 * </p>
 *
 * Some PMML producer software (most notably, R's legacy "pmml" package) are known to omit this attribute
 * (defaults to the "OneAgainstAll" value) when encoding the SupportVectorMachineModel element following the layout of the "OneAgainstOne" value.
 */
public class EnsureClassificationMethodTransformer implements PMMLTransformer<RuntimeException> {

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(SupportVectorMachineModel supportVectorMachineModel){
				MiningFunction miningFunction = supportVectorMachineModel.requireMiningFunction();

				switch(miningFunction){
					case CLASSIFICATION:
						ensureClassificationMethod(supportVectorMachineModel);
						break;
					default:
						break;
				}

				return super.visit(supportVectorMachineModel);
			}

			private void ensureClassificationMethod(SupportVectorMachineModel supportVectorMachineModel){
				// Use the Java Reflection API for determining if the attribute is set or not
				SupportVectorMachineModel.ClassificationMethod classificationMethod = ReflectionUtil.getFieldValue(PMMLAttributes.SUPPORTVECTORMACHINEMODEL_CLASSIFICATIONMETHOD, supportVectorMachineModel);

				if(classificationMethod == null){
					classificationMethod = detectClassificationMethod(supportVectorMachineModel);

					supportVectorMachineModel.setClassificationMethod(classificationMethod);
				}
			}

			private SupportVectorMachineModel.ClassificationMethod detectClassificationMethod(SupportVectorMachineModel supportVectorMachineModel){
				List<SupportVectorMachine> supportVectorMachines = supportVectorMachineModel.requireSupportVectorMachines();

				Object alternateBinaryTargetCategory = supportVectorMachineModel.getAlternateBinaryTargetCategory();
				if(alternateBinaryTargetCategory != null){

					if(supportVectorMachines.size() == 1){
						SupportVectorMachine supportVectorMachine = supportVectorMachines.get(0);

						Object targetCategory = supportVectorMachine.getTargetCategory();
						if(targetCategory != null){
							return SupportVectorMachineModel.ClassificationMethod.ONE_AGAINST_ONE;
						}

						throw new InvalidElementException(supportVectorMachine);
					}

					throw new InvalidElementException(supportVectorMachineModel);
				}

				for(SupportVectorMachine supportVectorMachine : supportVectorMachines){
					Object targetCategory = supportVectorMachine.getTargetCategory();
					Object alternateTargetCategory = supportVectorMachine.getAlternateTargetCategory();

					if(targetCategory != null){

						if(alternateTargetCategory != null){
							return SupportVectorMachineModel.ClassificationMethod.ONE_AGAINST_ONE;
						}

						return SupportVectorMachineModel.ClassificationMethod.ONE_AGAINST_ALL;
					}

					throw new InvalidElementException(supportVectorMachine);
				}

				throw new InvalidElementException(supportVectorMachineModel);
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}
}