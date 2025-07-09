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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.collect.Interner;
import com.google.common.collect.Table;
import org.dmg.pmml.InlineTable;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.Row;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndexNormalization;
import org.jpmml.model.InvalidAttributeException;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.MissingAttributeException;
import org.jpmml.model.PMMLException;
import org.jpmml.model.UnsupportedAttributeException;

public class TextUtil {

	private TextUtil(){
	}

	static
	public String normalize(TextIndex textIndex, String string){

		if(textIndex.hasTextIndexNormalizations()){
			List<TextIndexNormalization> textIndexNormalizations = textIndex.getTextIndexNormalizations();

			for(int i = 0, max = textIndexNormalizations.size(); i < max; i++){
				TextIndexNormalization textIndexNormalization = textIndexNormalizations.get(i);

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
			tokenizer = createTextTokenizer(textIndex, textIndexNormalization);
		}

		Boolean caseSensitive = textIndexNormalization.isCaseSensitive();
		if(caseSensitive == null){
			caseSensitive = textIndex.isCaseSensitive();
		}

		Integer maxLevenshteinDistance = textIndexNormalization.getMaxLevenshteinDistance();
		if(maxLevenshteinDistance == null){
			maxLevenshteinDistance = textIndex.getMaxLevenshteinDistance();

			if(maxLevenshteinDistance < 0){
				throw new InvalidAttributeException(textIndex, PMMLAttributes.TEXTINDEX_MAXLEVENSHTEINDISTANCE, maxLevenshteinDistance);
			}
		} else

		{
			if(maxLevenshteinDistance < 0){
				throw new InvalidAttributeException(textIndexNormalization, PMMLAttributes.TEXTINDEXNORMALIZATION_MAXLEVENSHTEINDISTANCE, maxLevenshteinDistance);
			}
		}

		InlineTable inlineTable = InlineTableUtil.getInlineTable(textIndexNormalization);
		if(inlineTable != null){
			String inField = textIndexNormalization.getInField();
			String outField = textIndexNormalization.getOutField();
			String regexField = textIndexNormalization.getRegexField();

			normalization:
			while(true){
				String normalizedString;

				try {
					normalizedString = normalize(inlineTable, inField, outField, regexField, string, tokenizer, caseSensitive, maxLevenshteinDistance);
				} catch(PMMLException pe){
					throw pe.ensureContext(textIndexNormalization);
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
		Table<Integer, String, Object> table = InlineTableUtil.getContent(inlineTable);

		int regexFlags = (caseSensitive ? 0 : (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

		List<Row> rows = inlineTable.getRows();
		for(int i = 0, max = rows.size(); i < max; i++){
			Row row = rows.get(i);

			Integer rowKey = (i + 1);

			String inValue = (String)table.get(rowKey, inColumn);
			if(inValue == null){
				throw new InvalidElementException("Cell " + EvaluationException.formatKey(inColumn) + " is not defined", row);
			}

			String outValue = (String)table.get(rowKey, outColumn);
			if(outValue == null){
				throw new InvalidElementException("Cell " + EvaluationException.formatKey(outColumn) + " is not defined", row);
			}

			String regexValue = (String)table.get(rowKey, regexColumn);

			// "If there is a regexField column and its value for that row is true, then the string in the inField column should be treated as a PCRE regular expression"
			boolean regex = ("true").equalsIgnoreCase(regexValue);
			if(regex){
				Pattern pattern = RegExUtil.compile(inValue, regexFlags, row);

				Matcher matcher = pattern.matcher(string);

				string = matcher.replaceAll(outValue);
			} else

			{
				Pattern pattern = RegExUtil.compile(Pattern.quote(inValue), regexFlags, row);

				Matcher matcher = pattern.matcher(string);

				string = matcher.replaceAll(outValue);
			}
		}

		return string;
	}


	static
	public TokenizedString tokenize(TextIndex textIndex, String text){
		boolean tokenize = textIndex.isTokenize();

		if(tokenize){
			TextTokenizer tokenizer = createTextTokenizer(textIndex, null);

			return tokenizer.tokenize(text);
		} else

		{
			throw new UnsupportedAttributeException(textIndex, PMMLAttributes.TEXTINDEX_TOKENIZE, tokenize);
		}
	}

	static
	public int termFrequency(TextIndex textIndex, TokenizedString textTokens, TokenizedString termTokens){

		if((textTokens.size() == 0) || (termTokens.size() == 0)){
			return 0;
		}

		boolean caseSensitive = textIndex.isCaseSensitive();

		int maxLevenshteinDistance = textIndex.getMaxLevenshteinDistance();
		if(maxLevenshteinDistance < 0){
			throw new InvalidAttributeException(textIndex, PMMLAttributes.TEXTINDEX_MAXLEVENSHTEINDISTANCE, maxLevenshteinDistance);
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
				throw new UnsupportedAttributeException(textIndex, countHits);
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
				throw new UnsupportedAttributeException(textIndex, localTermWeights);
		}

		try {
			return termFrequency(textTokens, termTokens, caseSensitive, maxLevenshteinDistance, bestHits, maxFrequency);
		} catch(PMMLException pe){
			throw pe.ensureContext(textIndex);
		}
	}

	static
	public Map<TokenizedString, Integer> termFrequencyTable(TextIndex textIndex, TokenizedString textTokens, Set<TokenizedString> termTokenSet, int maxLength){
		boolean caseSensitive = textIndex.isCaseSensitive();

		int maxLevenshteinDistance = textIndex.getMaxLevenshteinDistance();
		if(maxLevenshteinDistance < 0){
			throw new InvalidAttributeException(textIndex, PMMLAttributes.TEXTINDEX_MAXLEVENSHTEINDISTANCE, maxLevenshteinDistance);
		}

		return termFrequencyTable(textTokens, termTokenSet, caseSensitive, maxLevenshteinDistance, maxLength);
	}

	static
	public boolean matches(TextIndex textIndex, TokenizedString leftTokens, TokenizedString rightTokens){
		boolean caseSensitive = textIndex.isCaseSensitive();

		int maxLevenshteinDistance = textIndex.getMaxLevenshteinDistance();
		if(maxLevenshteinDistance < 0){
			throw new InvalidAttributeException(textIndex, PMMLAttributes.TEXTINDEX_MAXLEVENSHTEINDISTANCE, maxLevenshteinDistance);
		}

		return matches(leftTokens, rightTokens, caseSensitive, maxLevenshteinDistance);
	}

	static
	int termFrequency(TokenizedString textTokens, TokenizedString termTokens, boolean caseSensitive, int maxLevenshteinDistance, boolean bestHits, int maxFrequency){
		int frequency = 0;

		int bestLevenshteinDistance = Integer.MAX_VALUE;

		int textSize = textTokens.size();
		int termSize = termTokens.size();

		text:
		for(int i = 0, max = (textSize - termSize); i <= max; i++){
			int levenshteinDistance = 0;

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
					int tokenLevenshteinDistance = LevenshteinDistanceUtil.limitedCompare(textToken, termToken, caseSensitive, threshold);

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
	Map<TokenizedString, Integer> termFrequencyTable(TokenizedString textTokens, Set<TokenizedString> termTokenSet, boolean caseSensitive, int maxLevenshteinDistance, int maxLength){
		Map<TokenizedString, Integer> result = new HashMap<>();

		List<TokenizedString> termTokenList;

		if(caseSensitive && maxLevenshteinDistance == 0){
			termTokenList = null;
		} else

		{
			termTokenList = new ArrayList<>(termTokenSet);
		}

		for(int i = 0, textMax = textTokens.size(); i < textMax; i++){

			for(int length = 1; length <= maxLength; length++){

				if((i + length) > textMax){
					break;
				}

				TokenizedString sliceOfTextTokens = textTokens.slice(i, i + length);

				if(caseSensitive && maxLevenshteinDistance == 0){
					boolean matches = termTokenSet.contains(sliceOfTextTokens);

					if(matches){
						Integer count = result.get(sliceOfTextTokens);

						result.put(sliceOfTextTokens, count != null ? count + 1 : 1);
					}
				} else

				{
					for(int j = 0, termMax = termTokenList.size(); j < termMax; j++){
						TokenizedString termTokens = termTokenList.get(j);

						boolean matches = matches(sliceOfTextTokens, termTokens, caseSensitive, maxLevenshteinDistance);

						if(matches){
							Integer count = result.get(termTokens);

							result.put(termTokens, count != null ? count + 1 : 1);
						}
					}
				}
			}
		}

		return result;
	}

	static
	boolean matches(TokenizedString leftTokens, TokenizedString rightTokens, boolean caseSensitive, int maxLevenshteinDistance){

		if(leftTokens.size() != rightTokens.size()){
			return false;
		}

		int levenshteinDistance = 0;

		for(int i = 0, max = leftTokens.size(); i < max; i++){
			int threshold = (maxLevenshteinDistance - levenshteinDistance);

			String leftToken = leftTokens.get(i);
			String rightToken = rightTokens.get(i);

			if(threshold == 0){
				boolean equals;

				if(caseSensitive){
					equals = (leftToken).equals(rightToken);
				} else

				{
					equals = (leftToken).equalsIgnoreCase(rightToken);
				} // End if

				if(!equals){
					return false;
				}
			} else

			{
				int tokenLevenshteinDistance = LevenshteinDistanceUtil.limitedCompare(leftToken, rightToken, caseSensitive, threshold);

				if(tokenLevenshteinDistance < 0){
					return false;
				}

				levenshteinDistance += tokenLevenshteinDistance;
			}
		}

		return true;
	}

	static
	private TextTokenizer createTextTokenizer(TextIndex textIndex, TextIndexNormalization textIndexNormalization){

		if(textIndexNormalization != null){
			String wordRE = textIndexNormalization.getWordRE();
			String wordSeparatorCharacterRE = textIndexNormalization.getWordSeparatorCharacterRE();

			if(wordRE != null){
				return createTextMatcher(textIndex, textIndexNormalization);
			} else

			if(wordSeparatorCharacterRE != null){
				return createTextSplitter(textIndex, textIndexNormalization);
			}
		}

		String wordRE = textIndex.getWordRE();
		String wordSeparatorCharacterRE = textIndex.getWordSeparatorCharacterRE();

		if(wordRE != null){
			return createTextMatcher(textIndex, null);
		}

		return createTextSplitter(textIndex, null);
	}

	static
	private TextSplitter createTextSplitter(TextIndex textIndex, TextIndexNormalization textIndexNormalization){

		if(textIndexNormalization != null){
			String wordSeparatorCharacterRE = textIndexNormalization.getWordSeparatorCharacterRE();

			if(wordSeparatorCharacterRE != null){
				return new TextSplitter(wordSeparatorCharacterRE, textIndexNormalization);
			}
		}

		String wordSeparatorCharacterRE = textIndex.getWordSeparatorCharacterRE();

		return new TextSplitter(wordSeparatorCharacterRE, textIndex);
	}

	static
	private TextMatcher createTextMatcher(TextIndex textIndex, TextIndexNormalization textIndexNormalization){

		if(textIndexNormalization != null){
			String wordRE = textIndexNormalization.getWordRE();

			if(wordRE != null){
				return new TextMatcher(wordRE, textIndexNormalization);
			}
		}

		String wordRE = textIndex.getWordRE();
		if(wordRE == null){
			throw new MissingAttributeException(textIndex, PMMLAttributes.TEXTINDEX_WORDRE);
		}

		return new TextMatcher(wordRE, textIndex);
	}

	static
	abstract
	class StringProcessor {

		private TextIndex textIndex = null;

		private String value = null;


		public StringProcessor(TextIndex textIndex, String value){
			setTextIndex(textIndex);
			// The value should generate a cache hit both in identity comparison and object equality comparison modes
			setValue(value != null ? TextUtil.STRING_INTERNER.intern(value) : null);
		}

		abstract
		public TokenizedString process();

		public TextIndex getTextIndex(){
			return this.textIndex;
		}

		private void setTextIndex(TextIndex textIndex){
			this.textIndex = Objects.requireNonNull(textIndex);
		}

		public String getValue(){
			return this.value;
		}

		private void setValue(String value){
			this.value = Objects.requireNonNull(value);
		}
	}

	static
	class TextProcessor extends StringProcessor {

		TextProcessor(TextIndex textIndex, String value){
			super(textIndex, value);
		}

		@Override
		public TokenizedString process(){
			TextIndex textIndex = getTextIndex();
			String value = getValue();

			Cache<String, TokenizedString> textTokenCache = CacheUtil.getValue(textIndex, TextUtil.textTokenCaches, TextUtil.textTokenCacheLoader);

			TokenizedString textTokens = textTokenCache.getIfPresent(value);
			if(textTokens == null){
				String string = TextUtil.normalize(textIndex, value);

				textTokens = TextUtil.tokenize(textIndex, string);

				textTokenCache.put(value, textTokens);
			}

			return textTokens;
		}
	}

	static
	class TermProcessor extends StringProcessor {

		TermProcessor(TextIndex textIndex, String value){
			super(textIndex, value);
		}

		@Override
		public TokenizedString process(){
			TextIndex textIndex = getTextIndex();
			String value = getValue();

			Cache<String, TokenizedString> termTokenCache = CacheUtil.getValue(textIndex, TextUtil.termTokenCaches, TextUtil.termTokenCacheLoader);

			TokenizedString termTokens = termTokenCache.getIfPresent(value);
			if(termTokens == null){
				termTokens = TextUtil.tokenize(textIndex, value);

				termTokenCache.put(value, termTokens);
			}

			return termTokens;
		}
	}

	private static final Interner<String> STRING_INTERNER = InternerUtil.buildInterner();

	private static final Cache<TextIndex, Cache<String, TokenizedString>> textTokenCaches = CacheUtil.buildCache();

	private static final Callable<Cache<String, TokenizedString>> textTokenCacheLoader = new Callable<>(){

		@Override
		public Cache<String, TokenizedString> call(){
			return CacheUtil.buildCache();
		}
	};

	private static final Cache<TextIndex, Cache<String, TokenizedString>> termTokenCaches = CacheUtil.buildCache();

	private static final Callable<Cache<String, TokenizedString>> termTokenCacheLoader = new Callable<>(){

		@Override
		public Cache<String, TokenizedString> call(){
			return CacheUtil.buildCache();
		}
	};
}