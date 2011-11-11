package jp.nicovideo.live;


interface Cmd {
    String toString();
}


/**
 * コメントに対するコマンドのタイプセーフな列挙です。
 *
 * @see jp.nicovideo.live.CommandSet
 */
public enum Command implements Cmd {
    /* The anonymous. */
    ANON("184"),          // default.
    NOANON("");

    /**
     * フォントサイズ変更コマンドのタイプセーフな列挙です。
     */
    public enum Size implements Cmd {
        DEFAULT(""), // default.
        BIG("big"),
        SMALL("small");

        private String name;

        private Size(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 表示位置コマンドのタイプセーフな列挙です。
     */
    public enum Align implements Cmd {
        DEFAULT(""),      // default.
        HIDARI("hidari"), // owner only.
        MIGI("migi"),     // owner only.
        UE("ue"),
        SHITA("shita");

        private String name;

        private Align(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 色コマンドのタイプセーフな列挙です。
     */
    public enum Color implements Cmd {
        DEFAULT(""),  // default.
        RED("red"),
        BLUE("blue"),
        GREEN("green"),
        PINK("pink"),
        CYAN("cyan"),
        ORANGE("orange"),
        YELLOW("yellow"),
        PURPLE("purple"),
        /* The premium member only from here. */
        NICONICOWHITE("niconicowhite"), WHITE2("white2"),
        TRUERED("truered"), RED2("red2"),
        PASSIONORANGE("passionorange"), ORANGE2("orange2"),
        MADYELLOW("madyellow"), YELLOW2("yellow2"),
        ELEMENTALGREEN("elementalgreen"), GREEN2("green2"),
        MARINEBLUE("marineblue"), BLUE2("blue2"),
        NOBLEVIOLET("nobleviolet"), PURPLE2("purple2"),
        BLACK("black");

        private String name;

        private Color(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Mobile implements Cmd {
        DEFAULT(""), // default
        DOCOMO("docomo");

        private String name;

        private Mobile(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Se implements Cmd {
        DEFAULT(""), // default
        SE1("se1"),
        SE2("se2");

        private String name;

        private Se(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private String name;

    private Command(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
