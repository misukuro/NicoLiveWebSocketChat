package jp.nicovideo.live;

/**
 * NG ワードを表すクラスです。
 *
 * @see jp.nicovideo.live.NicoLive#getNgWordList
 */
public class NgWord {
    /**
     * NG ワードのタイプを表す列挙型です。
     */
    public enum Type {
        /** 運営、または放送主による NG ワード */
        WORD,

        /** 放送主による ID の NG */
        ID,

        /** 放送主によるコメントコマンドの NG */
        COMMAND
    }

    /* This NG word type. */
    private Type type;

    /* NG word. */
    private String ngWord;

    /* The register time. */
    private int registerTime;

    /* The use case unify word flag. */
    private boolean useCaseUnify;

    /* The regex word flag. */
    private boolean regex;

    /* The read only flag. */
    private boolean readonly;

    /**
     * 指定した タイプ、NG ワード、登録時間、アクセス指定で NgWord オブジェクトを作成します。
     * 登録時間は 1970 年 1 月 1 日 00:00:00 GMT からの秒数で指定します。
     * アクセス指定は読み込み専用なら true、それ以外は false を指定します。
     *
     * @param type NG ワードタイプ
     * @param ngWord NG ワード
     * @param registerTime 登録時間
     * @param readonly アクセス指定
     * @see jp.nicovideo.live.NgWord.Type
     */
    public NgWord(Type type, String ngWord, int registerTime, boolean useCaseUnify, boolean regex, boolean readonly) {
        this.type = type;
        this.ngWord = ngWord;
        this.registerTime = registerTime;
        this.useCaseUnify = useCaseUnify;
        this.regex = regex;
        this.readonly = readonly;
    }

    /**
     * この NG ワードのタイプを返します。
     *
     * @return NG ワードタイプ
     * @see jp.nicovideo.live.NgWord.Type
     */
    public Type getType() {
        return type;
    }

    /**
     * この NG ワードを返します。
     *
     * @return NG ワード
     */
    public String getNgWord() {
        return ngWord;
    }

    /**
     * この NG ワードが登録された時間を、1970 年 1 月 1 日 00:00:00 GMT からの秒数で返します。
     *
     * @return 登録された時間
     */
    public int getRegisterTime() {
        return registerTime;
    }

    /**
     * この NG ワードが大文字や小文字、ひらがなやカタカナを区別するかどうかを判定します。
     *
     * @return 区別するなら true、そうでなければ false
     */
    public boolean isUseCaseUnify() {
        return useCaseUnify;
    }

    /**
     * この NG ワードが正規表現かどうかを判定します。
     *
     * @return 正規表現なら true、そうでなければ false
     */
    public boolean isRegex() {
        return regex;
    }

    /**
     * この NG ワードが変更不可かどうかを判定します。
     *
     * @return 変更不可なら true を、それ以外では false
     */
    public boolean isReadonly() {
        return readonly;
    }
} // End of NgWord class.

