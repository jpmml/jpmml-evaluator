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
import java.util.List;

import org.dmg.pmml.InlineTable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextUtilTest {

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
		List<String> textTokens = Arrays.asList("x", "x", "x", "x");
		List<String> termTokens;

		assertEquals(0, termFrequency(textTokens, Arrays.asList("x", "x", "x", "x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, Arrays.asList("x", "x", "x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, Arrays.asList("x", "x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, Arrays.asList("x", "x"), true, 0, false, Integer.MAX_VALUE));
		assertEquals(4, termFrequency(textTokens, Arrays.asList("x"), true, 0, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "X", "x", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "Y", "x", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "X", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(2, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(3, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(4, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 1, false, 1));
		assertEquals(4, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "X", "X", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "Y", "Y", "x");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(3, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "X", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 3, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("x", "Y", "Y", "Y");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 3, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("X", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 1, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, true, 2, false, Integer.MAX_VALUE));
		assertEquals(2, termFrequency(textTokens, termTokens, true, 3, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("X", "X", "X", "X");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));

		termTokens = Arrays.asList("Y", "Y", "Y", "Y");

		assertEquals(0, termFrequency(textTokens, termTokens, true, 0, false, Integer.MAX_VALUE));
		assertEquals(0, termFrequency(textTokens, termTokens, false, 0, false, Integer.MAX_VALUE));
		assertEquals(1, termFrequency(textTokens, termTokens, true, 4, false, Integer.MAX_VALUE));
	}

	static
	private String normalize(List<List<String>> rows, String text, TextTokenizer tokenizer, boolean caseSensitive, int maxLevenshteinDistance){
		List<String> columns = Arrays.asList("string", "stem", "regex");

		InlineTable inlineTable = ExpressionUtilTest.createInlineTable(rows, columns);

		return TextUtil.normalize(inlineTable, columns.get(0), columns.get(1), columns.get(2), text, tokenizer, caseSensitive, maxLevenshteinDistance);
	}

	static
	private int termFrequency(List<String> textTokens, List<String> termTokens, boolean caseSensitive, int maxLevenshteinDistance, boolean bestHits, int maxFrequency){
		return TextUtil.termFrequency(textTokens, termTokens, caseSensitive, maxLevenshteinDistance, bestHits, maxFrequency);
	}
}