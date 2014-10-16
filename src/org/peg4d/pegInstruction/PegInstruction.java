package org.peg4d.pegInstruction;

public abstract class PegInstruction {
	public abstract void accept(PegInstructionVisitor visitor);
}
