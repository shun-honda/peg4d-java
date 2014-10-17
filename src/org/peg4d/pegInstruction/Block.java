package org.peg4d.pegInstruction;

public class Block extends PegInstruction {
	private PegInstruction[] locals;
	private PegInstruction[] child;
	
	public Block(PegInstruction[] locals, PegInstruction[] child) {
		this.locals = locals;
		this.child = child;
	}
	
	public final PegInstruction getLocal(int i) {
		return this.locals[i];
	}
	
	public final PegInstruction getChild(int i) {
		return this.child[i];
	}

	public final int getLocalSize() {
		return this.locals.length;
	}

	public final int getChildSize() {
		return this.child.length;
	}

	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
