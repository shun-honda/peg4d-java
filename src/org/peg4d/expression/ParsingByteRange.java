package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingByteRange extends ParsingExpression {
	public int startByteChar;
	public int endByteChar;
	ParsingByteRange(int startByteChar, int endByteChar) {
		super();
		this.startByteChar = startByteChar;
		this.endByteChar = endByteChar;
		this.minlen = 1;
	}
	@Override 
	ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("[\b" + startByteChar + "-" + endByteChar, this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	void setCount(int[] count) {
		for(int c = startByteChar; c <= endByteChar; c++) {
			count[c]++;
		}
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitByteRange(this);
	}
	@Override 
	public short acceptByte(int ch) {
		return (startByteChar <= ch && ch <= endByteChar) ? StringAccept : StringReject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int ch = context.source.byteAt(context.pos);
		if(startByteChar <= ch && ch <= endByteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}