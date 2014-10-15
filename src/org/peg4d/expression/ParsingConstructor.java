package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.UList;

public class ParsingConstructor extends ParsingList {
	public boolean leftJoin = false;
	int prefetchIndex = 0;
	ParsingConstructor(boolean leftJoin, UList<ParsingExpression> list) {
		super(list);
		this.leftJoin = leftJoin;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		if(leftJoin) {
			return ParsingExpression.uniqueExpression("{@}\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniqueExpression("{}\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		ParsingExpression ne = (lexOnly) ? ParsingExpression.newSequence(l) : ParsingExpression.newConstructor(this.leftJoin, l);
		if(this.isExpectedConnector()) {
			ne = ParsingExpression.newConnector(ne, -1);
		}
		return ne;
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitConstructor(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
//		ParsingObject left = context.left;
		for(int i = 0; i < this.prefetchIndex; i++) {
			if(!this.get(i).matcher.simpleMatch(context)) {
				context.rollback(startIndex);
				return false;
			}
		}
		int mark = context.markObjectStack();
		ParsingObject newnode = context.newParsingObject(startIndex, this);
		if(this.leftJoin) {
			context.lazyCommit(context.left);
			context.logLink(newnode, 0, context.left);
		}
		context.left = newnode;
		for(int i = this.prefetchIndex; i < this.size(); i++) {
			if(!this.get(i).matcher.simpleMatch(context)) {
				context.abortLinkLog(mark);
				context.rollback(startIndex);
				newnode = null;
				return false;
			}
		}
		newnode.setLength((int)(context.getPosition() - startIndex));
		//context.commitLinkLog2(newnode, startIndex, mark);
		//System.out.println("newnode: " + newnode.oid);
		context.left = newnode;
		newnode = null;
		return true;
	}
}