package org.peg4d;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingFunction;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOperation;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.jvm.ClassBuilder;
import org.peg4d.jvm.InvocationTarget;
import static org.peg4d.jvm.InvocationTarget.*;
import org.peg4d.jvm.Methods;
import org.peg4d.jvm.UserDefinedClassLoader;
import org.peg4d.jvm.ClassBuilder.MethodBuilder;
import org.peg4d.jvm.ClassBuilder.VarEntry;

public class JvmByteCodeGenerator extends GrammarFormatter implements Opcodes {
	private final static String packagePrefix = "org/peg4d/generated/";

	private static int nameSuffix = -1;

	private ClassBuilder cBuilder;

	/**
	 * current method builder
	 */
	private MethodBuilder mBuilder;

	/**
	 * represents argument (ParsingContext ctx)
	 */
	private VarEntry entry_context;

	// invocation target
	InvocationTarget target_byteAt = newVirtualTarget(ParsingSource.class, int.class, "byteAt", long.class);
	InvocationTarget target_consume = newVirtualTarget(ParsingContext.class, void.class, "consume", int.class);
	InvocationTarget target_getPosition = newVirtualTarget(ParsingContext.class, long.class, "getPosition");
	InvocationTarget target_rollback = newVirtualTarget(ParsingContext.class, void.class, "rollback", long.class);
	InvocationTarget target_isFailure = newVirtualTarget(ParsingContext.class, boolean.class, "isFailure");
	InvocationTarget target_markObjectStack = newVirtualTarget(ParsingContext.class, int.class, "markObjectStack");
	InvocationTarget target_abortLinkLog = newVirtualTarget(ParsingContext.class, void.class, "abortLinkLog", int.class);
	InvocationTarget target_rememberFailure = newVirtualTarget(ParsingContext.class, long.class, "rememberFailure");
	InvocationTarget target_forgetFailure = newVirtualTarget(ParsingContext.class, void.class, "forgetFailure", long.class);


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
		this.entry_context = this.mBuilder.defineArgument(ParsingContext.class);	// represent first argument of generating method

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
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// if cond
		this.getFieldOfContext("pos", long.class);
		this.getFieldOfContext("fpos", long.class);

		this.mBuilder.ifCmp(Type.LONG_TYPE, GeneratorAdapter.GT, thenLabel);

		// else
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.pushNull();
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.dup();
		this.mBuilder.getField(Type.getType(ParsingContext.class), "pos", Type.getType(long.class));
		this.mBuilder.putField(Type.getType(ParsingContext.class), "fpos", Type.getType(long.class));

