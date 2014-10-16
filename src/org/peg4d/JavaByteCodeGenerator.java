package org.peg4d;

import org.peg4d.pegInstruction.AllocLocal;
import org.peg4d.pegInstruction.Block;
import org.peg4d.pegInstruction.ByteAt;
import org.peg4d.pegInstruction.Call;
import org.peg4d.pegInstruction.Cond;
import org.peg4d.pegInstruction.ConstBool;
import org.peg4d.pegInstruction.ConstInt;
import org.peg4d.pegInstruction.ConstStr;
import org.peg4d.pegInstruction.Consume;
import org.peg4d.pegInstruction.Failure;
import org.peg4d.pegInstruction.GetFpos;
import org.peg4d.pegInstruction.GetLocal;
import org.peg4d.pegInstruction.GetNode;
import org.peg4d.pegInstruction.GetPos;
import org.peg4d.pegInstruction.If;
import org.peg4d.pegInstruction.IsFailed;
import org.peg4d.pegInstruction.Loop;
import org.peg4d.pegInstruction.PegInstructionVisitor;
import org.peg4d.pegInstruction.SetFpos;
import org.peg4d.pegInstruction.SetLocal;
import org.peg4d.pegInstruction.SetNode;
import org.peg4d.pegInstruction.SetPos;

public class JavaByteCodeGenerator implements PegInstructionVisitor {

	@Override
	public void visit(ConstInt inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(ConstBool inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(ConstStr inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Cond inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Block inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(AllocLocal inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(GetLocal inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(SetLocal inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Call inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(If inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Loop inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Consume inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}
	
	@Override
	public void visit(ByteAt inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(Failure inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(IsFailed inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(GetPos inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(SetPos inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(GetFpos inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(SetFpos inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(GetNode inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

	@Override
	public void visit(SetNode inst) {
		throw new RuntimeException("unimplemented visit method: " + inst.getClass());
	}

}
