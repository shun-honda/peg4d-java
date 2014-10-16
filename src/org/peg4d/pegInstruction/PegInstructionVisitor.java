package org.peg4d.pegInstruction;

public interface PegInstructionVisitor {
	public abstract void visit(ConstInt inst);
	public abstract void visit(ConstBool inst);
	public abstract void visit(ConstStr inst);
	public abstract void visit(Cond inst);
	public abstract void visit(Block inst);
	public abstract void visit(AllocLocal inst);
	public abstract void visit(GetLocal inst);
	public abstract void visit(SetLocal inst);
	public abstract void visit(Call inst);
	public abstract void visit(If inst);
	public abstract void visit(Loop inst);
	public abstract void visit(Consume inst);
	public abstract void visit(Failure inst);
	public abstract void visit(IsFailed inst);
	public abstract void visit(GetPos inst);
	public abstract void visit(SetPos inst);
	public abstract void visit(GetFpos inst);
	public abstract void visit(SetFpos inst);
	public abstract void visit(GetNode inst);
	public abstract void visit(SetNode inst);
}
