package org.peg4d.pegInstruction;

public class Failure extends PegInstruction {
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}
}
