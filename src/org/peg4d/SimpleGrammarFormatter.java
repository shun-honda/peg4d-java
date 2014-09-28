package org.peg4d;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Stack;

import org.peg4d.ParsingExpression;
import org.peg4d.vm.MachineInstruction;
import org.peg4d.vm.Opcode;

public class SimpleGrammarFormatter extends GrammarFormatter {
	UList<Opcode> opList = new UList<Opcode>(new Opcode[256]);
	HashMap<String, Integer> nonTerminalMap = new HashMap<String, Integer>();
	public SimpleGrammarFormatter() {
		super();
	}
	public SimpleGrammarFormatter(Grammar peg, StringBuilder sb) {
		super(peg, sb);
	}
	
	public void writeByteCode(String fileName, String grammerfileName) {
		byte[] byteCode = new byte[opList.size() * 16];
		int pos = 0;
		// Version of the specification (4 byte)
		int version = 1;
		byteCode[pos] = (byte) (0x000000ff & (version));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (version >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (version >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (version >> 24));
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
		for(int i = 0; i < opList.size(); i++) {
			Opcode op = opList.ArrayValues[i];
			byteCode[pos] = (byte) op.opcode.ordinal();
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (op.ndata));
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (op.ndata >> 8));
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (op.ndata >> 16));
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (op.ndata >> 24));
			pos++;
			if(op.bdata != null) {
				int j = 0;
				byteCode[pos] = (byte) op.bdata.length;
				pos++;
				while (j < op.bdata.length) {
					byteCode[pos] = op.bdata[j];
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
		long byteCodelength = pos - (bytecodelen_pos + 8);
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
}

class SimpleCodeGenerator extends SimpleGrammarFormatter {
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
	
	int labelId = 0;
	private int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	private void writeLabel(int label) {
		sb.append(" L" + label + ":\n");
		labelMap.put(label, opList.size());
	}
	
	private void writeCode(MachineInstruction mi) {
		sb.append("\t" + mi + "\n");
		opList.add(new Opcode(mi));
	}
	
	private void writeNewObjectCode(MachineInstruction mi, ParsingExpression p) {
		sb.append("\t" + mi + "\n");
		opList.add(new Opcode(mi, p));
	}

	private void writeJumpCode(MachineInstruction mi, int labelId) {
		sb.append("\t" + mi + " L" + labelId + "\n");
		opList.add(new Opcode(mi, labelId));
	}

	private void writeCode(MachineInstruction mi, String op) {
		sb.append("\t" + mi + " " + op + "\n");
		Opcode opcode = new Opcode(mi);
		if(mi == MachineInstruction.opMatchCharset | mi == MachineInstruction.opMatchText) {
			op = op.substring(1, op.length() - 1);
		}
		opcode.bdata = op.getBytes();
		opList.add(opcode);
	}
	
	private void writeCode(MachineInstruction mi, int op) {
		sb.append("\t" + mi + " " + op + "\n");
		Opcode opcode = new Opcode(mi);
		opcode.ndata = op;
		opList.add(opcode);
	}

	@Override
	public void formatHeader(StringBuilder sb) {
		this.sb = sb;
		opList =  new UList<Opcode>(new Opcode[256]);
		labelMap = new HashMap<Integer,Integer>();
		this.writeCode(MachineInstruction.EXIT);
		this.sb = null;
	}

	@Override
	public void formatRule(String ruleName, ParsingExpression e, StringBuilder sb) {
		this.sb = sb;
		this.formatRule(ruleName, e);
		this.writeCode(MachineInstruction.RET);
		this.sb = null;
	}
	
	@Override
	public void formatFooter(StringBuilder sb) {
		Stack<Integer> RetPosStack = new Stack<Integer>();
		for(int i = 0; i < opList.size(); i++) {
			Opcode op = opList.ArrayValues[i];
			if(op.isJumpCode()) {
				switch(op.opcode) {
				case CALL:
					String labelName;
					try {
						labelName = new String(op.bdata, "UTF-8");
						op.ndata = nonTerminalMap.get(labelName) - 1;
						RetPosStack.push(i);
						op.bdata = null;
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					break;
				case RET:
					break;
				default:
					op.ndata = labelMap.get(op.ndata) - 1;
				}
			}
			sb.append("["+i+"] " + op + "\n");
		}
	}

	private void formatRule(String ruleName, ParsingExpression e) {
		sb.append(ruleName + ":\n");
		e.visit(this);
	}
	
	@Override
	public void visitNonTerminal(NonTerminal e) {
		this.writeCode(MachineInstruction.CALL, e.ruleName);
	}
	@Override
	public void visitString(ParsingString e) {
		this.writeCode(MachineInstruction.opMatchText, ParsingCharset.quoteString('\'', e.text, '\''));
	}
	
	public void visitByte(ParsingByte e) {
		this.writeCode(MachineInstruction.opMatchText, e.byteChar);
	}
	@Override
	public void visitByteRange(ParsingByteRange e) {
		this.writeCode(MachineInstruction.opMatchCharset, e.toString());
	}
	@Override
	public void visitAny(ParsingAny e) {
		this.writeCode(MachineInstruction.opMatchAnyChar);
	}
	@Override
	public void visitTagging(ParsingTagging e) {
		this.writeCode(MachineInstruction.opTagging, e.tag.toString());
	}
	@Override
	public void visitValue(ParsingValue e) {
		this.writeCode(MachineInstruction.opValue, ParsingCharset.quoteString('\'', e.value, '\''));
	}
	@Override
	public void visitIndent(ParsingIndent e) {
		this.writeCode(MachineInstruction.opIndent);
	}
	@Override
	public void visitOptional(ParsingOption e) {
		int labelL = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opRememberSequencePosition);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeCode(MachineInstruction.opCommitSequencePosition);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
//		writeCode(MachineInstruction.opRestoreObject);
//		writeCode(MachineInstruction.opBacktrackPosition);
//		writeCode(MachineInstruction.opForgetFailurePosition);
		writeCode(MachineInstruction.opBackTrackSequencePosition);
		writeLabel(labelL);
		//writeCode(MachineInstruction.opRestoreObjectIfFailure);
	}
	@Override
	public void visitRepetition(ParsingRepetition e) {
		int labelL = newLabel();
		int labelE = newLabel();
		int labelE2 = newLabel();
		/*if(e.atleast == 1) {
			writeCode(MachineInstruction.opRememberPosition);
			e.inner.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL,labelE2);
			writeCode(MachineInstruction.opCommitPosition);
		}*/
		writeLabel(labelL);
		writeCode(MachineInstruction.opRememberPosition);
		writeCode(MachineInstruction.opStoreObject);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeCode(MachineInstruction.opDropStoredObject);
		writeCode(MachineInstruction.opCommitPosition);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
		writeCode(MachineInstruction.opRestoreObject);
		writeLabel(labelE2);
		writeCode(MachineInstruction.opBacktrackPosition);
	}
	
	@Override
	public void visitAnd(ParsingAnd e) {
		writeCode(MachineInstruction.opRememberPosition);
		e.inner.visit(this);
		writeCode(MachineInstruction.opBacktrackPosition);
	}

	@Override
	public void visitNot(ParsingNot e) {
		int labelL = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opRememberSequencePosition);
//		writeCode(MachineInstruction.opRememberFailurePosition);
//		writeCode(MachineInstruction.opRememberPosition);
//		writeCode(MachineInstruction.opStoreObject);
		e.inner.visit(this);
		writeCode(MachineInstruction.opRestoreNegativeObject);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeCode(MachineInstruction.opCommitPosition);
		writeCode(MachineInstruction.opForgetFailurePosition);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeCode(MachineInstruction.opCommitPosition);
		writeLabel(labelL);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		int labelF = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opStoreObject);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelF);
		writeCode(MachineInstruction.opConnectObject, ""+e.index);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.opDropStoredObject);
		writeLabel(labelE);
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		int labelF = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opRememberPosition);
		for(int i = 0; i < e.size(); i++) {
			ParsingExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF);
		}
		writeCode(MachineInstruction.opCommitPosition);
		//writeCode(MachineInstruction.opCommitSequencePosition);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeLabel(labelE);
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		int labelS = newLabel();
		int labelE1 = newLabel();
		for(int i = 0; i < e.size(); i++) {
//			writeCode(MachineInstruction.opStoreObject);
//			writeCode(MachineInstruction.opRememberPosition);
//			writeCode(MachineInstruction.opRememberPosition);
			writeCode(MachineInstruction.opRememberSequencePosition);
			e.get(i).visit(this);
			writeJumpCode(MachineInstruction.IFSUCC, labelS);
			if(i != e.size() - 1) {
				writeCode(MachineInstruction.opBackTrackSequencePosition);
			}
		}
		writeCode(MachineInstruction.opBacktrackPosition);
		writeCode(MachineInstruction.opForgetFailurePosition);
		writeCode(MachineInstruction.opDropStoredObject);
		writeJumpCode(MachineInstruction.JUMP, labelE1);
		writeLabel(labelS);
//		writeCode(MachineInstruction.opDropStoredObject);
//		writeCode(MachineInstruction.opCommitPosition);
//		writeCode(MachineInstruction.opCommitPosition);
		writeCode(MachineInstruction.opCommitSequencePosition);
		writeLabel(labelE1);
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		int labelF = newLabel();
		int labelE = newLabel();
		if(e.leftJoin) {
			writeCode(MachineInstruction.opLeftJoinObject);
		}
		else {
			writeNewObjectCode(MachineInstruction.opNewObject, e);
		}
		writeCode(MachineInstruction.opRememberPosition);
		for(int i = 0; i < e.size(); i++) {
			ParsingExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF);
		}
		writeCode(MachineInstruction.opCommitPosition);
		//writeCode(MachineInstruction.opCommitSequencePosition);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeLabel(labelE);
		//writeCode(MachineInstruction.opCommitObject);
	}
}
