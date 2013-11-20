package wyvern.tools.parsing.extensions;

import java.util.HashSet;

import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.parsing.*;
import wyvern.tools.rawAST.LineSequence;
import wyvern.tools.rawAST.Symbol;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.Sequence;
import wyvern.tools.typedAST.core.binding.ClassBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.declarations.*;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.util.CompilationContext;
import wyvern.tools.util.Pair;

/**
 * 	class NAME
 * 		[implements NAME]
 *		[class implements NAME]
 * 		DELCARATION*
 */

public class ClassParser implements DeclParser, TypeExtensionParser {
	private ClassParser() { }
	private static ClassParser instance = new ClassParser();
	public static ClassParser getInstance() { return instance; }

	//REALLY HACKY
	private static class MutableClassDeclaration extends ClassDeclaration {
		public MutableClassDeclaration(String name, String implementsName,
				String implementsClassName, Environment declEnv, FileLocation clsNameLine) {
			super(name, implementsName, implementsClassName, null, declEnv, clsNameLine);
		}

        public void setDeclEnv(Environment nd) {
            super.declEnvRef.set(nd);
        }

		public void setDecls(DeclSequence decl) {
			this.decls = decl;
			updateEnv();
		}
	}

	@Override
	public TypedAST parse(TypedAST first, CompilationContext ctx) {
		Pair<Environment, ContParser> p = parseDeferred(first,  ctx);
		return p.second.parse(new ContParser.SimpleResolver(p.first.extend(ctx.getEnv())));
	}

	@Override
	public Pair<Environment, RecordTypeParser> parseRecord(TypedAST first,
														   CompilationContext ctx) {
		Pair<Environment, ContParser> pair = parseDeferred(first, ctx);
		return new Pair<>(pair.first, (RecordTypeParser)pair.second);
	}

	@Override
	public boolean typeRequiredPartialParse(CompilationContext ctx) {
		if (ParseUtils.checkFirst("def", ctx)) { // Parses "class def". // FIXME: Should this connect to the keyword in Globals?
			return true;
		}
		return false;
	}

	@Override
	public Pair<Environment, ContParser> parseDeferred(TypedAST first,
			final CompilationContext ctx) {
		if (ParseUtils.checkFirst("def", ctx)) { // Parses "class def". // FIXME: Should this connect to the keyword in Globals?
			ParseUtils.parseSymbol(ctx);
			return DefParser.getInstance().parseDeferred(first, ctx, true);
		}
		
		Symbol s = ParseUtils.parseSymbol(ctx);
		String clsName = s.name;
		FileLocation clsNameLine = s.getLocation();

		String implementsName = "";
		String implementsClassName = "";

		final MutableClassDeclaration mutableDecl = new MutableClassDeclaration(clsName, implementsName, implementsClassName, null, clsNameLine);

		if (ctx.getTokens() == null) {
			return new Pair<Environment,ContParser>(mutableDecl.extend(Environment.getEmptyEnvironment()),new ContParser() {

                @Override
				public TypedAST parse(EnvironmentResolver env) {
					return mutableDecl;
				}
			});
		}
		
		final LineSequence lines = ParseUtils.extractLines(ctx); // Get potential body.
		
		if (lines.getFirst() != null && lines.getFirst().getFirst() != null &&
				lines.getFirst().getFirst().toString().equals("implements")) { // FIXME: hack, detected implements
			implementsName = lines.getFirst().getRest().getFirst().toString();
			lines.children.remove(0);
		}

		if (lines.getFirst() != null && lines.getFirst().getFirst() != null &&
				lines.getFirst().getFirst().toString().equals("class")) { // FIXME: hack, detected class
			if (lines.getFirst().getRest() != null && lines.getFirst().getRest().getFirst() != null &&
					lines.getFirst().getRest().getFirst().toString().equals("implements")) { // FIXME: hack, detected implements
				implementsClassName = lines.getFirst().getRest().getRest().getFirst().toString();
				lines.children.remove(0);
			}
		}

		// Process body.
		if (ctx.getTokens() != null)
			ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, ctx.getTokens());
		
		final MutableClassDeclaration mutableDeclf = new MutableClassDeclaration(clsName, implementsName, implementsClassName, null, clsNameLine);
		
		Environment newEnv = mutableDeclf.extend(Environment.getEmptyEnvironment()); 
		
		Environment typecheckEnv = ctx.getEnv().extend(newEnv);
		typecheckEnv = typecheckEnv.extend(new ClassBinding("class", mutableDeclf));
		
		
		return new Pair<Environment,ContParser>(newEnv, new RecordTypeParser.RecordTypeParserBase() {

            private Environment envs = null;
            private Environment envi;
            private Pair<Environment,ContParser> declAST;

			@Override
			public void doParseTypes(EnvironmentResolver r) {
				Environment external = r.getEnv(mutableDeclf);

				Environment envin = mutableDeclf.extend(external);
				envs = envin.extend(new ClassBinding("class", mutableDeclf));
				declAST = lines.accept(new ClassBodyParser(ctx), envs);
				envi = envs.extend(new NameBindingImpl("this", mutableDeclf.getType()));
				if (declAST.second instanceof RecordTypeParser)
					((RecordTypeParser)declAST.second).parseTypes(new SimpleResolver(envs));
				mutableDeclf.setDeclEnv(declAST.first);
			}

			@Override
            public void doParseInner(EnvironmentResolver envR) {
				if (declAST.second instanceof RecordTypeParser)
					((RecordTypeParser)declAST.second).parseInner(new SimpleResolver(envs));
				mutableDeclf.setDeclEnv(((ClassBodyParser.ClassBodyContParser)declAST.second).getInternalEnv());
            }

            @Override
			public TypedAST parse(EnvironmentResolver envR) {
                if (envs == null) {
                	parseTypes(envR);
                    parseInner(envR);
				}
				TypedAST innerAST = declAST.second.parse(new EnvironmentResolver() {
					@Override
					public Environment getEnv(TypedAST elem) {
						if (elem instanceof DefDeclaration && ((DefDeclaration) elem).isClass()) {
								return envs;
						}
						return envi;
					}
				});
				
				if (!(innerAST instanceof Declaration) && !(innerAST instanceof Sequence))
					ToolError.reportError(ErrorMessage.UNEXPECTED_INPUT, innerAST);
				
				// Make sure that all members have unique names.
				HashSet<String> names = new HashSet<>();
				for (Declaration d : DeclSequence.getDeclSeq(innerAST).getDeclIterator()) {
					if (names.contains(d.getName())) {
						ToolError.reportError(ErrorMessage.DUPLICATE_MEMBER, mutableDeclf, mutableDeclf.getName(), d.getName());
					}
					names.add(d.getName());
				}
				
				mutableDeclf.setDecls(DeclSequence.getDeclSeq(innerAST));

				return mutableDeclf;
			}
			
		});
	}
}
