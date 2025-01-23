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
package org.jpmml.evaluator.kryo;

import java.io.IOException;
import java.io.InputStream;

import com.esotericsoftware.kryo.Kryo;
import org.jpmml.model.DirectByteArrayOutputStream;
import org.jpmml.model.kryo.KryoSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

abstract
public class KryoSerializerTest {

	protected Kryo kryo = null;


	@BeforeEach
	public void setUp(){
		this.kryo = KryoUtil.createKryo();
	}

	@AfterEach
	public void tearDown(){
		this.kryo = null;
	}

	static
	protected <E> E checkedCloneRaw(KryoSerializer kryoSerializer, E object) throws Exception {
		E clonedObject = cloneRaw(kryoSerializer, object);

		assertEquals(object, clonedObject);
		assertNotSame(object, clonedObject);

		return clonedObject;
	}

	static
	protected <E> E cloneRaw(KryoSerializer serializer, Object object) throws IOException {
		DirectByteArrayOutputStream buffer = new DirectByteArrayOutputStream(10 * 1024);

		serializer.serializeRaw(object, buffer);

		try(InputStream is = buffer.getInputStream()){
			@SuppressWarnings("unchecked")
			E clonedObject = (E)serializer.deserializeRaw(is);

			assertEquals(-1, is.read());

			return clonedObject;
		}
	}
}