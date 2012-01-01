package webSocketChat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.nicovideo.live.CommentDefaultHandler;
import jp.nicovideo.live.CommentHandler;
import jp.nicovideo.live.ErrorType;
import jp.nicovideo.live.NicoLive;
import jp.nicovideo.live.RoomType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


import org.eclipse.jetty.websocket.WebSocket;

/* 
 ニコ生コメ鯖と通信を行うクラス
 コメントを受信したら、ユーザにコメントを返す
 */
public class MyWebSocket implements WebSocket.OnTextMessage {
	
	public String _liveId = null; //接続先の番組
	private WebSocket.Connection _outbound; //WebSocketの接続先
	private static Map<String, Connections> _connections = new HashMap<String, Connections>();
	private UserConfig _userConfig; //ユーザ設定情報
	/** ニコニコ動画アカウント */
	private String mail = "";
	private String pass = "";
		
	/** 
	 * コンストラクタ
	 * @param request
	 */
	public MyWebSocket(HttpServletRequest request) {
		_userConfig = new UserConfig();
	}

	@Override
	public void onClose(int arg0, String arg1) {
		System.out.println("closed");
		if(_liveId != null){
			this.Close();
		}
		_outbound = null;
		_userConfig = null;
	}

	@Override
	public void onOpen(Connection arg0) {
		_outbound = arg0;
	}
	
	//現在接続中の放送から受信登録を抹消する
	private void Close(){
		Connections connection = _connections.get(_liveId);
		connection.cliants.remove(_outbound);
		//全てのクライアントがいなくなればコメ鯖との通信を切断する
		if(connection.cliants.isEmpty()){
			String liveId = connection._live.getLiveNumber();
			connection.close();
			_connections.remove(liveId);
		}
		
	}
	
	@Override
	public void onMessage(String request) {
		// lv番号かどうか調べる
		String liveId=null;
		String livePrefix = null;
		
		Pattern regex1 = Pattern.compile("((lv|co|ch)\\d+)");
  		Matcher m1 = regex1.matcher(request);
  		if(m1.find()){
  			liveId = m1.group(1);
  			livePrefix = m1.group(2);
  		}
  		//lv番号でなければconfig設定を行う
  		if(liveId == null){
  			setConfig(request);
  			return;
  		}
  		
  		//既に接続中だったら切断する
  		if(_liveId != null){
  			this.Close();
  		}
 
  		//lv番号なら、既に接続中のクライアントを探す
  		//co等なら一度、getplayerstatusしてlv番号を取得する
  		Connections connection;
  		if(livePrefix == "lv"){
	  		_liveId = liveId;
	  		
	  		connection = _connections.get(liveId);
	  		//既にコメ鯖に接続中なら、クライアントリストに追加する
	  		if(connection != null){
	  			System.out.println("接続中だよ！");
	  			connection.addNewCliant(_outbound);
	  			return;
	  		}
  		}
  		System.out.println("接続してないよ！");
  		
		//新規でコメントサーバに接続する
  		Connections connections = new Connections();
		try {
			connections._live = new NicoLive(liveId, mail, pass);
			//セッションが無効になっていたらもう一度
			if(connections._live.getErrorType() == ErrorType.NotLogin){
				connections._live = new NicoLive(liveId, mail, pass);
			}
		} catch (IOException e1) {
			try {
				_outbound.sendMessage("{\"info\":{\"error\":\"" + e1.getMessage() + "\"}}");
			} catch (IOException e) {
		    	WebSocketChat.logger.warning(e.getMessage());
			}
	    	WebSocketChat.logger.warning(e1.getMessage());
			return;
		} catch (ClassNotFoundException e) {
	    	WebSocketChat.logger.warning(e.getMessage());
		} catch (SQLException e) {
	    	WebSocketChat.logger.warning(e.getMessage());
		}
		
		//接続完了通知
		try {
			_outbound.sendMessage("{\"info\":" +
					"{" +
					"\"liveid\":\"" + connections._live.getLiveNumber() + "\"," +
					"\"title\":\"" + connections._live.getTitle() + "\"," +
					"\"isOfficial\":\"" + connections._live.getOfficial() + "\"" +
					"}}");
		} catch (IOException e) {
			WebSocketChat.logger.warning(e.getMessage());
		}

		//コンソールに放送情報を表示
		System.out.println("lv = " + connections._live.getLiveNumber());
       System.out.println("title = " + connections._live.getTitle());
       //System.out.println("level = " + _live.getLevel());
       System.out.println("total comment count = " + connections._live.getCommentCount());
       System.out.println("total watch count = " + connections._live.getTotalWatchCount());
       System.out.println("room label = " + connections._live.getRoomLabel());
       System.out.println("co No = " + connections._live.getCommunityNumber());
       System.out.println("seat No = " + connections._live.getSeetNo());

       this._liveId = connections._live.getLiveNumber();
       
       //同時に接続されていたら、どちらかを切断する
       //co番号を入力されたときはここで、lv番号のクライアントリストに追加する
       if(_connections.containsKey(connections._live.getLiveNumber())){
    	    connection = _connections.get(connections._live.getLiveNumber());
    	    connections.close();
    	    connection.addNewCliant(_outbound);
    	    return;
       }else{
    	   _connections.put(connections._live.getLiveNumber(), connections);
        }
       
       //コメントの取得を開始する
       connections.cliants.add(_outbound);
       connections.StartGetComment();
	}
	
