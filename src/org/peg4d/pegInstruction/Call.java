package org.peg4d.pegInstruction;

public class Call extends PegInstruction {
	private String target;
	
	public Call(String target) {
		this.target = target;
	}
	
	public final String getTarget() {
		return this.target;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
	
}
