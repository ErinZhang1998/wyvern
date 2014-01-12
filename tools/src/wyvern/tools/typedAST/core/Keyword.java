package wyvern.tools.typedAST.core;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.parsing.LineParser;
import wyvern.tools.typedAST.abs.AbstractValue;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.types.Environment;
import wyvern.tools.types.KeywordType;
import wyvern.tools.types.Type;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.Map;

public class Keyword extends AbstractValue implements Value, CoreAST {
	private LineParser parser;
	
	public Keyword(LineParser parser) {
		this.parser = parser;
	}

	@Override
	public Type getType() {
		return KeywordType.getInstance();
	}

	@Override
	public LineParser getLineParser() {
		return parser;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		return new HashMap<>();
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return this;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		// nothing to write; we don't expect to roundtrip this type
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		// TODO I don't think that this'll ever show up, as such, throe an exception
		throw new RuntimeException();
	}

	private FileLocation location = FileLocation.UNKNOWN;
	public FileLocation getLocation() {
		return this.location;
	}
}
