package jp.nicovideo.live;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.nio.channels.FileChannel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

/* The import for SAX API. */
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import jp.nicovideo.live.NicoLive.GetNameTask;

/**
 * このクラスは、ニコニコ生放送のユーザー生放送を表現します。
 */
public class NicoLive {
	private static final String USER_AGENT = "ニコ生コメントビューワ(仮) katoken\\@morimati.info";
	
	
	private static final String BASE_URL = "http://live.nicovideo.jp";
	private static final String LOGIN_URL = "https://secure.nicovideo.jp/secure/login?site=niconico";
	private static final String WATCH_URL = BASE_URL + "/watch/%s";
	private static final String API_BASE = BASE_URL + "/api";
	private static final String STATUS_API = API_BASE + "/getplayerstatus?v=%s";
	private static final String PUBLISH_API = API_BASE
			+ "/getpublishstatus?v=%s";
	private static final String POSTKEY_API = API_BASE
			+ "/getpostkey?thread=%s&block_no=%d";
	private static final String OWNER_API = API_BASE + "/broadcast/%s";
	private static final String HEARTBEAT_API = API_BASE + "/heartbeat?v=%s";
	// private static final String NGWORD_API = API_BASE +
	// "/configurengword?video=%s&mode=get&video=%s";
	private static final String GET_COMMENT_XML = "<thread thread=\"%s\" res_from=\"-%s\" version=\"20061206\" />\0";
	private static final String USER_PAGE_URL_FORMAT = "http://www.nicovideo.jp/user/%s/friend";
	private static final String NAME_FILTER = "<h2><strong>([^<>]+)</strong>さん</h2>";

	private static final int UPDATE_INTERVAL = 45;
	private static final int RECEIVE_COMMENT = 10;
	private byte[] BOM = { (byte) 0xef, (byte) 0xbb, (byte) 0xbf };

	private int updateElapsedTime;

	/* 同じアカウントを使いまわすときセッションが無効になるのを回避するため静的に保持 */
	/* The user session of NicoNico. */
	private static HttpCookie cookie = null;

	/* The live title. */
	private String title;

	/* The community level. */
	private int level;

	/* The comment post ticket. */
	private String ticket;

	/* The NicoLive live number. */
	private String liveNumber;

	/* The Requested live */
	private String requiredLive;

	/* The total comment count, update per 45sec. */
	private int totalCommentCount;

	/* The total watch count, update per 45sec. */
	private int totalWatchCount;

	/* 現在のコメント数 */
	private int commentCount;

	/* The real watch count, update per 45sec. */
	/*
	 * ニコ生仕様変更の為、削除 private int realWatchCount;
	 * 
	 * /* The NG word list. private List<NgWord> ngWordList;
	 */

	/* The owner flag. */
	private boolean isOwner;

	/* The live start time. (seconds since 1970-01-01 00:00:00 UTC) */
	private int baseTime;

	// 枠の開場時刻
	private int openTime;

	// 放送の開始時刻
	private int startTime;

	// 枠の終了時間
	@SuppressWarnings("unused")
	private int endTime;

	/* The community or channel number. */
	private String communityNumber;

	/* The Provider Type */
	private String providerType;

	/* The room label. */
	private String roomLabel;

	/* The room seat number. */
	private String seetNo;

	/* Whether premium. */
	private boolean isPremium;

	/* NicoNico user id. */
	private String userId;

	/* The comment message server address. */
	private String msAddr;

	/* The comment message server port. */
	private int msPort;

	/* The comment message server thread. */
	private int msThread;

	/* The block number. */
	private int blockNo;

	/* The comment server socket. */
	// private final CopyOnWriteArraySet<Socket> sockets = new
	// CopyOnWriteArraySet<Socket>();

	// コメントサーバに接続するクラスのインスタンスリスト
	private ArrayList<CommentServer> rooms = new ArrayList<CommentServer>();

	// コメントサーバ
	private ArrayList<msThread> msThreads = new ArrayList<msThread>();

	/* Owner comment token */
	private String token;

	// エラータイプ
	private ErrorType errorType = ErrorType.Nothing;

	// 部屋の名前
	private RoomType roomType;

	// 放送形式(タイプ)
	private LiveType liveType = LiveType.Undefined;

	// 最初に接続したコメント部屋の情報
	private CommentServer thisRoom;

	// 公式生放送であるか
	private boolean isOfficial = false;

	/* The SAX parser. */
	private SAXParser saxParser;

	// サブスレッド
	private Thread getNameThread = new GetNameWork(); // 名前を取得するスレッド

	// タスクキュー
	private static Queue<GetNameTask> taskQueue = new LinkedBlockingQueue<GetNameTask>();

	// 重複したり無効なユーザーを検査し続けないようにするために使う
	private static HashSet<Integer> gotNameList = new HashSet<Integer>();
	
	//rogger
	final static Logger logger = Logger.getLogger("SampleLogging");

	/**
	 * 指定されたlv番号のユーザー生放送の NicoLive オブジェクトを作成します。
	 * 
	 * @param liveNumber
	 *            lv番号
	 * @throws java.io.IOException
	 *             生放送のステータスがなんらかの原因で取得できない場合、または放送が満員もしくはメンバーオンリーの場合
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public NicoLive(String liveNumber) throws IOException,
			ClassNotFoundException, SQLException {
		setCookie();
		init(liveNumber);
	}

	/**
	 * 指定されたメールアドレス、パスワードでニコニコ動画にログインした後、指定されたlv番号のユーザー生放送の NicoLive
	 * オブジェクトを作成します。
	 * 
	 * @param liveNumber
	 *            lv番号
	 * @param mailAddress
	 *            メールアドレス
	 * @param password
	 *            パスワード
	 * @throws java.io.IOException
	 *             生放送のステータスがなんらかの原因で取得できない場合、または放送が満員もしくはメンバーオンリーの場合
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public NicoLive(String liveNumber, String mailAddress, String password)
			throws IOException, ClassNotFoundException, SQLException {
	    /**
		    * ログ設定プロパティファイルのファイル名
		    */
		final String LOGGING_PROPERTIES = "javalog.properties";

		/**
		 * static initializer によるログ設定の初期化
		 */

		// クラスパスの中から ログ設定プロパティファイルを取得
		logger.fine("ログ設定: " + LOGGING_PROPERTIES
				+ " をもとにログを設定します。");
		final InputStream inStream = NicoLive.class
		.getClassLoader().getResourceAsStream(
				LOGGING_PROPERTIES);
		if (inStream == null) {
			logger.info("ログ設定: " + LOGGING_PROPERTIES
					+ " はクラスパス上に見つかりませんでした。");
		} else {
			try {
				LogManager.getLogManager().readConfiguration(
						inStream);
				logger.config(
						"ログ設定: LogManagerを設定しました。");
			} catch (IOException e) {
				logger.warning("ログ設定: LogManager設定の際に"
						+"例外が発生しました。:"+ e.toString());
			} finally {
				try {
					if (inStream != null) inStream.close();
				} catch (IOException e) {
					logger.warning("ログ設定: ログ設定プロパティ"
							+"ファイルのストリームクローズ時に例外が"
							+"発生しました。:"+ e.toString());
				}
			}
		}
		
