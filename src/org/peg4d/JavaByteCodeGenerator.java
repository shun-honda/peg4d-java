package org.peg4d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.peg4d.pegInstruction.AllocLocalInt;
import org.peg4d.pegInstruction.AllocLocalLong;
import org.peg4d.pegInstruction.AllocLocalParsingObject;
import org.peg4d.pegInstruction.Block;
import org.peg4d.pegInstruction.Call;
import org.peg4d.pegInstruction.Cond;
import org.peg4d.pegInstruction.ConstBool;
import org.peg4d.pegInstruction.ConstInt;
import org.peg4d.pegInstruction.ConstStr;
import org.peg4d.pegInstruction.Consume;
import org.peg4d.pegInstruction.Failure;
import org.peg4d.pegInstruction.GetByte;
import org.peg4d.pegInstruction.GetChar;
import org.peg4d.pegInstruction.GetFpos;
import org.peg4d.pegInstruction.GetLocalInt;
import org.peg4d.pegInstruction.GetLocalLong;
import org.peg4d.pegInstruction.GetLocalParsingObject;
import org.peg4d.pegInstruction.GetNode;
import org.peg4d.pegInstruction.GetPos;
import org.peg4d.pegInstruction.If;
import org.peg4d.pegInstruction.IsFailed;
import org.peg4d.pegInstruction.Loop;
import org.peg4d.pegInstruction.NumOfBytes;
import org.peg4d.pegInstruction.PegInstruction;
import org.peg4d.pegInstruction.PegInstructionVisitor;
import org.peg4d.pegInstruction.PegMethod;
import org.peg4d.pegInstruction.SetFpos;
import org.peg4d.pegInstruction.SetLocalInt;
import org.peg4d.pegInstruction.SetLocalLong;
import org.peg4d.pegInstruction.SetLocalParsingObject;
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

	private Deque<Map<String, VarEntry>> nameToEntryStack;


	// entry point
	public Class<?> generateParser(List<PegMethod> methodList) {
		this.cBuilder = new ClassBuilder(packagePrefix + "GeneratedParser" + ++nameSuffix, null, null, null);

		for(PegMethod pegMethod : methodList) {
			this.nameToEntryStack = new ArrayDeque<>();
			this.mBuilder = this.cBuilder.newMethodBuilder(ACC_PUBLIC | ACC_STATIC, 
					boolean.class, pegMethod.getMethodName(), ParsingContext.class);	//TODO: return class
			// initialize
			this.mBuilder.enterScope();
			this.entry_context = this.mBuilder.defineArgument(ParsingContext.class);

			// generate  method body
			pegMethod.getInst().accept(this);

			// finalize
			this.mBuilder.exitScope();
			this.generateIsFailed();
			this.mBuilder.not();
			this.mBuilder.returnValue();// currently return boolean value.
			this.mBuilder.endMethod();
		}

		this.cBuilder.visitEnd();
		byte[] byteCode = this.cBuilder.toByteArray();
		String className = this.cBuilder.getInternalName();

		// clear builder
		this.cBuilder = null;
		this.mBuilder = null;
		this.nameToEntryStack = null;

		return new UserDefinedClassLoader(packagePrefix).definedAndLoadClass(className, byteCode);
	}

	// scope manipulation api
	private void enterScope() {
		this.mBuilder.enterScope();
		this.nameToEntryStack.push(new HashMap<>());
	}

	private void exitScope() {
		this.mBuilder.exitScope();
		this.nameToEntryStack.pop();
	}

	private VarEntry newEntry(String varName, Class<?> varClass) {
		VarEntry entry = this.mBuilder.createNewVar(varClass);
		this.nameToEntryStack.peek().put(varName, entry);
		return entry;
	}

	private VarEntry lookupEntry(String varName) {
		for(Iterator<Map<String, VarEntry>> iterator = this.nameToEntryStack.iterator(); iterator.hasNext();) {
			Map<String, VarEntry> nameToEntryMap = iterator.next();
			VarEntry entry = nameToEntryMap.get(varName);
			if(entry != null) {
				return entry;
			}
		}
		throw new RuntimeException("undefined variable: " + varName);
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

	private void generateIsFailed() {
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.getFieldOfContext("left", ParsingObject.class);

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
		this.enterScope();
		// init local variable
		for(PegInstruction instruction : inst.getLocal()) {
			instruction.accept(this);
		}

		for(PegInstruction instruction : inst.getChild(0)) {
			instruction.accept(this);
		}
		this.exitScope();
	}

	@Override
	public void visit(Call inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		Method methodDesc = Methods.method(boolean.class, inst.getTarget(), ParsingContext.class);	//TODO:
		this.mBuilder.invokeStatic(this.cBuilder.getTypeDesc(), methodDesc);
	}

	@Override
	public void visit(If inst) {
		Label thenLabel = mBuilder.newLabel();
		Label mergeLabel = mBuilder.newLabel();

		// cond
		if(inst.getCond() instanceof Cond) {
			Cond cond = (Cond) inst.getCond();
			Type type = Type.getType(cond.getType());
			int op = 0;
			switch(cond.getOpType()) {
			case EQ:
				op = GeneratorAdapter.EQ;
				break;
			case GE:
				op = GeneratorAdapter.GE;
				break;
			case GT:
				op = GeneratorAdapter.GT;
				break;
			case LE:
				op = GeneratorAdapter.LE;
				break;
			case LT:
				op = GeneratorAdapter.LT;
				break;
			case NE:
				op = GeneratorAdapter.NE;
				break;
			default:
				throw new RuntimeException("unimplemented operation: " + cond.getOpType());
			}

			cond.getLeft().accept(this);
			cond.getRight().accept(this);
			this.mBuilder.ifCmp(type, op, thenLabel);
		}
		else {
			throw new RuntimeException("unimplemented");
		}

		// else
		inst.getElseBlock().accept(this);
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		inst.getThenBlock().accept(this);

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visit(Loop inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Consume inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getConsumeLength().accept(this);
		this.mBuilder.callInstanceMethod(ParsingContext.class, void.class, "consume", int.class);
	}

	@Override
	public void visit(Failure inst) {	// generate equivalent code to ParsingContext#failure
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

	@Override
	public void visit(IsFailed inst) {
		this.generateIsFailed();
	}

	@Override
	public void visit(GetPos inst) {
		this.getFieldOfContext("pos", long.class);
	}

	@Override
	public void visit(SetPos inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getVal().accept(this);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "pos", Type.getType(long.class));
	}

	@Override
	public void visit(GetFpos inst) {
		this.getFieldOfContext("fpos", long.class);
	}

	@Override
	public void visit(SetFpos inst) {
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getVal().accept(this);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "fpos", Type.getType(long.class));
	}

	@Override
	public void visit(GetNode inst) {	//get context.left
		this.getFieldOfContext("left", ParsingObject.class);
	}

	@Override
	public void visit(SetNode inst) {	// put to context.left
		this.mBuilder.loadFromVar(this.entry_context);
		inst.getVal().accept(this);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
	}

	@Override
	public void visit(AllocLocalInt inst) {
		this.newEntry(inst.getName(), int.class);
	}

	@Override
	public void visit(AllocLocalLong inst) {
		this.newEntry(inst.getName(), long.class);
	}

	@Override
	public void visit(AllocLocalParsingObject inst) {
		this.newEntry(inst.getName(), ParsingObject.class);
	}

	@Override
	public void visit(GetLocalInt inst) {
		this.mBuilder.loadFromVar(this.lookupEntry(inst.getName()));
	}

	@Override
	public void visit(GetLocalLong inst) {
		this.mBuilder.loadFromVar(this.lookupEntry(inst.getName()));
	}

	@Override
	public void visit(GetLocalParsingObject inst) {
		this.mBuilder.loadFromVar(this.lookupEntry(inst.getName()));
	}

	@Override
	public void visit(SetLocalInt inst) {
		inst.getVal().accept(this);
		this.mBuilder.storeToVar(this.lookupEntry(inst.getName()));
	}

	@Override
	public void visit(SetLocalLong inst) {
		inst.getVal().accept(this);
		this.mBuilder.storeToVar(this.lookupEntry(inst.getName()));
	}

	@Override
	public void visit(SetLocalParsingObject inst) {
		inst.getVal().accept(this);
		this.mBuilder.storeToVar(this.lookupEntry(inst.getName()));
	}

	@Override
	public void visit(GetByte inst) {
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "byteAt", long.class);
	}

	@Override
	public void visit(GetChar inst) {
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "charAt", long.class);
	}

	@Override
	public void visit(NumOfBytes inst) {
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "charLength", long.class);
	}

}
