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

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

public class SecondsSinceDate extends ComplexPeriod<SecondsSinceDate> {

	private Seconds seconds = null;


	public SecondsSinceDate(int year, LocalDateTime dateTime){
		this(new LocalDate(year, 1, 1), dateTime);
	}

	public SecondsSinceDate(LocalDate epoch, LocalDateTime dateTime){
		this(epoch, Seconds.secondsBetween(TypeUtil.toMidnight(epoch), dateTime));
	}

	public SecondsSinceDate(LocalDate epoch, Seconds seconds){
		super(epoch);

		setSeconds(seconds);
	}

	@Override
	public int intValue(){
		return getSeconds().getSeconds();
	}

	@Override
	public int compareTo(SecondsSinceDate that){

		if(!(this.getEpoch()).equals(that.getEpoch())){
			throw new ClassCastException();
		}

		return (this.getSeconds()).compareTo(that.getSeconds());
	}

	@Override
	public int hashCode(){
		return (31 * getEpoch().hashCode()) + getSeconds().hashCode();
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof SecondsSinceDate){
			SecondsSinceDate that = (SecondsSinceDate)object;

			return (this.getEpoch()).equals(that.getEpoch()) && (this.getSeconds()).equals(that.getSeconds());
		}

		return false;
	}

	public Seconds getSeconds(){
		return this.seconds;
	}

	private void setSeconds(Seconds seconds){
		this.seconds = seconds;
	}
}