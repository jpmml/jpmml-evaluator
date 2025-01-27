/*
 * Copyright (c) 2018 Villu Ruusmann
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
package org.jpmml.evaluator.example;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.beust.jcommander.Parameter;
import com.google.common.base.Joiner;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.adapters.NodeAdapter;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.NodeTransformer;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.evaluator.mining.HasSegmentResults;
import org.jpmml.evaluator.mining.SegmentResult;
import org.jpmml.evaluator.tree.HasDecisionPath;
import org.jpmml.model.visitors.AbstractVisitor;

public class RecordCountingExample extends Example {

	@Parameter (
		names = {"--model"},
		description = "Model PMML file",
		required = true
	)
	@ParameterOrder (
		value = 1
	)
	private File model = null;

	@Parameter (
		names = {"--input"},
		description = "Input CSV file",
		required = true
	)
	@ParameterOrder (
		value = 2
	)
	private File input = null;

	@Parameter (
		names = {"--separator"},
		description = "CSV cell separator character",
		converter = SeparatorConverter.class
	)
	@ParameterOrder (
		value = 3
	)
	private String separator = null;

	@Parameter (
		names = {"--missing-values"},
		description = "CSV missing value strings"
	)
	@ParameterOrder (
		value = 4
	)
	private List<String> missingValues = Arrays.asList("N/A", "NA");


	static
	public void main(String... args) throws Exception {
		execute(RecordCountingExample.class, args);
	}

	@Override
	public void execute() throws Exception {
		PMML pmml;

		NodeTransformer defaultNodeTransformer = NodeAdapter.NODE_TRANSFORMER_PROVIDER.get();

		try {
			NodeAdapter.NODE_TRANSFORMER_PROVIDER.set(null);

			pmml = readPMML(this.model);
		} finally {
			NodeAdapter.NODE_TRANSFORMER_PROVIDER.set(defaultNodeTransformer);
		}

		Function<String, String> cellParser = createCellParser(new HashSet<>(this.missingValues));

		Table table = readTable(this.input, this.separator);
		table.apply(cellParser);

		ModelEvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml);

		Evaluator evaluator = evaluatorBuilder.build();

		// Perform self-testing
		evaluator.verify();

		List<InputField> inputFields = evaluator.getInputFields();
		List<TargetField> targetFields = evaluator.getTargetFields();

		Table.Row arguments = table.new Row(0);

		for(int i = 0, max = table.getNumberOfRows(); i < max; i++){
			Map<String, ?> results = evaluator.evaluate(arguments);

			for(TargetField targetField : targetFields){
				String name = targetField.getName();

				Object value = results.get(name);

				HasSegmentResults hasSegmentResults = TypeUtil.cast(HasSegmentResults.class, value);

				Collection<? extends SegmentResult> segmentResults = hasSegmentResults.getSegmentResults();
				for(SegmentResult segmentResult : segmentResults){
					Object segmentValue = segmentResult.getTargetValue();

					HasDecisionPath hasDecisionPath = TypeUtil.cast(HasDecisionPath.class, segmentValue);

					List<Node> nodes = hasDecisionPath.getDecisionPath();
					for(Node node : nodes){
						Number recordCount = node.getRecordCount();

						if(recordCount == null){
							recordCount = 1d;
						} else

						{
							recordCount = (recordCount.doubleValue() + 1d);
						}

						node.setRecordCount(recordCount);
					}
				}
			}

			arguments.advance();
		}

		Joiner joiner = Joiner.on(separator);

		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(PMML pmml){
				printRow("Segment@id", "Node@id", "Node@recordCount", "depth");

				return super.visit(pmml);
			}

			@Override
			public VisitorAction visit(Node node){
				Deque<PMMLObject> parents = getParents();

				int depth = 0;

				Iterator<PMMLObject> parentIt = parents.iterator();

				while(true){
					PMMLObject parent = parentIt.next();

					if(parent instanceof Node){
						depth++;

						continue;
					}

					TreeModel treeModel = (TreeModel)parent;
					Segment segment = (Segment)parentIt.next();

					Number recordCount = node.getRecordCount();
					if(recordCount == null){
						recordCount = 0d;
					}

					printRow(segment.getId(), node.getId(), recordCount, depth);

					break;
				}

				return super.visit(node);
			}

			private void printRow(Object... cells){

				for(int i = 0; i < cells.length; i++){

					if(cells[i] == null){
						cells[i] = RecordCountingExample.this.missingValues.get(0);
					}
				}

				System.out.println(joiner.join(cells));
			}
		};

		visitor.applyTo(pmml);

		writePMML(pmml, this.model);
	}
}