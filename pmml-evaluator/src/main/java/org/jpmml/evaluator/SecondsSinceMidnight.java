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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

import com.google.common.primitives.Longs;

public class SecondsSinceMidnight extends SimplePeriod<SecondsSinceMidnight> {

	private long seconds = 0;


	public SecondsSinceMidnight(long seconds){
		setSeconds(seconds);
	}

	@Override
	public long longValue(){
		return getSeconds();
	}

	@Override
	public int compareTo(SecondsSinceMidnight that){
		return Long.compare(this.getSeconds(), that.getSeconds());
	}

	@Override
	public int hashCode(){
		return Longs.hashCode(getSeconds());
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof SecondsSinceMidnight){
			SecondsSinceMidnight that = (SecondsSinceMidnight)object;

			return (this.getSeconds() == that.getSeconds());
		}

		return false;
	}

	private SecondsSinceMidnight toNegative(){
		setSeconds(-1 * getSeconds());

		return this;
	}

	public long getSeconds(){
		return this.seconds;
	}

	private void setSeconds(long seconds){
		this.seconds = seconds;
	}

	static
	public SecondsSinceMidnight parse(String string){
		DateTimeFormatter formatter = SecondsSinceMidnight.FORMATTER;

		if(string.startsWith("-")){
			SecondsSinceMidnight period = formatter.parse(string.substring(1), SecondsSinceMidnight.QUERY);

			return period.toNegative();
		} else

		{
			SecondsSinceMidnight period = formatter.parse(string, SecondsSinceMidnight.QUERY);

			return period;
		}
	}

	static
	private DateTimeFormatter createFormatter(){
		DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
			.appendValue(SecondsSinceMidnight.HOURS_OF_EPOCH, 1, 4, SignStyle.NOT_NEGATIVE)
			.appendLiteral(':')
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral(':')
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2);

		return builder.toFormatter();
	}

	private static final TemporalField HOURS_OF_EPOCH = new TemporalField(){

		private ValueRange range = ValueRange.of(0, Long.MAX_VALUE);


		@Override
		public TemporalUnit getBaseUnit(){
			return ChronoUnit.HOURS;
		}

		@Override
		public TemporalUnit getRangeUnit(){
			return ChronoUnit.FOREVER;
		}

		@Override
		public ValueRange range(){
			return this.range;
		}

		@Override
		public boolean isDateBased(){
			return false;
		}

		@Override
		public boolean isTimeBased(){
			return false;
		}

		@Override
		public boolean isSupportedBy(TemporalAccessor temporal){
			return temporal.isSupported(this);
		}

		@Override
		public ValueRange rangeRefinedBy(TemporalAccessor temporal){
			return temporal.range(this);
		}

		@Override
		public long getFrom(TemporalAccessor temporal){
			return temporal.getLong(this);
		}

		@Override
		public <R extends Temporal> R adjustInto(R temporal, long value){
			return (R)temporal.with(this, value);
		}

		@Override
		public String toString(){
			return "HoursOfEpoch";
		}
	};

	private static final DateTimeFormatter FORMATTER = createFormatter();

	private static final TemporalQuery<SecondsSinceMidnight> QUERY = new TemporalQuery<SecondsSinceMidnight>(){

		@Override
		public SecondsSinceMidnight queryFrom(TemporalAccessor temporal){
			long hoursOfEpoch = temporal.getLong(SecondsSinceMidnight.HOURS_OF_EPOCH);
			long minutesOfHour = temporal.getLong(ChronoField.MINUTE_OF_HOUR);
			long secondsOfMinute = temporal.getLong(ChronoField.SECOND_OF_MINUTE);

			long seconds = (hoursOfEpoch * 60 * 60) + (minutesOfHour * 60) + secondsOfMinute;

			return new SecondsSinceMidnight(seconds);
		}
	};
}