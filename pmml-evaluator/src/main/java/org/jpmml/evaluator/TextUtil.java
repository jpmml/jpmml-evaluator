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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.collect.Table;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Row;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndexNormalization;
import org.jpmml.model.ReflectionUtil;

public class TextUtil {

	private TextUtil(){
	}

	static
	public String normalize(TextIndex textIndex, String string){

		if(textIndex.hasTextIndexNormalizations()){
			List<TextIndexNormalization> textIndexNormalizations = textIndex.getTextIndexNormalizations();

			for(TextIndexNormalization textIndexNormalization : textIndexNormalizations){
				string = TextUtil.normalize(textIndex, textIndexNormalization, string);
			}
		}

		return string;
	}

	static
	public String normalize(TextIndex textIndex, TextIndexNormalization textIndexNormalization, String string){
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

		Boolean caseSensitive = textIndexNormalization.isCaseSensitive();
		if(caseSensitive == null){
			caseSensitive = textIndex.isCaseSensitive();
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

		InlineTable inlineTable = InlineTableUtil.getInlineTable(textIndexNormalization);
		if(inlineTable != null){

			normalization:
			while(true){
				String normalizedString;

				try {
					normalizedString = normalize(inlineTable, textIndexNormalization.getInField(), textIndexNormalization.getOutField(), textIndexNormalization.getRegexField(), string, tokenizer, caseSensitive, maxLevenshteinDistance);
				} catch(PMMLException pe){
					pe.ensureContext(textIndexNormalization);

					throw pe;
				}

				// "If the recursive flag is set to true, then the normalization table is reapplied until none of its rows causes a change to the input text."
				if(textIndexNormalization.isRecursive()){

					if(!(normalizedString).equals(string)){
						string = normalizedString;

						continue normalization;
					}
				}

				return normalizedString;
			}
		}

		return string;
	}

	static
	String normalize(InlineTable inlineTable, String inColumn, String outColumn, String regexColumn, String string, TextTokenizer tokenizer, boolean caseSensitive, int maxLevenshteinDistance){
		Table<Integer, String, String> table = InlineTableUtil.getContent(inlineTable);

		int regexFlags = (caseSensitive ? 0 : (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

		List<Row> rows = inlineTable.getRows();
		for(int i = 0; i < rows.size(); i++){
			Row row = rows.get(i);

			Integer rowKey = (i + 1);

			String inValue = table.get(rowKey, inColumn);
			String outValue = table.get(rowKey, outColumn);
			if(inValue == null || outValue == null){
				throw new InvalidFeatureException(row);
			}

			String regexValue = table.get(rowKey, regexColumn);

			// "If there is a regexField column and its value for that row is true, then the string in the inField column should be treated as a PCRE regular expression"
			boolean regex = ("true").equalsIgnoreCase(regexValue);
			if(regex){
				Pattern pattern = RegExUtil.compile(inValue, regexFlags, row);

				Matcher matcher = pattern.matcher(string);

				string = matcher.replaceAll(outValue);
			} else

			{
				if(tokenizer != null){
					throw new UnsupportedFeatureException(row);
				}

				Pattern pattern = RegExUtil.compile(Pattern.quote(inValue), regexFlags, row);

				Matcher matcher = pattern.matcher(string);

				string = matcher.replaceAll(outValue);
			}
		}

		return string;
	}


	static
	public List<String> tokenize(TextIndex textIndex, String text){
		boolean tokenize = textIndex.isTokenize();

		if(tokenize){
			String wordSeparatorCharacterRE = textIndex.getWordSeparatorCharacterRE();

			Pattern pattern = RegExUtil.compile(wordSeparatorCharacterRE, textIndex);

			TextTokenizer tokenizer = new TextTokenizer(pattern);

			return tokenizer.tokenize(text);
		} else

		{
			throw new UnsupportedFeatureException(textIndex, ReflectionUtil.getField(TextIndex.class, "tokenize"), tokenize);
		}
	}

	static
	public int termFrequency(TextIndex textIndex, List<String> textTokens, List<String> termTokens){

		if(textTokens.isEmpty() || termTokens.isEmpty()){
			return 0;
		}

		boolean caseSensitive = textIndex.isCaseSensitive();

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
			return termFrequency(textTokens, termTokens, caseSensitive, maxLevenshteinDistance, bestHits, maxFrequency);
		} catch(PMMLException pe){
			pe.ensureContext(textIndex);

			throw pe;
		}
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

	static
	abstract
	class StringProcessor {

		private TextIndex textIndex = null;

		private FieldValue value = null;


		public StringProcessor(TextIndex textIndex, FieldValue value){
			setTextIndex(Objects.requireNonNull(textIndex));
			setValue(Objects.requireNonNull(value));
		}

		abstract
		public List<String> process();

		public TextIndex getTextIndex(){
			return this.textIndex;
		}

		private void setTextIndex(TextIndex textIndex){
			this.textIndex = textIndex;
		}

		public FieldValue getValue(){
			return this.value;
		}

		private void setValue(FieldValue value){
			this.value = value;
		}
	}

	static
	class TextProcessor extends StringProcessor {

		TextProcessor(TextIndex textIndex, FieldValue value){
			super(textIndex, value);
		}

		@Override
		public List<String> process(){
			TextIndex textIndex = getTextIndex();
			FieldValue value = getValue();

			Cache<FieldValue, List<String>> textTokenCache = CacheUtil.getValue(textIndex, TextUtil.textTokenCaches, TextUtil.textTokenCacheLoader);

			List<String> tokens = textTokenCache.getIfPresent(value);
			if(tokens == null){
				String string = TextUtil.normalize(textIndex, value.asString());

				tokens = TextUtil.tokenize(textIndex, string);

				textTokenCache.put(value, tokens);
			}

			return tokens;
		}
	}

	static
	class TermProcessor extends StringProcessor {

		TermProcessor(TextIndex textIndex, FieldValue value){
			super(textIndex, value);
		}

		@Override
		public List<String> process(){
			TextIndex textIndex = getTextIndex();
			FieldValue value = getValue();

			Cache<FieldValue, List<String>> termTokenCache = CacheUtil.getValue(textIndex, TextUtil.termTokenCaches, TextUtil.termTokenCacheLoader);

			List<String> tokens = termTokenCache.getIfPresent(value);
			if(tokens == null){
				String string = value.asString();

				tokens = TextUtil.tokenize(textIndex, string);

				termTokenCache.put(value, tokens);
			}

			return tokens;
		}
	}

	private static final Cache<TextIndex, Cache<FieldValue, List<String>>> textTokenCaches = CacheUtil.buildCache();

	private static final Callable<Cache<FieldValue, List<String>>> textTokenCacheLoader = new Callable<Cache<FieldValue, List<String>>>(){

		@Override
		public Cache<FieldValue, List<String>> call(){
			return CacheUtil.buildCache();
		}
	};

	private static final Cache<TextIndex, Cache<FieldValue, List<String>>> termTokenCaches = CacheUtil.buildCache();

	private static final Callable<Cache<FieldValue, List<String>>> termTokenCacheLoader = new Callable<Cache<FieldValue, List<String>>>(){

		@Override
		public Cache<FieldValue, List<String>> call(){
			return CacheUtil.buildCache();
		}
	};
}