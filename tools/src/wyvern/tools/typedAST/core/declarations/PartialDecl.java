package wyvern.tools.typedAST.core.declarations;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.parsing.ContParser;
import wyvern.tools.parsing.ContParser.EnvironmentResolver;
import wyvern.tools.parsing.LineParser;
import wyvern.tools.parsing.LineSequenceParser;
import wyvern.tools.parsing.RecordTypeParser;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.Pair;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class PartialDecl implements TypedAST {

	private Pair<Environment, ContParser> pair;
    private boolean preParsed = false;

	public PartialDecl(Pair<Environment, ContParser> pair) {
		this.pair = pair;
	}
	
	public Environment extend(Environment env) {
		return env.extend(pair.first);
	}

	public void preParseTypes(final Environment env) {
		preParsed = true;
		if (pair.second instanceof RecordTypeParser) {
			EnvironmentResolver r = new EnvironmentResolver() {

				@Override
				public Environment getEnv(TypedAST elem) {
					return env;
				}

			};
			((RecordTypeParser)pair.second).parseTypes(r);
		}
	}

    public void preParseDecls(final Environment env) {
        preParsed = true;
		if (pair.second instanceof RecordTypeParser) {
			EnvironmentResolver r = new EnvironmentResolver() {

				@Override
				public Environment getEnv(TypedAST elem) {
					return env;
				}

			};
			((RecordTypeParser)pair.second).parseInner(r);
		}
    }

	public TypedAST getAST(final Environment env) {
        if (!preParsed) {
			preParseTypes(env);
            preParseDecls(env);
		}
		return pair.second.parse(new EnvironmentResolver() {

			@Override
			public Environment getEnv(TypedAST elem) {
				return env;
			}
			
		});
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileLocation getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type typecheck(Environment env, Optional<Type> expected) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LineParser getLineParser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LineSequenceParser getLineSequenceParser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		return new HashMap<>();
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		throw new RuntimeException("Cannot rewrite a partial declaration");
	}
}