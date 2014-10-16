package org.peg4d.pegInstruction;

public class ConstInt extends PegInstruction {
	private int val;
	
	public ConstInt(int val) {
		this.val = val;
	}
	
	public final int getValue() {
		return this.val;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
}
