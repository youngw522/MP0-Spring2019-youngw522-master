package edu.illinois.cs.cs125.spring2019.mp0;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LocationAppTest {
    private MainActivity mainActivity;

    @Before
    public void setupActivity() {
        mainActivity = Robolectric.setupActivity(MainActivity.class);
    }

    // Robolectric tests are slow, but most of the delay is in setupActivity above.
    @Test(timeout=1000)
    public void centerButtonTest() {
        Assert.assertFalse(mainActivity.centered);
        mainActivity.processNewLocation(10.0, 12.8);
        Assert.assertTrue("button handler not attached", mainActivity.findViewById(R.id.center).performClick());
        Assert.assertTrue(mainActivity.centered);
    }
}
