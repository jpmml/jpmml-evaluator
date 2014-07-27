/*
 * Copyright (c) 2013 KNIME.com AG, Zurich, Switzerland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jpmml.evaluator;

import java.util.List;
import java.util.Map;

import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkArgument;

@Beta
public class InstanceClassificationMap extends ClassificationMap<String> implements HasEntityIdRanking, HasClusterId, HasAffinityRanking, HasClusterAffinity {

	private Object result = null;


	protected InstanceClassificationMap(Type type, Object result){
		super(type);

		checkArgument((Type.DISTANCE).equals(type) || (Type.SIMILARITY).equals(type));

		setResult(result);
	}

	@Override
	public Object getResult(){

		if(this.result == null){
			throw new EvaluationException();
		}

		return this.result;
	}

	private void setResult(Object result){
		this.result = result;
	}

	@Override
	public String getEntityId(){
		Map.Entry<String, Double> entry = getWinner();
		if(entry == null){
			return null;
		}

		return entry.getKey();
	}

	@Override
	public List<String> getEntityIdRanking(){
		return getWinnerKeys();
	}

	@Override
	public String getClusterId(){
		return getEntityId();
	}

	@Override
	public Double getAffinity(String id){
		return getFeature(id);
	}

	@Override
	public List<Double> getAffinityRanking(){
		return getWinnerValues();
	}

	@Override
	public Double getClusterAffinity(){
		return getAffinity(getClusterId());
	}
}