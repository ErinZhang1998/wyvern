package wyvern.tools.parsing.extensions;

import static wyvern.tools.parsing.ParseUtils.parseSymbol;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.ToolError;
import wyvern.tools.parsing.ContParser;
import wyvern.tools.parsing.DeclParser;
import wyvern.tools.parsing.ParseUtils;
import wyvern.tools.rawAST.ExpressionSequence;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.declarations.VarDeclaration;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.Pair;

public class VarParser implements DeclParser {
	private VarParser() { }
	private static VarParser instance = new VarParser();
	public static VarParser getInstance() { return instance; }
	
	@Override
	public TypedAST parse(TypedAST first, Pair<ExpressionSequence, Environment> ctx) {
		String varName = ParseUtils.parseSymbol(ctx).name;
		
		if (!ParseUtils.checkFirst(":", ctx)) {
			ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, ctx.first);
			return null;
		}
		parseSymbol(":", ctx);
		Type type = ParseUtils.parseType(ctx);
		
		if (ParseUtils.checkFirst("=", ctx)) {
			parseSymbol("=", ctx);
			TypedAST exp = ParseUtils.parseExpr(ctx);
			return new VarDeclaration(varName, type, exp);	
		} else if (ctx.first == null) {
			return new VarDeclaration(varName, type, null);
		} else {
			ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, ctx.first);
			return null;
		}
	}


	@Override
	public Pair<Environment, ContParser> parseDeferred(TypedAST first,
			final Pair<ExpressionSequence, Environment> ctx) {
		final String varName = ParseUtils.parseSymbol(ctx).name;
		
		if (ParseUtils.checkFirst("=", ctx)) {
			ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, ctx.first);
			return null;	
		} else if (ParseUtils.checkFirst(":", ctx)) {
			parseSymbol(":", ctx);
			final Type parsedType = ParseUtils.parseType(ctx);
			
			final Pair<ExpressionSequence, Environment> restctx = new Pair<ExpressionSequence,Environment>(ctx.first,ctx.second);
			ctx.first = null;
			
			final VarDeclaration intermvd = new VarDeclaration(varName, parsedType, null);
			
			return new Pair<Environment, ContParser>(Environment.getEmptyEnvironment().extend(new NameBindingImpl(varName, parsedType)), new ContParser(){

				@Override
				public TypedAST parse(EnvironmentResolver r) {
					if (restctx.first == null)
						return new VarDeclaration(varName, parsedType, null);
					else if (ParseUtils.checkFirst("=", restctx)) {
						ParseUtils.parseSymbol("=", restctx);
						Pair<ExpressionSequence,Environment> ctxi = new Pair<ExpressionSequence,Environment>(restctx.first, r.getEnv(intermvd));
						return new VarDeclaration(varName, parsedType, ParseUtils.parseExpr(restctx));
					} else {
						ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, ctx.first);
						return null;
					}
				}});
		} else {
			ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, ctx.first);
			return null;
		}
	}
}