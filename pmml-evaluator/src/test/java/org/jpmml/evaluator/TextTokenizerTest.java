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

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextTokenizerTest {

	@Test
	public void split(){
		Pattern pattern = Pattern.compile("\\s+");

		TextTokenizer tokenizer = new TextSplitter(pattern);

		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(""));

		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(" "));
		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize("\t\t\t"));

		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(","));
		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(",,"));
		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(", ,"));
		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(" , , "));

		TokenizedString expected = new TokenizedString("one");

		assertEquals(expected, tokenizer.tokenize("one"));
		assertEquals(expected, tokenizer.tokenize(" one"));
		assertEquals(expected, tokenizer.tokenize("one "));
		assertEquals(expected, tokenizer.tokenize("\u00BFone?"));

		expected = new TokenizedString("one", "two", "three");

		assertEquals(expected, tokenizer.tokenize("one two three"));
		assertEquals(expected, tokenizer.tokenize("one!,\t\u00BFtwo?,\tthree."));
	}

	@Test
	public void match(){
		Pattern pattern = Pattern.compile("\\S+");

		TextTokenizer tokenizer = new TextMatcher(pattern);

		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(""));

		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize(" "));
		assertEquals(TokenizedString.EMPTY, tokenizer.tokenize("\t\t\t"));

		assertEquals(new TokenizedString(","), tokenizer.tokenize(","));
		assertEquals(new TokenizedString(",,"), tokenizer.tokenize(",,"));
		assertEquals(new TokenizedString(",", ","), tokenizer.tokenize(", ,"));
		assertEquals(new TokenizedString(",", ","), tokenizer.tokenize(" , , "));

		assertEquals(new TokenizedString("one", "two", "three"), tokenizer.tokenize("one two three"));
		assertEquals(new TokenizedString("one!,", "\u00BFtwo?,", "three."), tokenizer.tokenize("one!,\t\u00BFtwo?,\tthree."));

		pattern = Pattern.compile("\\w{4,}");

		tokenizer = new TextMatcher(pattern);

		assertEquals(new TokenizedString("three", "four", "five", "seven"), tokenizer.tokenize("one two three four five six seven"));
		assertEquals(new TokenizedString("three"), tokenizer.tokenize("one!,\t\u00BFtwo?,\tthree."));
	}
}