package jp.nicovideo.live;


import org.junit.Test;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class CommandSetTest {
    @Test
    public void testDefaultCommandSet() {
        CommandSet set = new CommandSet();

        assertThat(set.toString(), is(" 184"));
    }

    @Test
    public void testAddCommandSet() {
        CommandSet set = new CommandSet();

        set.set(Command.Color.RED);
        assertThat(set.toString(), is(" 184 red"));

        set.set(Command.Size.BIG);
        assertThat(set.toString(), is(" 184 big red"));

        set.set(Command.Align.UE);
        assertThat(set.toString(), is(" 184 big ue red"));
    }

    @Test
    public void testRemoveCommandSet() {
        CommandSet set = new CommandSet();

        set.remove(Command.ANON);
        assertThat(set.toString(), is(""));
    }
}
