package org.peg4d.pegInstruction;

public class GetLocal extends PegInstruction {
	private String name;
	
	public GetLocal(String name) {
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
