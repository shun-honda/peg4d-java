package org.peg4d;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.peg4d.jvm.ClassBuilder;
import org.peg4d.jvm.Methods;
import org.peg4d.jvm.UserDefinedClassLoader;
import org.peg4d.jvm.ClassBuilder.MethodBuilder;
import org.peg4d.jvm.ClassBuilder.VarEntry;

public class JvmByteCodeGenerator extends GrammarFormatter implements Opcodes {
	//private final static String packagePrefix = "org/peg4d/genrated/";
	public final static String packagePrefix = "org/peg4d/";

	private static int nameSuffix = -1;

	private ClassBuilder cBuilder;

	/**
	 * current method builder
	 */
	private MethodBuilder mBuilder;

	/**
	 * represents argument (ParsingContext ctx)
	 */
	private VarEntry argEntry;

	@Override
	public String getDesc() {
		return "JVM ";
	}

	@Override
	public void formatHeader(StringBuilder sb) {	// initialize class builder.
		this.cBuilder = new ClassBuilder(packagePrefix + "GeneratedParser" + ++nameSuffix, null, null, null);
	}

	@Override
	public void formatFooter(StringBuilder sb) {
		this.cBuilder.visitEnd();	// finalize class builder
	}

	//FIXME: currently not support rule name, ':'.
	@Override
	public void formatRule(String ruleName, ParsingExpression e, StringBuilder sb) { // not use string builder
		String methodName = ruleName;	// currently, equivalent to rule name, FIXME:

		/**
		 * create new method builder. 
		 * ex. FILE ->  public static boolean FILE(ParsingContext ctx)
		 */
		this.mBuilder = this.cBuilder.newMethodBuilder(ACC_PUBLIC | ACC_STATIC, boolean.class, methodName, ParsingContext.class);

		// initialize
		this.mBuilder.enterScope(); // enter block scope
		this.argEntry = this.mBuilder.defineArgument(ParsingContext.class);	// represent first argument of generating method

		// generate method body
		e.visit(this);

		// finalize
		this.mBuilder.exitScope();
		this.mBuilder.returnValue(); // return stack top value (must be boolean type)
		this.mBuilder.endMethod();
	}

	/**
	 * finalize class builder and generate class form byte code
	 * @return
	 * generated parser class
	 */
	public Class<?> generateClass() {
		UserDefinedClassLoader loader = new UserDefinedClassLoader();
		return loader.definedAndLoadClass(this.cBuilder.getInternalName(), cBuilder.toByteArray());
	}

	// helper method.
	private void generateFailure() { // generate equivalent code to ParsingContext#failure
		
	}

	/**
	 * generate code of access ParsingContext field and put field value at stack top
	 * @param fieldName
	 * @param fieldClass
	 */
	private void generateFieldAccessOfParsingContext(String fieldName, Class<?> fieldClass) {
		this.mBuilder.loadFromVar(this.argEntry);
		this.mBuilder.getField(Type.getType(ParsingContext.class), fieldName, Type.getType(fieldClass));
	}

	// visitor api
	@Override
	public void visitNonTerminal(NonTerminal e) {
		Method methodDesc = Methods.method(boolean.class, e.ruleName, ParsingContext.class);
		this.mBuilder.loadFromVar(this.argEntry);
		this.mBuilder.invokeStatic(this.cBuilder.getTypeDesc(), methodDesc);
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
		this.mBuilder.push(true);
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		this.generateFailure();
		this.mBuilder.push(false);
	}

	@Override
	public void visitByte(ParsingByte e) {
		// generate byteAt
		Method methodDesc_byteAt = Methods.method(int.class, "byteAt", long.class);

		this.generateFieldAccessOfParsingContext("source", ParsingSource.class);
		this.generateFieldAccessOfParsingContext("pos", long.class);
		this.mBuilder.invokeVirtual(Type.getType(ParsingSource.class), methodDesc_byteAt);

		// compare to byteChar
		this.mBuilder.push(e.byteChar);
		this.mBuilder.math(GeneratorAdapter.EQ, Type.INT_TYPE);

		// generate if cond
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);

		// generate if block
		this.mBuilder.enterScope();

		Method methodDesc_consume = Methods.method(void.class, "consume", int.class);

		this.mBuilder.loadFromVar(this.argEntry);
		this.mBuilder.push((int) 1);
		this.mBuilder.invokeVirtual(Type.getType(ParsingContext.class), methodDesc_consume);
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		this.mBuilder.exitScope();

		// generate else block
		this.mBuilder.mark(elseLabel);
		this.generateFailure();
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitString(ParsingString e) {	//FIXME:
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAny(ParsingAny e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitValue(ParsingValue e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitUnary(ParsingUnary e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitNot(ParsingNot e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitOptional(ParsingOption e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitExport(ParsingExport e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitList(ParsingList e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitParsingFunction(ParsingFunction parsingFunction) {
		throw new RuntimeException("unimplemented visit method: " + parsingFunction.getClass());
	}

	@Override
	public void visitParsingOperation(ParsingOperation e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitParsingIfFlag(ParsingIf e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}
}
