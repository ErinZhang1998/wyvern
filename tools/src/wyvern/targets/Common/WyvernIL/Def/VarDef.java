package wyvern.targets.Common.WyvernIL.Def;

import wyvern.targets.Common.WyvernIL.Expr.Expression;
import wyvern.targets.Common.WyvernIL.Imm.Operand;
import wyvern.targets.Common.WyvernIL.Stmt.Pure;
import wyvern.targets.Common.WyvernIL.visitor.DefVisitor;

public class VarDef implements Definition {

	private String name;
	private Expression exn;

	public VarDef(String name, Expression exn) {
		this.name = name;
		this.exn = exn;
	}

	@Override
	public <R> R accept(DefVisitor<R> visitor) {
		return visitor.visit(this);
	}

	public Expression getExn() {
		return exn;
	}

	public String getName() {
		return name;
	}
	@Override
	public String toString() {
		return "var "+name+" = " + exn;
	}
}