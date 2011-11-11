package jp.nicovideo.live;

import org.junit.Test;
import org.junit.runner.JUnitCore;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NgWordTest {
    private static final String NGWORD = "test";
    static public final int REGTIME = 100000;

    @Test
    public void testGetNgWord() {
        NgWord ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, true, true);

        assertThat(ngword.getNgWord(), is(NGWORD));
    }

    @Test
    public void testGetNgType() {
        NgWord ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, true, true);
        assertThat(ngword.getType(), is(NgWord.Type.WORD));

        ngword = new NgWord(NgWord.Type.ID, NGWORD, REGTIME, true, true, true);
        assertThat(ngword.getType(), is(NgWord.Type.ID));
    }

    @Test
    public void testGetRegisterTime() {
        NgWord ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, true, true);

        assertThat(ngword.getRegisterTime(), is(REGTIME));
    }

    @Test
    public void testIsUseCaseUnify() {
        NgWord ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, true, true);
        assertThat(ngword.isUseCaseUnify(), is(true));

        ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, false, true, true);
        assertThat(ngword.isUseCaseUnify(), is(false));
    }

    @Test
    public void testIsRegex() {
        NgWord ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, true, true);
        assertThat(ngword.isRegex(), is(true));

        ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, false, true);
        assertThat(ngword.isRegex(), is(false));
    }

    @Test
    public void testIsReadonly() {
        NgWord ngword = new NgWord(NgWord.Type.WORD, NGWORD, REGTIME, true, true, true);

        assertThat(ngword.isReadonly(), is(true));
    }

    public static void main(String[] args) {
        JUnitCore.main(NgWordTest.class.getName());
    }
} // End of NgWordTest test class.

