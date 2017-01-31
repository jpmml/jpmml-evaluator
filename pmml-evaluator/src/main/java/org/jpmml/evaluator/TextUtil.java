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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Table;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Row;
import org.dmg.pmml.TableLocator;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndexNormalization;
import org.jpmml.model.ReflectionUtil;

public class TextUtil {

	private TextUtil(){
	}

	static
	public String normalize(TextIndex textIndex, TextIndexNormalization textIndexNormalization, String text){
		TextTokenizer tokenizer = null;

		Boolean tokenize = textIndexNormalization.isTokenize();
		if(tokenize == null){
			tokenize = textIndex.isTokenize();
		} // End if

		if(tokenize){
			PMMLObject locatable = textIndexNormalization;

			String wordSeparatorCharacterRE = textIndexNormalization.getWordSeparatorCharacterRE();
			if(wordSeparatorCharacterRE == null){
				locatable = textIndex;

				wordSeparatorCharacterRE = textIndex.getWordSeparatorCharacterRE();
			}

			Pattern pattern = RegExUtil.compile(wordSeparatorCharacterRE, locatable);

			tokenizer = new TextTokenizer(pattern);
		}

		Boolean caseSensitive = textIndexNormalization.isIsCaseSensitive();
		if(caseSensitive == null){
			caseSensitive = textIndex.isIsCaseSensitive();
		}

		PMMLObject locatable = textIndexNormalization;

		Integer maxLevenshteinDistance = textIndexNormalization.getMaxLevenshteinDistance();
		if(maxLevenshteinDistance == null){
			locatable = textIndex;

			maxLevenshteinDistance = textIndex.getMaxLevenshteinDistance();
		} // End if

		if(maxLevenshteinDistance < 0){
			throw new InvalidFeatureException(locatable, ReflectionUtil.getField(locatable.getClass(), "maxLevenshteinDistance"), maxLevenshteinDistance);
		}

		TableLocator tableLocator = textIndexNormalization.getTableLocator();
		if(tableLocator != null){
			throw new UnsupportedFeatureException(tableLocator);
		}

		InlineTable inlineTable = textIndexNormalization.getInlineTable();
		if(inlineTable != null){

			normalization:
			while(true){
				String normalizedText;

				try {
					normalizedText = normalize(inlineTable, text, textIndexNormalization.getInField(), textIndexNormalization.getOutField(), textIndexNormalization.getRegexField(), tokenizer, caseSensitive, maxLevenshteinDistance);
				} catch(PMMLException pe){
					pe.ensureContext(textIndexNormalization);

					throw pe;
				}

				// "If the recursive flag is set to true, the normalization table is reapplied until none of its rows causes a change to the input text."
				if(textIndexNormalization.isRecursive()){

					if(!(normalizedText).equals(text)){
						text = normalizedText;

						continue normalization;
					}
				}

				return normalizedText;
			}
		}

		return text;
	}

