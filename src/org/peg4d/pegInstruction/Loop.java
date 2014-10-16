package org.peg4d.pegInstruction;

public class Loop extends PegInstruction {
	private PegInstruction cond;
	private PegInstruction body;
	
	public Loop(PegInstruction cond, PegInstruction body) {
		this.cond = cond;
		this.body = body;
	}
	
	public final PegInstruction getCond() {
		return this.cond;
	}
	
	public final PegInstruction getBody() {
		return this.body;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
