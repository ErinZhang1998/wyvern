package wyvern.targets.JavaScript.typedAST;

import wyvern.targets.JavaScript.types.JSObjectType;
import wyvern.targets.JavaScript.visitors.JSCodegenVisitor;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.abs.AbstractTypedAST;
import wyvern.tools.typedAST.abs.AbstractValue;
import wyvern.tools.typedAST.core.Application;
import wyvern.tools.typedAST.interfaces.*;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.TreeWriter;

import java.util.HashMap;
import java.util.Map;

public class JSFunction extends AbstractValue implements ApplyableValue, CoreAST {
	private String name;
	private Type type;
	public JSFunction(Type type, String name) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return this.name;
	}
	
	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		return new HashMap<>();
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> newChildren) {
		return new JSFunction(type, name);
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		// no arguments at the moment; also not intended for serialization		
	}

	@Override
	public Value evaluateApplication(Application app, Environment env) {
		throw new RuntimeException("JSFunctions may not me invoked within the interpreter");
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		if (!(visitor instanceof JSCodegenVisitor))
			throw new RuntimeException("JSFunctions can only be traversed by a JS codegeneration visitor.");
		((JSCodegenVisitor)visitor).visit(this);
	}

	private FileLocation location = FileLocation.UNKNOWN;
	public FileLocation getLocation() {
		return this.location;
	}
}
