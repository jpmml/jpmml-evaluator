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
import org.jpmml.model.TermUtil;

/**
 * @see TextIndex#getWordSeparatorCharacterRE()
 */
public class TextSplitter extends TextTokenizer {

	public TextSplitter(String wordSeparatorCharacterRE, PMMLObject context){
		this(RegExUtil.compile(wordSeparatorCharacterRE, context));
	}

	public TextSplitter(Pattern pattern){
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
			String token = TermUtil.trimPunctuation(string);

			if(!token.isEmpty()){
				return Collections.singletonList(token);
			}

			return Collections.emptyList();
		}

		List<String> tokens = new ArrayList<>(Math.max(string.length() / 4, 16));

		int index = 0;

		do {
			int start = matcher.start();
			int end = matcher.end();

			String token = TermUtil.trimPunctuation(string.substring(index, start));
			if(!token.isEmpty()){
				tokens.add(token);
			}

			index = end;
		} while(matcher.find());

		String token = TermUtil.trimPunctuation(string.substring(index));
		if(!token.isEmpty()){
			tokens.add(token);
		}

		return tokens;
	}
}