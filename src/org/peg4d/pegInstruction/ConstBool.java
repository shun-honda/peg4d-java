package org.peg4d.pegInstruction;

public class ConstBool extends PegInstruction{
	private boolean val;
	
	public ConstBool(boolean val) {
		this.val = val;
	}
	
	public final boolean getVal() {
		return this.val;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
}
