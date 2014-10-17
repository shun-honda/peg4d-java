package org.peg4d.pegInstruction;

public class SetLocalLong extends PegInstruction {
	private String name;
	private PegInstruction val;
	
	public SetLocalLong(String name, PegInstruction val) {
		this.name = name;
		this.val = val;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final PegInstruction getVal() {
		return this.val;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
