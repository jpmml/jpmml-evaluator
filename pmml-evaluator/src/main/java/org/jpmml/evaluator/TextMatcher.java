/*
 * Copyright (c) 2021 Villu Ruusmann
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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.TextIndex;

/**
 * @see TextIndex#getWordRE()
 */
public class TextMatcher extends TextTokenizer {

	public TextMatcher(String wordRE, PMMLObject context){
		this(RegExUtil.compile(wordRE, context));
	}

	public TextMatcher(Pattern pattern){
		super(pattern);
	}

	@Override
	public List<String> tokenize(String string){
		Pattern pattern = getPattern();

		if(("").equals(string)){
			return Collections.emptyList();
		}

		Matcher matcher = pattern.matcher(string);

		if(!matcher.find()){
			return Collections.emptyList();
		}

		List<String> tokens = new ArrayList<>(Math.max(string.length() / 4, 16));

		do {
			tokens.add(matcher.group());
		} while(matcher.find());

		return tokens;
	}
}