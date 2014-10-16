package org.peg4d.pegInstruction;

import org.objectweb.asm.Type;

public class Cond extends PegInstruction{
	private Type type;
	private OpType optype;
	private PegInstruction left;
	private PegInstruction right;
	
	public Cond(Type type, OpType optype, PegInstruction left, PegInstruction right) {
		this.type = type;
		this.optype = optype;
		this.left = left;
		this.right = right;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public OpType getOpType() {
		return this.optype;
	}
	
	public PegInstruction getLeft() {
		return this.left;
	}
	
	public PegInstruction getRight() {
		return this.right;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
	
}

enum OpType {
	EQ,
	NE,
	LT,
	LE,
	GT,
	GE
}
