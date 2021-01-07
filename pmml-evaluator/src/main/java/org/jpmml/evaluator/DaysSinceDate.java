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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.google.common.primitives.Longs;

public class DaysSinceDate extends ComplexPeriod<DaysSinceDate> {

	private long days = 0;


	public DaysSinceDate(LocalDate epoch, LocalDate date){
		this(epoch, ChronoUnit.DAYS.between(epoch, date));
	}

	public DaysSinceDate(LocalDate epoch, long days){
		super(epoch);

		setDays(days);
	}

	@Override
	public long longValue(){
		return getDays();
	}

	@Override
	public int compareTo(DaysSinceDate that){

		if(!(this.getEpoch()).equals(that.getEpoch())){
			throw new ClassCastException();
		}

		return Long.compare(this.getDays(), that.getDays());
	}

	@Override
	public int hashCode(){
		return (31 * getEpoch().hashCode()) + Longs.hashCode(getDays());
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof DaysSinceDate){
			DaysSinceDate that = (DaysSinceDate)object;

			return (this.getEpoch()).equals(that.getEpoch()) && (this.getDays() == that.getDays());
		}

		return false;
	}

	public long getDays(){
		return this.days;
	}

	private void setDays(long days){
		this.days = days;
	}
}