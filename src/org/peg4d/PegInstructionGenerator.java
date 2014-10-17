package org.peg4d;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
import org.peg4d.expression.ParsingList;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOperation;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingUnary;
import org.peg4d.expression.ParsingValue;
import org.peg4d.pegInstruction.AllocLocal;
import org.peg4d.pegInstruction.Block;
import org.peg4d.pegInstruction.ByteAt;
import org.peg4d.pegInstruction.Call;
import org.peg4d.pegInstruction.CharAt;
import org.peg4d.pegInstruction.Cond;
import org.peg4d.pegInstruction.ConstInt;
import org.peg4d.pegInstruction.Consume;
import org.peg4d.pegInstruction.Failure;
import org.peg4d.pegInstruction.GetByte;
import org.peg4d.pegInstruction.GetLocal;
import org.peg4d.pegInstruction.If;
import org.peg4d.pegInstruction.OpType;
import org.peg4d.pegInstruction.PegInstruction;
import org.peg4d.pegInstruction.PegMethod;
import org.peg4d.pegInstruction.SetLocal;

public class PegInstructionGenerator extends GrammarFormatter {
	
	private List<PegMethod> pegMethodList;
	private Stack<PegInstruction> pegInstStack;
	private PegMethod method;
	
	public PegInstructionGenerator() {
		this.pegMethodList = new ArrayList<PegMethod>();
		this.pegInstStack = new Stack<PegInstruction>();
	}
	
	@Override
	public void formatRule(String ruleName, ParsingExpression e, StringBuilder sb) { // not use string builder
		this.method = new PegMethod(ruleName);
		e.visit(this);
		this.method.setInst(this.pegInstStack.pop());
		this.pegMethodList.add(this.method);
		this.method = null;
	}

	// visitor api
	@Override
	public void visitNonTerminal(NonTerminal e) {
		this.pegInstStack.push(new Call(e.ruleName));
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		this.pegInstStack.push(new Failure());
	}
	
	@Override
	public void visitByte(ParsingByte e) {
		// condition
		ByteAt byteAt = new ByteAt();
		ConstInt constInt = new ConstInt(e.byteChar);
		Cond cond = new Cond(int.class, OpType.EQ, byteAt, constInt);
		
		// then block
		PegInstruction[] thenInsts = new PegInstruction[1];
		thenInsts[0] = new Consume(new ConstInt(1));
		Block thenBlock = new Block(null, thenInsts);
		
		// else block
		PegInstruction[] elseInsts = new PegInstruction[1];
		elseInsts[0] = new Failure();
		Block elseBlock = new Block(null, elseInsts);
		
		// If
		this.pegInstStack.push(new If(cond, thenBlock, elseBlock));
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		PegInstruction[] locals = new PegInstruction[2];
		locals[0] = new AllocLocal("ch", int.class);
		locals[1] = new SetLocal("ch", new ByteAt());
		
		// first condition
		Cond firstcond = new Cond(int.class, OpType.GE, new GetLocal("ch"), new ConstInt(e.startByteChar));
		If firstIf = new If(firstcond, new Consume(new ConstInt(1)), new Failure());
		
		// second condition
		Cond scondcond = new Cond(int.class, OpType.LE, new GetLocal("ch"), new ConstInt(e.startByteChar));
		
		// If
		PegInstruction[] child = new PegInstruction[1];
		child[0] = new If(scondcond, firstIf, new Failure());
		Block block = new Block(locals, child);
		this.pegInstStack.push(block);
	}

	@Override
	public void visitString(ParsingString e) {	//FIXME:
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAny(ParsingAny e) {
		// condition
		Cond cond = new Cond(int.class, OpType.NE, new CharAt(), new ConstInt(-1));
		
		// then Block
		PegInstruction[] locals = new PegInstruction[2];
		locals[0] = new AllocLocal("len", int.class);
		locals[1] = new SetLocal("len", new GetByte());
		PegInstruction[] child = new PegInstruction[1];
		child[0] = new Consume(new GetLocal("len"));
		Block thenBlock = new Block(locals, child);
		
		// If
		If If = new If(cond, thenBlock, new Failure());
		this.pegInstStack.push(If);
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
