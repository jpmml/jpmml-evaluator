/*
 * Copyright (c) 2017 Villu Ruusmann
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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TextUtil {

	private TextUtil(){
	}

	static
	public int frequency(List<String> text, List<String> term, int maxLevenshteinDistance, boolean bestHits, int maxFrequency){

		if(maxLevenshteinDistance < 0 || maxFrequency < 0){
			throw new IllegalArgumentException();
		}

		int textSize = text.size();
		int termSize = term.size();

		int frequency = 0;

		int levenshteinDistance = Integer.MAX_VALUE;

		text:
		for(int i = 0, max = (textSize - termSize); i <= max; i++){
			int termLevenshteinDistance = 0;

			term:
			for(int j = 0; j < termSize; j++){
				int threshold = (maxLevenshteinDistance - termLevenshteinDistance);

				if(threshold == 0){

					if(!(text.get(i + j)).equals(term.get(j))){
						continue text;
					}
				} else

				{
					int wordLevenshteinDistance = StringUtils.getLevenshteinDistance(text.get(i + j), term.get(j), threshold);

					if(wordLevenshteinDistance < 0){
						continue text;
					}

					termLevenshteinDistance += wordLevenshteinDistance;
				}
			}

			if(bestHits){

				if(termLevenshteinDistance < levenshteinDistance){
					frequency = 1;
				} else

				if(termLevenshteinDistance == levenshteinDistance){
					frequency++;
				} else

				{
					continue text;
				} // End if

				if((levenshteinDistance == 0) && (frequency >= maxFrequency)){
					return frequency;
				}
			} else

			{
				frequency++;

				if(frequency >= maxFrequency){
					return frequency;
				}
			}
		}

		return Math.min(maxFrequency, frequency);
	}
}