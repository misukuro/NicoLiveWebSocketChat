
package jp.nicovideo.live;


import java.io.File;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;


public class CookieFromFirefoxTest {

    static File dbFile = null;
    CookieFromFirefox cff = null;

    public CookieFromFirefoxTest() {
        try {
            dbFile = new File(getClass().getClassLoader().getResource("test.sqlite").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void newCookieFromFirefox() throws IOException {
        cff = new CookieFromFirefox();

        try {
            cff.setFile(dbFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIsCookieFile() {
        assertThat(cff.isCookieFile("SQLite".getBytes()), is(true));
        assertThat(cff.isCookieFile("test".getBytes()), is(false));
        assertThat(cff.isCookieFile("SQLita".getBytes()), is(false));
    }

    @Test
    public void testGetValue() throws IOException {
        assertThat(cff.getValue(Value.DOMAIN, Value.NAME), is(Value.VALUE));
        assertThat(cff.getValue("test", "failed"), nullValue());
    }

    public static void main(String[] args) {
        JUnitCore.main(CookieFromFirefoxTest.class.getName());
    }
} // End of CookieFromFirefoxTest test class.
