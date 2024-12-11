package org.jpmml.evaluator;

import java.util.List;

class TableUtil {

	private TableUtil(){
	}

	static
	<E> List<E> ensureSize(List<E> values, int size){

		while(values.size() < size){
			values.add(null);
		}

		return values;
	}
}