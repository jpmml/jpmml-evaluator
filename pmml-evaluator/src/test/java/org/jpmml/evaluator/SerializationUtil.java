/*
 * Copyright (c) 2025 Villu Ruusmann
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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jpmml.model.DirectByteArrayOutputStream;

public class SerializationUtil {

	private SerializationUtil(){
	}

	static
	public <E> E clone(E object) throws Exception {
		DirectByteArrayOutputStream buffer = new DirectByteArrayOutputStream(1024 * 1024);

		try(ObjectOutputStream objectOs = new ObjectOutputStream(buffer)){
			objectOs.writeObject(object);
		}

		try(ObjectInputStream objectIs = new ObjectInputStream(buffer.getInputStream())){
			return (E)objectIs.readObject();
		}
	}
}