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
package org.jpmml.evaluator.naive_bayes;

import java.util.Iterator;
import java.util.List;

import org.dmg.pmml.Extension;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.naive_bayes.BayesInput;
import org.dmg.pmml.naive_bayes.BayesInputs;
import org.dmg.pmml.naive_bayes.TargetValueStats;
import org.jpmml.evaluator.PMMLTransformer;
import org.jpmml.model.visitors.AbstractVisitor;

/**
 * <p>
 * A PMML transformer that extracts {@link BayesInput} elements from {@link Extension} elements.
 * </p>
 *
 * The support for continuous fields using the {@link TargetValueStats} element was officially introduced in PMML schema version 4.2.
 * However, it is possible to encounter this markup in older PMML schema version documents (most notably, produced by R's legacy "pmml" package),
 * where the "incompatible" BayesInput element is wrapped into an Extension element:
 * <pre>{@code
 * <BayesInputs>
 *   <BayesInput>
 *     <PairCounts/>
 *   </BayesInput>
 *   <Extension>
 *     <BayesInput>
 *       <TargetValueStats/>
 *     </BayesInput>
 *   </Extension>
 * </BayesInputs>
 * }</pre>
 */
public class ExtractBayesInputsTransformer implements PMMLTransformer<RuntimeException> {

	@Override
	public PMML apply(PMML pmml){
		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(BayesInputs bayesInputs){

				if(bayesInputs.hasExtensions()){
					List<Extension> extensions = bayesInputs.getExtensions();

					for(Iterator<Extension> it = extensions.iterator(); it.hasNext(); ){
						Extension extension = it.next();

						List<?> objects = extension.getContent();

						BayesInput bayesInput = extractBayesInput(objects);
						if(bayesInput != null){
							bayesInputs.addBayesInputs(bayesInput);

							if(objects.isEmpty()){
								it.remove();
							}
						}
					}
				}

				return super.visit(bayesInputs);
			}

			private BayesInput extractBayesInput(List<?> objects){

				for(Iterator<?> it = objects.iterator(); it.hasNext(); ){
					Object object = it.next();

					if(object instanceof BayesInput){
						BayesInput bayesInput = (BayesInput)object;

						it.remove();

						return bayesInput;
					}
				}

				return null;
			}
		};

		visitor.applyTo(pmml);

		return pmml;
	}
}