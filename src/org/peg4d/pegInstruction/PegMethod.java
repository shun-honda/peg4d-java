package org.peg4d.pegInstruction;

public class PegMethod {
	String methodName;
	PegInstruction inst;
	
	public PegMethod(String methodName) {
		this.methodName = methodName;
	}
	
	public PegMethod(String methodName, PegInstruction inst) {
		this.methodName = methodName;
		this.inst = inst;
	}
	
	public final String getMethodName() {
		return this.methodName;
	}
	
	public final PegInstruction getInst() {
		return this.inst;
	}
	
	public final void setInst(PegInstruction inst) {
		this.inst = inst;
	}
}
