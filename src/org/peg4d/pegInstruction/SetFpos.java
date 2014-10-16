package org.peg4d.pegInstruction;

public class SetFpos extends PegInstruction {
	PegInstruction val;

	public SetFpos(PegInstruction val) {
		this.val = val;
	}
	
	public final PegInstruction getVal() {
		return this.val;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
}
