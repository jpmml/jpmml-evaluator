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

import org.joda.time.Days;
import org.joda.time.LocalDate;

public class DaysSinceDate extends ComplexPeriod<DaysSinceDate> {

	private Days days = null;


	public DaysSinceDate(int year, LocalDate date){
		this(new LocalDate(year, 1, 1), date);
	}

	public DaysSinceDate(LocalDate epoch, LocalDate date){
		this(epoch, Days.daysBetween(epoch, date));
	}

	public DaysSinceDate(LocalDate epoch, Days days){
		super(epoch);

		setDays(days);
	}

	@Override
	public int intValue(){
		return getDays().getDays();
	}

	@Override
	public int compareTo(DaysSinceDate that){

		if(!(this.getEpoch()).equals(that.getEpoch())){
			throw new ClassCastException();
		}

		return (this.getDays()).compareTo(that.getDays());
	}

	@Override
	public int hashCode(){
		return (31 * getEpoch().hashCode()) + getDays().hashCode();
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof DaysSinceDate){
			DaysSinceDate that = (DaysSinceDate)object;

			return (this.getEpoch()).equals(that.getEpoch()) && (this.getDays()).equals(that.getDays());
		}

		return false;
	}

	public Days getDays(){
		return this.days;
	}

	private void setDays(Days days){
		this.days = days;
	}
}