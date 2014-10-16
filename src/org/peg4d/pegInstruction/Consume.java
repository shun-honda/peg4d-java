package org.peg4d.pegInstruction;

public class Consume extends PegInstruction {
	private int n;
	
	public Consume(int n) {
		this.n = n;
	}
	
	public final int getConsumeLength() {
		return this.n;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
