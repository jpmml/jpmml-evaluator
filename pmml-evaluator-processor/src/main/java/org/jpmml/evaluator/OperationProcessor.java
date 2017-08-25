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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.codemodel.writer.FilterCodeWriter;

@SupportedAnnotationTypes (
	value = {"org.jpmml.evaluator.Operation"}
)
public class OperationProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
		final
		JCodeModel codeModel = new JCodeModel();

		try {
			createReportingClass(codeModel, "DoubleValue", codeModel.DOUBLE);
			createReportingClass(codeModel, "FloatValue", codeModel.FLOAT);

			createReportingClass(codeModel, "ComplexDoubleVector", codeModel.DOUBLE);
			createReportingClass(codeModel, "ComplexFloatVector", codeModel.FLOAT);
			createReportingClass(codeModel, "SimpleDoubleVector", codeModel.DOUBLE);
			createReportingClass(codeModel, "SimpleFloatVector", codeModel.FLOAT);

			File outputDir = new File(System.getProperty("basedir"), "target/generated-sources/apt");
			if(!outputDir.exists()){
				outputDir.mkdirs();
			}

			CodeWriter codeWriter = new FilterCodeWriter(new FileCodeWriter(outputDir, "UTF-8")){

				@Override
				public Writer openSource(JPackage pkg, String fileName) throws IOException {
					Writer writer = super.openSource(pkg, fileName);

					writer.write(OperationProcessor.LICENSE);

					return writer;
				}
			};

			try {
				codeModel.build(codeWriter);
			} finally {
				codeWriter.close();
			}
		} catch(Exception e){
			throw new RuntimeException(e);
		}

		return true;
	}

	private void createReportingClass(JCodeModel codeModel, String name, JPrimitiveType type) throws JClassAlreadyExistsException {
		Elements elements = super.processingEnv.getElementUtils();
		Types types = super.processingEnv.getTypeUtils();

		TypeElement operationElement = elements.getTypeElement("org.jpmml.evaluator.Operation");

		DeclaredType operationType = types.getDeclaredType(operationElement);

		ExecutableElement valueElement = getValueMethod(operationElement);

		JDefinedClass clazz = codeModel._class(JMod.PUBLIC, "org.jpmml.evaluator.Reporting" + name, ClassType.CLASS);
		clazz._extends(codeModel.ref("org.jpmml.evaluator." + name));
		clazz._implements(codeModel.ref("org.jpmml.evaluator.HasReport"));

		JFieldVar reportField = clazz.field(JMod.PRIVATE, String.class, "report", JExpr.lit(""));

		if(name.endsWith("Value")){
			createCopyMethod(clazz);
		}

		TypeElement clazzElement = elements.getTypeElement("org.jpmml.evaluator." + name);

		for(int level = 0; clazzElement != null; level++){
			TypeMirror superClazz = clazzElement.getSuperclass();

			List<? extends Element> enclosedElements = clazzElement.getEnclosedElements();
			for(Element enclosedElement : enclosedElements){

				if(enclosedElement instanceof ExecutableElement){
					ExecutableElement executableElement = (ExecutableElement)enclosedElement;

					ElementKind kind = executableElement.getKind();

					AnnotationMirror annotationMirror = getAnnotation(executableElement, operationType);
					if(annotationMirror != null){
						Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();

						AnnotationValue elementValue = elementValues.get(valueElement);
						if(elementValue == null){
							throw new RuntimeException();
						}

						switch(kind){
							case CONSTRUCTOR:
								createReportingConstructor(clazz, executableElement, (String)elementValue.getValue(), type);
								break;
							case METHOD:
								createReportingMethod(clazz, executableElement, (String)elementValue.getValue(), type);
								break;
							default:
								break;
						}
					} else

					{
						if(level != 0){
							continue;
						}

						switch(kind){
							case CONSTRUCTOR:
								createConstructor(clazz, executableElement);
								break;
							default:
								break;
						}
					}
				}
			}

			if(superClazz instanceof DeclaredType){
				DeclaredType declaredType = (DeclaredType)superClazz;

				clazzElement = (TypeElement)declaredType.asElement();

				continue;
			}

			break;
		}

		if(name.endsWith("Vector")){
			createValueMethods(clazz, type);
		}

		createReportAccessorMethods(clazz, reportField);

		if(name.endsWith("Value")){
			createFormatMethod(clazz, type);
		}
	}

	private ExecutableElement getValueMethod(TypeElement element){
		List<? extends Element> enclosedElements = element.getEnclosedElements();

		for(Element enclosedElement : enclosedElements){

			if(("value").equals(String.valueOf(enclosedElement.getSimpleName()))){
				return (ExecutableElement)enclosedElement;
			}
		}

		throw new RuntimeException();
	}

	private AnnotationMirror getAnnotation(Element element, DeclaredType annotationType){
		Types types = super.processingEnv.getTypeUtils();

		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		for(AnnotationMirror annotationMirror : annotationMirrors){

			if(types.isSameType(annotationMirror.getAnnotationType(), annotationType)){
				return annotationMirror;
			}
		}

		return null;
	}

	static
	private void createReportingConstructor(JDefinedClass clazz, ExecutableElement executableElement, String operation, JPrimitiveType type){
		JCodeModel codeModel = clazz.owner();

		JClass stringBufferClazz = codeModel.ref(StringBuffer.class);

		JMethod constructor = clazz.constructor(JMod.PUBLIC);

		List<? extends VariableElement> parameterElements = executableElement.getParameters();
		for(VariableElement parameterElement : parameterElements){
			constructor.param(toType(codeModel, parameterElement.asType()), String.valueOf(parameterElement.getSimpleName()));
		}

		JBlock body = constructor.body();

		body.add(createSuperInvocation(clazz, constructor));

		JVar sbVariable = body.decl(stringBufferClazz, "sb", JExpr._new(stringBufferClazz).arg(JExpr.lit(256)));

		body.add(createReportInvocation(clazz, sbVariable, operation, constructor.params(), type));

		body.add(JExpr.invoke("setReport").arg(sbVariable.invoke("toString")));

		overloadConstructor(clazz, constructor);
	}

	static
	private void createConstructor(JDefinedClass clazz, ExecutableElement executableElement){
		JCodeModel codeModel = clazz.owner();

		JMethod constructor = clazz.constructor(JMod.PUBLIC);

		List<? extends VariableElement> parameterElements = executableElement.getParameters();
		for(VariableElement parameterElement : parameterElements){
			constructor.param(toType(codeModel, parameterElement.asType()), String.valueOf(parameterElement.getSimpleName()));
		}

		JBlock body = constructor.body();

		body.add(createSuperInvocation(clazz, constructor));

		overloadConstructor(clazz, constructor);
	}

	static
	private void overloadConstructor(JDefinedClass clazz, JMethod constructor){
		JMethod overloadedConstructor = clazz.constructor(0);

		List<JVar> parameters = constructor.params();
		for(JVar parameter : parameters){
			overloadedConstructor.param(parameter.type(), parameter.name());
		}

		JBlock body = overloadedConstructor.body();

		body.add(createSuperInvocation(clazz, overloadedConstructor));

		JVar reportParameter = overloadedConstructor.param(String.class, "report");

		body.add(JExpr.invoke("setReport").arg(reportParameter));
	}

	static
	private void createReportingMethod(JDefinedClass clazz, ExecutableElement executableElement, String operation, JPrimitiveType type){
		JCodeModel codeModel = clazz.owner();

		JClass stringBufferClazz = codeModel.ref(StringBuffer.class);

		JMethod method = clazz.method(JMod.PUBLIC, clazz, String.valueOf(executableElement.getSimpleName()));
		method.annotate(Override.class);

		List<? extends VariableElement> parameterElements = executableElement.getParameters();
		for(VariableElement parameterElement : parameterElements){
			method.param(toType(codeModel, parameterElement.asType()), String.valueOf(parameterElement.getSimpleName()));
		}

		JBlock body = method.body();

		JVar resultVariable = body.decl(clazz, "result", JExpr.cast(clazz, createSuperInvocation(clazz, method)));

		JVar sbVariable = body.decl(stringBufferClazz, "sb", JExpr._new(stringBufferClazz).arg(JExpr.lit(256)));

		body.add(createReportInvocation(clazz, sbVariable, operation, method.params(), type));

		body.add(JExpr.invoke("setReport").arg(sbVariable.invoke("toString")));

		body._return(resultVariable);
	}

	static
	private void createCopyMethod(JDefinedClass clazz){
		JMethod method = clazz.method(JMod.PUBLIC, clazz, "copy");
		method.annotate(Override.class);

		JBlock body = method.body();

		body._return(JExpr._new(clazz).arg(JExpr._super().ref("value")));
	}

	static
	private void createValueMethods(JDefinedClass clazz, JPrimitiveType type){
		JCodeModel codeModel = clazz.owner();

		if((codeModel.DOUBLE).equals(type)){
			JClass valueClazz = codeModel.ref("org.jpmml.evaluator.ReportingDoubleValue");

			createGetMethod(clazz, valueClazz, JExpr.direct("doubleValue(index)"));

			createAggregationMethod(clazz, valueClazz, "max", JExpr.invoke("doubleMax"), "<apply><max/>${this}</apply>", type);
			createAggregationMethod(clazz, valueClazz, "median", JExpr.invoke("doubleMedian"), "<apply><median/>${this}</apply>", type);
			createAggregationMethod(clazz, valueClazz, "sum", JExpr.invoke("doubleSum"), "<apply><plus/>${this}</apply>", type);
		} else

		if((codeModel.FLOAT).equals(type)){
			JClass valueClazz = codeModel.ref("org.jpmml.evaluator.ReportingFloatValue");

			createGetMethod(clazz, valueClazz, JExpr.direct("floatValue(index)"));

			createAggregationMethod(clazz, valueClazz, "max", JExpr.invoke("floatMax"), "<apply><max/>${this}</apply>", type);
			createAggregationMethod(clazz, valueClazz, "median", JExpr.invoke("floatMedian"), "<apply><median/>${this}</apply>", type);
			createAggregationMethod(clazz, valueClazz, "sum", JExpr.invoke("floatSum"), "<apply><plus/>${this}</apply>", type);
		} else

		{
			throw new RuntimeException();
		}
	}

	static
	private void createGetMethod(JDefinedClass clazz, JClass valueClazz, JExpression valueExpression){
		JCodeModel codeModel = clazz.owner();

		JMethod method = clazz.method(JMod.PUBLIC, valueClazz, "get");
		method.annotate(Override.class);

		method.param(codeModel.INT, "index");

		JBlock body = method.body();

		body._return(JExpr._new(valueClazz).arg(valueExpression));
	}

	static
	private void createAggregationMethod(JDefinedClass clazz, JClass valueClazz, String name, JExpression valueExpression, String operation, JPrimitiveType type){
		JCodeModel codeModel = clazz.owner();

		JClass stringBufferClazz = codeModel.ref(StringBuffer.class);

		JMethod method = clazz.method(JMod.PUBLIC, valueClazz, name);
		method.annotate(Override.class);

		JBlock body = method.body();

		JVar sbVariable = body.decl(stringBufferClazz, "sb", JExpr._new(stringBufferClazz).arg(JExpr.lit(256)));

		body.add(createReportInvocation(clazz, sbVariable, operation, Collections.<JVar>emptyList(), type));

		body._return(JExpr._new(valueClazz).arg(valueExpression).arg(sbVariable.invoke("toString")));
	}

	static
	private void createReportAccessorMethods(JDefinedClass clazz, JFieldVar reportField){

		if(true){
			JMethod getterMethod = clazz.method(JMod.PUBLIC, String.class, "getReport");
			getterMethod.annotate(Override.class);

			JBlock body = getterMethod.body();

			body._return(JExpr.refthis(reportField.name()));
		} // End if

		if(true){
			JMethod setterMethod = clazz.method(JMod.PRIVATE, void.class, "setReport");

			JVar reportParameter = setterMethod.param(String.class, reportField.name());

			JBlock body = setterMethod.body();

			body.assign(JExpr.refthis(reportField.name()), reportParameter);
		}
	}

	static
	private void createFormatMethod(JDefinedClass clazz, JPrimitiveType type){
		JCodeModel codeModel = clazz.owner();

		JClass numberClazz = codeModel.ref(Number.class);
		JClass stringBufferClazz = codeModel.ref(StringBuffer.class);

		JType numberListClazz;

		try {
			numberListClazz = codeModel.parseType("java.util.List<? extends java.lang.Number>");
		} catch(ClassNotFoundException cnfe){
			throw new RuntimeException(cnfe);
		}

		JMethod method = clazz.method(JMod.STATIC | JMod.PRIVATE, String.class, "format");

		JVar valuesParameter = method.param(numberListClazz, "values");

		JBlock body = method.body();

		JVar sbVariable = body.decl(stringBufferClazz, "sb", JExpr._new(stringBufferClazz).arg(valuesParameter.invoke("size").mul(JExpr.lit(32))));

		JForEach forStatement = body.forEach(numberClazz, "value", valuesParameter);

		JBlock forBody = forStatement.body();

		forBody.add(createReportInvocation(clazz, sbVariable, "${0}", Collections.singletonList(forStatement.var()), type));

		body._return(sbVariable.invoke("toString"));
	}

	static
	private JInvocation createSuperInvocation(JDefinedClass clazz, JMethod method){
		JInvocation invocation;

		if(method.type() != null){
			invocation = JExpr._super().invoke(method.name());
		} else

		{
			invocation = JExpr.invoke("super");
		}

		List<JVar> parameters = method.params();
		for(JVar parameter : parameters){
			invocation.arg(parameter);
		}

		return invocation;
	}

	static
	private JInvocation createReportInvocation(JDefinedClass clazz, JVar sbVariable, String operation, List<JVar> parameters, JPrimitiveType type){
		JCodeModel codeModel = clazz.owner();

		JClass numberClazz = codeModel.ref(Number.class);

		JType valueClazz;

		try {
			valueClazz = codeModel.parseType("org.jpmml.evaluator.Value<? extends java.lang.Number>");
		} catch(ClassNotFoundException cnfe){
			throw new RuntimeException(cnfe);
		}

		Matcher matcher = OperationProcessor.PATTERN.matcher(operation);

		JExpression invocation = sbVariable;

		int pos = 0;

		while(matcher.find()){
			String id = matcher.group(1);

			String string = operation.substring(pos, matcher.start());
			if(string.length() > 0){
				invocation = appendContent(invocation, JExpr.lit(string));
			} // End if

			if(("this").equals(id)){
				invocation = appendContent(invocation, JExpr.invoke("getReport"));
			} else

			{
				int index = Integer.parseInt(id);

				JVar parameter = parameters.get(index);

				JType parameterType = parameter.type();
				if(parameterType.isPrimitive()){
					JExpression value;

					if(parameterType.equals(type)){
						value = parameter;
					} else

					{
						value = JExpr.cast(type, parameter);
					}

					invocation = appendContent(invocation, JExpr.lit("<cn>"), value, JExpr.lit("</cn>"));
				} else

				if((parameterType.fullName()).equals(numberClazz.fullName()) || (parameterType.fullName()).equals(valueClazz.fullName())){
					JExpression value;

					if((codeModel.DOUBLE).equals(type)){
						value = parameter.invoke("doubleValue");
					} else

					if((codeModel.FLOAT).equals(type)){
						value = parameter.invoke("floatValue");
					} else

					{
						throw new RuntimeException();
					}

					invocation = appendContent(invocation, JExpr.lit("<cn>"), value, JExpr.lit("</cn>"));
				} else

				{
					invocation = appendContent(invocation, JExpr.invoke("format").arg(parameter));
				}
			}

			pos = matcher.end();
		}

		if(pos < operation.length()){
			invocation = appendContent(invocation, JExpr.lit(operation.substring(pos)));
		}

		return (JInvocation)invocation;
	}

	static
	private JExpression appendContent(JExpression expression, JExpression... arguments){
		JInvocation invocation = null;

		for(JExpression argument : arguments){

			if(invocation == null){

				if(expression instanceof JVar){
					JVar variable = (JVar)expression;

					invocation = variable.invoke("append").arg(argument);
				} else

				if(expression instanceof JInvocation){
					invocation = (JInvocation)expression;

					invocation = invocation.invoke("append").arg(argument);
				} else

				{
					throw new RuntimeException();
				}
			} else

			{
				invocation = invocation.invoke("append").arg(argument);
			}
		}

		return invocation;
	}

	static
	private JType toType(JCodeModel codeModel, TypeMirror typeMirror){
		String name = typeMirror.toString();

		if(name.endsWith("Value<?>")){
			name = name.substring(0, name.length() - "Value<?>".length()) + "Value<? extends java.lang.Number>";
		}

		try {
			return codeModel.parseType(name);
		} catch(ClassNotFoundException cnfe){
			throw new RuntimeException(cnfe);
		}
	}

	private static final Pattern PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

	private static final String LICENSE =
		"/*\n" +
		" * Copyright (c) 2017 Villu Ruusmann\n" +
		" *\n" +
		" * This file is part of JPMML-Evaluator\n" +
		" *\n" +
		" * JPMML-Evaluator is free software: you can redistribute it and/or modify\n" +
		" * it under the terms of the GNU Affero General Public License as published by\n" +
		" * the Free Software Foundation, either version 3 of the License, or\n" +
		" * (at your option) any later version.\n" +
		" *\n" +
		" * JPMML-Evaluator is distributed in the hope that it will be useful,\n" +
		" * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
		" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
		" * GNU Affero General Public License for more details.\n" +
		" *\n" +
		" * You should have received a copy of the GNU Affero General Public License\n" +
		" * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.\n" +
		" */";
}