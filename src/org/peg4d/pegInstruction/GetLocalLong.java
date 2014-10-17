package org.peg4d.pegInstruction;

public class GetLocalLong extends PegInstruction {
	private String name;
	
	public GetLocalLong(String name) {
		this.name = name;
	}
	
	public final String getName() {
		return this.name;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
