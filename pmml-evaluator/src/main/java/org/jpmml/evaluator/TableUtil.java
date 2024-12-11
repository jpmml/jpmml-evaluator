package org.jpmml.evaluator;

import java.util.List;

class TableUtil {

	private TableUtil(){
	}

	static
	<E> E get(List<E> values, int index){

		if(index < values.size()){
			return values.get(index);
		}

		return null;
	}

	static
	<E> E set(List<E> values, int index, E value){

		if(index < values.size()){
			return values.set(index, value);
		} else

		{
			TableUtil.ensureSize(values, index);

			values.add(value);

			return null;
		}
	}

	static
	<E> List<E> ensureSize(List<E> values, int size){

		while(values.size() < size){
			values.add(null);
		}

		return values;
	}
}