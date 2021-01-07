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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.google.common.primitives.Longs;

public class SecondsSinceDate extends ComplexPeriod<SecondsSinceDate> {

	private long seconds = 0;


	public SecondsSinceDate(LocalDate epoch, LocalDateTime dateTime){
		this(epoch, ChronoUnit.SECONDS.between(epoch.atStartOfDay(), dateTime));
	}

	public SecondsSinceDate(LocalDate epoch, long seconds){
		super(epoch);

		setSeconds(seconds);
	}

	@Override
	public long longValue(){
		return getSeconds();
	}

	@Override
	public int compareTo(SecondsSinceDate that){

		if(!(this.getEpoch()).equals(that.getEpoch())){
			throw new ClassCastException();
		}

		return Long.compare(this.getSeconds(), that.getSeconds());
	}

	@Override
	public int hashCode(){
		return (31 * getEpoch().hashCode()) + Longs.hashCode(getSeconds());
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof SecondsSinceDate){
			SecondsSinceDate that = (SecondsSinceDate)object;

			return (this.getEpoch()).equals(that.getEpoch()) && (this.getSeconds() == that.getSeconds());
		}

		return false;
	}

	public long getSeconds(){
		return this.seconds;
	}

	private void setSeconds(long seconds){
		this.seconds = seconds;
	}
}