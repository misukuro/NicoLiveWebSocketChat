package jp.nicovideo.live;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.IOException;
import java.sql.SQLException;


/**
 * Mozilla Firefox の Cookie ファイルを扱うクラスです。
 *
 * @see jp.nicovideo.live.Cookie
 */
public class CookieFromFirefox implements Cookie {

    private static final String COOKIE_FILE_HEADER = "SQLite";
    private static final String COOKIE_FILE = "cookies.sqlite";
    private static final String INI_FILE = "profiles.ini";

    private String firefoxDir = null;

    private Connection connection;
    private Statement statement;

    /**
     * CookieFromFirefox を作成します。
     */
    public CookieFromFirefox() throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch(ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * 与えられた Cookie ファイルのパスを使って CookieFromFirefox を作成します。
     *
     * @param cookieFile ファイル
     * @throws java.io.IOException Cookie ファイルが開けない場合
     */
    public CookieFromFirefox(File cookieFile) throws IOException {
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
            if (!cookieFile.isFile()) {
                throw new IOException("cookie file not exists");
            }

            if (connection != null ) {
                connection.close();
                connection = null;
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
        final String QUERY = "SELECT value FROM moz_cookies WHERE host = \"" + domain + "\" AND name = \"" + name + "\"";
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
        File path = null;

        if (osName.equals("Linux")) {
            firefoxDir = System.getProperty("user.home") + "/.mozilla/firefox";
        } else if (osName.startsWith("Windows")) {
            firefoxDir = System.getenv("APPDATA") + "/Mozilla/Firefox";
        } else if (osName.startsWith("Mac")) {
            firefoxDir = System.getProperty("user.home") + "/Library/Application Support/Firefox";
        }

        File iniFile = new File(firefoxDir, INI_FILE);

        path = new File(getProfileDir(iniFile), COOKIE_FILE);

        return firefoxDir == null ? null : path;
    }

    /**
     * Mozilla Firefox の Cookie ファイルかどうか判定します。
     *
     * @param header Cookie ファイルのヘッダーのバイト配列
     * @return Mozilla Firefox の Cookie ファイルであれば true、それ以外なら false
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
    private File getProfileDir(File iniFile) {
        File path = null;
        BufferedReader inbuf = null;

        try {
            inbuf = new BufferedReader(new FileReader(iniFile));
            String line;
            final String pathField = "Path=";

            while ((line = inbuf.readLine()) != null) {
                if (line.startsWith(pathField)) {
                    path = new File(firefoxDir, line.substring(pathField.length()));

                    break;
                }
            }
        } catch(IOException e) {
            System.err.println(e);
            try {
                inbuf.close();
            } catch(IOException e2) {
                System.exit(1);
            }
            System.exit(1);
        } finally {
            try {
                if (inbuf != null) {
                    inbuf.close();
                }
            } catch (IOException e) {
                System.exit(1);
            }
        }

        return path;
    }
}
