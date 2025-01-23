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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.InlineTable;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.TextIndexNormalization;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextUtilTest {

	@Test
	public void processTerms(){
		TextIndex textIndex = new TextIndex();

		TextUtil.TermProcessor termProcessor = new TextUtil.TermProcessor(textIndex, new String("brown fox"));

		TokenizedString firstTokens = termProcessor.process();

		assertEquals(new TokenizedString("brown", "fox"), firstTokens);

		termProcessor = new TextUtil.TermProcessor(textIndex, new String("brown fox"));

		TokenizedString secondTokens = termProcessor.process();

		assertSame(firstTokens, secondTokens);

		textIndex = new TextIndex();

		termProcessor = new TextUtil.TermProcessor(textIndex, new String("brown fox"));

		TokenizedString thirdTokens = termProcessor.process();

		assertEquals(firstTokens, thirdTokens);
		assertNotSame(firstTokens, thirdTokens);

		assertEquals(secondTokens, thirdTokens);
		assertNotSame(firstTokens, thirdTokens);
	}

	@Test
	public void processTexts(){
		TextIndex textIndex = new TextIndex()
			.addTextIndexNormalizations(new TextIndexNormalization());

		TextUtil.TextProcessor textProcessor = new TextUtil.TextProcessor(textIndex, new String("The quick brown fox jumps over the lazy dog"));

		TokenizedString firstTokens = textProcessor.process();

		assertEquals(new TokenizedString("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"), firstTokens);

		textProcessor = new TextUtil.TextProcessor(textIndex, new String("The quick brown fox jumps over the lazy dog"));

		TokenizedString secondTokens = textProcessor.process();

		assertSame(firstTokens, secondTokens);
	}

	@Test
	public void normalize(){
		List<List<String>> regexRows = Arrays.asList(
			Arrays.asList("[\u00c0|\u00c1|\u00c2|\u00c3|\u00c4|\u00c5]", "A", "true")
		);

		List<List<String>> noRegexRows = Arrays.asList(
			Arrays.asList("\u00c0", "A", null),
			Arrays.asList("\u00c1", "A", null),
			Arrays.asList("\u00c2", "A", null),
			Arrays.asList("\u00c3", "A", "false"),
			Arrays.asList("\u00c4", "A", "false"),
			Arrays.asList("\u00c5", "A", "false")
		);

		for(List<List<String>> rows : Arrays.asList(regexRows, noRegexRows)){
			String text = "\u00c0BC";

			assertEquals("ABC", normalize(rows, text, null, true, 0));

			text = text.toLowerCase();

			assertEquals(text, normalize(rows, text, null, true, 0));
			assertEquals("Abc", normalize(rows, text, null, false, 0));

			text = "\u00c0\u00c2\u00c4";

			assertEquals("AAA", normalize(rows, text, null, true, 0));

			text = text.toLowerCase();

			assertEquals(text, normalize(rows, text, null, true, 0));
			assertEquals("AAA", normalize(rows, text, null, false, 0));
		}
	}

	@Test
	public void termFrequency(){
		TokenizedString textTokens = new TokenizedString("x", "x", "x", "x");
		TokenizedString termTokens;

		assertEquals(0, termFrequency(textTokens, new TokenizedString("x", "x", "x", "x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, new TokenizedString("x", "x", "x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, new TokenizedString("x", "x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, new TokenizedString("x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(4, termFrequency(textTokens, new TokenizedString("x"), true, 0, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "X", "x", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "Y", "x", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "X", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(2, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(3, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(4, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(4, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "X", "X", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "Y", "Y", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "X", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 3, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("x", "Y", "Y", "Y");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 3, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("X", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, true, 3, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("X", "X", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));

		termTokens = new TokenizedString("Y", "Y", "Y", "Y");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 4, false, Integer.MAX_VALUE));
	}

	@Test
	public void termFrequencyTable(){
		TokenizedString textTokens = new TokenizedString("a", "b", "A", "c");
		Set<TokenizedString> termTokenSet = new HashSet<TokenizedString>(){

			{
				add(new TokenizedString("a"));
				add(new TokenizedString("a", "b"));
				add(new TokenizedString("a", "b", "c"));
			}
		};

		Map<TokenizedString, Integer> expectedTermFrequencyTable = new HashMap<>();
		expectedTermFrequencyTable.put(new TokenizedString("a"), 2);

		Map<TokenizedString, Integer> termFrequencyTable = TextUtil.termFrequencyTable(textTokens, termTokenSet, false, 0, 1);

		assertEquals(expectedTermFrequencyTable, termFrequencyTable);

		expectedTermFrequencyTable.put(new TokenizedString("a", "b"), 1);

		termFrequencyTable = TextUtil.termFrequencyTable(textTokens, termTokenSet, false, 0, 2);

		assertEquals(expectedTermFrequencyTable, termFrequencyTable);

		expectedTermFrequencyTable = new HashMap<>();
		expectedTermFrequencyTable.put(new TokenizedString("a"), 4);

		termFrequencyTable = TextUtil.termFrequencyTable(textTokens, termTokenSet, false, 1, 1);

		assertEquals(expectedTermFrequencyTable, termFrequencyTable);

		termFrequencyTable = TextUtil.termFrequencyTable(textTokens, termTokenSet, true, 1, 1);

		assertEquals(expectedTermFrequencyTable, termFrequencyTable);

		expectedTermFrequencyTable.put(new TokenizedString("a", "b"), 2);

		termFrequencyTable = TextUtil.termFrequencyTable(textTokens, termTokenSet, false, 1, 2);

		assertEquals(expectedTermFrequencyTable, termFrequencyTable);

		expectedTermFrequencyTable.put(new TokenizedString("a", "b"), 1);

		termFrequencyTable = TextUtil.termFrequencyTable(textTokens, termTokenSet, true, 1, 2);

		assertEquals(expectedTermFrequencyTable, termFrequencyTable);
	}

	@Test
	public void matches(){
		TokenizedString leftTokens = new TokenizedString("A", "B", "C");
		TokenizedString rightTokens = new TokenizedString("a", "b", "c");

		assertFalse(TextUtil.matches(leftTokens, rightTokens, true, 0));

		assertTrue(TextUtil.matches(leftTokens, rightTokens, false, 0));

		rightTokens = new TokenizedString("A", "X", "C");

		assertFalse(TextUtil.matches(leftTokens, rightTokens, true, 0));
		assertTrue(TextUtil.matches(leftTokens, rightTokens, true, 1));

		rightTokens = new TokenizedString("A", "XX", "C");

		assertFalse(TextUtil.matches(leftTokens, rightTokens, true, 1));
		assertTrue(TextUtil.matches(leftTokens, rightTokens, true, 2));

		rightTokens = new TokenizedString("a", "x", "c");

		assertFalse(TextUtil.matches(leftTokens, rightTokens, true, 0));
		assertFalse(TextUtil.matches(leftTokens, rightTokens, true, 2));
		assertTrue(TextUtil.matches(leftTokens, rightTokens, true, 3));

		assertFalse(TextUtil.matches(leftTokens, rightTokens, false, 0));
		assertTrue(TextUtil.matches(leftTokens, rightTokens, false, 1));

		rightTokens = new TokenizedString("a", "xx", "c");

		assertFalse(TextUtil.matches(leftTokens, rightTokens, true, 3));
		assertTrue(TextUtil.matches(leftTokens, rightTokens, true, 4));

		assertFalse(TextUtil.matches(leftTokens, rightTokens, false, 1));
		assertTrue(TextUtil.matches(leftTokens, rightTokens, false, 2));
	}

	static
	private String normalize(List<List<String>> rows, String text, TextTokenizer tokenizer, boolean caseSensitive, int maxLevenshteinDistance){
		List<String> columns = Arrays.asList("string", "stem", "regex");

		InlineTable inlineTable = ExpressionUtilTest.createInlineTable(rows, columns);

		return TextUtil.normalize(inlineTable, columns.get(0), columns.get(1), columns.get(2), text, tokenizer, caseSensitive, maxLevenshteinDistance);
	}

	static
	private int termFrequency(TokenizedString textTokens, TokenizedString termTokens, boolean caseSensitive, int maxLevenshteinDistance, boolean bestHits, int maxFrequency){
		return TextUtil.termFrequency(textTokens, termTokens, caseSensitive, maxLevenshteinDistance, bestHits, maxFrequency);
	}
}