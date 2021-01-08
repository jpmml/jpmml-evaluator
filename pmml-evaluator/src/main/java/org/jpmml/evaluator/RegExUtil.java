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

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.dmg.pmml.PMMLObject;

public class RegExUtil {

	private RegExUtil(){
	}

	static
	public Pattern compile(String regex, PMMLObject context){
		return compile(regex, 0, context);
	}

	static
	public Pattern compile(String regex, int flags, PMMLObject context){
		CompilationTask compilationTask = new CompilationTask(regex, flags);

		// The compilation task should generate a cache hit both in identity comparison and object equality comparison modes
		compilationTask = INTERNER.intern(compilationTask);

		try {
			return patternCache.get(compilationTask);
		} catch(ExecutionException | UncheckedExecutionException e){
			String message = "Regex pattern could not be compiled";

			throw (context != null ? new EvaluationException(message, context) : new EvaluationException(message))
				.initCause(e.getCause());
		}
	}

	static
	private class CompilationTask {

		private String regex = null;

		private int flags = 0;


		private CompilationTask(String regex, int flags){
			setRegEx(Objects.requireNonNull(regex));
			setFlags(flags);
		}

		public Pattern call() throws PatternSyntaxException {
			String regex = getRegEx();
			int flags = getFlags();

			return Pattern.compile(regex, flags);
		}

		@Override
		public int hashCode(){
			return (31 * getRegEx().hashCode()) + getFlags();
		}

		@Override
		public boolean equals(Object object){

			if(object instanceof CompilationTask){
				CompilationTask that = (CompilationTask)object;

				return Objects.equals(this.getRegEx(), that.getRegEx()) && (this.getFlags() == that.getFlags());
			}

			return false;
		}

		public String getRegEx(){
			return this.regex;
		}

		private void setRegEx(String regex){
			this.regex = regex;
		}

		public int getFlags(){
			return this.flags;
		}

		private void setFlags(int flags){
			this.flags = flags;
		}
	}

	private static final Interner<CompilationTask> INTERNER = Interners.newWeakInterner();

	private static final LoadingCache<CompilationTask, Pattern> patternCache = CacheUtil.buildLoadingCache(new CacheLoader<CompilationTask, Pattern>(){

		@Override
		public Pattern load(CompilationTask compilationTask){
			return compilationTask.call();
		}
	});
}