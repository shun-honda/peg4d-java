package org.peg4d.pegInstruction;

public interface PegInstructionVisitor {
	public abstract void visit(ConstInt inst);
	public abstract void visit(ConstBool inst);
	public abstract void visit(ConstStr inst);
	public abstract void visit(Cond inst);
	public abstract void visit(Block inst);
	public abstract void visit(AllocLocalInt inst);
	public abstract void visit(AllocLocalLong inst);
	public abstract void visit(AllocLocalParsingObject inst);
	public abstract void visit(GetLocalInt inst);
	public abstract void visit(GetLocalLong inst);
	public abstract void visit(GetLocalParsingObject inst);
	public abstract void visit(SetLocalInt inst);
	public abstract void visit(SetLocalLong inst);
	public abstract void visit(SetLocalParsingObject inst);
	public abstract void visit(Call inst);
	public abstract void visit(If inst);
	public abstract void visit(Loop inst);
	public abstract void visit(Consume inst);
	public abstract void visit(GetByte inst);
	public abstract void visit(GetChar inst);
	public abstract void visit(NumOfBytes inst);
	public abstract void visit(Failure inst);
	public abstract void visit(IsFailed inst);
	public abstract void visit(GetPos inst);
	public abstract void visit(SetPos inst);
	public abstract void visit(GetFpos inst);
	public abstract void visit(SetFpos inst);
	public abstract void visit(GetNode inst);
	public abstract void visit(SetNode inst);
}
