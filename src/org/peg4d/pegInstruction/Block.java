package org.peg4d.pegInstruction;

import java.util.ArrayList;
import java.util.List;

public class Block extends PegInstruction {
	private List<PegInstruction> localList;
	private List<PegInstruction> childList;
	
	public Block() {
		this.localList = new ArrayList<PegInstruction>();
		this.childList = new ArrayList<PegInstruction>();
	}
	
	public Block appendLocal(PegInstruction inst) {
		this.localList.add(inst);
		return this;
	}
	
	public Block appendChild(PegInstruction inst) {
		this.localList.add(inst);
		return this;
	}
	
	public final List<PegInstruction> getLocal() {
		return this.localList;
	}
	
	public final List<PegInstruction> getChild(int i) {
		return this.childList;
	}

	public final int getLocalSize() {
		return this.locals.length;
	}

	public final int getChildSize() {
		return this.child.length;
	}

	@Override
	public void accept(PegInstructionVisitor visitor) {
		visitor.visit(this);
	}

}
