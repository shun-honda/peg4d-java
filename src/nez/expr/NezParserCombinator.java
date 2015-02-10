package nez.expr;

import nez.Grammar;
import nez.ParserCombinator;

public class NezParserCombinator extends ParserCombinator {

	NezParserCombinator(Grammar grammar) {
		super(grammar);
	}
	
	private static Grammar peg = null;
	public final static Grammar newGrammar() {
		if(peg == null) {
			peg = new NezParserCombinator(new Grammar("nez")).load();
		}
		return peg;
	}

	public Expression EOL() {
		return c("\\r\\n");
	}

	public Expression EOT() {
		return Not(Any());
	}

	public Expression S() {
		return Choice(c(" \\t\\r\\n"), t("\u3000"));
	}

	public Expression DIGIT() {
		return c("0-9");
	}

	public Expression LETTER() {
		return c("A-Za-z_");
	}

	public Expression HEX() {
		return c("0-9A-Fa-f");
	}

	public Expression W() {
		return c("A-Za-z0-9_");
	}

	public Expression INT() {
		return Sequence(P("DIGIT"), ZeroMore(P("DIGIT")));
	}
	
	public Expression NAME() {
		return Sequence(P("LETTER"), ZeroMore(P("W")));
	}

	public Expression COMMENT() {
		return Choice(
			Sequence(t("/*"), ZeroMore(Not(t("*/")), Any()), t("*/")),
			Sequence(t("//"), ZeroMore(Not(P("EOL")), Any()), P("EOL"))
		);
	}

	public Expression SPACING() {
		return ZeroMore(Choice(P("S"), P("COMMENT")));
	}
	
	public Expression Integer() {
		return Constructor(P("INT"), Tag(NezTag.Integer));
	}

	public Expression Name() {
		return Constructor(P("LETTER"), ZeroMore(P("W")), Tag(NezTag.Name));
	}

	public Expression DotName() {
		return Constructor(P("LETTER"), ZeroMore(c("A-Za-z0-9_.")), Tag(NezTag.Name));
	}

	public Expression HyphenName() {
		return Constructor(P("LETTER"), ZeroMore(Choice(P("W"), t("-"))), Tag(NezTag.Name));
	}

