package org.peg4d.pegcode;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.peg4d.Grammar;
import org.peg4d.ParsingRule;
import org.peg4d.UList;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingApply;
import org.peg4d.expression.ParsingAssert;
import org.peg4d.expression.ParsingBlock;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingCatch;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingName;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;
import org.peg4d.vm.Opcode;
import org.peg4d.vm.Instruction;

public class CodeGenerator extends GrammarFormatter {
	
	int codeIndex = 0;
	
	UList<Opcode> codeList = new UList<Opcode>(new Opcode[256]);	
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
	HashMap<String, Integer> callMap = new HashMap<String, Integer>();
	
	private void writeByteCode(String grammerfileName) {
		byte[] byteCode = new byte[codeList.size() * 16];
		int pos = 0;
		// Version of the specification (2 byte)
		byte[] version = new byte[2];
		version[0] = 0;
		version[1] = 1;
		byteCode[pos] = version[0];
		pos++;
		byteCode[pos] = version[1];
		pos++;
		
		// Length of grammerfileName (4 byte)
		int fileNamelen = grammerfileName.length();
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen >> 24));
		pos++;
		
		// GrammerfileName (n byte)
		byte[] name = grammerfileName.getBytes();
		for (int i = 0; i < fileNamelen; i++) {
			byteCode[pos] = name[i];
			pos++;
		}
		
		int bytecodelen_pos = pos;
		pos = pos + 8;
		
		// byte code (m byte)
		for(int i = 0; i < codeList.size(); i++) {
			Opcode code = codeList.ArrayValues[i];
			byteCode[pos] = (byte) code.inst.ordinal();
			pos++;
			if (code.ndata != null) {
				byteCode[pos] = (byte) (0x000000ff & (code.ndata.length));
				pos++;
				for(int j = 0; j < code.ndata.length; j++){
					byteCode[pos] = (byte) (0x000000ff & (code.ndata[j]));
					pos++;
					byteCode[pos] = (byte) (0x000000ff & (code.ndata[j] >> 8));
					pos++;
					byteCode[pos] = (byte) (0x000000ff & (code.ndata[j] >> 16));
					pos++;
					byteCode[pos] = (byte) (0x000000ff & (code.ndata[j] >> 24));
					pos++;
				}
			}
			else {
				byteCode[pos] = 0;
				pos++;
			}
			if(code.name != null) {
				int j = 0;
				byteCode[pos] = (byte) code.name.length();
				pos++;
				byte[] nameByte = code.name.getBytes();
				while (j < code.name.length()) {
					byteCode[pos] = nameByte[j];
					j++;
					pos++;
				}
			}
			else {
				byteCode[pos] = 0;
				pos++;
			}
		}
		// Length of byte code (8 byte) 
		long byteCodelength = codeList.size();
		pos = bytecodelen_pos;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 24));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 32));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 40));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 48));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 56));
		
		try {
			String fileName = "mytest/" + grammerfileName.substring(0, grammerfileName.indexOf(".")) + ".bin";
			FileOutputStream fos = new FileOutputStream(fileName);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(byteCode);
			bos.flush();
			bos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private Opcode newCode(Instruction inst) {
		Opcode code = new Opcode(inst);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata) {
		Opcode code = new Opcode(inst, ndata);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata1, int ndata2) {
		Opcode code = new Opcode(inst, ndata1, ndata2);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, String name) {
		Opcode code = new Opcode(inst, name);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata, String name) {
		Opcode code = new Opcode(inst, ndata, name);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	public final void writeCode(Instruction inst) {
		codeList.add(newCode(inst));
	}
	
	public final void writeCode(Instruction inst, int ndata) {
		codeList.add(newCode(inst, ndata));
	}
	
	public final void writeCode(Instruction inst, int ndata1, int ndata2) {
		codeList.add(newCode(inst, ndata1, ndata2));
	}
	
	public final void writeCode(Instruction inst, String name) {
		codeList.add(newCode(inst, name));
	}
	
	public final void writeCode(Instruction inst, int ndata, String name) {
		codeList.add(newCode(inst, ndata, name));
	}
	
	class FailurePoint {
		int id;
		FailurePoint prev;
		FailurePoint(int id, FailurePoint prev) {
			this.prev = prev;
			this.id = id;
		}
	}
	
	int labelId = 0;
	FailurePoint fLabel = null;
	private void pushFailureJumpPoint() {
		fLabel = new FailurePoint(labelId, fLabel);
		labelId += 1;
	}
	
	private int popFailureJumpPoint(ParsingRule r) {
		FailurePoint fLabel = this.fLabel;
		this.fLabel = this.fLabel.prev;
		labelMap.put(fLabel.id, codeIndex);
		System.out.println("L" + fLabel.id);
		return fLabel.id;
	}
	
	private int popFailureJumpPoint(ParsingExpression e) {
		FailurePoint fLabel = this.fLabel;
		this.fLabel = this.fLabel.prev;
		labelMap.put(fLabel.id, codeIndex);
		System.out.println("L" + fLabel.id);
		return fLabel.id;
	}
	
	private int jumpFailureJump() {
		return this.fLabel.id;
	}
	
	private int jumpPrevFailureJump() {
		return this.fLabel.prev.id;
	}
	
	public final int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	
	public final void writeLabel(int label) {
		labelMap.put(label, codeIndex);
		System.out.println("L" + label);
	}
	
	public final void writeJumpCode(Instruction inst, int labelId) {
		codeList.add(newCode(inst, labelId));
	}
	
	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			if (r.ruleName.equals("File")) {
				this.formatRule(r, sb);		//string builder is not used.
				break;
			}
		}
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.ruleName.equals("File")) {
				if (!r.ruleName.startsWith("\"")) {
					this.formatRule(r, sb);		//string builder is not used.
				}
			}
		}
		this.formatFooter();
		writeByteCode(peg.getName());
	}

	@Override
	public void formatHeader() {
		System.out.println("\nGenerate Byte Code\n");
		writeCode(Instruction.EXIT);
	}
	
	@Override
	public void formatFooter() {
		System.out.println();
		for (int i = 0; i < codeList.size(); i++) {
			Opcode code = codeList.ArrayValues[i];
			if (code.isJumpCode()) {
				switch (code.inst) {
				case CALL:
					code.ndata[0] = this.callMap.get(code.name);
					System.out.println("[" + i + "] " + code + " " + code.ndata[0]);
					break;
				case RET:
					System.out.println("[" + i + "] " + code);
					break;
				case EXIT:
					System.out.println("[" + i + "] " + code);
					break;
				default:
					code.ndata[0] = this.labelMap.get(code.ndata[0]);
					System.out.println("[" + i + "] " + code);
					break;
				}
			}
			else {
				System.out.println("[" + i + "] " + code);
			}
		}
	}

	@Override
	public String getDesc() {
		return "vm";
	}

	@Override
	public void visitRule(ParsingRule e) {
		this.callMap.put(e.ruleName, this.codeIndex);
		System.out.println(e.ruleName + ":");
		this.pushFailureJumpPoint();
		e.expr.visit(this);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.RET);
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		writeCode(Instruction.CALL, 0, e.ruleName);
		writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		writeCode(Instruction.FAIL);
	}

	@Override
	public void visitByte(ParsingByte e) {
		writeCode(Instruction.BYTE, e.byteChar);
		writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		writeCode(Instruction.CHAR, e.startByteChar, e.endByteChar);
		writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeCode(Instruction.ANY);
		writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitString(ParsingString e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitNot(ParsingNot e) {
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHp);
		e.inner.visit(this);
		writeCode(Instruction.STOREp);
		writeCode(Instruction.FAIL);
		writeJumpCode(Instruction.JUMP, this.jumpPrevFailureJump());
		this.popFailureJumpPoint(e);
		writeCode(Instruction.SUCC);
		writeCode(Instruction.STOREp);
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		writeCode(Instruction.PUSHp);
		e.inner.visit(this);
		writeCode(Instruction.STOREp);
	}

	@Override
	public void visitOptional(ParsingOption e) {
		int label = newLabel();
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHp);
		e.inner.visit(this);
		writeCode(Instruction.POP);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.SUCC);
		writeCode(Instruction.STOREp);
		writeLabel(label);
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		int label = newLabel();
		int end = newLabel();
		this.pushFailureJumpPoint();
		writeLabel(label);
		writeCode(Instruction.PUSHp);
		e.inner.visit(this);
		writeCode(Instruction.REPCOND, end);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.SUCC);
		writeCode(Instruction.STOREp);
		writeLabel(end);
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		int label = newLabel();
		for(int i = 0; i < e.size(); i++) {
			this.pushFailureJumpPoint();
			writeCode(Instruction.PUSHp);
			e.get(i).visit(this);
			writeJumpCode(Instruction.JUMP, label);
			this.popFailureJumpPoint(e.get(i));
			if (i != e.size() - 1) {
				writeCode(Instruction.SUCC);
			}
			writeCode(Instruction.STOREp);
		}
		writeJumpCode(Instruction.JUMP, jumpFailureJump());
		writeLabel(label);
		writeCode(Instruction.POP);
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		int label = newLabel();
		for(int i = 0; i < e.prefetchIndex; i++) {
			e.get(i).visit(this);
		}
		this.pushFailureJumpPoint();
		if (e.leftJoin) {
			writeCode(Instruction.PUSHo);
			writeCode(Instruction.PUSHm);
			writeCode(Instruction.PUSHo);
			writeCode(Instruction.NEW);
			writeCode(Instruction.NEWJOIN);
			writeCode(Instruction.LINK);
		}
		else {
			writeCode(Instruction.PUSHo);
			writeCode(Instruction.PUSHm);
			writeCode(Instruction.NEW);
		}
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		writeCode(Instruction.SETendp);
		writeCode(Instruction.POP);
		writeCode(Instruction.POPo);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.ABORT);
		writeCode(Instruction.STOREo);
		writeJumpCode(Instruction.JUMP, this.jumpFailureJump());
		writeLabel(label);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		int label = newLabel();
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHconnect);
		writeCode(Instruction.PUSHm);
		e.inner.visit(this);
		writeCode(Instruction.COMMIT);
		writeCode(Instruction.LINK, e.index);
		//writeCode(Instruction.STOREo);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		//writeCode(Instruction.SUCC);
		writeCode(Instruction.ABORT);
		writeCode(Instruction.STOREo);
		writeJumpCode(Instruction.JUMP, jumpFailureJump());
		writeLabel(label);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		writeCode(Instruction.TAG, "#" + e.tag.toString());
	}

	@Override
	public void visitValue(ParsingValue e) {
		writeCode(Instruction.VALUE, e.value);
	}

	@Override
	public void visitExport(ParsingExport e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitName(ParsingName e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitApply(ParsingApply e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

}