		if (NicoLive.cookie == null) {
			setCookie(mailAddress, password);
			// 名前取得用スレッドを待機させる
			getNameThread.start();
		}
		init(liveNumber);
	}

	/**
	 * ニコ生コメント鯖の接続情報を表すクラス
	 */
	class CommentServer {
		// コメントサーバのアドレス
		public String address;

		// コメントサーバのポート
		public int port;

		// コメントサーバのスレッド
		public int thread;

		// コメントサーバとのソケット通信用
		private Socket sock;

		// コメ鯖へのメッセージストリーム
		public DataOutputStream msgOutStream;

		// コメ鯖からのメッセージストリーム
		public InputStream msgInStream;

		// この部屋の種類
		public RoomType roomType;

		// ルームラベル(席入り)
		public String roomLabel = "";

		// シートブロック
		public int seatBlock;

		// シートナンバー
		public int seatNo;

		// SAX
		SAXParser sax;

		// this thread is valid ?
		public boolean isValid = true;

		private boolean isCheck = false;

		// コンストラクター
		public CommentServer(String msAddr, int msPort, int msThread,
				RoomType roomType) {
			this.address = msAddr;
			this.port = msPort;
			this.thread = msThread;
			this.roomType = roomType;
		}

		public CommentServer() {}

		/*
		 * コメント部屋接続情報を元にソケット通信を開始する これを呼び出す前に、isValidReserchを必ず呼んで下さい
		 * 
		 * @param ms コメント部屋の接続情報
		 */
		public void StartSocketCommunication() throws UnknownHostException,
				IOException, ClassNotFoundException, SQLException {
			// System.out.println(address + "と" + port + "と" + thread);
			// if host is null or invalid thread, return
			if (address == null || !isValid) {
				return;
			}
			sock = new Socket(address, port);
			sock.setKeepAlive(true);
			msgOutStream = new DataOutputStream(sock.getOutputStream());
			msgOutStream.writeBytes(String.format(GET_COMMENT_XML, thread,
					RECEIVE_COMMENT));
			msgInStream = sock.getInputStream();

			System.out.println(address + "と" + port + "と" + thread + " "
					+ (isCheck ? this.isValid : ""));
		}

		private String getAttrValue(final String name) {
			Socket so = null;
			DataOutputStream out = null;
			InputStream in = null;

			class SAXHandler extends NicoLiveSAXHandler {
				private String value;

				public String getValue() {
					return value;
				}

				@Override
				public void start(String uri, String localName, String qName,
						Attributes attributes) {
					value = attributes.getValue(name);
				}
			}

			SAXHandler saxHandler = new SAXHandler();

			try {
				so = new Socket(address, port);
				out = new DataOutputStream(so.getOutputStream());
				out.writeBytes(String.format(GET_COMMENT_XML, thread,
						RECEIVE_COMMENT));

				in = so.getInputStream();

				sax = SAXParserFactory.newInstance().newSAXParser();
				String s = getSingleElement(in);
				//System.out.println(s);
				InputSource source = new InputSource(new StringReader(s));

				sax.parse(source, saxHandler);
			} catch (UnknownHostException e) {
		    	logger.warning(e.getMessage());
			} catch (ParserConfigurationException e) {
				logger.warning(e.getMessage());
			} catch (SAXException e) {
				logger.warning(e.getMessage());
			} catch (IOException e) {
				logger.warning(e.getMessage());
			} finally {
				try {
					if (in != null) {
						in.close();
						in = null;
					}
					if (out != null) {
						out.close();
						out = null;
					}
					if (so != null) {
						so.close();
						so = null;
					}
					sax = null;
				} catch (IOException e) {
			    	logger.warning(e.getMessage());
				}
			}

			return saxHandler.getValue();
		}

		/**
		 * 有効なコメント部屋かどうか調べる メッセージサーバからの応答に、last_res属性がなければ無効
		 * 
		 * @return
		 */
		public boolean isValidReserch() {
			String last_res = "";
			last_res = getAttrValue("last_res");
			// System.out.println(last_res);
			if (!(last_res == null)) {
				isValid = true;
				return true;
			}
			return false;
		}

		/**
		 * コメントサーバとの接続を切断します
		 * 
		 * @throws IOException
		 */
		public void SocketClose() throws IOException {
			this.msgInStream.close();
			this.msgOutStream.close();
			this.sock.close();
			this.msgInStream = null;
			this.msgOutStream = null;
			this.sock = null;
		}
	}

	/**
	 * 指定されたlv番号のユーザー生放送でこのオブジェクトを初期化します。
	 * 
	 * @param liveNumber
	 *            lv番号
	 * @throws java.io.IOException
	 *             生放送のステータスがなんらかの原因で取得できない場合、または放送が満員もしくはメンバーオンリーの場合
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private void init(String liveNumber) throws IOException,
			ClassNotFoundException, SQLException {
		this.requiredLive = liveNumber;
		this.liveNumber = liveNumber;

		if (isOwner) {
			setInformation(String.format(PUBLISH_API, liveNumber),
					new PublishStatus());
		}

		// 席がとれるまで30回繰り返す
		for (int i = 0; i < 30; i++) {
			System.out.println(String.format("%stimes connecting now ...",
					i + 1));
			try {
				setInformation(String.format(STATUS_API, liveNumber),
						new LiveStatus());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}

			// 席取りが完了していればループから抜ける
			if (seetNo != null) {
				break;
			}

			if (this.errorType == ErrorType.Closed) {
				throw new IOException("この放送は終了しました");
			}
			if (this.errorType == ErrorType.CommunityOnly) {
				throw new IOException("コミュニティ限定のためアクセスできませんでした。");
			}
			if (this.errorType == ErrorType.Commingsoon) {
				throw new IOException("この放送はまだ開場していません");
			}
			if (this.errorType == ErrorType.Maintenance) {
				throw new IOException("ニコ生はメンテ中です");
			}
			if (this.errorType == ErrorType.NotFound){
				throw new IOException("この放送は存在しません");
			}
			if (this.errorType == ErrorType.NotLogin) {
				NicoLive.cookie = null; // 無効なセッション
			}
		}

		// エラーだったら
		if (HasError()) {
			throw new IOException(ErrorMessage());
		}

		// 公式生放送であるか調べる
		Pattern p = Pattern.compile("omsg(\\d+).live.nicovideo.jp");
		Matcher m = p.matcher(this.msAddr);
		if (m.find()) {
			isOfficial = true;
		} else {
			isOfficial = false;
		}

		// コメント部屋の名前を取得する
		if (!isOfficial) { // ユーザ生放送の場合
			if (roomLabel.equals("立ち見A列")) { // 立ち見A
				this.roomType = RoomType.StandA;
			} else if (roomLabel.equals("立ち見B列")) { // 立ち見B
				this.roomType = RoomType.StandB;
			} else if (roomLabel.equals("立ち見C列")) { // 立ち見C
				this.roomType = RoomType.StandC;
			} else if (roomLabel.equals("立ち見席")) { // 立ち見席
				this.roomType = RoomType.Stand; // チャンネル生
			} else { // アリーナ
				this.roomType = RoomType.Arena;
			}
		} else { // 公式生放送の場合
					// TODO:ルームラベル
			Pattern patternR = Pattern.compile("2F右");
			Matcher matcherR = patternR.matcher(this.roomLabel);
			Pattern patternL = Pattern.compile("2F左");
			Matcher matcherL = patternL.matcher(this.roomLabel);
			Pattern patternA = Pattern.compile("アリーナ");
			Matcher matcherA = patternA.matcher(this.roomLabel);
			Pattern patternS = Pattern.compile("スタンド");
			Matcher matcherS = patternA.matcher(this.roomLabel);

			if (this.roomLabel.equals("アリーナ 最前列")) {
				this.roomType = RoomType.ForwardArena;
			} else if (this.roomLabel.equals("アリーナ")) {
				this.roomType = RoomType.FrontArena;
			} else if (this.roomLabel.equals("裏アリーナ")) {
				this.roomType = RoomType.BackArena;
			} else if (this.roomLabel.equals("1F中央 最前列")) {
				this.roomType = RoomType.Forward1FCenter;
			} else if (this.roomLabel.equals("1F中央 前方")) {
				this.roomType = RoomType.Front1FCenter;
			} else if (this.roomLabel.equals("1F中央 後方")) {
				this.roomType = RoomType.Back1FCenter;
			} else if (this.roomLabel.equals("1F中央")) {
				this.roomType = RoomType.Center1F;
				this.liveType = LiveType.Official2;
			} else if (this.roomLabel.equals("1F右 前方")) {
				this.roomType = RoomType.Front1FRight;
			} else if (this.roomLabel.equals("1F右 後方")) {
				this.roomType = RoomType.Back1FRight;
			} else if (this.roomLabel.equals("1F右")) {
				this.roomType = RoomType.Right1F;
				this.liveType = LiveType.Official2;
			} else if (this.roomLabel.equals("1F左 前方")) {
				this.roomType = RoomType.Front1FLeft;
			} else if (this.roomLabel.equals("1F左 後方")) {
				this.roomType = RoomType.Back1FLeft;
			} else if (this.roomLabel.equals("1F左")) {
				this.roomType = RoomType.Left1F;
				this.liveType = LiveType.Official2;
			} else if (this.roomLabel.equals("2F中央 最前列")) {
				this.roomType = RoomType.Forward2FCenter;
			} else if (this.roomLabel.equals("2F中央 前方")) {
				this.roomType = RoomType.Front2FCenter;
			} else if (this.roomLabel.equals("2F中央")) {
				this.roomType = RoomType.Center2F;
				this.liveType = LiveType.Official2;
			} else if (matcherR.find()) { // 2F右
				Pattern pattern = Pattern.compile("Aブロック");
				Matcher matcher = pattern.matcher(this.roomLabel);
				if (matcher.find()) {
					this.roomType = RoomType.A2FRight;
				} else {
					pattern = Pattern.compile("Bブロック");
					matcher = pattern.matcher(this.roomLabel);
					if (matcher.find()) {
						this.roomType = RoomType.B2FRight;
					} else {
						pattern = Pattern.compile("Cブロック");
						matcher = pattern.matcher(this.roomLabel);
						if (matcher.find()) {
							this.roomType = RoomType.C2FRight;
						} else {
							pattern = Pattern.compile("Dブロック");
							matcher = pattern.matcher(this.roomLabel);
							if (matcher.find()) {
								this.roomType = RoomType.D2FRight;
							} else {
								if (this.roomLabel.equals("2F右")) {
									this.roomType = RoomType.Right2F;
									this.liveType = LiveType.Official2;
								} else {
									this.roomType = RoomType.Unknown;
								}
							}
						}
					}
				}
			} else if (matcherL.find()) { // 2F左
				Pattern pattern = Pattern.compile("Aブロック");
				Matcher matcher = pattern.matcher(this.roomLabel);
				if (matcher.find()) {
					this.roomType = RoomType.A2FLeft;
				} else {
					pattern = Pattern.compile("Bブロック");
					matcher = pattern.matcher(this.roomLabel);
					if (matcher.find()) {
						this.roomType = RoomType.B2FLeft;
					} else {
						pattern = Pattern.compile("Cブロック");
						matcher = pattern.matcher(this.roomLabel);
						if (matcher.find()) {
							this.roomType = RoomType.C2FLeft;
						} else {
							pattern = Pattern.compile("Dブロック");
							matcher = pattern.matcher(this.roomLabel);
							if (matcher.find()) {
								this.roomType = RoomType.D2FLeft;
							} else {
								if (this.roomLabel.equals("2F左")) {
									this.roomType = RoomType.Left2F;
									this.liveType = LiveType.Official2;
								} else {
									this.roomType = RoomType.Unknown;
								}
							}
						}
					}
				}
			}else if(matcherA.find()){ //ニコファーレアリーナ
				Pattern pattern = Pattern.compile("Aブロック");
				Matcher matcher = pattern.matcher(this.roomLabel);
				if (matcher.find()) {
					this.roomType = RoomType.ArenaA;
				} else {
					pattern = Pattern.compile("Bブロック");
					matcher = pattern.matcher(this.roomLabel);
					if (matcher.find()) {
						this.roomType = RoomType.ArenaB;
					} else {
						pattern = Pattern.compile("Cブロック");
						matcher = pattern.matcher(this.roomLabel);
						if (matcher.find()) {
							this.roomType = RoomType.ArenaC;
						} else {
							pattern = Pattern.compile("Dブロック");
							matcher = pattern.matcher(this.roomLabel);
							if (matcher.find()) {
								this.roomType = RoomType.ArenaD;
							} else {
								this.roomType = RoomType.Unknown;
							}
						}
					}
				}
				if(this.roomType != RoomType.Unknown){
					this.liveType = LiveType.NicoFarre;
				}
			}else if(matcherS.find()){ //ニコファーレスタンド
				Pattern pattern = Pattern.compile("Aブロック");
				Matcher matcher = pattern.matcher(this.roomLabel);
				if (matcher.find()) {
					this.roomType = RoomType.StandABlock;
				} else {
					pattern = Pattern.compile("Bブロック");
					matcher = pattern.matcher(this.roomLabel);
					if (matcher.find()) {
						this.roomType = RoomType.StandBBlock;
					} else {
						pattern = Pattern.compile("Cブロック");
						matcher = pattern.matcher(this.roomLabel);
						if (matcher.find()) {
							this.roomType = RoomType.StandCBlock;
						} else {
							pattern = Pattern.compile("Dブロック");
							matcher = pattern.matcher(this.roomLabel);
							if (matcher.find()) {
								this.roomType = RoomType.StandDBlock;
							} else {
								this.roomType = RoomType.Unknown;
							}
						}
					}
				}
				if(this.roomType != RoomType.Unknown){
					this.liveType = LiveType.NicoFarre;
				}
			} else {
				Pattern pattern = Pattern.compile("立ち見席");
				Matcher matcher = pattern.matcher(this.roomLabel);
				if (matcher.find()) {
					this.roomType = RoomType.Stand;
				} else {
					if(this.roomLabel.equals("デッキ")){
						this.roomType = RoomType.Deck;
						this.liveType = LiveType.Official2;
					}else{
						this.roomType = RoomType.Unknown;
					}
				}
			}
		}

		// 最前列なし公式生放送かどうか調べる
		if (Integer.parseInt(this.seetNo) > 499) {
			this.liveType = LiveType.Official2;
			System.out.println("この放送は最前列がありません");
			if(this.roomType == RoomType.FrontArena){
				this.roomType = RoomType.BackArena;
			}
		}

		// 情報セット
		setStatus();

		// 米鯖通信開始
		this.thisRoom = new CommentServer(msAddr, msPort, msThread, roomType);
		thisRoom.StartSocketCommunication();
		thisRoom.seatNo = Integer.parseInt(this.seetNo);
		thisRoom.seatBlock = Integer.parseInt(this.seetNo) / 500;
		this.blockNo = thisRoom.seatBlock;
		rooms.add(thisRoom);
		thisRoom.roomLabel = this.roomLabel + "-" + this.seetNo + "番";
		if (this.isOfficial) {
			this.registLiveInfo(thisRoom);
		}

		ticket = getAttrValue("ticket");
		String last_res = getAttrValue("last_res");
		if(last_res != null){
			commentCount = Integer.parseInt(getAttrValue("last_res"));
		}
		// ngWordList = getNgWordList();
	} // End of init() function.

	/**
	 * 公式生放送のアリーナに接続します
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private void OfficialAnotherConnect() throws UnknownHostException,
			IOException, ClassNotFoundException, SQLException {
		// 最初に既に接続したことあるならば、推測せずに接続する
		if (this.findLiveFromSQLite(this.liveNumber)) {
			rooms.remove(0);
			return;
		}
		// 最前列なし放送のときは推測アルゴリズムを変える
		if (this.liveType == LiveType.Official2) {
			OfficialAnotherConnect2();
			return;
		}
		if(this.liveType == LiveType.NicoFarre){
			NicoFarreAnotherConnect();
			return;
		}		
		// なかったので計算する
		if (this.roomType == RoomType.ForwardArena) {
			// アリーナに接続する
			CommentServer Arena;
			Arena = findNext(thisRoom);
			Arena.roomType = RoomType.FrontArena;
			Arena.StartSocketCommunication();
			rooms.add(Arena);
			registLiveInfo(Arena);
			// アリーナ裏に接続する
			Arena = findNext(Arena);
			Arena.roomType = RoomType.BackArena;
			Arena.StartSocketCommunication();
			rooms.add(Arena);
			registLiveInfo(Arena);
		} else if (this.roomType == RoomType.FrontArena) {
			// アリーナ最前列に接続する
			CommentServer target;
			target = findPrevious(thisRoom);
			target.roomType = RoomType.ForwardArena;
			if (target.isValidReserch()) {
				target.StartSocketCommunication();
				rooms.add(target);
				registLiveInfo(target);
			}else{
				System.out.println(this.liveNumber + ":最前列はありません");
			}
			// アリーナ裏に接続する
			target = findNext(thisRoom);
			target.roomType = RoomType.BackArena;
			target.StartSocketCommunication();
			rooms.add(target);
			registLiveInfo(target);
		} else { // アリーナ裏,1F中央~2F左後方までの間
			CommentServer target = thisRoom;
			System.out.println(target.roomType);
			for (int i = roomType.getIntValue() - 1; i >= RoomType.ForwardArena
					.getIntValue(); i--) {
				// 立ち見以上、最前列なし生放送は推測できない
				if (i > 19) {
					break;
				}
				target = findPrevious(target);
				target.roomType = RoomType.valueOf(i);
				System.out.println(target.roomType + " " + i);

				if (target.roomType == RoomType.BackArena) {
					target.StartSocketCommunication();
					rooms.add(target);
					registLiveInfo(target);
				} else if (target.roomType == RoomType.FrontArena) {
					target.StartSocketCommunication();
					rooms.add(target);
					registLiveInfo(target);
				} else if (target.roomType == RoomType.ForwardArena) {
					target.StartSocketCommunication();
					rooms.add(target);
					registLiveInfo(target);
					break;
				}
			}
			// アリーナ以外の接続を無効化する
			if (this.thisRoom.roomType != RoomType.BackArena
					&& thisRoom.roomType != RoomType.Unknown
					&& thisRoom.roomType != RoomType.Stand
					&& thisRoom.roomType != RoomType.Left1F
					&& thisRoom.roomType != RoomType.Right1F
					&& thisRoom.roomType != RoomType.Left2F
					&& thisRoom.roomType != RoomType.Right2F
					&& thisRoom.roomType != RoomType.Center1F
					&& thisRoom.roomType != RoomType.Center2F) {
				rooms.remove(0);
			}
		}
	}
	
	/**
	 * ニコファーレのアリーナに接続します
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private void NicoFarreAnotherConnect() throws UnknownHostException,
			IOException, ClassNotFoundException, SQLException {
		// なかったので計算する
		if (this.roomType == RoomType.ArenaA) {
			// アリーナに接続する
			CommentServer Arena;
			Arena = findNext(thisRoom);
			Arena.roomType = RoomType.ArenaB;
			Arena.StartSocketCommunication();
			rooms.add(Arena);
			registLiveInfo(Arena);
			// アリーナ裏に接続する
			Arena = findNext(Arena);
			Arena.roomType = RoomType.ArenaC;
			Arena.StartSocketCommunication();
			rooms.add(Arena);
			registLiveInfo(Arena);
		} else if (this.roomType == RoomType.ArenaB) {
			// アリーナ最前列に接続する
			CommentServer target;
			target = findPrevious(thisRoom);
			target.roomType = RoomType.ArenaA;
			if (target.isValidReserch()) {
				target.StartSocketCommunication();
				rooms.add(target);
				registLiveInfo(target);
			}else{
				System.out.println(this.liveNumber + ":最前列はありません");
			}
			// アリーナ裏に接続する
			target = findNext(thisRoom);
			target.roomType = RoomType.ArenaC;
			target.StartSocketCommunication();
			rooms.add(target);
			registLiveInfo(target);
		} else { // アリーナ裏,1F中央~2F左後方までの間
			CommentServer target = thisRoom;
			System.out.println(target.roomType);
			for (int i = roomType.getIntValue() - 1; i >= RoomType.ArenaA
					.getIntValue(); i--) {
				target = findPrevious(target);
				target.roomType = RoomType.valueOf(i);
				System.out.println(target.roomType + " " + i);

				if (target.roomType == RoomType.ArenaC) {
					target.StartSocketCommunication();
					rooms.add(target);
					registLiveInfo(target);
				} else if (target.roomType == RoomType.ArenaB) {
					target.StartSocketCommunication();
					rooms.add(target);
					registLiveInfo(target);
				} else if (target.roomType == RoomType.ArenaA) {
					target.StartSocketCommunication();
					rooms.add(target);
					registLiveInfo(target);
					break;
				}
			}
			// アリーナ以外の接続を無効化する
			if (this.thisRoom.roomType != RoomType.BackArena
					&& thisRoom.roomType != RoomType.Unknown
					&& thisRoom.roomType != RoomType.Stand
					&& thisRoom.roomType != RoomType.Left1F
					&& thisRoom.roomType != RoomType.Right1F
					&& thisRoom.roomType != RoomType.Left2F
					&& thisRoom.roomType != RoomType.Right2F
					&& thisRoom.roomType != RoomType.Center1F
					&& thisRoom.roomType != RoomType.Center2F) {
				rooms.remove(0);
			}
		}
	}
	

	/**
	 * 最前列なしの公式生放送において、上位のコメント部屋に接続します。
	 * アリーナ、立ち見席なら、当該座席の0~499番のコメント部屋に接続します
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void OfficialAnotherConnect2() throws UnknownHostException,
			IOException, ClassNotFoundException, SQLException {
		// なかったので計算する
		CommentServer target = thisRoom;
		
		//アリーナ(公式)、立ち見席、デッキ、不明の部屋なら
		if(thisRoom.roomType == RoomType.FrontArena ||
				thisRoom.roomType == RoomType.Stand ||
				thisRoom.roomType == RoomType.Deck ||
				thisRoom.roomType == RoomType.Unknown){
			// 500番以降なら0番~999番(2スレッド)に接続する
			int nextBlockNo = this.blockNo - 1;
			//1000番以降に接続したなら、破棄する
			if (nextBlockNo > 2) {
				CommentServer room = rooms.get(0);
				room.SocketClose();
				rooms.remove(0);
			}
			// 当該コメント部屋の0~999番に接続する
			for (; nextBlockNo >= 0; nextBlockNo--) {
				target = findPrevious(target);
				target.seatBlock = nextBlockNo;
				//アリーナ(500~999番)なら、最前列として表示し接続情報を記録する
				if (this.roomType == RoomType.FrontArena) {
					target.roomType = RoomType.FrontArena;
					if(nextBlockNo < 2){
						registLiveInfo(target);
					}
				}else{
					target.roomType = thisRoom.roomType;
				}
				target.StartSocketCommunication();
				if (nextBlockNo < 2) {
					rooms.add(target);
				}
			}
		}else{
			//順番に上の部屋にアクセスしていき、有効な上位2つの部屋にアクセスする
			CommentServer firstRoom = thisRoom;
			CommentServer secondRoom = thisRoom;
			int i;
			for(i=0; i < 50; i++){
				target = findPrevious(target);
				if(!target.isValidReserch()){ //無効なコメント部屋に接続した
					break;
				}
				firstRoom = secondRoom;
				secondRoom = target;
				i++;
			}
			if(i > 1){
				rooms.remove(0);
				firstRoom.roomType = RoomType.BackArena;
				secondRoom.roomType = RoomType.FrontArena;
				firstRoom.StartSocketCommunication();
				secondRoom.StartSocketCommunication();
				rooms.add(firstRoom);
				rooms.add(secondRoom);
				//DBに登録する
				registLiveInfo(firstRoom);
				registLiveInfo(secondRoom);
			}else{
				//なにもしない
			}
			firstRoom = null;
			secondRoom = null;
		}
		
		//アリーナ(500番~999番)なら、裏アリーナに接続する
		if(this.roomType == RoomType.Arena){
			target = findNext(thisRoom);
			target.roomType = RoomType.BackArena;
		}
		/*if (rooms.size() > 2) {
			rooms.remove(1);
		}*/
		target=null;

	}

	/**
	 * 現在接続しているコメント部屋以外の部屋にも接続します
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void AnotherCommentServerConnect() throws UnknownHostException,
			IOException, ClassNotFoundException, SQLException {
		if (this.isOfficial) {
			OfficialAnotherConnect();
			return;
		}
		/*
		 * if(this.findLiveFromSQLite(this.liveNumber)){ return; }
		 */
		if (this.roomType == RoomType.StandA) { // 立ち見A
			// アリーナへの接続
			CommentServer Arena;
			Arena = findPrevious(thisRoom);
			Arena.roomType = RoomType.Arena;
			Arena.StartSocketCommunication();
			rooms.add(Arena);

			// 立ち見Bへの接続
			if (this.level >= 66) {
				CommentServer StandB;
				StandB = findNext(thisRoom);
				StandB.roomType = RoomType.StandB;
				StandB.StartSocketCommunication();
				rooms.add(StandB);

				// 立ち見Cへの接続
				if (this.level >= 105) {
					CommentServer StandC;
					StandC = findNext(StandB);
					StandC.roomType = RoomType.StandC;
					StandC.StartSocketCommunication();
					rooms.add(StandC);
				}
			}
		} else if (this.roomType == RoomType.StandB) { // 立ち見B
			// 立ち見Aへの接続
			CommentServer StandA;
			StandA = findPrevious(thisRoom);
			StandA.roomType = RoomType.StandA;
			StandA.StartSocketCommunication();
			rooms.add(StandA);

			// アリーナへの接続
			CommentServer Arena;
			Arena = findPrevious(StandA);
			Arena.roomType = RoomType.Arena;
			Arena.StartSocketCommunication();
			rooms.add(Arena);

			// 立ち見Cへの接続
			if (this.level >= 105) {
				CommentServer StandC;
				StandC = findNext(thisRoom);
				StandC.roomType = RoomType.StandC;
				StandC.StartSocketCommunication();
				rooms.add(StandC);
			}
		} else if (this.roomType == RoomType.StandC) { // 立ち見C
			// 立ち見Bへの接続
			CommentServer StandB;
			StandB = findPrevious(thisRoom);
			StandB.roomType = RoomType.StandB;
			StandB.StartSocketCommunication();
			rooms.add(StandB);

			// 立ち見Aへの接続
			CommentServer StandA;
			StandA = findPrevious(StandB);
			StandA.roomType = RoomType.StandA;
			StandA.StartSocketCommunication();
			rooms.add(StandA);

			// アリーナへの接続
			CommentServer Arena;
			Arena = findPrevious(StandA);
			Arena.roomType = RoomType.Arena;
			Arena.StartSocketCommunication();
			rooms.add(Arena);
		} else { // アリーナ
					// 立ち見Aへの接続
			CommentServer StandA;
			StandA = findNext(thisRoom);
			StandA.roomType = RoomType.StandA;
			StandA.StartSocketCommunication();
			rooms.add(StandA);

			// 立ち見Bへの接続
			if (this.level >= 66) {
				CommentServer StandB;
				StandB = findNext(StandA);
				StandB.roomType = RoomType.StandB;
				StandB.StartSocketCommunication();
				rooms.add(StandB);

				// 立ち見Cへの接続
				if (this.level >= 105) {
					CommentServer StandC;
					StandC = findNext(StandB);
					StandC.roomType = RoomType.StandC;
					StandC.StartSocketCommunication();
					rooms.add(StandC);
				}
			}
		}
	}

	// １つ手前のコメント部屋の接続情報を求める
	private CommentServer findPrevious(CommentServer ms) {
		CommentServer target = new CommentServer();
		if (!this.isOfficial) { // ユーザ生
			target.address = ms.address;
			target.thread = ms.thread - 1;
			if (ms.port == 2805) {
				target.port = 2814;
				target.address = prevAddress(ms.address);
			} else {
				target.port = ms.port - 1;
			}
		} else { // 公式生
			target.address = prevAddress(ms.address);
			target.thread = ms.thread - 1;
			if (target.address.equals("omsg104.live.nicovideo.jp")) {
				target.port = ms.port - 1;
				if (target.port == 2804) {
					target.port = 2814;
				}
			} else {
				target.port = ms.port;
			}
		}
		return target;
	}

	// １つ後のコメント部屋の接続情報を求める
	private CommentServer findNext(CommentServer ms) {
		CommentServer target = new CommentServer();
		if (!this.isOfficial) { // ユーザ生
			target.address = ms.address;
			target.thread = ms.thread + 1;
			if (ms.port == 2814) {
				target.port = 2805;
				target.address = nextAddress(ms.address);
			} else {
				target.port = ms.port + 1;
			}
		} else { // 公式生
			target.address = nextAddress(ms.address);
			target.thread = ms.thread + 1;
			if (target.address.equals("omsg101.live.nicovideo.jp")) {
				target.port = ms.port + 1;
				if (target.port > 2814) {
					target.port = 2805;
				}
			} else {
				target.port = ms.port;
			}
		}
		return target;
	}

	// １つ手前のコメントサーバのアドレスを求める
	private String prevAddress(String address) {
		int msNumber; // 番号部分

		if (!isOfficial) { // ユーザ生
			// 正規表現で番号部分を取り出す
			Pattern p = Pattern.compile("msg(\\d+).live.nicovideo.jp");
			Matcher m = p.matcher(address);
			if (m.find()) {
				msNumber = Integer.parseInt(m.group(1));
			} else {
				return null;
			}

			// デクリメント
			if (msNumber == 101) {
				msNumber = 104;
			} else {
				msNumber--;
			}

			// 整形
			// System.out.println(String.format("msg%s.live.nicovideo.jp",
			// msNumber));
			return String.format("msg%s.live.nicovideo.jp", msNumber);
		} else { // 公式生
					// 正規表現で番号部分を取り出す
			Pattern p = Pattern.compile("omsg(\\d+).live.nicovideo.jp");
			Matcher m = p.matcher(address);
			if (m.find()) {
				msNumber = Integer.parseInt(m.group(1));
			} else {
				return null;
			}

			// デクリメント
			if (msNumber == 101) {
				msNumber = 104;
			} else {
				msNumber--;
			}

			// 整形
			// System.out.println(String.format("omsg%s.live.nicovideo.jp",
			// msNumber));
			return String.format("omsg%s.live.nicovideo.jp", msNumber);
		}
	}

	// １つ後のコメントサーバのアドレスを求める
	private String nextAddress(String address) {
		int msNumber; // 番号部分

		if (!isOfficial) { // ユーザ生
			// 正規表現で番号部分を取り出す
			Pattern p = Pattern.compile("msg(\\d+).live.nicovideo.jp");
			Matcher m = p.matcher(address);
			if (m.find()) {
				msNumber = Integer.parseInt(m.group(1));
			} else {
				return null;
			}

			// デクリメント
			if (msNumber == 104) {
				msNumber = 101;
			} else {
				msNumber++;
			}

			// 整形
			// System.out.println(String.format("msg%s.live.nicovideo.jp",
			// msNumber));
			return String.format("msg%s.live.nicovideo.jp", msNumber);
		} else { // 公式生
					// 正規表現で番号部分を取り出す
			Pattern p = Pattern.compile("omsg(\\d+).live.nicovideo.jp");
			Matcher m = p.matcher(address);
			if (m.find()) {
				msNumber = Integer.parseInt(m.group(1));
			} else {
				return null;
			}

			// デクリメント
			if (msNumber == 104) {
				msNumber = 101;
			} else {
				msNumber++;
			}

			// 整形
			// System.out.println(String.format("omsg%s.live.nicovideo.jp",
			// msNumber));
			return String.format("omsg%s.live.nicovideo.jp", msNumber);
		}
	}

	/**
	 * 指定されたハンドラーを使って番組情報を取得、設定します。
	 * 
	 * @param handler
	 *            ハンドラー
	 * @throws java.io.IOException
	 *             生放送のステータスがなんらかの原因で取得できない場合、または放送が満員もしくはメンバーオンリーの場合
	 */
	private void setInformation(String apiUrl, NicoLiveSAXHandler handler)
			throws IOException {
		// 接続するたびにエラー情報をリセット
		this.errorType = ErrorType.Nothing;
		//getplayerstatus前に放送ページにアクセスする(使用変更)
		httpGetContent("http://live.nicovideo.jp/watch/" + this.liveNumber);

		PushbackInputStream in = new PushbackInputStream(
				httpGetContent(apiUrl), BOM.length);
		// UTF-8 BOMのスキップ
		byte[] b = new byte[BOM.length];
		in.read(b, 0, b.length);

		if (!Arrays.equals(b, BOM)) {
			in.unread(b);
		}

		InputSource source = new InputSource(new InputStreamReader(in, "UTF-8"));

		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(source, handler);
		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	/**
	 * 現在のlv番号を返します。
	 * 
	 * @return lvから始まる放送番号 (e.g. lv999999)
	 */
	public String getLiveNumber() {
		return liveNumber;
	}

	/**
	 * この放送のタイトルを返します。
	 * 
	 * @return タイトル
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * この放送が公式生放送であるかを返します
	 * 
	 * @return
	 */
	public boolean getOfficial() {
		return isOfficial;
	}

	/**
	 * この放送のコミュニティレベルを返します。
	 * 
	 * @return コミュニティレベル
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * 現在の総コメント数を取得します。
	 * 
	 * @return 総コメント数
	 */
	public int getTotalCommentCount() {
		updateCount();

		return totalCommentCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	/**
	 * 現在の総来場者数を返します。
	 * 
	 * @return 総来場者数
	 */
	public int getTotalWatchCount() {
		updateCount();

		return totalWatchCount;
	}

	/**
	 * エラータイプを返します
	 * 
	 * @return
	 */
	public ErrorType getErrorType() {
		return this.errorType;
	}

	/*
	 * ニコ生仕様変更の為、削除
	 * 
	 * 現在のリアルタイム来場者数を返します。
	 * 
	 * @return リアルタイム来場者数 public int getRealWatchCount() { updateCount();
	 * 
	 * return realWatchCount; }
	 */

	/**
	 * この放送の基準時刻を、1970 年 1 月 1 日 00:00:00 GMT からの秒数で返します。
	 * 
	 * @return 枠の基準時刻
	 */
	public int getBaseTime() {
		return baseTime;
	}

	/**
	 * この枠の開場時間をUNIXタイムで返します。
	 * 
	 * @return 枠の開場時間
	 */
	public int getOpenTime() {
		return openTime;
	}

	/**
	 * この放送の開始時刻をUNIXタイムで返します。
	 * 
	 * @return 放送開始時間
	 */
	public int getStartTime() {
		return startTime;
	}

	/**
	 * この放送の枠の開始時間と放送の開始時間の差を返します。
	 * 
	 * @return　枠の開始時間と放送の開始時間の差
	 */
	public int getStartTimeDelay() {
		return startTime - openTime;
	}

	/**
	 * この放送のコミュニティ、またはチャンネル番号を返します。
	 * 
	 * @return co から始まるコミュニティ番号、または ch から始まるチャンネル番号
	 */
	public String getCommunityNumber() {
		return communityNumber;
	}

	/**
	 * この放送のルームラベルを取得します。
	 * 
	 * @return ルームラベル
	 */
	public String getRoomLabel() {
		return roomLabel;
	}

	/**
	 * 現在の座席番号を文字列で返します。
	 * 
	 * @return 座席番号
	 */
	public String getSeetNo() {
		return seetNo;
	}

	/**
	 * 現在の経過時間を秒で取得(baseTime→openTimeに変更)
	 * 
	 * @return 経過時間(秒)
	 */
	public int elapsedTime() {
		return (int) (System.currentTimeMillis() / 1000 - openTime);
	}

	/**
	 * 指定された CommentHandler を使用してコメントを処理します。
	 * 
	 * @param handler
	 *            使用する CommentHandler
	 * @see jp.nicovideo.live.CommentHandler
	 * @see jp.nicovideo.live.CommentDefaultHandler
	 * @throws java.io.IOException
	 *             コメントが取得できない場合
	 */
	public void addCommentHandler(final CommentHandler handler)
			throws IOException {
		// 部屋ごとにthreadを作成
		for (CommentServer room : rooms) {
			msThread thread = new msThread(room.msgInStream, handler,
					room.roomType);
			thread.setDaemon(true);
			thread.start();
			msThreads.add(thread);
		}
	} // End of addCommentHandler() function.

	/**
	 * メッセージサーバにソケット通信で接続するクラス コメントを受信して解析します
	 * 
	 */
	private class msThread extends Thread {

		private InputStream stream;
		private final CommentHandler handler;
		private final RoomType roomType;

		public msThread(InputStream stream, final CommentHandler handler,
				RoomType roomType) {
			this.stream = stream;
			this.handler = handler;
			this.roomType = roomType;
		}

		public void run() {
			try {
				String s;
				SAXHandler saxHandler = new SAXHandler(this.roomType);
				SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
				s = getSingleElement(stream);
				try {
					do {
						//System.out.println(s);
						InputSource source = new InputSource(
								new StringReader(s));
						sax.parse(source, saxHandler);
					} while (!saxHandler.isEnd()
							&& !(s = getSingleElement(stream))
									.equals("/disconnect"));
				} catch (java.net.SocketException e) {
					logger.warning(e.getMessage());
				}

			} catch (SAXException e) {
				logger.warning(e.getMessage());
			} catch (IOException e) {
				logger.warning(e.getMessage());
			} catch (ParserConfigurationException e) {
				logger.warning(e.getMessage());
			}
		}

		class SAXHandler extends NicoLiveSAXHandler {
			private Map<String, String> map = new HashMap<String, String>();
			private boolean _isEnd = false;
			private RoomType roomType;

			public SAXHandler(RoomType roomType) {
				// TODO Auto-generated constructor stub
				this.roomType = roomType;
			}

			public boolean isEnd() {
				return _isEnd;
			}

			@Override
			public void start(String uri, String localName, String qName,
					Attributes attributes) {
				if (qName.equals("chat")) {
					int len = attributes.getLength();

					for (int i = 0; i < len; i++) {
						map.put(attributes.getQName(i), attributes.getValue(i));
					}

					/* 一般会員の場合、premium 属性が飛んでこないので明示的に追加 */
					if (!map.containsKey(CommentHandler.MEMBER_TYPE)) {
						map.put(CommentHandler.MEMBER_TYPE,
								CommentHandler.Member.NORMAL);
					}
				}
			}

			@Override
			public void end(String uri, String localName, String qName)
					throws IOException, ClassNotFoundException, SQLException {
				if (qName.equals("chat")) {
					String type = map.get(CommentHandler.MEMBER_TYPE);

					if (type != null
							&& (type.equals(CommentHandler.Member.OWNER) || type
									.equals(CommentHandler.Member.SYSTEM))) {
						if (innerText().equals("/disconnect")) {
							_isEnd = true;
						}
						// NGの仕様変更のため削除
						// else if (innerText().matches("^/ng(add|del)\\s+.*$"))
						// {
						// ngWordList = null;
					}

					handler.commentAttrReceived(map);
					handler.commentReceived(innerText(), roomType);
				}

				map.clear();
			}
		} // End of SAXHandler class.

	}

	/**
	 * 指定したコマンドセットを使用してコメントを投稿します。
	 * 
	 * @param comment
	 *            コメントする文字列
	 * @param cmd
	 *            コマンドセット
	 * @return コメントに成功すれば、true、失敗すれば false
	 * @see jp.nicovideo.live.CommandSet
	 */
	public boolean postComment(String comment, CommandSet cmd) {
		String res = getAttrValue("last_res");
		blockNo = Integer.parseInt(res) / 100;
		InputStream keyContent = httpGetContent(String.format(POSTKEY_API,
				msThread, blockNo));
		String param, postkey;

		if (keyContent == null) {
			return false;
		}

		try {
			param = (new BufferedReader(new InputStreamReader(keyContent,
					"UTF-8"))).readLine();
			postkey = param.substring(param.indexOf('=') + 1);
			int premium = isPremium ? 1 : 0;

			// SecMSec format. (e.g., 11122)
			int vpos = elapsedTime() * 100
					+ Calendar.getInstance().get(Calendar.MILLISECOND) / 10;
			comment = comment.replace("&", "&amp;").replace("<", "&lt;")
					.replace(">", "&gt;");
			final String postXMLTmpl = "<chat thread=\"%s\" ticket=\"%s\" vpos=\"%d\" postkey=\"%s\" mail=\"%s\" user_id=\"%s\" premium=\"%d\">%s</chat>\0";
			final String postXML = String.format(postXMLTmpl, msThread, ticket,
					vpos, postkey, cmd.toString(), userId, premium, comment);
			for (CommentServer room : rooms) {
				room.msgOutStream.write(postXML.getBytes("UTF-8"));
			}

			return true;
		} catch (IOException e) {
			logger.warning(e.getMessage());
			return false;
		}
	} // End of postComment() function.

	/**
	 * コメントを投稿する簡易メソッドです。
	 * 
	 * @param comment
	 *            コメントする文字列
	 * @return コメントに成功すれば、true、失敗すれば false
	 */
	public boolean postComment(String comment) {
		CommandSet cmd = new CommandSet();

		return postComment(comment, cmd);
	}

	/**
	 * コメントを投稿する簡易メソッドです。
	 * 
	 * @param comment
	 *            コメントする文字列
	 * @param owner
	 *            オーナーコメントの場合は true
	 * @return コメントに成功すれば、true、失敗すれば false
	 */
	public boolean postComment(String comment, boolean owner) {
		CommandSet cmd = new CommandSet();
		cmd.remove(Command.ANON);

		return postComment(comment, cmd, owner);
	} // End of postComment() function.

	/**
	 * 指定したコマンドセットを使用してコメントを投稿します。
	 * 
	 * @param comment
	 *            コメントする文字列
	 * @param cmd
	 *            コマンドセット
	 * @param owner
	 *            オーナーコメントの場合は true
	 * @return コメントに成功すれば、true、失敗すれば false
	 * @see jp.nicovideo.live.CommandSet
	 */
	public boolean postComment(String comment, CommandSet cmd, boolean owner) {
		if (!owner) {
			return postComment(comment, cmd);
		}

		BufferedReader in = null;
		PrintStream out = null;
		HttpURLConnection http = null;

		try {
			String postData = String.format(
					"is184=true&mail=%s&body=%s&token=%s", cmd.toString(),
					URLEncoder.encode(comment, "UTF-8"), token);

			URL api = new URL(String.format(OWNER_API, liveNumber));
			http = (HttpURLConnection) api.openConnection();
			http.setRequestMethod("POST");
			http.setDoOutput(true);
			http.setRequestProperty("Cookie",
					cookie.getName() + "=" + cookie.getValue());
			http.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			http.setRequestProperty("Content-Length",
					Integer.toString(postData.length()));

			out = new PrintStream(http.getOutputStream());
			out.print(postData);

			in = new BufferedReader(new InputStreamReader(
					http.getInputStream(), "UTF-8"));

			return in.readLine().contains("error") ? false : true;
		} catch (Exception e) {
			logger.warning(e.getMessage());
			return false;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				logger.warning(e.getMessage());
				return false;
			}
		}
	} // End of postComment() function.

	/**
	 * NG ワードを表す NgWord オブジェクトのリストを取得します。
	 * 
	 * @return NgWord オブジェクトの List
	 * @throws java.io.IOException
	 *             何らかの理由で NG ワードの取得に失敗した場合
	 * @see java.util.List
	 * @see jp.nicovideo.live.NgWord
	 * 
	 *      public List<NgWord> getNgWordList() throws IOException { if
	 *      (ngWordList == null) { ngWordList = new ArrayList<NgWord>(); final
	 *      String url = String.format(NGWORD_API, liveNumber, liveNumber);
	 *      InputStream in = httpGetContent(url); InputSource source = new
	 *      InputSource(new InputStreamReader(in, "UTF-8"));
	 * 
	 *      try { saxParser = SAXParserFactory.newInstance().newSAXParser();
	 * 
	 *      if (in != null) { saxParser.parse(source, new NicoLiveSAXHandler() {
	 *      private boolean useCaseUnify = false; private boolean regex = false;
	 *      private boolean readonly; private String ngWord; private NgWord.Type
	 *      type; private int registerTime;
	 * @Override public void start(String uri, String localName, String qName,
	 *           Attributes attributes) throws SAXException { if
	 *           (qName.equals("response_ngword")) { String status =
	 *           attributes.getValue("status");
	 * 
	 *           if (status == null || status.equals("fail")) { throw new
	 *           SAXException("Invalid status XML"); } } else if
	 *           (qName.equals("ngclient")) { useCaseUnify =
	 *           Boolean.parseBoolean(attributes.getValue("use_case_unify"));
	 *           regex = Boolean.parseBoolean(attributes.getValue("is_regex"));
	 *           readonly =
	 *           Boolean.parseBoolean(attributes.getValue("readonly")); } } //
	 *           End of start() function.
	 * @Override public void end(String uri, String localName, String qName) {
	 *           if (qName.equals("type")) { type =
	 *           NgWord.Type.valueOf(innerText().toUpperCase(Locale.ENGLISH)); }
	 *           else if (qName.equals("source")) { ngWord = innerText(); } else
	 *           if (qName.equals("register_time")) { String time = innerText();
	 * 
	 *           registerTime = time.equals("") ? 0 : Integer.parseInt(time); }
	 *           else if (qName.equals("ngclient")) { ngWordList.add(new
	 *           NgWord(type, ngWord, registerTime, useCaseUnify, regex,
	 *           readonly)); } } }); } } catch (SAXException e) { throw new
	 *           IOException(e); } catch (ParserConfigurationException e) {
	 *           throw new IOException(e); } }
	 * 
	 *           return ngWordList; }
	 */
	/**
	 * 現在のニコニコ生放送への接続を閉じます。
	 */
	public void close() {
		try {
			msThreads.clear();
			for (CommentServer room : rooms) {
				room.SocketClose();
			}
			rooms.removeAll(rooms);
		} catch (IOException e) {
			logger.warning(e.getMessage());
			// do nothing.
		} finally{
			msThreads = null;
			rooms = null;
		}
	}

	/* The private section. */
	protected void finalize() {
		this.close();
	}

	private void setCookie() throws IOException {
		CookieFromFile cff = new CookieFromFile();
		final String tmp = System.getProperty("java.io.tmpdir");
		File from = null;
		File to = null;

		try {
			for (String type : cff.getSupported()) {
				from = cff.defaultCookieFilePath(type);
				to = new File(tmp, from.getName());

				if (!from.isFile()) {
					continue;
				}

				fileCopy(from, to); // For Firefox 3.5 or later.
				cff.init(to, type);
				cookie = cff.getCookie(".nicovideo.jp", "user_session");

				if (cookie != null) {
					break;
				}
			}

			if (cookie == null) {
				throw new IOException("cookie of browsers is not found");
			}
		} finally {
			if (to != null) {
				to.delete();
			}

			if (cff != null) {
				cff.close();
			}
		}
	}

	private void setCookie(String mailAddress, String password)
			throws IOException {
		URL api = new URL(LOGIN_URL);
		HttpsURLConnection https = (HttpsURLConnection) api.openConnection();
		mailAddress = URLEncoder.encode(mailAddress, "UTF-8");
		password = URLEncoder.encode(password, "UTF-8");
		final String cParam = String.format("mail=%s&password=%s", mailAddress,
				password);

		https.setRequestMethod("POST");
		https.setDoOutput(true);
		https.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		https.setRequestProperty("Content-Length",
				Integer.toString(cParam.length()));

		PrintStream out = new PrintStream(https.getOutputStream());
		out.print(cParam);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				https.getInputStream(), "UTF-8"));

		String s = null;
		while ((s = in.readLine()) != null) {
			if (s.contains("ログイン情報が間違っています！")) {
				throw new IOException("Incorrect mail address or password");
			}
		}

		final String sc = https.getHeaderField("Set-Cookie");
		final String[] session = sc.substring(0, sc.indexOf(";")).split("=");

		cookie = new HttpCookie(session[0], session[1]);
	}

	/* Webからデータを持ってくる */
	private InputStream httpGetContent(String url) {
		if(NicoLive.cookie == null){
			return null;
		}
		HttpURLConnection http = null;
		try {
			URL api = new URL(url);
			http = (HttpURLConnection) api.openConnection();
			http.setRequestProperty("Cookie",
					cookie.getName() + "=" + cookie.getValue());
			http.setRequestProperty("User-Agent", USER_AGENT);
			// 302エラーが出ていないか確認
			// セッションが切れているときはログイン画面に誘導される
			int status = http.getResponseCode();
			if (status == HttpURLConnection.HTTP_MOVED_TEMP) {
				System.out.println(http.getHeaderField("Location"));
			}
			// Content-typeがtext/htmlならR-18放送
			if(http.getContentType().equals("text/html") && url.indexOf("api") != -1){
				URL accept = new URL("http://live.nicovideo.jp/r18accept");
				URLConnection uc = accept.openConnection();
				uc.setDoOutput(true);
				uc.setRequestProperty("Cookie",
						cookie.getName() + "=" + cookie.getValue());
				uc.setRequestProperty("User-Agent", "ニコ生コメントビューワ(仮) katoken@morimati.info");
				OutputStream output = uc.getOutputStream();
				String postStr = "next_url=http://live.nicovideo.jp/api/getplayerstatus/lv73571971&series_confirm_value=true";
				PrintStream out = new PrintStream(output);
				out.print(postStr);
				out.close();
				InputStream is = uc.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String s;
				while((s=reader.readLine()) != null){
					System.out.println(s);
				}
				return httpGetContent(url);
			}
			return http.getInputStream();
		} catch (IOException e) {
			logger.warning(e.getMessage());
			try {
				int respCode = ((HttpURLConnection) http).getResponseCode();
				//メンテ中ならXMLを返す
				if(respCode == 503){
					if(http.getContentType().equals("application/xml")){
						InputStream es = ((HttpURLConnection)http).getErrorStream();
						//this.errorType = ErrorType.Maintenance;
						return es;
					}
				}
				InputStream es = ((HttpURLConnection) http).getErrorStream();
				System.out.println("HTTP接続エラー：" + respCode);
				return es;
			} catch (IOException ex) {
				logger.warning(ex.getMessage());
				return null;
			}
		}
	}

	private String getSingleElement(InputStream in) throws IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream(256);
		int c;
		while ((c = in.read()) != 0x00) {
			bo.write(c);
		}

		return bo.toString("UTF-8");
	}

	private String getAttrValue(final String name) {
		Socket so = null;
		DataOutputStream out = null;
		InputStream in = null;

		class SAXHandler extends NicoLiveSAXHandler {
			private String value;

			public String getValue() {
				return value;
			}

			@Override
			public void start(String uri, String localName, String qName,
					Attributes attributes) {
				value = attributes.getValue(name);
			}
		}

		SAXHandler saxHandler = new SAXHandler();

		try {
			so = new Socket(msAddr, msPort);
			out = new DataOutputStream(so.getOutputStream());
			out.writeBytes(String.format(GET_COMMENT_XML, msThread,
					commentCount + 100));

			in = so.getInputStream();

			SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
			String s = getSingleElement(in);
			InputSource source = new InputSource(new StringReader(s));

			sax.parse(source, saxHandler);
		} catch (UnknownHostException e) {
			logger.warning(e.getMessage());
		} catch (ParserConfigurationException e) {
			logger.warning(e.getMessage());
		} catch (SAXException e) {
			logger.warning(e.getMessage());
		} catch (IOException e) {
			logger.warning(e.getMessage());
		} finally {
			try {
				if (in != null) {
					in.close();
					in = null;
				}
				if (out != null) {
					out.close();
					out = null;
				}
				if (so != null) {
					so.close();
					so = null;
				}
			} catch (IOException e) {
				logger.warning(e.getMessage());
			}
		}

		return saxHandler.getValue();
	} // End of getAttrValue() function.

	private void updateCount() {
		final int elapsed = elapsedTime();
		boolean isUpdate = false;

		if (elapsed - updateElapsedTime >= UPDATE_INTERVAL) {
			updateElapsedTime = elapsed;
			isUpdate = true;
		}

		if (isUpdate) {
			InputStream in = httpGetContent(String.format(HEARTBEAT_API,
					liveNumber));
			InputSource source = null;

			try {
				source = new InputSource(new InputStreamReader(in, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.warning(e.getMessage());
				return;
			}

			try {
				SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
				sax.parse(source, new NicoLiveSAXHandler() {
					@Override
					public void start(String uri, String localName,
							String qName, Attributes attributes)
							throws SAXException {
						if (qName.equals("heartbeat")) {
							String status = attributes.getValue("status");

							if (status == null || status.equals("fail")) {
								throw new SAXException();
							}
						}
					}

					@Override
					public void end(String uri, String localName, String qName) {
						if (qName.equals("watchCount")) {
							totalWatchCount = Integer.parseInt(innerText());
						} else if (qName.equals("commentCount")) {
							commentCount = Integer.parseInt(innerText());
							/*
							 * ニコ生仕様変更の為、削除 } else if
							 * (qName.equals("freeSlotNum")) { realWatchCount =
							 * calcRealWatchCount
							 * (Integer.parseInt(innerText()));
							 */
						}
					}
				});
			} catch (ParserConfigurationException e) {
				logger.warning(e.getMessage());
			} catch (SAXException e) {
				logger.warning(e.getMessage());
			} catch (IOException e) {
				logger.warning(e.getMessage());
			}
		}
	} // End of updateCount() function.

	private void fileCopy(File from, File to) throws IOException {
		FileChannel ifc = null;
		FileChannel ofc = null;

		try {
			ifc = (new FileInputStream(from)).getChannel();
			ofc = (new FileOutputStream(to)).getChannel();

			ifc.transferTo(0, ifc.size(), ofc);
		} finally {
			if (ifc != null) {
				ifc.close();
			}

			if (ofc != null) {
				ofc.close();
			}
		}
	}

	/**
	 * コミュレベルとタイトルを取得する
	 * 
	 * @throws IOException
	 */
	private void setStatus() throws IOException {
		final String url = String.format(WATCH_URL, liveNumber);
		InputStreamReader in = new InputStreamReader(httpGetContent(url),
				"UTF-8");

		// コミュレベルとタイトルを取得する
		BufferedReader br = new BufferedReader(in);
		Pattern regex1 = Pattern.compile("<title>(.+)-\\sニコニコ生放送</title>");
		Pattern regex2 = Pattern
				.compile("レベル：<strong\\s+style=\"font-size:\\s+14px;\">(\\d+)</strong>");
		String s;
		while ((s = br.readLine()) != null) {
			Matcher m1 = regex1.matcher(s);
			Matcher m2 = regex2.matcher(s);
			if (m1.find()) {
				title = m1.group(1);
			}
			if (m2.find()) {
				level = Integer.parseInt(m2.group(1));
				break;
			}
		}
	}

	/* The inner class. */
	/**
	 * SAXParser.parse() に渡す Handler です。 getplayerstatusを解析します
	 */
	private class LiveStatus extends NicoLiveSAXHandler {

		boolean isFail = false;

		@Override
		public void start(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equals("getplayerstatus")) {
				String status = attributes.getValue("status");

				if (status == null) {
					SetError(ErrorType.ParseError);
					throw new SAXException("解析に失敗しました。statusはnullを返されました。");
				}

				isFail = status.equals("fail") ? true : false;
			}
			if (qName.equals("nicolive_api")) {
				String status = attributes.getValue("status");

				if (status == null) {
					SetError(ErrorType.ParseError);
					throw new SAXException("解析に失敗しました。statusはnullを返されました。");
				}

				isFail = status.equals("fail") ? true : false;
			}
		} // End of startElement() function.

		@Override
		public void end(String uri, String localName, String qName)
				throws SAXException {
			if (isFail && qName.equals("code")) {
				// エラーコードを取得する
				SetError(innerText());
				throw new SAXException(ErrorMessage());
			}

			if (qName.equals("watch_count")) {
				totalWatchCount = Integer.parseInt(innerText());
			} else if (qName.equals("id")) {
				liveNumber = innerText();
			} else if (qName.equals("comment_count")) {
				commentCount = Integer.parseInt(innerText());
			} else if (qName.equals("base_time")) {
				baseTime = Integer.parseInt(innerText());
			} else if (qName.equals("open_time")) {
				openTime = Integer.parseInt(innerText());
			} else if (qName.equals("start_time")) {
				startTime = Integer.parseInt(innerText());
			} else if (qName.equals("end_time")) {
				endTime = Integer.parseInt(innerText());
			} else if (qName.equals("default_community")) {
				communityNumber = innerText();
			} else if (qName.equals("provider_type")) {
				providerType = innerText();
				if (providerType.equals("community")) {
					liveType = LiveType.User;
				} else if (providerType.equals("official")) {
					liveType = LiveType.Official1;
				} else {
					liveType = LiveType.Channel;
				}
			} else if (qName.equals("room_label")) {
				roomLabel = innerText();
			} else if (qName.equals("room_seetno")) {
				seetNo = innerText();
			} else if (qName.equals("is_premium")) {
				isPremium = innerText().equals("1");
			} else if (qName.equals("user_id")) {
				userId = innerText();
			} else if (qName.equals("addr")) {
				msAddr = innerText();
			} else if (qName.equals("port")) {
				msPort = Integer.parseInt(innerText());
			} else if (qName.equals("thread")) {
				msThread = Integer.parseInt(innerText());
			} else if (qName.equals("is_owner")) {
				isOwner = innerText().equals("1");
			}
		} // End of endElement() function.
	} // End of LiveStatus inner class.

	/**
	 * SAXParser.parse() に渡す Handler です。
	 */
	private class PublishStatus extends NicoLiveSAXHandler {

		boolean isFail = false;

		@Override
		public void start(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equals("getpublishstatus")) {
				String status = attributes.getValue("status");

				if (status == null) {
					throw new SAXException("Invalid status XML");
				}

				isFail = status.equals("fail") ? true : false;
			}
		} // End of startElement() function.

		@Override
		public void end(String uri, String localName, String qName)
				throws SAXException {
			if (isFail && qName.equals("code")) {
				throw new SAXException(innerText());
			}

			if (qName.equals("token")) {
				token = innerText();
			}
		} // End of endElement() function.
	} // End of LiveStatus inner class.

	private void SetError(ErrorType errorType) {
		this.errorType = errorType;
	}

	/**
	 * 放送エラーがあれば、真を返します
	 * 
	 * @return エラーをもっているかの真理値
	 */
	public boolean HasError() {
		return (this.errorType != ErrorType.Nothing);
	}

	private void SetError(String errCode) {
		if (errCode.equals("closed")) {
			this.errorType = ErrorType.Closed;
		} else if (errCode.equals("notlogin")) {
			this.errorType = ErrorType.NotLogin;
		} else if (errCode.equals("notfound")) {
			this.errorType = ErrorType.NotFound;
		} else if (errCode.equals("invalid_thread")) {
			this.errorType = ErrorType.NotFound;
		} else if (errCode.equals("invalid_v1")) {
			this.errorType = ErrorType.NotFound;
		} else if (errCode.equals("NOT_FOUND")) {
			this.errorType = ErrorType.NotFound;
		} else if (errCode.equals("unknown_error")) {
			this.errorType = ErrorType.Unknown;
		} else if (errCode.equals("unknown")) {
			this.errorType = ErrorType.Unknown;
		} else if (errCode.equals("maintenance")) {
			this.errorType = ErrorType.Maintenance;
		} else if (errCode.equals("server_error")) {
			this.errorType = ErrorType.ServerError;
		} else if (errCode.equals("DELETED")) {
			this.errorType = ErrorType.Deleted;
		} else if (errCode.equals("full")) {
			this.errorType = ErrorType.Full;
		} else if (errCode.equals("permission_denied")) {
			this.errorType = ErrorType.PermissionDenied;
		} else if (errCode.equals("require_community_member")) {
			this.errorType = ErrorType.CommunityOnly;
		} else if (errCode.equals("access_locked")) {
			this.errorType = ErrorType.AccessLocked;
		} else if (errCode.equals("comingsoon")) {
			this.errorType = ErrorType.Commingsoon;
		} else {
			this.errorType = ErrorType.Undefined;
		}
	}

	/**
	 * エラーメッセージを返します。
	 * 
	 * @return エラーメッセージ
	 */
	public String ErrorMessage() {
		switch (this.errorType) {
		case Nothing:
			return null;
			
		case NotLogin:
			return "ログインが完了していません。";
			
		case NotFound:
			return "指定したIDは無効です。";
			
		case Unknown:
			return "サーバーに障害が発生しています。";

		case Maintenance:
			return "メンテナンス中です。";

		case ServerError:
			return "サーバーに接続できませんでした。";

		case Deleted:
			return "削除されています。";

		case Closed:
			return "生放送は終了しています。";

		case ParseError:
			return "解析エラーが発生しました。フォーマットに異常がある可能性があります。";

		case Limitation:
			return "接続が制限されているため、情報を取得できませんでした。";

		case Full:
			return "番組が満席のため、情報を取得できませんでした。";

		case Undefined:
			return "未定義のエラーが発生しました。";

		case PermissionDenied:
			return "アクセスが拒否されました。";

		case CommunityOnly:
			return "コミュニティ限定のためアクセスできませんでした。";

		case AccessLocked:
			return "アクセス規制により情報を取得できませんでした。";

		case Commingsoon:
			return "まだ開場されていませんでした。";

		}
		return null;
	}

	/**
	 * ユーザIDからユーザ名を取得します 将来的には既に取得済みのユーザであれば、戻り値でユーザ名を返す予定
	 * 
	 * @param userId
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public String getNameFromUserId(String userId)
			throws ClassNotFoundException, SQLException {
		// 184idをはじいて、生IDをint型に直す
		int uid;
		try {
			uid = Integer.parseInt(userId);
		} catch (NumberFormatException e) {
			return null;
		}
		// DBからユーザ名が取得できたらそれを返す
		String name = findNameFromSQLite(uid);
		if (name != null) {
			return name;
		}

		// このプロセスで既に取得中or待機中なら帰る
		if (gotNameList.contains(uid)) {
			return null;
		}

		// 作業タスクに追加
		synchronized (taskQueue) {
			taskQueue.add(new GetNameTask(uid));
			gotNameList.add(uid);
			taskQueue.notify();
			if (gotNameList.size() > 50) {
				gotNameList.remove(gotNameList.size() - 1);
			}
		}

		return null;
	}

	// ユーザネーム取得情報クラス
	class GetNameTask {
		// フィールド
		public int userId;
		public int failedCount;

		// コンストラクタ
		public GetNameTask(int uid) {
			this.userId = uid;
			this.failedCount = 0;
		}

		// 取得失敗したらカウントプラス
		public void Fail() {
			this.failedCount++;
		}
	}

	// サブスレッドループ

	/**
	 * 名前を取得する
	 */
	class GetNameWork extends Thread {
		public void run() {
			GetNameTask task = null;

			while (true) {
				synchronized (taskQueue) {
					if (taskQueue.isEmpty()) {
						task = null;
						try {
							taskQueue.wait();
						} catch (InterruptedException e) {
							logger.warning(e.getMessage());
						}
						continue;
					} else {
						task = taskQueue.poll();
					}
				}

				String url = String.format(USER_PAGE_URL_FORMAT, task.userId);

				try {
					InputStream sourceStream = httpGetContent(url);
					PushbackInputStream in = new PushbackInputStream(
							sourceStream, BOM.length);

					// UTF-8 BOMのスキップ
					byte[] b = new byte[BOM.length];
					in.read(b, 0, b.length);

					if (!Arrays.equals(b, BOM)) {
						in.unread(b);
					}

					// ユーザページを取得
					InputStreamReader source = new InputStreamReader(in,
							"UTF-8");

					// 名前を取得する
					BufferedReader br = new BufferedReader(source);
					Pattern regex1 = Pattern.compile(NAME_FILTER);
					String s = null;
					boolean hasName = false; // 名前が見付かったか
					while ((s = br.readLine()) != null) {
						Matcher m1 = regex1.matcher(s);
						// System.out.println(s);
						if (m1.find()) {
							registName(task.userId, m1.group(1));
							String rest = taskQueue.size() > 0 ? ":waiting for :"
									+ taskQueue.size() + "people"
									: "";
							 System.out.println(task.userId + ":" +
							 m1.group(1) + rest);
							// System.out.println(task.userId + rest);
							hasName = true;
							break;
						}
					}
					in.close();
					br.close();
					if (!hasName) {
						String res = slurp(sourceStream);
						if (res.isEmpty()) {
							GetNameFail(task, "ページが読み込めませんでした。");
							continue;
						}
						GetNameFail(task, "名前取得：正規表現のパターンと一致しません:");
					}
					source.close();
					sourceStream.close();
				} catch (IOException e) {
					GetNameFail(task, e.getMessage());
					logger.warning(e.getMessage());
				} catch (ClassNotFoundException e) {
					logger.warning(e.getMessage());
				} catch (SQLException e) {
					logger.warning(e.getMessage());
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.warning(e.getMessage());
				}
			}
		}

		/**
		 * InputStreamをStringに変換する
		 * 
		 * @param in
		 *            InputStream
		 * @return
		 * @throws IOException
		 */
		private String slurp(InputStream in) throws IOException {
			StringBuffer out = new StringBuffer();
			byte[] b = new byte[4096];
			for (int n; (n = in.read(b)) != -1;) {
				out.append(new String(b, 0, n));
			}
			return out.toString();
		}

		// 取得失敗時の動作
		private void GetNameFail(GetNameTask task, String message) {
			task.Fail();

			// ３回までリトライ
			if (task.failedCount < 3) {
				System.out.println("Failed to get name:" + task.failedCount
						+ "times:" + task.userId);
				synchronized (taskQueue) {
					taskQueue.add(task);
				}
			} else {
				System.out.println("Failed to get name:" + task.userId
						+ "message:" + message);
			}
		}
	}

	/**
	 * ユーザの名前をsqliteに登録します
	 * 
	 * @param userId
	 * @param name
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void registName(int userId, String name)
			throws ClassNotFoundException, SQLException {
		// データベースに接続
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
		String sql;
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String dataFile = "jdbc:sqlite:"
					+ new File(".").getAbsoluteFile().getParent()
					+ "/chat.sqlite";
			connection = DriverManager.getConnection(dataFile);

			// テーブル確認
			sql = "insert into users values(?,?, null)";
			statement = connection.prepareStatement(sql);
			statement.setLong(1, userId);
			statement.setString(2, name);
			//System.out.println(userId + "：" + name);
			int res = statement.executeUpdate();

			// DBを閉じる
			connection.close();

		} catch (SQLException e) {
			logger.warning(e.getMessage());
		} finally { // 念のため
			if (connection != null) {
				connection.close();
				connection = null;
			}
			if (statement != null) {
				statement.close();
				statement = null;
			}
			if (result != null) {
				result.close();
				result = null;
			}
		}
	}

	/**
	 * 公式生放送の接続情報を記録する
	 * 
	 * @param target
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void registLiveInfo(CommentServer target)
			throws ClassNotFoundException, SQLException {
		// データベースに接続
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
		String sql;
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String dataFile = "jdbc:sqlite:"
					+ new File(".").getAbsoluteFile().getParent()
					+ "/chat.sqlite";
			connection = DriverManager.getConnection(dataFile);

			// 既に挿入されていればなにもしない
			sql = "select * from live where liveid = ? and roomid = ? and seatblock = ? LIMIT 1";
			statement = connection.prepareStatement(sql);
			statement.setString(1, this.liveNumber);
			statement.setInt(2, target.roomType.getIntValue());
			statement.setInt(3, Integer.parseInt(this.seetNo) / 500);
			result = statement.executeQuery();
			if (result.next()) {
				return;
			}
			result.close();
			// threadでも検索する
			sql = "select * from live where thread = ? LIMIT 1";
			statement = connection.prepareStatement(sql);
			statement.setInt(1, target.thread);
			result = statement.executeQuery();
			if (result.next()) {
				return;
			}

			// テーブル確認
			sql = "insert into live values(?,?,?,?,?,?,?,?)";
			statement = connection.prepareStatement(sql);
			statement.setString(1, this.liveNumber);
			statement.setInt(2, target.roomType.getIntValue());
			statement.setInt(3, target.seatBlock);
			statement.setInt(4, target.thread);
			statement.setInt(5, target.port);
			statement.setString(6, target.address);
			statement.setInt(7, target.seatNo);
			statement.setString(8, target.roomLabel);
			// System.out.println(userId + "：" + name);
			int res = statement.executeUpdate();

			// DBを閉じる
			connection.close();

		} catch (SQLException e) {
			logger.warning(e.getMessage());
		} finally { // 念のため
			if (connection != null) {
				connection.close();
				connection = null;
			}
			if (statement != null) {
				statement.close();
				statement = null;
			}
			if (result != null) {
				result.close();
				result = null;
			}
		}
	}

	/**
	 * SQLiteからユーザ名を取得します
	 * 
	 * @param userId
	 * @return 名前があればそれを返し、なければnullを返す
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private String findNameFromSQLite(int userId)
			throws ClassNotFoundException, SQLException {
		// データベースに接続し、初期設定(テーブル等の作成)を行う
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
		String sql;
		PreparedStatement statement = null;
		ResultSet result = null;
		String name = null; // ユーザ名

		try {
			String dataFile = "jdbc:sqlite:"
					+ new File(".").getAbsoluteFile().getParent()
					+ "/chat.sqlite";
			connection = DriverManager.getConnection(dataFile);

			// テーブル確認
			sql = "select * from users where userid = ? LIMIT 1";
			statement = connection.prepareStatement(sql);
			statement.setLong(1, userId);
			result = statement.executeQuery();
			if (result.next()) {
				name = result.getString(2);
			}
			// DBを閉じる
			connection.close();
			result.close();
			statement.close();

			return name;

		} catch (SQLException e) {
			logger.warning(e.getMessage());
		} finally { // 念のため
			if (connection != null) {
				connection.close();
				connection = null;
			}
			if (statement != null) {
				statement.close();
				statement = null;
			}
			if (result != null) {
				result.close();
				result = null;
			}
		}
		return null;
	}

	/**
	 * SQLiteから放送接続情報を取得します
	 * 
	 * @param liveId
	 * @return 情報が取得できたならば、真を返します
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	private boolean findLiveFromSQLite(String liveId)
			throws ClassNotFoundException, SQLException, UnknownHostException,
			IOException {
		// データベースに接続する
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
		String sql;
		PreparedStatement statement = null;
		ResultSet result = null;
		boolean hasInfo = false;
		boolean hasArenaInfo = false;
		int roomId; // 部屋ID
		int i;
		int count = 0;

		try {
			String dataFile = "jdbc:sqlite:"
					+ new File(".").getAbsoluteFile().getParent()
					+ "/chat.sqlite";
			connection = DriverManager.getConnection(dataFile);
			
			/*LiveIdからアリーナ接続情報があるか調べる
			//roomidが1~3がアリーナ
			sql = "select * from live where liveid = ? and roomid between 1 and 3";
			statement = connection.prepareStatement(sql);
			statement.setString(1, liveId);
			result = statement.executeQuery();
			if(!result.next()){
				hasArenaInfo = true;
			}
			*/

			// LiveIdから過去の接続情報を検索する
			sql = "select * from live where liveid = ? order by thread";
			statement = connection.prepareStatement(sql);
			statement.setString(1, liveId);
			result = statement.executeQuery();

			for (i = 1; result.next(); i++) {
				// 接続する
				CommentServer target = new CommentServer();
				roomId = result.getInt(2);
				target.thread = result.getInt(4);
				target.port = result.getInt(5);
				target.address = result.getString(6);
				if (roomId == 1) { // アリーナ最前列
					target.roomType = RoomType.ForwardArena;
					count++;
				} else if (roomId == 2) { // アリーナ
					target.roomType = RoomType.FrontArena;
					count++;
				} else if (roomId == 3) { // 裏アリーナ
					target.roomType = RoomType.BackArena;
					count++;
				} else if (roomId == RoomType.ArenaA.getIntValue()){ //アリーナAブロック
					target.roomType = RoomType.ArenaA;
					count++;
				} else if (roomId == RoomType.ArenaB.getIntValue()){ //アリーナBブロック
					target.roomType = RoomType.ArenaB;
					count++;
				} else if (roomId == RoomType.ArenaC.getIntValue()){ //アリーナCブロック
					target.roomType = RoomType.ArenaC;
					count++;
				} else { //アリーナ以外の場合
					/*アリーナ接続情報がないので一番大きなスレッドに接続する
					if(hasArenaInfo){
						//現在の接続先よりも上の部屋であるなら
						if(this.roomType.getIntValue() > roomId){
							target.roomType = RoomType.valueOf(roomId);
							target.StartSocketCommunication();
							rooms.add(target);
							//推測しない
							hasInfo = true;
							count++;
							System.out.println(target.roomType);
							break;
						}
						
					}
					*/
					target.roomType = RoomType.Unknown;
				}
				// アリーナ系なら接続を開始する
				if (target.roomType != RoomType.Unknown) {
					target.StartSocketCommunication();
					rooms.add(target);
				}
			}
			// アリーナに２以下しか接続できなかったら無効化する
			if (count > 1) {
				hasInfo = true;
			} else {
				// 推測が可能そうなら、接続を切断する。
				if (this.roomType.getIntValue() < 20) {
					CommentServer room;
					for (int j = count; j > 0; j--) {
						room = rooms.get(j);
						System.out.println(room.roomType + "を切断しました");
						room.SocketClose();
						rooms.remove(j);
					}
				}
			}
			if(hasInfo){
				System.out.println("DBに" + count + "個のアリーナ接続情報がありました");
			}
			// DBを閉じる
			connection.close();

			return hasInfo;

		} catch (SQLException e) {
			logger.warning(e.getMessage());
		} finally { // 念のため
			if (connection != null) {
				connection.close();
				connection = null;
			}
			if (statement != null) {
				statement.close();
				statement = null;
			}
			if (result != null) {
				result.close();
				result = null;
			}
		}
		return false;
	}

} // End of NicoLive class.
