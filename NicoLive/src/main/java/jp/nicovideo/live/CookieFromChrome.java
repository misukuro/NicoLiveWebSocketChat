package jp.nicovideo.live;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.IOException;
import java.sql.SQLException;


/**
 * Google Chrome の Cookie ファイルを扱うクラスです。
 *
 * @see jp.nicovideo.live.Cookie
 */
public class CookieFromChrome implements Cookie {

    private static final String COOKIE_FILE_HEADER = "SQLite";
    private static final String COOKIE_FILE = "Cookies";

    private Connection connection;
    private Statement statement;

    /**
     * CookieFromChrome を作成します。
     */
    public CookieFromChrome() throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch(ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * 与えられた Cookie ファイルのパスを使って CookieFromChrome を作成します。
     *
     * @param cookieFile ファイル
     * @throws java.io.IOException Cookie ファイルが開けない場合
     */
    public CookieFromChrome(File cookieFile) throws IOException {
        this();
        setFile(cookieFile);
    }

    /**
     * 与えられた Cookie ファイルを使うように設定します。
     *
     * @param cookieFile ファイル
     * @throws java.io.IOException Cookie ファイルが開けない場合
     */
    public void setFile(File cookieFile) throws IOException {

        try {
            if (connection != null ) {
                connection.close();
                connection = null;
            }

            if (!cookieFile.isFile()) {
                throw new IOException("cookie file not exists");
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + cookieFile.toString());
            statement = connection.createStatement();
            statement.setQueryTimeout(10);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * 指定したドメイン、名前で Cookie ファイルから値を検索します。
     *
     * @param domain Cookie の値のドメイン
     * @param name 検索する Cookie の名前
     * @return 検索した Cookie の値
     * @throws IOException 値が見つからなかった、または何らかのエラーの場合
     */
    public String getValue(String domain, String name) throws IOException {
        final String QUERY = "SELECT value FROM cookies WHERE host_key = '" + domain + "' AND name = '" + name + "'";
        ResultSet rs = null;
        String result = null;

        try {
            rs = statement.executeQuery(QUERY);

            if (rs.next()) {
                result = rs.getString("value");
            }
        } catch(SQLException e) {
            if (!e.getMessage().contains("no such table")) {
                throw new IOException(e);
            }
        }

        return result;
    }

    /**
     * デフォルトの Cookie ファイルのパスを取得します。
     *
     * @return ファイルパス。サポートされてない OS の場合は null を返す
     */
    public File defaultCookieFilePath() {
        String osName = System.getProperty("os.name");
        File chromeDir = null;

        if (osName.equals("Linux")) {
            chromeDir = getProfileDir();
        } else if (osName.startsWith("Windows")) {
            chromeDir = new File(System.getenv("APPDATA") + "/Google/Chrome/User Data/Default");
        }

        return chromeDir == null ? null : new File(chromeDir, COOKIE_FILE);
    }

    /**
     * Google Chrome の Cookie ファイルかどうか判定します。
     *
     * @param header Cookie ファイルのヘッダーのバイト配列
     * @return Google Chrome の Cookie ファイルであれば true、それ以外なら false
     */
    public boolean isCookieFile(byte[] header) {
        if (COOKIE_FILE_HEADER.startsWith(new String(header))) {
            return true;
        } else {
            return false;
        }
    }

    public void close() throws IOException {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }


    /* Private section. */
    private File getProfileDir() {
        String basedir = System.getProperty("user.home") + File.separator + ".config";
        String profDir = "Default";

        File path = new File(basedir, "chromium" + File.separator + profDir);

        if (!path.isDirectory()) {
            path = new File(basedir, "google-chrome" + File.separator + profDir);
        }

        return path;
    }
}
