package org.peg4d.pegInstruction;

public class GetPos extends PegInstruction {
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
}
