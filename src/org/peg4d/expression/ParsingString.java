package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingString extends ParsingExpression {
	public String text;
	public byte[] utf8;
	ParsingString(String text, byte[] utf8) {
		super();
		this.text = text;
		this.utf8 = utf8;
		this.minlen = utf8.length;
	}
	@Override
	ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("''\b" + text, this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitString(this);
	}
	@Override public short acceptByte(int ch) {
		if(this.utf8.length == 0) {
			return WeakReject;
		}
		return ((this.utf8[0] & 0xff) == ch) ? StringAccept : StringReject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(this.utf8.length);
			return true;
		}
		else {
			context.failure(this);
			return false;
		}
	}
}