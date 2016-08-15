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
package org.jpmml.evaluator.visitors;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitable;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.association.AssociationModel;
import org.dmg.pmml.association.Itemset;
import org.dmg.pmml.clustering.ClusteringModel;
import org.dmg.pmml.neural_network.NeuralInputs;
import org.dmg.pmml.neural_network.NeuralLayer;
import org.dmg.pmml.neural_network.NeuralNetwork;
import org.dmg.pmml.neural_network.NeuralOutputs;
import org.dmg.pmml.support_vector_machine.Coefficients;
import org.dmg.pmml.support_vector_machine.SupportVectors;
import org.dmg.pmml.support_vector_machine.VectorDictionary;
import org.dmg.pmml.support_vector_machine.VectorFields;
import org.jpmml.evaluator.InvalidFeatureException;
import org.jpmml.model.ReflectionUtil;

/**
 * <p>
 * A Visitor that inspects a class model object for invalid features.
 * </p>
 *
 * @see FeatureInspector#applyTo(Visitable)
 * @see InvalidFeatureException
 */
public class InvalidFeatureInspector extends FeatureInspector<InvalidFeatureException> {

	@Override
	public VisitorAction visit(PMMLObject object){
		List<Field> fields = ReflectionUtil.getInstanceFields(object.getClass());

		for(Field field : fields){
			Object value = ReflectionUtil.getFieldValue(field, object);

			if(value instanceof List){
				List<?> collection = (List<?>)value;

				// The getter method may have initialized the field with an empty ArrayList instance
				if(collection.size() == 0){
					value = null;
				}
			} // End if

			// The field is set
			if(value != null){
				continue;
			}

			XmlElement element = field.getAnnotation(XmlElement.class);
			if(element != null && element.required()){
				report(new InvalidFeatureException(object, field));
			}

			XmlAttribute attribute = field.getAnnotation(XmlAttribute.class);
			if(attribute != null && attribute.required()){
				report(new InvalidFeatureException(object, field));
			}
		}

		return super.visit(object);
	}

	@Override
	public VisitorAction visit(final AssociationModel associationModel){
		check(new CollectionSize(associationModel){

			@Override
			public Integer getSize(){
				return associationModel.getNumberOfItems();
			}

			@Override
			public Collection<?> getCollection(){
				return associationModel.getItems();
			}

			@Override
			public boolean evaluate(int left, int right){
				// "The numberOfItems attribute may be greater than or equal to the number of items contained in the model"
				return (left >= right);
			}
		});

		check(new CollectionSize(associationModel){

			@Override
			public Integer getSize(){
				return associationModel.getNumberOfItemsets();
			}

			@Override
			public Collection<?> getCollection(){
				return associationModel.getItemsets();
			}
		});

		check(new CollectionSize(associationModel){

			@Override
			public Integer getSize(){
				return associationModel.getNumberOfRules();
			}

			@Override
			public Collection<?> getCollection(){
				return associationModel.getAssociationRules();
			}
		});

		return super.visit(associationModel);
	}

	@Override
	public VisitorAction visit(final ClusteringModel clusteringModel){
		check(new CollectionSize(clusteringModel){

			@Override
			public Integer getSize(){
				return clusteringModel.getNumberOfClusters();
			}

			@Override
			public Collection<?> getCollection(){
				return clusteringModel.getClusters();
			}
		});

		return super.visit(clusteringModel);
	}

	@Override
	public VisitorAction visit(final Coefficients coefficients){
		check(new CollectionSize(coefficients){

			@Override
			public Integer getSize(){
				return coefficients.getNumberOfCoefficients();
			}

			@Override
			public Collection<?> getCollection(){
				return coefficients.getCoefficients();
			}
		});

		return super.visit(coefficients);
	}

	@Override
	public VisitorAction visit(final DataDictionary dataDictionary){
		check(new CollectionSize(dataDictionary){

			@Override
			public Integer getSize(){
				return dataDictionary.getNumberOfFields();
			}

			@Override
			public Collection<?> getCollection(){
				return dataDictionary.getDataFields();
			}
		});

		return super.visit(dataDictionary);
	}

