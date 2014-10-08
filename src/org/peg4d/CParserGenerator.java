package org.peg4d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CParserGenerator extends ParsingExpressionVisitor {
	protected StringBuilder sb, header_sb;
	int indent_level = 0;
	int dephth = 0;
	int connectNum = 0;
	boolean backtrackflag = false;
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
		indent_level++;
		dephth++;
		sb.append("\tuint8_t c;\n");
		e.visit(this);
		indent_level--;
		dephth--;
		sb.append("\tif(ParserContext_IsFailure(context)) {\n");
		sb.append("\t\treturn 1;\n");
		sb.append("\t}\n");
		sb.append("\treturn 0;\n");
		sb.append("}\n\n");
		backtrackflag = false;
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "parse_" + e.ruleName + "(context, input);\n");
	}
	
	@Override
	public void visitByte(ParsingByte e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "c = InputSource_GetUint8(input);\n");
		sb.append(indent + "if(c != (uint8_t)"+ e.byteChar +") {\n");
		sb.append(indent + "\tParserContext_RecordFailurePos(context, input, 1);\n");
		sb.append(indent + "}\n");
	}
	
	@Override
	public void visitByteRange(ParsingByteRange e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "c = InputSource_GetUint8(input);\n");
		sb.append(indent + "if(c < (uint8_t)"+ e.startByteChar +" || c > (uint8_t)" + e.endByteChar + ") {\n");
		sb.append(indent + "\tParserContext_RecordFailurePos(context, input, 1);\n");
		sb.append(indent + "}\n");
	}
	
	@Override
	public void visitAny(ParsingAny e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "c = InputSource_GetUint8(input);\n");
		sb.append(indent + "if (c == (uint8_t)-1) {\n");
		sb.append(indent + "\tParserContext_RecordFailurePos(context, input, 1);\n");
		sb.append(indent + "}\n");
	}
	
	@Override
	public void visitTagging(ParsingTagging e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "NODE_SetTag(context->current_node, (uint8_t*)\"" + e.tag + "\", input);\n");
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
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "NODE node;\n");
		sb.append(indent + "while(!ParserContext_IsFailure(context)) {\n");
		indent_level++;
		sb.append(indent + "\tnode = context->current_node;\n");
		dephth++;
		e.inner.visit(this);
		dephth--;
		indent_level--;
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
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "NODE *parent_"+ dephth + connectNum +" = context->current_node;\n");
		dephth++;
		e.inner.visit(this);
		dephth--;
		sb.append(indent + "NODE_AppendChild(parent, context->current_node);\n");
		sb.append(indent + "context->current_node = parent;\n");
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "int seqencebacktrackpos_" + dephth + " = input->pos;\n");
		for(int j = 0; j < e.size(); j++) {
			dephth++;
			e.get(j).visit(this);
			dephth--;
			sb.append(indent + "if(ParserContext_IsFailure(context)) {\n");
			sb.append(indent + "\tgoto fail;\n");
			sb.append(indent + "}\n");
		}
		sb.append(indent + "goto succ;\n");
		sb.append("fail:\n");
		sb.append(indent + "input->pos = seqencebacktrackpos_" + dephth + ";\n");
		sb.append("succ:\n");
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "int choicebacktrackpos_" + dephth + " = input->pos;\n");
		sb.append(indent + "NODE *node = NODE_New(NODE_TYPE_DEFAULT, input->pos);\n");
		sb.append(indent + "node = context->current_node;\n");
		for(int j = 0; j < e.size() - 1; j++) {
			dephth++;
			e.get(j).visit(this);
			dephth--;
			sb.append(indent + "if(ParserContext_IsFailure(context)) {\n");
			sb.append(indent + "\tinput->pos = choicebacktrackpos_" + dephth + ";\n");
			sb.append(indent + "\tcontext->current_node = node;\n");
			sb.append(indent + "}\n");
			sb.append(indent + "else {\n");
			sb.append(indent + "\tgoto choice_succ;\n");
			sb.append(indent + "}\n");
		}
		e.get(e.size() - 1).visit(this);
		sb.append(indent + "if(ParserContext_IsFailure(context)) {\n");
		sb.append(indent + "\tinput->pos = choicebacktrackpos_" + dephth + ";\n");
		sb.append(indent + "}\n");
		sb.append("choice_succ:\n");
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		String indent = "";
		int i = 0;
		while(i < indent_level) {
			indent += "\t";
			i++;
		}
		sb.append(indent + "int constbacktrackpos_" + dephth + " = input->pos;\n");
		sb.append(indent + "context->current_node = NODE_New(NODE_TYPE_DEFAULT, input->pos);\n");
		for(int j = 0; j < e.size(); j++) {
			connectNum = j;
			dephth++;
			e.get(j).visit(this);
			dephth--;
			sb.append(indent + "if(ParserContext_IsFailure(context)) {\n");
			sb.append(indent + "\tgoto const_fail;\n");
			sb.append(indent + "}\n");
		}
		sb.append(indent + "goto const_succ;\n");
		sb.append("const_fail:\n");
		sb.append(indent + "input->pos = constbacktrackpos_" + dephth + ";\n");
		sb.append("const_succ:\n");
	}
}
