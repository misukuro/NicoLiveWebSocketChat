package jp.nicovideo.live;


import java.util.EnumMap;


/**
 * コメントの属性コマンドのコレクションセットです。
 *
 * @see jp.nicovideo.live.Command
 */
public class CommandSet {
    private enum Key { CMD, Size, Align, Color, Mobile, Se };
    private EnumMap<Key, Cmd> map;

    /**
     * デフォルトのコマンドセットを作成します。
     * デフォルトでは、184 コメントのみセットされています。
     */
    public CommandSet() {
        map = new EnumMap<Key, Cmd>(Key.class);

        map.put(Key.CMD, Command.ANON); // default is anonymous comment.
    }

    /**
     * コメントに対するコマンドを設定します。
     *
     * @param cmd コメントに対するコマンド
     * @see jp.nicovideo.live.Command
     */
    public void set(Command cmd) {
        map.put(Key.CMD, cmd);
    }

    /**
     * コメントに対するサイズ変更のコマンドを設定します。
     *
     * @param size サイズ変更のコマンド
     * @see jp.nicovideo.live.Command
     */
    public void set(Command.Size size) {
        map.put(Key.Size, size);
    }

    /**
     * コメントに対する表示位置のコマンドを設定します。
     *
     * @param align 表示位置のコマンド
     * @see jp.nicovideo.live.Command
     */
    public void set(Command.Align align) {
        map.put(Key.Align, align);
    }

    /**
     * コメントに対する色のコマンドを設定します。
     *
     * @param color 色のコマンド
     * @see jp.nicovideo.live.Command
     */
    public void set(Command.Color color) {
        map.put(Key.Color, color);
    }

    /**
     * コメントに対するモバイルコマンドを設定します。
     *
     * @param mobile モバイルコマンド
     * @see jp.nicovideo.live.Command
     */
    public void set(Command.Mobile mobile) {
        map.put(Key.Mobile, mobile);
    }

    /**
     * コメントに対する SE コマンドを設定します。
     * これは放送主限定のコマンドです。
     *
     * @param se SE コマンド
     * @see jp.nicovideo.live.Command
     */
    public void set(Command.Se se) {
        map.put(Key.Se, se);
    }

    /**
     * 設定されているコマンドを削除します。
     * 指定したコマンドが設定されていない場合は単に無視されます。
     *
     * @param cmd 削除するコマンド
     */
    public void remove(Command cmd) {
        map.remove(Key.CMD);
    }

    /**
     * 設定されているサイズ変更のコマンドを削除します。
     * 指定したコマンドが設定されていない場合は単に無視されます。
     *
     * @param size 削除するサイズ変更コマンド
     */
    public void remove(Command.Size size) {
        map.remove(Key.Size);
    }

    /**
     * 設定されている表示位置のコマンドを削除します。
     * 指定したコマンドが設定されていない場合は単に無視されます。
     *
     * @param align 削除する表示位置のコマンド
     */
    public void remove(Command.Align align) {
        map.remove(Key.Align);
    }

    /**
     * 設定されている色のコマンドを削除します。
     * 指定したコマンドが設定されていない場合は単に無視されます。
     *
     * @param color 削除する色のコマンド
     */
    public void remove(Command.Color color) {
        map.remove(Key.Color);
    }

    /**
     * 設定されているモバイルコマンドを削除します。
     * 指定したコマンドが設定されていない場合は単に無視されます。
     *
     * @param mobile 削除するモバイルコマンド
     */
    public void remove(Command.Mobile mobile) {
        map.remove(Key.Mobile);
    }

    /**
     * 設定されている SE コマンドを削除します。
     * 指定したコマンドが設定されていない場合は単に無視されます。
     *
     * @param se 削除する SE コマンド
     */
    public void remove(Command.Se se) {
        map.remove(Key.Se);
    }

    public String toString() {
        StringBuilder s = new StringBuilder("");

        for (Cmd c : map.values()) {
            s.append(" ");
            s.append(c.toString());
        }

        return s.toString();
    }
}