		//merge
		this.mBuilder.mark(mergeLabel);
	}

	/**
	 * generate code of access ParsingContext field and put field value at stack top
	 * @param fieldName
	 * @param fieldClass
	 */
	private void getFieldOfContext(String fieldName, Class<?> fieldClass) {
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), fieldName, Type.getType(fieldClass));
	}

	// visitor api
	@Override
	public void visitNonTerminal(NonTerminal e) {
		Method methodDesc = Methods.method(boolean.class, e.ruleName, ParsingContext.class);
		this.mBuilder.loadFromVar(this.entry_context);
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
		// generate if cond
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// generate byteAt

		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInvocationTarget(this.target_byteAt);

		// push byteChar
		this.mBuilder.push(e.byteChar);
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.NE, elseLabel);

		// generate if block
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.push((int) 1);
		this.mBuilder.callInvocationTarget(this.target_consume);
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		// generate else block
		this.mBuilder.mark(elseLabel);
		this.generateFailure();
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		this.mBuilder.enterScope();

		// generate byteAt
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInvocationTarget(this.target_byteAt);

		// generate variable
		VarEntry entry_ch = this.mBuilder.createNewVarAndStore(int.class);

		Label andRightLabel = this.mBuilder.newLabel();
		Label thenLabel = this.mBuilder.newLabel();
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// and left
		this.mBuilder.push(e.startByteChar);
		this.mBuilder.loadFromVar(entry_ch);
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.LE, andRightLabel);
		this.mBuilder.goTo(elseLabel);

		// and right
		this.mBuilder.mark(andRightLabel);
		this.mBuilder.loadFromVar(entry_ch);
		this.mBuilder.push(e.endByteChar);
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.LE, thenLabel);
		this.mBuilder.goTo(elseLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.push(1);
		this.mBuilder.callInvocationTarget(this.target_consume);
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		// else 
		this.mBuilder.mark(elseLabel);
		this.generateFailure();
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitString(ParsingString e) {	//FIXME:
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAny(ParsingAny e) {
		// generate charAt
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "charAt", long.class);

		// compare to -1
		this.mBuilder.push((int) -1);

		// generate if condition
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.EQ, elseLabel);

		// generate if block
		this.mBuilder.enterScope();
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "charLength", long.class);

		VarEntry entry_len = this.mBuilder.createNewVarAndStore(int.class);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_len);
		this.mBuilder.callInvocationTarget(this.target_consume);
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
	public void visitTagging(ParsingTagging e) {
		this.getFieldOfContext("left", ParsingObject.class);

		// new ParsingTag(String tagName)
		Type typeDesc = Type.getType(ParsingTag.class);
		this.mBuilder.newInstance(typeDesc);
		this.mBuilder.dup();
		this.mBuilder.push(e.tag.toString());	// push tag name
		this.mBuilder.invokeConstructor(typeDesc, Methods.constructor(String.class));

		this.mBuilder.callInstanceMethod(ParsingObject.class, void.class, "setTag", ParsingTag.class);
		this.mBuilder.push(true);
	}

	@Override
	public void visitValue(ParsingValue e) {
		this.getFieldOfContext("left", ParsingObject.class);

		this.mBuilder.push(e.value);
		this.mBuilder.callInstanceMethod(ParsingObject.class, void.class, "setValue", String.class);
		this.mBuilder.push(true);
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

//	@Override
//	public void visitUnary(ParsingUnary e) {
//		throw new RuntimeException("unimplemented visit method: " + e.getClass());
//	}

	@Override
	public void visitNot(ParsingNot e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_rememberFailure);
		VarEntry entry_f = this.mBuilder.createNewVarAndStore(long.class);

		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		// if cond
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		e.inner.visit(this);
		this.mBuilder.push(true);
		this.mBuilder.ifICmp(Type.BOOLEAN, thenLabel);

		// else
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_f);
		this.mBuilder.callInvocationTarget(this.target_forgetFailure);

		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.loadFromVar(entry_left);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(true);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);
		this.generateFailure();
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		this.mBuilder.enterScope();

		// generate getPosition
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);

		// generate variable
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		e.visit(this);

		// generate rollback
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);
		
		// generate isFailure
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_isFailure);

		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NEG, elseLabel);
		// then
		this.mBuilder.push(false);
		this.mBuilder.goTo(mergeLabel);

		// else
		this.mBuilder.mark(elseLabel);
		this.mBuilder.push(true);
		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitOptional(ParsingOption e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_rememberFailure);
		VarEntry entry_f = this.mBuilder.createNewVarAndStore(long.class);

		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		// if cond
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		e.inner.visit(this);
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, thenLabel);

		// else
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_left);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_f);
		this.mBuilder.callInvocationTarget(this.target_forgetFailure);

		// merge
		this.mBuilder.mark(mergeLabel);
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(true);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.push((long)-1);
		VarEntry entry_ppos = this.mBuilder.createNewVarAndStore(long.class);

		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		Label continueLabel = this.mBuilder.newLabel();
		Label breakLabel = this.mBuilder.newLabel();
		Label whileBlockLabel = this.mBuilder.newLabel();

		// while continue
		this.mBuilder.mark(continueLabel);
		// while cond
		this.mBuilder.loadFromVar(entry_ppos);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.ifCmp(Type.LONG_TYPE, GeneratorAdapter.LT, whileBlockLabel);
		this.mBuilder.goTo(breakLabel);

		// while then
		{
			this.mBuilder.mark(whileBlockLabel);
			this.mBuilder.enterScope();

			this.getFieldOfContext("left", ParsingObject.class);
			VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

			Label thenLabel = this.mBuilder.newLabel();
			Label mergeLabel = this.mBuilder.newLabel();

			e.inner.visit(this);
			this.mBuilder.push(true);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, thenLabel);
			this.mBuilder.goTo(mergeLabel);

			// then
			this.mBuilder.mark(thenLabel);
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

			this.mBuilder.pushNull();
			this.mBuilder.storeToVar(entry_left);
			this.mBuilder.goTo(breakLabel);

			// merge
			this.mBuilder.mark(mergeLabel);
			this.mBuilder.loadFromVar(entry_pos);
			this.mBuilder.storeToVar(entry_ppos);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.callInvocationTarget(this.target_getPosition);
			this.mBuilder.storeToVar(entry_pos);

			this.mBuilder.pushNull();
			this.mBuilder.storeToVar(entry_left);

			this.mBuilder.exitScope();
			this.mBuilder.goTo(continueLabel);
		}

		// break
		this.mBuilder.mark(breakLabel);
		this.mBuilder.push(true);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitExport(ParsingExport e) {	//TODO:
		this.mBuilder.push(true);
	}

