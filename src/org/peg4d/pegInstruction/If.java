package org.peg4d.pegInstruction;

public class If extends PegInstruction {
	private PegInstruction cond;
	private PegInstruction thenBlock;
	private PegInstruction elseBlock;
	
	public If(PegInstruction cond, PegInstruction thenBlock, PegInstruction elseBlock) {
		this.cond = cond;
		this.thenBlock = thenBlock;
		this.elseBlock = elseBlock;
	}
	
	public final PegInstruction getCond() {
		return this.cond;
	}
	
	public final PegInstruction getThenBlock() {
		return this.thenBlock;
	}
	
	public final PegInstruction getElseBlock() {
		return this.elseBlock;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
	
}
