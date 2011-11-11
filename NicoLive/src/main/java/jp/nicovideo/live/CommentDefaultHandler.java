package jp.nicovideo.live;


import java.sql.SQLException;
import java.util.Map;

/**
 * コメントとその属性を受け取る抽象ハンドラークラスです。
 * このクラスのメソッドはすべて空です。
 * このクラスは、ハンドラークラスの作成を容易にするためのものです。
 * 
 * このクラスを拡張して CommentHandler を作成し、対象になるイベントのメソッドをオーバーライドします。
 * CommentHandler インターフェースを実装する場合は、その中のメソッドをすべて定義する必要があります。
 * この abstract クラスでは、実装が必要なすべてのメソッドについて空の定義をしてあるので、必要なイベント用のメソッドを定義するだけで済みます。
 *
 * @see jp.nicovideo.live.CommentHandler
 */
public abstract class CommentDefaultHandler implements CommentHandler {
    /**
     * コメント本体を受信したときに呼び出されます。
     * デフォルトでは何も行いません。
     * ユーザーはこのメソッドをオーバーライドして、
     * 受信したコメントに対して独自の処理（コメントの表示、ファイルへの出力など）を実行できます。
     *
     * @param comment 受信したコメント文字列
     * @see jp.nicovideo.live.CommentHandler#commentReceived
     */
    //@Override
    public void commentReceived(String comment) {
        // dummy
    }

    /**
     * コメントの属性値を受信したときに呼び出されます。
     *
     * デフォルトでは何も行いません。
     * ユーザーはこのメソッドをオーバーライドして、
     * 受信したコメントの属性値に対して独自の処理（属性値の表示、ファイルへの出力）を実行できます。
     *
     * @param map 属性の名前をキーにした属性の値のマップ
     * @throws SQLException 
     * @throws ClassNotFoundException 
     * @see jp.nicovideo.live.CommentHandler#commentAttrReceived
     * @see java.util.Map
     */
    //@Override
    public void commentAttrReceived(Map<String, String> map) throws ClassNotFoundException, SQLException {
        // please override.
    }
}