//	@Override
//	public void visitList(ParsingList e) {
//		throw new RuntimeException("unimplemented visit method: " + e.getClass());
//	}

	@Override
	public void visitSequence(ParsingSequence e) {
		this.mBuilder.enterScope();

		// generate context.getPosition
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);

		// store to pos
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		// generate context.markObjectStack
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_markObjectStack);
		// store to mark
		VarEntry entry_mark = this.mBuilder.createNewVarAndStore(int.class);

		// temporary contains return value.
		this.mBuilder.push(true);
		VarEntry entry_return = this.mBuilder.createNewVarAndStore(boolean.class);

		Label breakLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();
		for(int i = 0; i < e.size(); i++) {
			// if cond
			e.get(i).visit(this);	// TODO: support matcher
			this.mBuilder.push(false);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, breakLabel);
		}
		this.mBuilder.goTo(mergeLabel);

		// break
		this.mBuilder.mark(breakLabel);
		// generate abortLinkLog
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_mark);
		this.mBuilder.callInvocationTarget(this.target_abortLinkLog);

		// generate rollback
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);

		// save return value
		this.mBuilder.push(false);
		this.mBuilder.storeToVar(entry_return);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.loadFromVar(entry_return);

		this.mBuilder.exitScope();
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		this.mBuilder.enterScope();

		// generate context.rememberFailure and store to f
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_rememberFailure);
		VarEntry entry_f = this.mBuilder.createNewVarAndStore(long.class);

		// generate context.left and store to left
		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		// temporary contains return value
		this.mBuilder.push(false);
		VarEntry entry_return = this.mBuilder.createNewVarAndStore(boolean.class);

		Label breakLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();
		for(int i = 0; i < e.size(); i++) {
			// store to context.left
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

			e.get(i).visit(this);	// simpleMatch
			this.mBuilder.push(true);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, breakLabel);
		}
		this.mBuilder.goTo(mergeLabel);

		// break
		this.mBuilder.mark(breakLabel);
		// generate context.forgetFailure
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_f);
		this.mBuilder.callInvocationTarget(this.target_forgetFailure);

		// save return value
		this.mBuilder.push(true);
		this.mBuilder.storeToVar(entry_return);

		// merge
		this.mBuilder.mark(mergeLabel);

		// store null to left
		this.mBuilder.pushNull();
		this.mBuilder.storeToVar(entry_left);

		// push return value
		this.mBuilder.loadFromVar(entry_return);

		this.mBuilder.exitScope();
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
