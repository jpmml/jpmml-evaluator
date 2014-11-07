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

import org.joda.time.Chronology;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeUtils;
import org.joda.time.DurationField;
import org.joda.time.DurationFieldType;
import org.joda.time.Seconds;
import org.joda.time.field.FieldUtils;
import org.joda.time.field.PreciseDurationDateTimeField;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class SecondsSinceMidnight extends SimplePeriod<SecondsSinceMidnight> {

	private Seconds seconds = null;


	public SecondsSinceMidnight(Seconds seconds){
		setSeconds(seconds);
	}

	@Override
	public int intValue(){
		return getSeconds().getSeconds();
	}

	@Override
	public int compareTo(SecondsSinceMidnight that){
		return (this.getSeconds()).compareTo(that.getSeconds());
	}

	@Override
	public int hashCode(){
		return getSeconds().hashCode();
	}

	@Override
	public boolean equals(Object object){

		if(object instanceof SecondsSinceMidnight){
			SecondsSinceMidnight that = (SecondsSinceMidnight)object;

			return (this.getSeconds()).equals(that.getSeconds());
		}

		return false;
	}

	public Seconds getSeconds(){
		return this.seconds;
	}

	private void setSeconds(Seconds seconds){
		this.seconds = seconds;
	}

	static
	public DateTimeFormatter getFormat(){

		if(SecondsSinceMidnight.format == null){
			SecondsSinceMidnight.format = createFormat();
		}

		return SecondsSinceMidnight.format;
	}

	static
	private DateTimeFormatter createFormat(){
		DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
			.appendSignedDecimal(HoursOfEpochFieldType.getInstance(), 1, 4)
			.appendLiteral(':')
			.appendFixedDecimal(DateTimeFieldType.minuteOfHour(), 2)
			.appendLiteral(':')
			.appendFixedDecimal(DateTimeFieldType.secondOfMinute(), 2);

		return builder.toFormatter();
	}

	private static DateTimeFormatter format = null;

	static
	private class HoursOfEpochFieldType extends DateTimeFieldType {

		private HoursOfEpochFieldType(){
			super("hoursOfEpoch");
		}

		@Override
		public DurationFieldType getDurationType(){
			return DurationFieldType.hours();
		}

		@Override
		public DurationFieldType getRangeDurationType(){
			return null;
		}

		@Override
		public DateTimeField getField(Chronology chronology){
			chronology = DateTimeUtils.getChronology(chronology);

			return new PreciseDurationDateTimeField(this, chronology.hours()){

				@Override
				public int get(long millis){
					long hours = (millis / HoursOfEpochFieldType.millisInHour);

					return FieldUtils.safeToInt(hours);
				}

				@Override
				public DurationField getRangeDurationField(){
					return null;
				}

				@Override
				public int getMinimumValue(){
					return 0;
				}

				@Override
				public int getMaximumValue(){
					return Integer.MAX_VALUE;
				}
			};
		}

		@Override
		public int hashCode(){
			return getName().hashCode();
		}

		@Override
		public boolean equals(Object object){

			if(object instanceof HoursOfEpochFieldType){
				HoursOfEpochFieldType that = (HoursOfEpochFieldType)object;

				return (this.getName()).equals(that.getName());
			}

			return false;
		}

		static
		public HoursOfEpochFieldType getInstance(){

			if(HoursOfEpochFieldType.instance == null){
				HoursOfEpochFieldType.instance = new HoursOfEpochFieldType();
			}

			return HoursOfEpochFieldType.instance;
		}

		private static HoursOfEpochFieldType instance;

		private static final long millisInHour = (60L * 60L * 1000L);
	}
}