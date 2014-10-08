package org.peg4d;

public class CParserGenerator extends ParsingExpressionVisitor {
	protected StringBuilder sb;
	public CParserGenerator() {
		sb = new StringBuilder();
	}
	
	public void generateCParser(Grammar peg) {
		UList<ParsingRule> list = peg.getRuleList();
		for(int i = 0; i < list.size(); i++) {
			ParsingRule rule = list.ArrayValues[i];
			this.generateRuleFunction(rule.ruleName, rule.expr);
		}
	}
	
	public void generateRuleFunction(String ruleName, ParsingExpression e) {
		sb.append("int parse_" + ruleName + "(context)\n{\n");
		e.visit(this);
		sb.append("}\n");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
	}
	
	@Override
	public void visitByte(ParsingByte e) {
	}
	
	@Override
	public void visitByteRange(ParsingByteRange e) {
	}
	
	@Override
	public void visitAny(ParsingAny e) {
	}
	
	@Override
	public void visitTagging(ParsingTagging e) {
	}
	
	@Override
	public void visitValue(ParsingValue e) {
	}
	
	@Override
	public void visitIndent(ParsingIndent e) {
	}
	
	@Override
	public void visitOptional(ParsingOption e) {
	}
	
	@Override
	public void visitRepetition(ParsingRepetition e) {
	}
	
	@Override
	public void visitAnd(ParsingAnd e) {
	}

	@Override
	public void visitNot(ParsingNot e) {
	}

	@Override
	public void visitConnector(ParsingConnector e) {
	}

	@Override
	public void visitSequence(ParsingSequence e) {
	}

	@Override
	public void visitChoice(ParsingChoice e) {
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
	}
}
