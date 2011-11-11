package jp.nicovideo.live;


import java.io.File;
import java.net.HttpCookie;

import java.io.IOException;
import java.net.URISyntaxException;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.JUnitCore;


public class CookieFromFileTest {
    private static final String DBFILE = "test.sqlite";
    private CookieFromFile cookieFirefox = null;

    public CookieFromFileTest() {
        try {
            File dbFile = new File(getClass().getClassLoader().getResource(DBFILE).toURI());
            cookieFirefox = new CookieFromFile(dbFile, "firefox");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCookieFromFile() throws IOException {
        HttpCookie result = cookieFirefox.getCookie(Value.DOMAIN, Value.NAME);

        assumeNotNull(result);
        assertThat(result.getName(), is(Value.NAME));
        assertThat(result.getValue(), is(Value.VALUE));
    }

    public static void main(String[] args) {
        JUnitCore.main(CookieFromFileTest.class.getName());
    }
}
