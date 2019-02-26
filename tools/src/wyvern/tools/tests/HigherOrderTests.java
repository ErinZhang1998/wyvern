package wyvern.tools.tests;

import org.hamcrest.core.StringContains;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import wyvern.target.corewyvernIL.expression.StringLiteral;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.tools.errors.ToolError;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.RegressionTests;

/**
 * Test suite for polymorphic effects with higher order functions
 */
@Category(RegressionTests.class)
public class HigherOrderTests {
    private static final String PATH = TestUtil.BASE_PATH;

    @BeforeClass
    public static void setupResolver() {
        TestUtil.setPaths();
        WyvernResolver.getInstance().addPath(PATH);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void upperBound() throws ParseException {
        expectedException.expect(ToolError.class);
        expectedException.expectMessage(StringContains.containsString(
                "outside of the upper bound"
        ));
        TestUtil.doTestScriptModularly(PATH, "higherOrderEffects.ubClient", Util.stringType(), new StringLiteral("abc"));
    }

    @Test
    public void accepted1() throws ParseException {
        TestUtil.doTestScriptModularly(PATH, "higherOrderEffects.acceptedClient1", Util.stringType(), new StringLiteral("abc"));
    }

    @Test
    public void rejected1() throws ParseException {
        expectedException.expect(ToolError.class);
        expectedException.expectMessage(StringContains.containsString(
                "outside of the upper bound"
        ));
        TestUtil.doTestScriptModularly(PATH, "higherOrderEffects.rejectedClient1", Util.stringType(), new StringLiteral("abc"));
    }

}
