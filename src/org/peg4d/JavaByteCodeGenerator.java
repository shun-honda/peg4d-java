package org.peg4d;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.peg4d.jvm.ClassBuilder;
import org.peg4d.jvm.Methods;
import org.peg4d.jvm.ClassBuilder.MethodBuilder;
import org.peg4d.jvm.ClassBuilder.VarEntry;
import org.peg4d.jvm.UserDefinedClassLoader;
import org.peg4d.pegInstruction.AllocLocal;
import org.peg4d.pegInstruction.Block;
import org.peg4d.pegInstruction.ByteAt;
import org.peg4d.pegInstruction.Call;
import org.peg4d.pegInstruction.Cond;
import org.peg4d.pegInstruction.ConstBool;
import org.peg4d.pegInstruction.ConstInt;
import org.peg4d.pegInstruction.ConstStr;
import org.peg4d.pegInstruction.Consume;
import org.peg4d.pegInstruction.Failure;
import org.peg4d.pegInstruction.GetFpos;
import org.peg4d.pegInstruction.GetLocal;
import org.peg4d.pegInstruction.GetNode;
import org.peg4d.pegInstruction.GetPos;
import org.peg4d.pegInstruction.If;
import org.peg4d.pegInstruction.IsFailed;
import org.peg4d.pegInstruction.Loop;
import org.peg4d.pegInstruction.PegInstructionVisitor;
import org.peg4d.pegInstruction.PegMethod;
import org.peg4d.pegInstruction.SetFpos;
import org.peg4d.pegInstruction.SetLocal;
import org.peg4d.pegInstruction.SetNode;
import org.peg4d.pegInstruction.SetPos;

public class JavaByteCodeGenerator implements PegInstructionVisitor, Opcodes {
	public final static String packagePrefix = "org/peg4d/generated/";

	private static int nameSuffix = -1;

	/**
	 * for class generation
	 */
	private ClassBuilder cBuilder;

	/**
	 * for method generation
	 */
	private MethodBuilder mBuilder;

	/**
	 * contains var index of first argument(ParsingContext context)
	 */
	private VarEntry entry_context;

	// entry point
	public Class<?> generateParser(List<PegMethod> methodList) {
		this.cBuilder = new ClassBuilder(packagePrefix + "GeneratedParser" + ++nameSuffix, null, null, null);

		for(PegMethod pegMethod : methodList) {
			this.mBuilder = this.cBuilder.newMethodBuilder(ACC_PUBLIC | ACC_STATIC, 
					boolean.class, pegMethod.getMethodName(), ParsingContext.class);	//TODO: return class
			// initialize
			this.mBuilder.enterScope();
			this.entry_context = this.mBuilder.defineArgument(ParsingContext.class);

			// generate  method body
			pegMethod.getInst().accept(this);

			// finalize
			this.mBuilder.exitScope();
			this.mBuilder.returnValue();// currently return boolean value.
			this.mBuilder.endMethod();
		}

		this.cBuilder.visitEnd();
		byte[] byteCode = this.cBuilder.toByteArray();
		String className = this.cBuilder.getInternalName();

		// clear builder
		this.cBuilder = null;
		this.mBuilder = null;

		return new UserDefinedClassLoader(packagePrefix).definedAndLoadClass(className, byteCode);
	}

	@Override
	public void visit(ConstInt inst) {
		this.mBuilder.push(inst.getValue());
	}

	@Override
	public void visit(ConstBool inst) {
		this.mBuilder.push(inst.getVal());
	}

	@Override
	public void visit(ConstStr inst) {
		this.mBuilder.push(inst.getVal());
	}

	@Override
	public void visit(Cond inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Block inst) {
		this.mBuilder.enterScope();


		this.mBuilder.exitScope();
	}

	@Override
	public void visit(AllocLocal inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(GetLocal inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(SetLocal inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Call inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		Method methodDesc = Methods.method(boolean.class, inst.getTarget(), ParsingContext.class);	//TODO:
		this.mBuilder.invokeStatic(this.cBuilder.getTypeDesc(), methodDesc);
	}

	@Override
	public void visit(If inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Loop inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Consume inst) {
		this.mBuilder.push(inst.getConsumeLength());
		this.mBuilder.callInstanceMethod(ParsingContext.class, void.class, "consume", int.class);
	}
	
	@Override
	public void visit(ByteAt inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Failure inst) {	// generate equivalent code to ParsingContext#failure
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// if cond
		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), "pos", Type.getType(long.class));

		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), "fpos", Type.getType(long.class));

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

	@Override
	public void visit(IsFailed inst) {
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

		// if cond
		this.mBuilder.ifNonNull(elseLabel);
		// then
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);
		// else
		this.mBuilder.mark(elseLabel);
		this.mBuilder.push(false);
		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visit(GetPos inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), "pos", Type.getType(long.class));
	}

	@Override
	public void visit(SetPos inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getVal().accept(this);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "pos", Type.getType(long.class));
	}

	@Override
	public void visit(GetFpos inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), "fpos", Type.getType(long.class));
	}

	@Override
	public void visit(SetFpos inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getVal().accept(this);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "fpos", Type.getType(long.class));
	}

	@Override
	public void visit(GetNode inst) {	//get context.left
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
	}

	@Override
	public void visit(SetNode inst) {	// put to context.left
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getVal().accept(this);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
	}

}
