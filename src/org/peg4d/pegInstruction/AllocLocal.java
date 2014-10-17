package org.peg4d.pegInstruction;

public class AllocLocal extends PegInstruction {
	private String name;
	private Class<?> type;
	
	public AllocLocal(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final Class<?> getType() {
		return this.type;
	}
	
	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
