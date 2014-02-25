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

import java.io.*;

abstract
public class ArchiveBatch implements Batch {

	private String name = null;

	private String dataset = null;


	public ArchiveBatch(String name, String dataset){
		setName(name);
		setDataset(dataset);
	}

	public InputStream open(String path){
		Class<?> clazz = getClass();

		return clazz.getResourceAsStream(path);
	}

	public InputStream getModel(){
		return open("/pmml/" + (getName() + getDataset()) + ".pmml");
	}

	public InputStream getInput(){
		return open("/csv/" + getDataset() + ".csv");
	}

	public InputStream getOutput(){
		return open("/csv/" + (getName() + getDataset()) + ".csv");
	}

	public String getName(){
		return this.name;
	}

	private void setName(String name){
		this.name = name;
	}

	public String getDataset(){
		return this.dataset;
	}

	private void setDataset(String dataset){
		this.dataset = dataset;
	}
}