	//ユーザの設定情報を保持するだけのクラス
	class UserConfig{
		public boolean configHtml=true; //タグを表示しない
		public boolean configHidden=false; //hiddenコメント無視
		public boolean configHb=false; //追い出し通知しない
		public boolean configBsp=true; //BSP等はアリーナのみ表示
	}
	
	/**
	 * 番組情報とそれに接続しているクライアントリストを保持するクラス
	 *
	 */
	class Connections{
		/** 同じ番組に接続しているクライアントリスト */
		public Set<WebSocket.Connection> cliants = new CopyOnWriteArraySet<WebSocket.Connection>();
		/** 放送の情報 */
		public NicoLive _live;
		/** コメ鯖との通信を受け取る */
		public NicoLiveCliant _liveCliant;
		
		/** コメントを取得するためのクラス */
		class NicoLiveCliant{
			public void OnConnect(){
				try {
					_live.AnotherCommentServerConnect();
					_live.addCommentHandler(new ReceiveHandler(_live, cliants));
				}catch (IOException e){
					System.out.println(e.toString());
			    	WebSocketChat.logger.warning(e.getMessage());
			    } catch (ClassNotFoundException e) {
			    	WebSocketChat.logger.warning(e.getMessage());
				} catch (SQLException e) {
			    	WebSocketChat.logger.warning(e.getMessage());
				}
			}
		}
		
		/** コメントを取得する処理を別スレッドで行う */
		public void StartGetComment(){
	       Thread thread = new Thread() {
		       @Override
		       public void run() {
		    	   _liveCliant = new NicoLiveCliant();
		    	   _liveCliant.OnConnect();
		       	}
	        };
	       thread.setDaemon(true);
	       thread.start();
		}
		
		/** コメントサーバと切断する */
		public void close(){
			this._live.close();
    	    this._live = null;
    	    this._liveCliant = null;
    	    this.cliants = null;
		}
		
		/** 接続中の放送に新しいクライアントを追加する	 */
		public void addNewCliant(WebSocket.Connection outbound){
			cliants.add(outbound);
			//接続完了通知
			try {
				outbound.sendMessage("{\"info\":" +
						"{" +
						"\"liveid\":\"" + _live.getLiveNumber() + "\"," +
						"\"title\":\"" + _live.getTitle() + "\"," +
						"\"isOfficial\":\"" + _live.getOfficial() + "\"" +
						"}}");
			} catch (IOException e) {
				WebSocketChat.logger.warning(e.getMessage());
			}
		}
	}


	private void setConfig(String request){
		String[] config = request.split(":");
		System.out.println(request);
		if(!config[0].equals("config")){
			//config設定でないのでなにもしない
			return;
		}
		String[] setting = config[1].split("=");
		if(setting[0].equals("html")){
			this._userConfig.configHtml = setting[1].equals("1") ? true : false;
		}else if(setting[0].equals("hidden")){
			this._userConfig.configHidden = setting[1].equals("1") ? true : false;
		}else if(setting[0].equals("hb")){
			this._userConfig.configHb = setting[1].equals("1") ? true : false;
		}else if(setting[0].equals("bsp")){
			this._userConfig.configBsp = setting[1].equals("1") ? true : false;
		}
	}


	/**
	 * コメント受信のハンドラークラスです。
	 * CommentDefaultHandler クラスを継承し、
	 * CommentDefaultHandler#commentReceived() 及び
	 * CommentDefaultHandler#commentAttrReceived() メソッドをオーバーライドすることにより、
	 * コメントの受信、及びコメントの属性(e.g. ユーザーIDやプレミアムかどうか)に対する処理を定義できます。
	 * CommentDefaultHandler クラス、または CommentHandler インターフェースを実装したクラスを
	 * NicoLive#addCommentHandler() メソッドの引数に指定することで、
	 * コメントを受信する度に commentReceived()、及び commentAttrReceived() が呼ばれます。
	 */
	class ReceiveHandler extends CommentDefaultHandler {
	    /** 出力コメント */
	    private String output = "";

	    /** コメント番号 */
	    private String number = null;

