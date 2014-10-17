package org.peg4d.pegInstruction;

public class ByteAt extends PegInstruction {
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
