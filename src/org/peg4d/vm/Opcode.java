package org.peg4d.vm;

public final class Opcode {
	public MachineInstruction opcode;
    public int ndata = 0;
    public byte[] bdata = null;
    public Opcode(MachineInstruction mi) {
    	this.opcode = mi;
    }
    public Opcode(MachineInstruction mi, int ndata) {
    	this.opcode = mi;
    	this.ndata = ndata;
    }
    public Opcode(MachineInstruction mi, byte[] bdata) {
		this.opcode = mi;
		this.bdata = bdata;
    }
    @Override
	public String toString() {
    	if(this.bdata == null) {
    		return opcode.toString() + " " + ndata; 
    	}
    	else {
    		return opcode.toString() + " " + ndata; 
    	}
    }
    public final boolean isJumpCode() {
    	return this.opcode.compareTo(MachineInstruction.IFFAIL) <= 0;
    }
}