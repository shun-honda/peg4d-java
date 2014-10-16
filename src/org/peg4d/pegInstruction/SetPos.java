package org.peg4d.pegInstruction;

public class SetPos extends PegInstruction {
	PegInstruction val;
	
	public SetPos(PegInstruction val) {
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
