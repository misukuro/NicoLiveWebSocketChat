package jp.nicovideo.live;


import java.io.File;

import java.io.IOException;


interface Cookie {
    /**
     * ドメインと名前を指定して Cookie の値を取得します。
     *
     * @param domain Cookie の値のドメイン
     * @param name Cookie の値の名前
     * @return 取得した値。取得出来なかったときは空文字を返す。
     */
    String getValue(String domain, String name) throws IOException;

    /**
     * ブラウザデフォルトの Cookie ファイルのパスを返す。
     *
     * @return Cookie ファイルのパス
     */
    File defaultCookieFilePath();

    /**
     * 使用する Cookie ファイルを指定します。
     *
     * @param cookieFile 使用する Cookie ファイル
     */
    void setFile(File cookieFile) throws IOException;

    /**
     * 使用できる Cookie ファイルかどうか調べます。
     *
     * @param header Cookie ファイルのヘッダ
     * @return 使用できる Cookie であれば true、それ以外なら false
     */
    boolean isCookieFile(byte[] header);

    /**
     * Cookie ファイルを閉じるなどの解放処理をします。
     */
    void close() throws IOException;
}
