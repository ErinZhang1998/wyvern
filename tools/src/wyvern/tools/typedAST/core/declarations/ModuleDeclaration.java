package wyvern.tools.typedAST.core.declarations;

import wyvern.tools.errors.FileLocation;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.TypeBinding;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.TypeType;
import wyvern.tools.types.extensions.Unit;
import wyvern.tools.util.Reference;
import wyvern.tools.util.TreeWriter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ModuleDeclaration extends Declaration implements CoreAST {
	protected String name;
	protected DeclSequence decls;
	protected Environment declEvalEnv;
	protected Reference<Environment> declEnv;
	protected Reference<Environment> typeEquivalentEnvironment;
	protected TypeType asc;
	protected List<ImportDeclaration> imports;

	protected ModuleDeclaration(String name, DeclSequence decls, FileLocation location) {
		this.decls = decls;
		this.location = location;
        this.name = name;
		typeEquivalentEnvironment = new Reference<>();
		declEnv = new Reference<>();
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		//TODO
	}

	public Type getClassType() {
		return new ClassType(declEnv, typeEquivalentEnvironment);//this.typeBinding.getType();
	}

	@Override
	public Type getType() {
		return getClassType();
	}


	@Override
	public Type doTypecheck(Environment env) {

		// FIXME: Currently allow this and class in both class and object methods. :(

		if (decls != null)
			for (Declaration decl : decls.getDeclIterator()) {
				decl.typecheckSelf(env);
			}
		return Unit.getInstance();
	}

	@Override
	protected Environment doExtend(Environment old) {
		Type classType = getClassType();
		return old.extend(new TypeBinding(name, classType)).extend(new NameBindingImpl(name, classType));
	}

	@Override
	public Environment extendWithValue(Environment old) {
		return old;
	}

	@Override
	public void evalDecl(Environment evalEnv, Environment declEnv) {
		declEvalEnv = declEnv.extend(evalEnv);
		Environment thisEnv = decls.extendWithDecls(Environment.getEmptyEnvironment());
	}

	public Declaration getDecl(String opName) {
		for (Declaration d : decls.getDeclIterator()) {
			// TODO: handle fields too
			if (d.getName().equals(opName))
				return d;
		}
		return null;	// can't find it
	}

	public DeclSequence getDecls() {
		return decls;
	}

	@Override
	public String getName() {
		return "";//nameBinding.getName();
	}

	private FileLocation location = FileLocation.UNKNOWN;

	@Override
	public FileLocation getLocation() {
		return location; // TODO: NOT IMPLEMENTED YET.
	}

	public NameBinding lookupDecl(String name) {
		return declEnv.get().lookup(name);
	}
}
