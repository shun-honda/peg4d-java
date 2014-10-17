package org.peg4d.pegInstruction;

public class GetLocalParsingObject extends PegInstruction{
	private String name;
	
	public GetLocalParsingObject(String name) {
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
