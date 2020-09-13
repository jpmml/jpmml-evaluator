/*
 * Copyright (c) 2020 Villu Ruusmann
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
package org.jpmml.evaluator.kryo;

import java.io.Serializable;

import com.esotericsoftware.kryo.Kryo;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableTableSerializer;
import org.jpmml.evaluator.ModelEvaluator;

public class KryoUtil {

	private KryoUtil(){
	}

	static
	public void init(Kryo kryo){
		org.jpmml.model.kryo.KryoUtil.init(kryo);
	}

	static
	public void register(Kryo kryo){
		org.jpmml.model.kryo.KryoUtil.register(kryo);

		// java.util.*
		UnmodifiableCollectionsSerializer.registerSerializers(kryo);

		// com.google.common.collect.*
		ImmutableListSerializer.registerSerializers(kryo);
		ImmutableMapSerializer.registerSerializers(kryo);
		ImmutableSetSerializer.registerSerializers(kryo);
		ImmutableMultimapSerializer.registerSerializers(kryo);
		ImmutableTableSerializer.registerSerializers(kryo);

		kryo.addDefaultSerializer(ModelEvaluator.class, ModelEvaluatorSerializer.class);
	}

	static
	public <S extends Serializable> S clone(Kryo kryo, S object){
		return org.jpmml.model.kryo.KryoUtil.clone(kryo, object);
	}
}