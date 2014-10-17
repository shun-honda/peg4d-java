package org.peg4d.pegInstruction;

public class GetLocalInt extends PegInstruction {
	private String name;
	
	public GetLocalInt(String name) {
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
