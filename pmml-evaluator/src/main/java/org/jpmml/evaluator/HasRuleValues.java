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

import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import org.dmg.pmml.AssociationRule;
import org.dmg.pmml.FeatureType;
import org.dmg.pmml.Item;
import org.dmg.pmml.Itemset;
import org.dmg.pmml.OutputField;

/**
 * @see FeatureType#RULE_VALUE
 */
public interface HasRuleValues extends ResultFeature {

	Map<String, Item> getItems();

	Map<String, Itemset> getItemsets();

	BiMap<String, AssociationRule> getAssociationRuleRegistry();

	List<AssociationRule> getRuleValues(OutputField.Algorithm algorithm);
}