	    /** ユーザー ID */
	    private String userid = null;
	    
	    /** ユーザ名 */
	    private String userName = null;

	    /** コメント主の種類 */
		private String type = null;

	    /** コメントのコマンド */
	    private String command = null;
	    
	    /** コメント投稿時間 */
	    private int vpos;

	    /** ニコ生 */
	    private NicoLive live = null;	   
	    
	    /** 前回の送出時のコメント **/
	    private String prevMessage = "";
	    
	    /** 前回の送出時のユーザID **/
	    private String prevUserID = "";
	    
	    /** クライアントリスト */
	    private Set<WebSocket.Connection> cliants;
	    
	    /**
	     * 指定した NicoLive オブジェクトでハンドラークラスを作成します。
	     *
	     * @param live ニコ生
	     * @param config ユーザ設定
	     */
	    public ReceiveHandler(final NicoLive live, Set<WebSocket.Connection> cliants) throws IOException {
	    	this.live = live;
	    	this.cliants = cliants;
	    }

	    /**
	     * 受信したコメントに対する処理を行います。
	     * このメソッドは CommentDefaultHandler クラスのメソッドをオーバーライドしています。
	     *
	     * @param comment コメント
	     */		
	    @Override
	    public void commentReceived(String comment, RoomType roomType) {
	    	if(!type.equals("1") && !type.equals("0") && !type.equals("3") && !type.equals("6") && !type.equals("2") && !type.equals("7")){
	    		//System.out.println(type + ":【" + roomType.toString() + "】" + comment);
	    	}
	    	//送出コメントとユーザを記憶する
	    	//公式生の転送コメントの仕様変更により、
	    	//コマンドで判定できなくなったので、
	    	//コメント内容とユーザIDが前回と同じであれば送出しない
	    	if (prevMessage.equals(comment)
	    			 && prevUserID.equals(userid) && 
	    			 (roomType != RoomType.Arena && 
	    					 (type.equals(CommentHandler.Member.BASKSTAGEPASS) 
	    							 || type.equals(CommentHandler.Member.OFFICIAL))))
	    	{
	    		return;
	    	}
	    	prevMessage = comment;
	    	prevUserID = userid;
	    	
	    	//コメント部屋を判別
	    	String room = this.live.getRoomLabel();
	    	if(!live.getOfficial()){ //ユーザ生放送
		    	if(roomType == RoomType.Arena){
		    		room = "アリーナ";
		    	}else if(roomType == RoomType.StandA){
		    		room = "立ち見A";
		    	}else if(roomType == RoomType.StandB){
		    		room = "立ち見B";
		    	}else if(roomType == RoomType.StandC){
		    		room = "立ち見C";
		    	}
	    	}else{ //公式生放送(最前列無しの場合も)
	    		if(roomType == RoomType.ForwardArena){
	    			room = "ｱﾘｰﾅ最前";
	    		}else if(roomType == RoomType.FrontArena){
	    			room = "アリーナ";
	    		}else if(roomType == RoomType.BackArena){
	    			room = "裏ｱﾘｰﾅ";
	    		}else if(roomType == RoomType.Center1F){
	    			room = "1F中央";
	    		}else if(roomType == RoomType.Center2F){
	    			room = "2F中央";
	    		}else if(roomType == RoomType.Left1F){
	    			room = "1F左";
	    		}else if(roomType == RoomType.Left2F){
	    			room = "2F左";
	    		}else if(roomType == RoomType.Right1F){
	    			room = "1F右";
	    		}else if(roomType == RoomType.Right2F){
	    			room = "2F右";
	    		}else if(roomType == RoomType.ArenaA){
	    			room = "ArenaA";
	    		}else if(roomType == RoomType.ArenaB){
	    			room = "ArenaB";
	    		}else if(roomType == RoomType.ArenaC){
	    			room = "ArenaC";
	    		}
	    	}
	    	//投稿時刻を整形する
	    	String prognessTime;
	    	if(vpos >= 3600){
	    		prognessTime = String.format("%d:%02d:%02d", vpos/3600, vpos/60-vpos/3600*60, vpos%60);
	    	}else{
	    		prognessTime = String.format("%02d:%02d", vpos/60, vpos%60);
	    	}

	    	/**コマンドにhidden,NotTalkがあれば表示しない
	    	if(this.userConfig.configHidden){
	    		if(command.contains("hidden") || command.contains("NotTalk")){
	    			return;
	    		}
	    	}
	    	*/

	    	//バックステージパスはコメント部分だけ送信
	    	if(type.equals(CommentHandler.Member.BASKSTAGEPASS) ||
	    			type.equals(CommentHandler.Member.OFFICIAL)){
	    		Pattern regex1 = Pattern.compile("/press\\sshow\\s[a-z0-9]+\\s(.+)\\s@\\s(.+)");
	    		Matcher m1 = regex1.matcher(comment);
	    		if(m1.find()){
	    			comment = m1.group(1);
	    		}
	    	}

	    	//広告情報を整形する
	    	if(type.equals(CommentHandler.Member.OWNER)){
	    		Pattern regex1 = Pattern.compile("/koukoku\\sshow2\\s.+【広告設定されました】(.+)（クリックしてもっと見る）");
	    		Matcher m1 = regex1.matcher(comment);
	    		Pattern regex2 = Pattern.compile("/koukoku\\sshow2\\s.+【広告結果】(.+)");
	    		Matcher m2 = regex2.matcher(comment);

	    		if(m1.find()){
	    			comment = "【広告】" + m1.group(1);
	    		}else if(m2.find()){
	    			comment = "【広告】" + m2.group(1);
	    		}

	    	}

	    	//htmlタグを排除する
	    	/**
	    	if(this.userConfig.configHtml){
	    		Pattern regex1 = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>");
	    		Matcher m1 = regex1.matcher(comment);
	    		comment = m1.replaceAll("");
	    	}
	    	*/
	    	
	    	//エスケープ
	    	comment = sanitizing(comment);
	    	
	    	//userのjson組み立て
	    	String user;
        	if(userName != null){
        		user = "{\"user\":{\"userId\":\"" + userid + "\", \"name\":\"" + userName + "\"}}";
        	}else{
        		user = "{\"user\":{\"userId\":\"" + userid + "\", \"name\":\"" + "" + "\"}}";
        	}

	    	/* 出力形式にフォーマット(TODO:JSON形式で出力) */
	    	//output = String.format("%s‌  ‌%s‌  ‌%s‌  ‌%s‌  ‌%s", room, number, output, userid, type);
	    
	    	//JSONフォーマットに整形
	    	output = String.format("{\"room\":\"%s\",\"number\":\"%s\",\"comment\":\"%s\",\"userid\": %s,\"type\":\"%s\",\"vpos\":\"%s\"}", room, number, comment, user, type, prognessTime);
	    		    	
	    	//クライアントに送信する
	    	try
	    	{
	    		for(WebSocket.Connection connection : cliants){
	    			connection.sendMessage(output);
	    		}
	    	}
	    	catch(IOException e)
	    	{
	    		System.out.println(e.getMessage());
		    	WebSocketChat.logger.warning(e.getMessage());
	    	}
	    	//System.out.println(type);
	    	//System.out.println(output);
	    }
	    
