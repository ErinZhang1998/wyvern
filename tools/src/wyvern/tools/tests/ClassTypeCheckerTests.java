package wyvern.tools.tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import junit.framework.Assert;

import org.junit.Test;

import wyvern.stdlib.Globals;
import wyvern.tools.errors.ToolError;
import wyvern.tools.parsing.BodyParser;
import wyvern.tools.rawAST.RawAST;
import wyvern.tools.simpleParser.Phase1Parser;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.Sequence;
import wyvern.tools.typedAST.core.declarations.ClassDeclaration;
import wyvern.tools.typedAST.core.declarations.DeclSequence;
import wyvern.tools.typedAST.core.declarations.TypeDeclaration;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.types.Environment;
import wyvern.tools.types.SubtypeRelation;
import wyvern.tools.types.Type;
import wyvern.tools.types.extensions.ClassType;
import wyvern.tools.types.extensions.TypeType;
import wyvern.tools.types.extensions.Unit;

public class ClassTypeCheckerTests {
	@Test
	public void testClassDeclaration() throws IOException {
		String testFileName;
		URL url;
		
		testFileName = "wyvern/tools/tests/samples/SimpleClass.wyv";
		url = ClassTypeCheckerTests.class.getClassLoader().getResource(testFileName);
		if (url == null) {
			Assert.fail("Unable to open " + testFileName + " file.");
			return;
		}
		InputStream is = url.openStream();
		Reader reader = new InputStreamReader(is);

		testFileName = "wyvern/tools/tests/samples/parsedSimpleClass.prsd";
		url = ClassTypeCheckerTests.class.getClassLoader().getResource(testFileName);
		Scanner s = new Scanner(url.openStream());
		String parsedTestFileAsString = s.nextLine();
		s.close();
		
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals(parsedTestFileAsString, parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		typedAST.typecheck(env);
		
		//Value resultValue = typedAST.evaluate(env);
		//Assert.assertEquals("()", resultValue.toString());
	}
	
	@Test
	public void testImplements() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"type StackFactory\n"
				+"    def Stack() : Stack\n"
				+"    def StackWithFirst(firstElement : Int) : Stack\n"
				+"\n"
				+"class StackImpl\n"
				+"    implements Stack\n"
				+"    var top : Int?\n"
				+"\n"
				+"    def push(element : Int)\n"
				+"        print(\"test\")\n"
				+"\n"
				+"    def pop() : Int?\n"
				+"        val result : Int = 42\n"
				+"        result\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L type StackFactory {$I {$L def Stack () : Stack $L} {$L def StackWithFirst (firstElement : Int) : Stack $L} $I} $L} {$L class StackImpl {$I {$L implements Stack $L} {$L var top : Int ? $L} {$L def push (element : Int) {$I {$L print (\"test\") $L} $I} $L} {$L def pop () : Int ? {$I {$L val result : Int = 42 $L} {$L result $L} $I} $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration()]]", typedAST.toString());		

