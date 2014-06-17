package wyvern.tools.types;

import java.util.HashSet;

public abstract class AbstractTypeImpl implements Type {

	@Override
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes) {
		// S-Refl
		if (this.equals(other)) {
			return true;
		}

		// S-Assumption
		if (subtypes.contains(new SubtypeRelation(this, other))) {
			return true;
		}
		
		// S-Trans
		HashSet<Type> t2s = new HashSet<Type>();
		for (SubtypeRelation sr : subtypes) {
			if (sr.getSubtype().equals(this)) {
				t2s.add(sr.getSupertype());
			}
		}
		for (Type t : t2s) {
			if (subtypes.contains(new SubtypeRelation(t, other))) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean subtype(Type other) {
		return this.subtype(other, new HashSet<SubtypeRelation>());
	}
	
	public boolean isSimple() {
		return true; // default is correct for most types
	}

}