	static
	String normalize(InlineTable inlineTable, String text, String inColumn, String outColumn, String regexColumn, TextTokenizer tokenizer, boolean caseSensitive, int maxLevenshteinDistance){
		Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

		int regexFlags = (caseSensitive ? 0 : (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

		List<Row> rows = inlineTable.getRows();
		for(int rowKey = 1; rowKey <= rows.size(); rowKey++){
			Row row = rows.get(rowKey - 1);

			String inValue = table.get(rowKey, inColumn);
			String outValue = table.get(rowKey, outColumn);
			if(inValue == null || outValue == null){
				throw new InvalidFeatureException(row);
			}

			String regexValue = table.get(rowKey, regexColumn);

			// "If there is a regexField column and its value for that row is true, the string in the inField column should be treated as a PCRE regular expression"
			boolean regex = ("true").equalsIgnoreCase(regexValue);
			if(regex){
				Pattern pattern = RegExUtil.compile(inValue, regexFlags, row);

				Matcher matcher = pattern.matcher(text);

				text = matcher.replaceAll(outValue);
			} else

			{
				if(tokenizer != null){
					throw new UnsupportedFeatureException(row);
				}

				Pattern pattern = RegExUtil.compile(Pattern.quote(inValue), regexFlags, row);

				Matcher matcher = pattern.matcher(text);

				text = matcher.replaceAll(outValue);
			}
		}

		return text;
	}

	static
	public int termFrequency(TextIndex textIndex, String text, String term){

		if(("").equals(text)){
			return 0;
		}

		TextTokenizer tokenizer = null;

		boolean tokenize = textIndex.isTokenize();
		if(tokenize){
			String wordSeparatorCharacterRE = textIndex.getWordSeparatorCharacterRE();

			Pattern pattern = RegExUtil.compile(wordSeparatorCharacterRE, textIndex);

			tokenizer = new TextTokenizer(pattern);
		}

		boolean caseSensitive = textIndex.isIsCaseSensitive();

		int maxLevenshteinDistance = textIndex.getMaxLevenshteinDistance();
		if(maxLevenshteinDistance < 0){
			throw new InvalidFeatureException(textIndex, ReflectionUtil.getField(TextIndex.class, "maxLevenshteinDistance"), maxLevenshteinDistance);
		}

		boolean bestHits;

		TextIndex.CountHits countHits = textIndex.getCountHits();
		switch(countHits){
			case BEST_HITS:
				bestHits = true;
				break;
			case ALL_HITS:
				bestHits = false;
				break;
			default:
				throw new UnsupportedFeatureException(textIndex, countHits);
		}

		int maxFrequency;

		TextIndex.LocalTermWeights localTermWeights = textIndex.getLocalTermWeights();
		switch(localTermWeights){
			case BINARY:
				maxFrequency = 1;
				break;
			case TERM_FREQUENCY:
			case LOGARITHMIC:
				maxFrequency = Integer.MAX_VALUE;
				break;
			default:
				throw new UnsupportedFeatureException(textIndex, localTermWeights);
		}

		try {
			return termFrequency(text, term, tokenizer, caseSensitive, maxLevenshteinDistance, bestHits, maxFrequency);
		} catch(PMMLException pe){
			pe.ensureContext(textIndex);

			throw pe;
		}
	}

	static
	int termFrequency(String text, String term, TextTokenizer tokenizer, boolean caseSensitive, int maxLevenshteinDistance, boolean bestHits, int maxFrequency){

		if(tokenizer == null){
			throw new UnsupportedFeatureException();
		}

		List<String> textTokens = tokenizer.tokenize(text);
		List<String> termTokens = tokenizer.tokenize(term);

		return termFrequency(textTokens, termTokens, caseSensitive, maxLevenshteinDistance, bestHits, maxFrequency);
	}

	static
	int termFrequency(List<String> textTokens, List<String> termTokens, boolean caseSensitive, int maxLevenshteinDistance, boolean bestHits, int maxFrequency){
		int frequency = 0;

		int bestLevenshteinDistance = Integer.MAX_VALUE;

		int textSize = textTokens.size();
		int termSize = termTokens.size();

		text:
		for(int i = 0, max = (textSize - termSize); i <= max; i++){
			int levenshteinDistance = 0;

			term:
			for(int j = 0; j < termSize; j++){
				int threshold = (maxLevenshteinDistance - levenshteinDistance);

				String textToken = textTokens.get(i + j);
				String termToken = termTokens.get(j);

				if(threshold == 0){
					boolean equals;

					if(caseSensitive){
						equals = (textToken).equals(termToken);
					} else

					{
						equals = (textToken).equalsIgnoreCase(termToken);
					} // End if

					if(!equals){
						continue text;
					}
				} else

				{
					int tokenLevenshteinDistance = StringUtil.getLevenshteinDistance(textToken, termToken, caseSensitive, threshold);

					if(tokenLevenshteinDistance < 0){
						continue text;
					}

					levenshteinDistance += tokenLevenshteinDistance;
				}
			}

			if(bestHits){

				if(levenshteinDistance < bestLevenshteinDistance){
					frequency = 1;

					bestLevenshteinDistance = levenshteinDistance;
				} else

				if(levenshteinDistance == bestLevenshteinDistance){
					frequency++;
				} else

				{
					continue text;
				} // End if

				if((bestLevenshteinDistance == 0) && (frequency >= maxFrequency)){
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