		// FIXME: Type checking Declarations is different!!!
		if (typedAST instanceof Declaration) {
			((Declaration) typedAST).typecheckAll(env);
		} else {
			Type resultType = typedAST.typecheck(env);
			Assert.assertEquals(Unit.getInstance(), resultType);
		}
	}
	
	@Test
	public void testImplementsFail() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"type StackFactory\n"
				+"    def Stack() : Stack\n"
				+"    def StackWithFirst(firstElement : Int) : Stack\n"
				+"\n"
				+"class StackImpl\n"
				+"    implements Stack\n"
				+"    var top : Int?\n"
				+"\n"
				+"    def pop() : Int\n"
				+"        val result:Int = 42\n"
				+"        result\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L type StackFactory {$I {$L def Stack () : Stack $L} {$L def StackWithFirst (firstElement : Int) : Stack $L} $I} $L} {$L class StackImpl {$I {$L implements Stack $L} {$L var top : Int ? $L} {$L def pop () : Int {$I {$L val result : Int = 42 $L} {$L result $L} $I} $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration()]]", typedAST.toString());		

		try {
			typedAST.typecheck(env);
		} catch (ToolError e) {
			Assert.assertEquals("wyvern.tools.errors.ToolError: StackImpl is not a subtype of Stack on line number Test:11,15", e.toString());
			return;
		}
		
		Assert.fail("Expected Wyvern compiler to detect error!");
	}
	
	@Test
	public void testImplementsFailByName() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"type StackFactory\n"
				+"    def Stack() : Stack\n"
				+"    def StackWithFirst(firstElement : Int) : Stack\n"
				+"\n"
				+"class StackImpl\n"
				+"    implements StackBar\n"
				+"    var top : Int?\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L type StackFactory {$I {$L def Stack () : Stack $L} {$L def StackWithFirst (firstElement : Int) : Stack $L} $I} $L} {$L class StackImpl {$I {$L implements StackBar $L} {$L var top : Int ? $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration()]]", typedAST.toString());		

		try {
			typedAST.typecheck(env);
		} catch (ToolError e) {
			Assert.assertEquals("wyvern.tools.errors.ToolError: Type StackBar has no declaration in the context on line number Test:11,15", e.toString());
			return;
		}
		Assert.fail("Expected Wyvern compiler to detect error!");
	}
	
	@Test
	public void testClassImplements() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"type StackFactory\n"
				+"    def Stack() : Stack\n"
				+"    def StackWithFirst(firstElement : Int) : Stack\n"
				+"\n"
				+"class Link\n"
				+"    val data : Int\n"
				+"    val next : Link?\n"
				+"\n"
				+"    class def Link(d:Int, n:Link?) : Link\n"
				+"        new\n"
				+"            data=d\n"
				+"            next=n\n"
				+"\n"
				+"class StackImpl\n"
				+"    implements Stack\n"
				+"    class implements StackFactory\n"
				+"    var top : Int?\n"
				+"    var list : Link?\n"
				+"\n"
				+"    class def Stack() : Stack = new\n"
				+"\n"
				+"    class def StackWithFirst(firstElement : Int) : Stack\n"
				+"        new\n"
				+"            list = Link.Link(firstElement, null)\n" +
				"    def push(el : Int) : Unit =\n" +
				"        this.list = Link.Link(el, (this.list))\n" +
				"    def pop() : Int =\n" +
				"        val old : Link = this.list\n" +
				"        this.list = this.list.next\n" +
				"        old.data"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L type StackFactory {$I {$L def Stack () : Stack $L} {$L def StackWithFirst (firstElement : Int) : Stack $L} $I} $L} {$L class Link {$I {$L val data : Int $L} {$L val next : Link ? $L} {$L class def Link (d : Int , n : Link ?) : Link {$I {$L new {$I {$L data = d $L} {$L next = n $L} $I} $L} $I} $L} $I} $L} {$L class StackImpl {$I {$L implements Stack $L} {$L class implements StackFactory $L} {$L var top : Int ? $L} {$L var list : Link ? $L} {$L class def Stack () : Stack = new $L} {$L class def StackWithFirst (firstElement : Int) : Stack {$I {$L new {$I {$L list = Link . Link (firstElement , null) $L} $I} $L} $I} $L} {$L def push (el : Int) : Unit = {$I {$L this . list = Link . Link (el , (this . list)) $L} $I} $L} {$L def pop () : Int = {$I {$L val old : Link = this . list $L} {$L this . list = this . list . next $L} {$L old . data $L} $I} $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration(), MutableClassDeclaration()]]", typedAST.toString());		

		Type resultType = typedAST.typecheck(env);
		Assert.assertEquals(Unit.getInstance(), resultType);
	}
	
	@Test
	public void testClassImplementsFail() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"type StackFactory\n"
				+"    def Stack() : Stack\n"
				+"    def StackWithFirst(firstElement : Int) : Stack\n"
				+"\n"
				+"class StackImpl\n"
				+"    class implements StackFactory\n"
				+"    var top : Int?\n"
				+"\n"
				+"    class def Stack() : StackImpl = new\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L type StackFactory {$I {$L def Stack () : Stack $L} {$L def StackWithFirst (firstElement : Int) : Stack $L} $I} $L} {$L class StackImpl {$I {$L class implements StackFactory $L} {$L var top : Int ? $L} {$L class def Stack () : StackImpl = new $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration()]]", typedAST.toString());		

		try {
			typedAST.typecheck(env);
		} catch (ToolError e) {
			Assert.assertEquals("wyvern.tools.errors.ToolError: StackImpl is not a subtype of StackFactory on line number Test:11,15", e.toString());
			return;
		}
		Assert.fail("Expected Wyvern compiler to detect error!");
	}
	
	@Test
	public void testClassImplementsFailByName() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"type StackFactory\n"
				+"    def Stack() : Stack\n"
				+"    def StackWithFirst(firstElement : Int) : Stack\n"
				+"\n"
				+"class StackImpl\n"
				+"    class implements StackFactoryFoo\n"
				+"    var top : Int?\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L type StackFactory {$I {$L def Stack () : Stack $L} {$L def StackWithFirst (firstElement : Int) : Stack $L} $I} $L} {$L class StackImpl {$I {$L class implements StackFactoryFoo $L} {$L var top : Int ? $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration()]]", typedAST.toString());		

		
		try {
			typedAST.typecheck(env);
		} catch (ToolError e) {
			Assert.assertEquals("wyvern.tools.errors.ToolError: Type StackFactoryFoo has no declaration in the context on line number Test:11,15", e.toString());
			return;
		}
	}
	
	@Test
	public void testFunctionApplicationTypeCheck() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Int)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"class StackImpl\n"
				+"    implements Stack\n"
				+"\n"
				+"    class def Stack() : Stack = new\n"
				+"\n"
				+"    var top : Int?\n"
				+"\n"
				+"    def push(element : Int)\n"
				+"        this.top = element\n"
				+"\n"
				+"    def pop() : Int?\n"
				+"        this.top\n"
				+"\n"
				+"def doIt()\n"
				+"    val s : Stack = StackImpl.Stack()\n"
				+"    s.push(42)\n"
				+"    s.push(\"wrong type\")\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Int) $L} {$L def pop () : Int ? $L} $I} $L} {$L class StackImpl {$I {$L implements Stack $L} {$L class def Stack () : Stack = new $L} {$L var top : Int ? $L} {$L def push (element : Int) {$I {$L this . top = element $L} $I} $L} {$L def pop () : Int ? {$I {$L this . top $L} $I} $L} $I} $L} {$L def doIt () {$I {$L val s : Stack = StackImpl . Stack () $L} {$L s . push (42) $L} {$L s . push (\"wrong type\") $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		try {
			typedAST.typecheck(env);
		} catch (ToolError e) {
			Assert.assertEquals("wyvern.tools.errors.ToolError: Actual argument type Str does not match formal argument type Int on line number Test:23,11", e.toString());
			return;
		}
		Assert.fail("Expected Wyvern compiler to detect error!");
	}
	
	@Test
	public void testMultilineMethods() {
		Reader reader = new StringReader("\n"
				+"type Stack\n"
				+"    prop top : Int?\n"
				+"    def push(element : Str)\n"
				+"    def pop() : Int?\n"
				+"\n"
				+"class StackImpl\n"
				+"    implements Stack\n"
				+"\n"
				+"    class def Stack() : Stack = new\n"
				+"\n"
				+"    var top : Int?\n"
				+"\n"
				+"    def push(element : Str)\n"
				+"        this.top = element\n"
				+"\n"
				+"    def pop() : Int?\n"
				+"        this.top\n"
				+"\n"
				+"def doIt()\n"
				+"    val s:Stack = StackImpl.Stack()\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				+"    s.push(\"42\")\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		Assert.assertEquals("{$I {$L type Stack {$I {$L prop top : Int ? $L} {$L def push (element : Str) $L} {$L def pop () : Int ? $L} $I} $L} {$L class StackImpl {$I {$L implements Stack $L} {$L class def Stack () : Stack = new $L} {$L var top : Int ? $L} {$L def push (element : Str) {$I {$L this . top = element $L} $I} $L} {$L def pop () : Int ? {$I {$L this . top $L} $I} $L} $I} $L} {$L def doIt () {$I {$L val s : Stack = StackImpl . Stack () $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} {$L s . push (\"42\") $L} $I} $L} $I}",
				parsedResult.toString());
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		typedAST.typecheck(env);
	}
	
	@Test
	public void testNameConflict1() {
		Reader reader = new StringReader("\n"
				+"type A\n"
				+"    prop a : Int\n"
				+"    def b : Int\n"
				+"\n"
				+"type B\n"
				+"    def a : Int\n"
				+"    prop b : Int\n"
				+"\n"
				+"class AImpl\n"
				+"    implements A\n"
				+"\n"
				+"    class def make() : AImpl\n"
				+"        new\n"
				+"\n"
				+"    var a : Int\n"
				+"\n"
				+"    def aa() : Int\n"
				+"        this.a\n"
				+"\n"
				+"    def b : Int\n"
				+"        this.a\n"
				+"\n"
				+"    def setB (bbb : Int)\n"
				+"        this.a = bbb\n"
				+"\n"
				+"def doIt() : Unit\n"
				+"    val a1:A = AImpl.make()\n"
				+"    val a2:B = AImpl.make()\n"
				+"    val checkMe1:Int = a1.a\n"
				+"    val checkMe2:Int = a2.a\n"
				+"    val checkMe3:Int = a2.a\n"
				
				// What Would You Do?
//				+"    val a3:B = a1\n"
//				+"    val checkMe4:Int = a1.a\n"
//				+"    val checkMe5:Unit -> Int = a3.a\n"
//				+"    val checkMe6:Unit -> Int = a1.b\n"
//				+"    val checkMe7:Int = a3.b\n"
                
				+"    null\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		typedAST.typecheck(env);

		DeclSequence ds = (DeclSequence) ((Sequence) typedAST).iterator().next();
		
		Iterator<Declaration> i = ds.getDeclIterator().iterator();
		TypeDeclaration tA = (TypeDeclaration) i.next();
		TypeDeclaration tB = (TypeDeclaration) i.next();
		
		TypeType tAt = (TypeType) tA.getType();
		TypeType tBt = (TypeType) tB.getType();

		HashSet<SubtypeRelation> subtypes = new HashSet<SubtypeRelation>();

		Assert.assertFalse(tAt.subtype(tBt, subtypes));
		Assert.assertFalse(tBt.subtype(tAt, subtypes));
	}
	
	@Test
	public void testNameConflict2() {
		Reader reader = new StringReader("\n"
				+"type A\n"
				+"    def a() : Int\n"
				+"    def b() : Int\n"
				+"\n"
				+"type B\n"
				+"    def a() : Int\n"
				+"    def b() : Int\n"
				+"\n"
				+"class AImpl\n"
// FIXME:				+"    implements A\n"
				+"\n"
// FIXME:				+"    class def make() : A\n"
// FIXME:				+"        new\n"
				+"\n"
				+"    var a : Int\n"
				+"\n"
				+"    def b() : Int\n"
				+"        this.a\n"
				+"\n"
				+"def doIt() : Unit\n"
// FIXME:				+"    val a1:A = AImpl.make()\n"
// FIXME:				+"    val a2:B = AImpl.make()\n"
// FIXME:				+"    val checkMe1:Unit -> Int = a1.a\n"
// FIXME:				+"    val checkMe2:Unit -> Int = a2.a\n"
// FIXME:				+"    val checkMe3:Int = a2.a()\n"
				
				// What Would You Do?
//				+"    val a3:B = a1\n"
//				+"    val checkMe4:Unit -> Int = a1.a\n"
//				+"    val checkMe5:Unit -> Int = a3.a\n"
//				+"    val checkMe6:Unit -> Int = a1.b\n"
//				+"    val checkMe7:Unit -> Int = a3.b\n"
                
				+"    null\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		typedAST.typecheck(env);

		DeclSequence ds = (DeclSequence) ((Sequence) typedAST).iterator().next();
		
		Iterator<Declaration> i = ds.getDeclIterator().iterator();
		TypeDeclaration tA = (TypeDeclaration) i.next();
		TypeDeclaration tB = (TypeDeclaration) i.next();
		
		TypeType tAt = (TypeType) tA.getType();
		TypeType tBt = (TypeType) tB.getType();

		HashSet<SubtypeRelation> subtypes = new HashSet<SubtypeRelation>();

		Assert.assertTrue(tAt.subtype(tBt, subtypes));
		Assert.assertTrue(tBt.subtype(tAt, subtypes));
	}

	@Test
	public void testNameConflict3() {
		Reader reader = new StringReader("\n"
				+"type A\n"
				+"    def a() : Int\n"
				+"    prop b : Unit -> Int\n"
				+"\n"
				+"type B\n"
				+"    prop a : Unit -> Int\n"
				+"    def b() : Int\n"
				+"\n"
				+"class AImpl\n"
				+"    implements A\n"
				+"\n"
				+"    class def make() : A\n"
				+"        new\n"
				+"\n"
				+"    def a() : Int\n"
				+"\n"
				+"    var b : Unit -> Int\n"
				+"\n"
				+"def doIt() : Unit\n"
				+"    val a1:A = AImpl.make()\n"
// FIXME:				+"    val a2:B = AImpl.make()\n"
				+"    val checkMe1:Unit -> Int = a1.a\n"
//				+"    val checkMe2:Unit -> Int = a2.a\n"
//				+"    val checkMe3:Int = a2.a()\n"
				
				// What Would You Do?
// FIXME:				+"    val a3:B = a1\n"
				+"    val checkMe4:Unit -> Int = a1.a\n"
//				+"    val checkMe5:Unit -> Int = a3.a\n"
// FIXME:				+"    val checkMe6:Unit -> Int = a1.b\n"
//				+"    val checkMe7:Unit -> Int = a3.b\n"
                
				+"    null\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		typedAST.typecheck(env);

		DeclSequence ds = (DeclSequence) ((Sequence) typedAST).iterator().next();
		
		Iterator<Declaration> i = ds.getDeclIterator().iterator();
		TypeDeclaration tA = (TypeDeclaration) i.next();
		TypeDeclaration tB = (TypeDeclaration) i.next();
		
		TypeType tAt = (TypeType) tA.getType();
		TypeType tBt = (TypeType) tB.getType();

		HashSet<SubtypeRelation> subtypes = new HashSet<SubtypeRelation>();

//		Assert.assertTrue(tAt.subtype(tBt, subtypes)); // FIXME:
//		Assert.assertTrue(tBt.subtype(tAt, subtypes)); // FIXME:
	}

	@Test
	public void testNestedTypes1() {
		Reader reader = new StringReader("\n"
				+"type A\n"
				+"    def a : Int\n"
				+"    def b : Int\n"
				+"    type AInner\n"
				+"        def a : Int\n"
				+"\n"
				+"type B\n"
				+"    def a : Int\n"
				+"    def b : Int\n"
				+"    type AInner\n"
				+"        prop a : Int\n"
				+"\n"
				+"class AImpl\n"
				+"    def a : Int\n"				
				+"    def b : Int\n"
				+"    type ABC\n"
				+"        def t : Int\n"
				+"    class DEF\n"
				+"        def t : Int\n"
				+"\n"
				+"class BImpl\n"
//				+"    implements B\n"
				+"    def a : Int\n"				
				+"    def b : Int\n"
				+"    type ABC\n"
				+"        def t : Int\n"
				+"    class DEF\n"
				+"        prop t : Int\n"
				+"\n"
				+"def doIt() : Unit\n"
				+"    null\n"
				);
		RawAST parsedResult = Phase1Parser.parse("Test", reader);
		
		Environment env = Globals.getStandardEnv();

		TypedAST typedAST = parsedResult.accept(new BodyParser(), env);
		Assert.assertEquals("[[MutableTypeDeclaration(), MutableTypeDeclaration(), MutableClassDeclaration(), MutableClassDeclaration(), DefDeclaration()]]", typedAST.toString());		

		typedAST.typecheck(env);

		DeclSequence ds = (DeclSequence) ((Sequence) typedAST).iterator().next();
		
		Iterator<Declaration> i = ds.getDeclIterator().iterator();
		TypeDeclaration tA = (TypeDeclaration) i.next();
		TypeDeclaration tB = (TypeDeclaration) i.next();
		ClassDeclaration cAImpl = (ClassDeclaration) i.next();
		ClassDeclaration cBImpl = (ClassDeclaration) i.next();
		
		TypeType tAt = (TypeType) tA.getType();
		TypeType tBt = (TypeType) tB.getType();
		ClassType cAImplt = (ClassType) cAImpl.getType();
		ClassType cBImplt = (ClassType) cBImpl.getType();

		HashSet<SubtypeRelation> subtypes = new HashSet<SubtypeRelation>();

		//Should this work?
		Assert.assertTrue(tBt.subtype(tAt, subtypes));
		Assert.assertFalse(cAImplt.subtype(cBImplt, subtypes));
		Assert.assertTrue(cBImplt.subtype(cAImplt, subtypes));
	}
}