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
import java.util.regex.PatternSyntaxException;

import org.dmg.pmml.TextIndex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegExUtilTest {

	@Test
	public void compile(){
		String invalidRegex = "[";

		EvaluationException exception = assertThrows(EvaluationException.class, () -> RegExUtil.compile(invalidRegex, null));

		assertEquals(null, exception.getContext());
		assertInstanceOf(PatternSyntaxException.class, exception.getCause());

		TextIndex textIndex = new TextIndex();

		exception = assertThrows(EvaluationException.class, () -> RegExUtil.compile(invalidRegex, textIndex));

		assertEquals(textIndex, exception.getContext());
		assertInstanceOf(PatternSyntaxException.class, exception.getCause());

		String validRegex = "\\s+";

		Pattern firstPattern = RegExUtil.compile(new String(validRegex), null);
		Pattern secondPattern = RegExUtil.compile(new String(validRegex), null);

		assertSame(firstPattern, secondPattern);
	}
}