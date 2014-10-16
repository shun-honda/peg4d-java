package org.peg4d;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingFunction;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingList;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOperation;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingUnary;
import org.peg4d.expression.ParsingValue;
import org.peg4d.pegInstruction.PegMethod;

public class PegInstructionGenerator extends GrammarFormatter {
	
	private List<PegMethod> pegMethodList;
	private PegMethod method;
	
	public PegInstructionGenerator() {
		this.pegMethodList = new ArrayList<PegMethod>();
	}
	
	@Override
	public void formatRule(String ruleName, ParsingExpression e, StringBuilder sb) { // not use string builder
		this.method = new PegMethod(ruleName);
		e.visit(this);
		this.pegMethodList.add(this.method);
		this.method = null;
	}

	// visitor api
	@Override
	public void visitNonTerminal(NonTerminal e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}
	
	@Override
	public void visitByte(ParsingByte e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitString(ParsingString e) {	//FIXME:
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAny(ParsingAny e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitValue(ParsingValue e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitUnary(ParsingUnary e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitNot(ParsingNot e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitOptional(ParsingOption e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitExport(ParsingExport e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitList(ParsingList e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitParsingFunction(ParsingFunction parsingFunction) {
		throw new RuntimeException("unimplemented visit method: " + parsingFunction.getClass());
	}

	@Override
	public void visitParsingOperation(ParsingOperation e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitParsingIfFlag(ParsingIf e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}
}