	@Override
	public VisitorAction visit(final Itemset itemset){
		check(new CollectionSize(itemset){

			@Override
			public Integer getSize(){
				return itemset.getNumberOfItems();
			}

			@Override
			public Collection<?> getCollection(){
				return itemset.getItemRefs();
			}
		});

		return super.visit(itemset);
	}

	@Override
	public VisitorAction visit(final NeuralInputs neuralInputs){
		check(new CollectionSize(neuralInputs){

			@Override
			public Integer getSize(){
				return neuralInputs.getNumberOfInputs();
			}

			@Override
			public Collection<?> getCollection(){
				return neuralInputs.getNeuralInputs();
			}
		});

		return super.visit(neuralInputs);
	}

	@Override
	public VisitorAction visit(final NeuralLayer neuralLayer){
		check(new CollectionSize(neuralLayer){

			@Override
			public Integer getSize(){
				return neuralLayer.getNumberOfNeurons();
			}

			@Override
			public Collection<?> getCollection(){
				return neuralLayer.getNeurons();
			}
		});

		return super.visit(neuralLayer);
	}

	@Override
	public VisitorAction visit(final NeuralNetwork neuralNetwork){
		check(new CollectionSize(neuralNetwork){

			@Override
			public Integer getSize(){
				return neuralNetwork.getNumberOfLayers();
			}

			@Override
			public Collection<?> getCollection(){
				return neuralNetwork.getNeuralLayers();
			}
		});

		return super.visit(neuralNetwork);
	}

	@Override
	public VisitorAction visit(final NeuralOutputs neuralOutputs){
		check(new CollectionSize(neuralOutputs){

			@Override
			public Integer getSize(){
				return neuralOutputs.getNumberOfOutputs();
			}

			@Override
			public Collection<?> getCollection(){
				return neuralOutputs.getNeuralOutputs();
			}
		});

		return super.visit(neuralOutputs);
	}

	@Override
	public VisitorAction visit(final SupportVectors supportVectors){
		check(new CollectionSize(supportVectors){

			@Override
			public Integer getSize(){
				return supportVectors.getNumberOfSupportVectors();
			}

			@Override
			public Collection<?> getCollection(){
				return supportVectors.getSupportVectors();
			}
		});

		return super.visit(supportVectors);
	}

	@Override
	public VisitorAction visit(final VectorDictionary vectorDictionary){
		check(new CollectionSize(vectorDictionary){

			@Override
			public Integer getSize(){
				return vectorDictionary.getNumberOfVectors();
			}

			@Override
			public Collection<?> getCollection(){
				return vectorDictionary.getVectorInstances();
			}
		});

		return super.visit(vectorDictionary);
	}

	@Override
	public VisitorAction visit(final VectorFields vectorFields){
		check(new CollectionSize(vectorFields){

			@Override
			public Integer getSize(){
				return vectorFields.getNumberOfFields();
			}

			@Override
			public Collection<?> getCollection(){
				return vectorFields.getContent();
			}
		});

		return super.visit(vectorFields);
	}

	private void check(Condition condition){
		boolean result = condition.evaluate();

		if(!result){
			PMMLObject object = condition.getObject();

			if(object != null){
				report(new InvalidFeatureException(object));
			} else

			{
				report(new InvalidFeatureException());
			}
		}
	}

	abstract
	private class Condition {

		private PMMLObject object = null;


		public Condition(PMMLObject object){
			setObject(object);
		}

		abstract
		public boolean evaluate();

		public PMMLObject getObject(){
			return this.object;
		}

		private void setObject(PMMLObject object){
			this.object = object;
		}
	}

	abstract
	private class CollectionSize extends Condition {

		public CollectionSize(PMMLObject object){
			super(object);
		}

		abstract
		public Integer getSize();

		abstract
		public Collection<?> getCollection();

		@Override
		public boolean evaluate(){
			Integer size = getSize();

			if(size != null){
				Collection<?> collection = getCollection();

				return evaluate(size.intValue(), collection.size());
			}

			return true;
		}

		public boolean evaluate(int left, int right){
			return (left == right);
		}
	}
}