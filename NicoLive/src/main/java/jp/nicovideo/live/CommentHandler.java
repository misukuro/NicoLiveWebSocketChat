package jp.nicovideo.live;


import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;


/**
 * コメント受信の通知を受け取ります。
 */
public interface CommentHandler {

    /** 属性マップの会員タイプのキー MEMBER_TYPE で得られる会員タイプ値の定数インターフェース。
     * なんかダサいから、もっといい実装方法(or 代替案)募集。
     */
    public interface Member {

        /** プし彡マム乙 */
        public static final String PREMIUM = "1";

        /** 運営システムのコマンド */
        public static final String SYSTEM = "2"; //アラート

        /** 放送主、または運営システム */
        public static final String OWNER = "3";

        /** バックステージパス及び公式の運営コメント */
        public static final String OFFICIAL = "6";

        /** バックステージパス及び運営コメント? */
        public static final String BASKSTAGEPASS = "7";
        
        /**  iPhoneプレーヤーからのコメント? */
        public static final String IPHONE = "8";
        
        /** iPhoneプレーヤーからのコメント? */
        public static final String IPHONE2 = "9";

        /** 一般会員 */
        public static final String NORMAL = "0"; //手動で０に設定。元は空欄

        /* 性別の判定はできないため削除
        /** 男性の一般会員 
        public static final String NORMAL_MALE = "8";

        /** 男性のプレミアム会員 
        public static final String PREMIUM_MALE = "9";

        /** 女性の一般会員 
        public static final String NORMAL_FEMALE = "24";

        /** 女性のプレミアム会員 
        public static final String PREMIUM_FEMALE = "25";
        */
    }

    /** 属性マップの匿名コメント値のキー */
    public static final String ANONYMITY = "anonymity";

    /** 属性マップの日付(UNIXタイム)のキー */
    public static final String DATE      = "date";

    /** 属性マップのコメントコマンドのキー */
    public static final String COMMAND      = "mail";

    /** 属性マップのコメント番号のキー */
    public static final String NUMBER    = "no";

    /**
     * 属性マップの会員タイプのキー。
     * このキーで取得できる値が Member.PREMIUM ならプレミアム会員、
     * Member.SYSTEM なら運営システムのコマンド(e.g. /vote)、
     * Member.OWNER なら放送主、または運営のシステム(e.g. /disconnect)、
     * Member.OFFICIAL なら公式の運営コメント、
     * 取得できない(null になる)場合は一般会員
     *
     * @see CommentHandler.Member
     */
    public static final String MEMBER_TYPE   = "premium";

    /** 属性マップのユーザーID のキー */
    public static final String USER_ID   = "user_id";

    /** 属性マップのビデオ位置のキー */
    public static final String VPOS      = "vpos";

    /** 属性マップのセルフコメントのキー */
    public static final String YOURPOST = "yourpost";
    
    /** 属性マップのスレッドのキー */
    public static final String THREAD = "thread";
    
    /**
     * 属性マップの名前のキー。
     * このキーで取得できるのは公式の運営コメントの方のハンドルです。
     * つまり、公式の運営コメントしかこの属性マップはセットされません。
     * (e.g. Fooさん、はらさん)
     */
    public static final String NAME = "name";

    /**
     * コメント本体を受信したときに呼び出されます。
     * このメソッドはコメントを受信する度に呼ばれます。
     *
     * @param comment 受信したコメント文字列
     * @param roomType 
     * @throws IOException 
     */
    void commentReceived(String comment, RoomType roomType) throws IOException;

    /**
     * コメントの属性を受信したときに呼び出されます。
     * このメソッドはコメントの属性を受信する度に呼ばれます。
     * このインターフェースに属性の名前の定数が定義されていますが、
     * その属性がない場合もあります。これは会員タイプやユーザー ID、コメントコマンドも例外ではありません。
     * <pre>if (map.get(CommentHandler.ANONYMITY) != null)</pre>
     * などとして null でないことをチェックください。
     *
     * @param map 属性の名前をキーにした属性の値のマップ
     * @throws SQLException 
     * @throws ClassNotFoundException 
     * @see java.util.Map
     */
    void commentAttrReceived(Map<String, String> map) throws ClassNotFoundException, SQLException;
}
