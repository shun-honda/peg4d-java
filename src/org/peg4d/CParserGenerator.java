package org.peg4d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CParserGenerator extends ParsingExpressionVisitor {
	protected StringBuilder sb, header_sb;
	int level = 0;
	public CParserGenerator() {
		sb = new StringBuilder();
		header_sb = new StringBuilder();
	}
	
	public void generateCParser(Grammar peg) {
		UList<ParsingRule> list = peg.getRuleList();
		this.generateHeader();
		for(int i = 0; i < list.size(); i++) {
			ParsingRule rule = list.ArrayValues[i];
			this.generateRuleFunction(rule.ruleName, rule.expr);
		}
		System.out.println("\nOutput C Parser:\n");
		System.out.println(header_sb.toString());
		System.out.println(sb.toString());
		
		try{
			  File file = new File("../simplevm/src/parser.generated.c");
			  FileWriter filewriter = new FileWriter(file);
			  filewriter.write(header_sb.toString() + sb.toString());
			  filewriter.close();
		}catch(IOException e){
			System.out.println(e);
		}
	}
	
	public void generateHeader() {
		header_sb.append("#include \"pegvm.h\"\n");
		header_sb.append("#include \"input_source.h\"\n");
		header_sb.append("#include \"node.h\"\n");
		header_sb.append("#include <stdio.h>\n");
	}
	
	public void generateRuleFunction(String ruleName, ParsingExpression e) {
		header_sb.append("int parse_" + ruleName + "(ParserContext *context, InputSource *input);\n");
		sb.append("int parse_" + ruleName + "(ParserContext *context, InputSource *input)\n{\n");
		level++;
		e.visit(this);
		level--;
		sb.append("\tif(ParserContext_IsFailure(context)) {\n");
		sb.append("\t\treturn 1;\n");
		sb.append("\t}\n");
		sb.append("\treturn 0;\n");
		sb.append("}\n\n");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		sb.append("parse_" + e.ruleName + "(context);");
	}
	
	@Override
	public void visitByte(ParsingByte e) {
		String indent = "";
		int i = 0;
		while(i < level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "uint8_t c = InputSource_GetUint8(input);\n");
		sb.append(indent + "if(c != (uint8_t)"+ e.byteChar +") {\n");
		sb.append(indent + "\tParserContext_RecordFailurePos(context, input, 1);\n");
		sb.append(indent + "}\n");
	}
	
	@Override
	public void visitByteRange(ParsingByteRange e) {
		String indent = "";
		int i = 0;
		while(i < level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "uint8_t c = InputSource_GetUint8(input);\n");
		sb.append(indent + "if(c < (uint8_t)"+ e.startByteChar +" || c > (uint8_t)" + e.endByteChar + ") {\n");
		sb.append(indent + "\tParserContext_RecordFailurePos(context, input, 1);\n");
		sb.append(indent + "}\n");
	}
	
	@Override
	public void visitAny(ParsingAny e) {
		String indent = "";
		int i = 0;
		while(i < level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "uint8_t c = InputSource_GetUint8(input);\n");
		sb.append(indent + "if (c == (uint8_t)-1) {\n");
		sb.append(indent + "\tParserContext_RecordFailurePos(context, input, 1);\n");
		sb.append(indent + "}\n");
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
		String indent = "";
		int i = 0;
		while(i < level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "NODE node;\n");
		sb.append(indent + "while(!ParserContext_IsFailure(context)) {\n");
		level++;
		sb.append(indent + "\tnode = context->current_node;\n");
		e.inner.visit(this);
		level--;
		sb.append(indent + "}\n");
		sb.append(indent + "context->current_node = node;\n");
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
