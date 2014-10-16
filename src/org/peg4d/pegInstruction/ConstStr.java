package org.peg4d.pegInstruction;

public class ConstStr extends PegInstruction {
	private String val;
	
	public ConstStr(String val) {
		this.val = val;
	}
	
	public final String getVal() {
		return this.val;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
