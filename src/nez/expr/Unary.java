package nez.expr;

import java.util.TreeMap;

import nez.ast.SourcePosition;

public abstract class Unary extends Expression {
	Expression inner;
	Unary(SourcePosition s, Expression inner) {
		super(s);
		this.inner = inner;
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final Expression get(int index) {
		return this.inner;
	}
	@Override
	public final Expression set(int index, Expression e) {
		Expression old = this.inner;
		this.inner = e;
		return old;
	}
	@Override
	public String getInterningKey() {
		return this.getPredicate();
	}

	@Override
	public Expression removeNodeOperator() {
		return this.dupUnary(inner.removeNodeOperator());
	}
	
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		return this.dupUnary(inner.removeFlag(undefedFlags));
	}

	abstract Expression dupUnary(Expression inner);

}