	public Expression String() {
		Expression StringContent  = ZeroMore(Choice(
			t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any())
		));
		return Sequence(t("\""), Constructor(StringContent, Tag(NezTag.String)), t("\""));
	}

	public Expression SingleQuotedString() {
		Expression StringContent  = ZeroMore(Choice(
			t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any())
		));
		return Sequence(t("'"),  Constructor(StringContent, Tag(NezTag.CharacterSequence)), t("'"));
	}

	public Expression ValueReplacement() {
		Expression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), Any())
		));
		return Sequence(t("`"), Constructor(ValueContent, Tag(NezTag.Value)), t("`"));
	}

	public Expression NonTerminal() {
		return Constructor(
				P("LETTER"), 
				ZeroMore(c("A-Za-z0-9_:")), 
				Tag(NezTag.NonTerminal)
		);
	}
	
	public Expression CHAR() {
		return Choice( 
			Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")),
			Sequence(t("\\x"), P("HEX"), P("HEX")),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), Any())
		);
	}

	public Expression Charset() {
		Expression _CharChunk = Sequence(
			Constructor (P("CHAR"), Tag(NezTag.Character)), 
			Optional(
				LeftJoin(t("-"), Link(Constructor(P("CHAR"), Tag(NezTag.Character))), Tag(NezTag.List))
			)
		);
		return Sequence(t("["), Constructor(ZeroMore(Link(_CharChunk)), Tag(NezTag.Character)), t("]"));
	}

	public Expression Constructor() {
		Expression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		Expression Connector  = Choice(t("@"), t("^"));
		Expression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));
		return Constructor(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, P("S"), Tag(NezTag.LeftJoin)), 
				Tag(NezTag.Constructor)
			), 
			P("_"), 
			Optional(Sequence(Link(P("Expr")), P("_"))),
			ConstructorEnd
		);
	}
	
	public Expression Func() {
		return Sequence(t("<"), Constructor(
		Choice(
			Sequence(t("debug"),   P("S"), Link(P("Expr")), Tag(NezTag.Debug)),
			Sequence(t("memo"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(NezTag.Memo)),
			Sequence(t("match"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(NezTag.Match)),
//			Sequence(t("fail"),   P("S"), Link(P("SingleQuotedString")), P("_"), t(">"), Tag(NezTag.Fail)),
//			Sequence(t("catch"), Tag(NezTag.Catch)),
			Sequence(t("if"), P("S"), Optional(t("!")), Link(P("Name")), Tag(NezTag.If)),
			Sequence(t("with"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.With)),
			Sequence(t("without"), P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Without)),
			Sequence(t("block"), Optional(Sequence(P("S"), Link(P("Expr")))), Tag(NezTag.Block)),
			Sequence(t("indent"), Tag(NezTag.Indent)),
				//						Sequence(t("choice"), Tag(NezTag.Choice)),
//			Sequence(t("powerset"), P("S"), Link(P("Expr")), Tag(NezTag.PowerSet)),
//			Sequence(t("permutation"), P("S"), Link(P("Expr")), Tag(NezTag.Permutation)),
//			Sequence(t("perm"), P("S"), Link(P("Expr")), Tag(NezTag.PermutationExpr)),
			Sequence(t("scan"), P("S"), Link(Constructor(DIGIT(), ZeroMore(DIGIT()))), t(","), P("S"), Link(P("Expr")), t(","), P("S"), Link(P("Expr")), Tag(NezTag.Scan)),
			Sequence(t("repeat"), P("S"), Link(P("Expr")), Tag(NezTag.Repeat)),
			Sequence(t("is"), P("S"), Link(P("Name")), Tag(NezTag.Is)),
			Sequence(t("isa"), P("S"), Link(P("Name")), Tag(NezTag.Isa)),
			Sequence(t("def"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Def)),
			Sequence(t("name"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Def)),
			Sequence(Optional(t("|")), t("append-choice"), Tag(NezTag.Choice))
//			Sequence(Optional(t("|")), t("stringfy"), Tag(NezTag.Stringfy)),
//			Sequence(Optional(t("|")), t("apply"), P("S"), Link(P("Expr")), Tag(NezTag.Apply))
		)), P("_"), t(">")
		);
	}

	public Expression Term() {
		Expression _Any = Constructor(t("."), Tag(NezTag.Any));
		Expression _Tagging = Sequence(t("#"), Constructor(c("A-Za-z0-9"), ZeroMore(c("A-Za-z0-9_.")), Tag(NezTag.Tagging)));
		Expression _Byte = Constructor(t("0x"), P("HEX"), P("HEX"), Tag(NezTag.Byte));
		Expression _Unicode = Constructor(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag(NezTag.Byte));
		return Choice(
			P("SingleQuotedString"), P("Charset"), P("Func"),  
			_Any, P("ValueReplacement"), _Tagging, _Byte, _Unicode,
			Sequence(t("("), P("_"), P("Expr"), P("_"), t(")")),
			P("Constructor"), P("String"), P("NonTerminal") 
		);
	}
	
	public Expression SuffixTerm() {
		Expression Connector  = Choice(t("@"), t("^"));
		return Sequence(
			P("Term"), 
			Optional(
				LeftJoin(
					Choice(
						Sequence(t("*"), Optional(Link(1, P("Integer"))), Tag(NezTag.Repetition)), 
						Sequence(t("+"), Tag(NezTag.OneMoreRepetition)), 
						Sequence(t("?"), Tag(NezTag.Option)),
						Sequence(Connector, Optional(Link(1, P("Integer"))), Tag(NezTag.Connector))
					)
				)
			)
		);
	}
	
	public Expression Predicate() {
		return Choice(
			Constructor(
				Choice(
					Sequence(t("&"), Tag(NezTag.And)),
					Sequence(t("!"), Tag(NezTag.Not)),
					Sequence(t("@["), P("_"), Link(1, P("Integer")), P("_"), t("]"), Tag(NezTag.Connector)),							
					Sequence(t("@"), Tag(NezTag.Connector))
				), 
				Link(0, P("SuffixTerm"))
			), 
			P("SuffixTerm")
		);
	}

	public Expression NOTRULE() {
		return Not(Choice(P("Rule"), P("Import")));
	}

	public Expression Sequence() {
		return Sequence(
			P("Predicate"), 
			Optional(
				LeftJoin(
					P("_"), 
					P("NOTRULE"),
					Link(P("Predicate")),
					ZeroMore(
						P("_"), 
						P("NOTRULE"),
						Link(P("Predicate"))
					),
					Tag(NezTag.Sequence) 
				)
			)
		);
	}

	public Expression Expr() {
		return Sequence(
			P("Sequence"), 
			Optional(
				LeftJoin(
					P("_"), t("/"), P("_"), 
					Link(P("Sequence")), 
					ZeroMore(
						P("_"), t("/"), P("_"), 
						Link(P("Sequence"))
					),
					Tag(NezTag.Choice) 
				)
			)
		);
	}
		
	public Expression DOC() {
		return Sequence(
			ZeroMore(Not(t("]")), Not(t("[")), Any()),
			Optional(Sequence(t("["), P("DOC"), t("]"), P("DOC") ))
		);
	}

	public Expression Annotation() {
		return Sequence(
			t("["),
			Constructor(
				Link(P("HyphenName")),
				Optional(
					t(":"),  P("_"), 
					Link(Constructor(P("DOC"), Tag(NezTag.Text))),
					Tag(NezTag.Annotation)
				)
			),
			t("]"),
			P("_")
		);
	}

	public Expression Annotations() {
		return Constructor(
			Link(P("Annotation")),
			ZeroMore(Link(P("Annotation"))),
			Tag(NezTag.List) 
		);	
	}
	
	public Expression Rule() {
		return Constructor(
			Link(0, Choice(P("Name"), P("String"))), P("_"), 
//			Optional(Sequence(Link(3, P("Param_")), P("_"))),
			Optional(Sequence(Link(2, P("Annotations")), P("_"))),
			t("="), P("_"), 
			Link(1, P("Expr")),
			Tag(NezTag.Rule) 
		);
	}
	
	public Expression Import() {
//		return Constructor(
//			t("import"), 
//			P("S"), 
//			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
//			Optional(Sequence(P("S"), t("as"), P("S"), Link(P("Name")))),
//			Tag(NezTag.Import)
//		);
		return Constructor(
			t("import"), P("S"), 
			Link(P("NonTerminal")),
			ZeroMore(P("_"), t(","), P("_"),  Link(P("NonTerminal"))), P("_"), 
			t("from"), P("S"), 
			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
		Tag(NezTag.Import)
	);
	}
	
	public Expression Chunk() {
		return Sequence(
			P("_"), 
			Choice(
				P("Rule"), 
				P("Import")
			), 
			P("_"), 
			Optional(Sequence(t(";"), P("_")))
		);
	}

	public Expression File() {
		return Constructor(
			P("_"), 
			ZeroMore(Link(P("Chunk"))),
			Tag(NezTag.List)
		);
	}

}
