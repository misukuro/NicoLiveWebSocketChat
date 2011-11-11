package jp.nicovideo.live;


import java.io.File;
import java.io.FileInputStream;
import java.net.HttpCookie;
import java.util.Hashtable;
import java.io.IOException;


public class CookieFromFile {
    private Cookie icookie = null;
    private Hashtable<String, Cookie> supportedFormat = null;
    @SuppressWarnings("unused")
	private HttpCookie cookie = null;

    /**
     * 指定されたファイルからクッキーを取得する CookieFromFile を新しく作成します。
     *
     * @param cookieFile Cookie ファイルのパス
     * @throws java.io.IOException ファイルが存在しないか、サポートされてない形式の場合
     */
    public CookieFromFile(File cookieFile, String type) throws IOException {
        this();
        init(cookieFile, type);
    }

    public CookieFromFile() throws IOException {
        supportedFormat = new Hashtable<String, Cookie>();

        supportedFormat.put("firefox", new CookieFromFirefox());
        supportedFormat.put("chrome", new CookieFromChrome());
    }

    public void init(File cookieFile, String type) throws IOException {
        FileInputStream file = null;
        icookie = supportedFormat.get(type.toLowerCase());

        if (icookie == null) {
            throw new IOException("unsupported browser type");
        }

        byte[] head = new byte[5];
        try {
            file = new FileInputStream(cookieFile);
            file.read(head);
        } finally {
            if (file != null) {
                file.close();
            }
        }

        if (!icookie.isCookieFile(head)) {
            throw new IOException("unsupported file format");
        }

        icookie.setFile(cookieFile);
    }

    public HttpCookie getCookie(String domain, String name) throws IOException {
        String value = icookie.getValue(domain, name);

        return value == null ? null : new HttpCookie(name, value);
    }

    public String[] getSupported() {
        return supportedFormat.keySet().toArray(new String[0]);
    }

    public File defaultCookieFilePath(String type) throws IOException {
        Cookie c = supportedFormat.get(type);

        if (c == null) {
            throw new IOException("unsupported browser type");
        }

        return c.defaultCookieFilePath();
    }

    public void close() throws IOException {
        if (icookie != null) {
            icookie.close();
        }
    }
}
