package org.peg4d.pegInstruction;

public class Consume extends PegInstruction {
	private PegInstruction len;
	
	public Consume(PegInstruction len) {
		this.len = len;
	}
	
	public final PegInstruction getConsumeLength() {
		return this.len;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