	    private String sanitizing(String src){
	    	if(src == null){return null;}
    		src = src.replaceAll("&", "&amp;");
	    	src = src.replaceAll("<", "&lt;");
	    	src = src.replaceAll(">", "&gt;");
	    	src = src.replaceAll("\"", "&quot;");
	    	src = src.replaceAll("\'", "&#39;");
	    	src = src.replaceAll("\r\n", "\n").replaceAll("[\n|\r]", "<br>");
    		return src;
	    }

	    /**
	     * 受信したコメントの属性に対する処理を行います。
	     * これは commentReceived() メソッドの前に呼ばれます。
	     * このメソッドは CommentDefaultHandler クラスのメソッドをオーバーライドしています。
	     *
	     * @param attrMap 属性のマップ
	     * @throws SQLException 
	     * @throws ClassNotFoundException 
	     */
	    @Override
	    public void commentAttrReceived(Map<String, String> attrMap) throws ClassNotFoundException, SQLException {
	        /* コメ番の取得 */
	        number = attrMap.get(CommentHandler.NUMBER);

	        /* ユーザー ID の取得 */
	        userid = attrMap.get(CommentHandler.USER_ID);
	        
	        //ユーザIDから名前を取得する
	        if(!userid.equals("900000000") && !userid.equals("394")){
	        	userName = sanitizing(live.getNameFromUserId(userid));
	        }

	        /* コメントの種類を取得。isOwner のコメント参照 */
	        type = attrMap.get(CommentHandler.MEMBER_TYPE);

	        /* コメントのコマンドを取得 */
	        command = sanitizing(attrMap.get(CommentHandler.COMMAND));
	        
	        /* 公式運営コメントが誰なのかを取得 */
	        String adminName = attrMap.get(CommentHandler.NAME);

	        /* 運営システムからのコメントはコマンドが飛んでこないので対策 */
	        if (command == null) {
	            command = "";
	        }
	        
	        /* コメントの投稿時間を求める */
	        if(attrMap.get(CommentHandler.VPOS) != null){ //vposを送らないコメビュ対策(ry
	        	vpos = Integer.parseInt(attrMap.get(CommentHandler.VPOS)) / 100 - live.getStartTimeDelay();
	    	}

	    }
	    
	} // End of ReceiveHandler class.

}
