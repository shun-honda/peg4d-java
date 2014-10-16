package org.peg4d.pegInstruction;

public class SetNode extends PegInstruction {
	PegInstruction val;

	public SetNode(PegInstruction val) {
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
