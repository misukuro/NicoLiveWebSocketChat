package webSocketChat;

import java.io.IOException;
import java.util.Set;
import java.util.Map;
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
	
	public String _liveId; //接続先の番組
	private WebSocket.Connection _outbound; //WebSocketの接続先
	private NicoLive _live; //ニコ生の接続情報を表す
	private NicoLiveCliant _liveCliant;
	private UserConfig _userConfig; //ユーザ設定情報
	
	/** ニコニコ動画アカウント */
	private String mail = "";
	private String pass = "websocket";
		
	/** 
	 * コンストラクタ
	 * @param request
	 */
	public MyWebSocket(HttpServletRequest request) {
		//クッキーからユーザ設定情報を読み取る
		_userConfig = new UserConfig();
		/*
		Cookie[] cookies = request.getCookies();
		Cookie cookie;
		if(cookies == null){return;}

		for (int i = 0; i < cookies.length; i++) {
			cookie = cookies[i];
			System.out.println(cookie.getName());
			//System.out.println(cookie.getValue());
			if (cookie.getName().equals("html")) {
				_userConfig.configHtml = cookie.getValue().equals("1") ? true : false;
				//System.out.println(_userConfig.configHtml + "html");
			}else if(cookie.getName().equals("hidden")){
				_userConfig.configHidden = cookie.getValue().equals("1") ? true : false;
				//System.out.println(_userConfig.configHidden + "hidden");
			}else if(cookie.getName().equals("hb")){
				_userConfig.configHb = cookie.getValue().equals("1") ? true : false;
				//System.out.println(_userConfig.configHb + "hb");
			}else if(cookie.getName().equals("bsp")){
				_userConfig.configBsp = cookie.getValue().equals("1") ? true : false;
				//System.out.println(_userConfig.configBsp + "bsp");
			}
		}
		*/
	}
	/*ユーザが接続
	public void onConnect(WebSocket.Connection outbound) {
		_outbound = outbound;	       	
	}
	*/
	@Override
	public void onClose(int arg0, String arg1) {
		System.out.println("closed");
		// TODO Auto-generated method stub
		if(_live != null){
			_live.close();
			_live = null;
		}
		_outbound = null;
		_liveCliant = null;
		_userConfig = null;
	}

	@Override
	public void onOpen(Connection arg0) {
		System.out.println("test");
		// TODO Auto-generated method stub
		_outbound = arg0;
	}
	
	@Override
	public void onMessage(String request) {
		// lv番号かどうか調べる
		String liveId=null;
		Pattern regex1 = Pattern.compile("((lv|co|ch)\\d+)");
  		Matcher m1 = regex1.matcher(request);
  		if(m1.find()){
  			liveId = m1.group(1);
  		}
  		//lv番号でなければconfig設定を行う
  		if(liveId == null){
  			setConfig(request);
  			return;
  		}
  		
		//既に接続中だったら切断する
		if(_live != null){
			_live.close();
			_live = null;
		}
		//コメントサーバに接続する
		try {
			_live = new NicoLive(liveId, mail, pass);
			//セッションが無効になっていたらもう一度
			if(_live.getErrorType() == ErrorType.NotLogin){
				_live = new NicoLive(liveId, mail, pass);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
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
					"\"liveid\":\"" + _live.getLiveNumber() + "\"," +
					"\"title\":\"" + _live.getTitle() + "\"," +
					"\"isOfficial\":\"" + _live.getOfficial() + "\"" +
					"}}");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//コンソールに放送情報を表示
		System.out.println("lv = " + _live.getLiveNumber());
       System.out.println("title = " + _live.getTitle());
       //System.out.println("level = " + _live.getLevel());
       System.out.println("total comment count = " + _live.getCommentCount());
       System.out.println("total watch count = " + _live.getTotalWatchCount());
       System.out.println("room label = " + _live.getRoomLabel());
       System.out.println("co No = " + _live.getCommunityNumber());
       System.out.println("seat No = " + _live.getSeetNo());
       
       //コメントを取得する処理を別スレッドで行う
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
	
	//コメントを取得するためのクラス
	class NicoLiveCliant{
		public void OnConnect(){
			try {
				_live.AnotherCommentServerConnect();
				_live.addCommentHandler(new ReceiveHandler(_live, _userConfig));
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
	
	//ユーザの設定情報を保持するだけのクラス
	class UserConfig{
		public boolean configHtml=true; //タグを表示しない
		public boolean configHidden=false; //hiddenコメント無視
		public boolean configHb=false; //追い出し通知しない
		public boolean configBsp=true; //BSP等はアリーナのみ表示
	}
	

	/*
	public void onDisconnect() {
		// TODO Auto-generated method stub
		if(_live != null){
			_live.close();
			_live = null;
		}
		_outbound = null;
		_liveCliant = null;
		_userConfig = null;
	}
	*/



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
	
	/*public void onMessage(byte frame, byte[] data, int offset, int length) {
		// TODO Auto-generated method stub
		_live.close();
	}
	*/


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
	    
	    /** ユーザ設定 */
	    private UserConfig userConfig = null;	   
	    /**
	     * 指定した NicoLive オブジェクトでハンドラークラスを作成します。
	     *
	     * @param live ニコ生
	     * @param config ユーザ設定
	     */
	    public ReceiveHandler(final NicoLive live, UserConfig config) throws IOException {
	    	this.live = live;
	    	this.userConfig = config;
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
	    	//アリーナ以外の運営コメント(3)・バックステージパス(6,7)・宣伝通知(2)を表示しない
	    	if(this.userConfig.configBsp){
	    		if(!RoomType.Arena.equals(roomType)){
	    			if(type.equals(CommentHandler.Member.OWNER) ||
	    					type.equals(CommentHandler.Member.BASKSTAGEPASS) ||
	    					type.equals(CommentHandler.Member.SYSTEM) ||
	    					type.equals(CommentHandler.Member.OFFICIAL)){
	    				return;
	    			}
	    		}
	    	}

	    	//コマンドにhidden,NotTalkがあれば表示しない
	    	if(this.userConfig.configHidden){
	    		if(command.contains("hidden") || command.contains("NotTalk")){
	    			return;
	    		}
	    	}

	    	//運営コマンドは表示しない
	    	if(this.userConfig.configHb && 
	    			(type.equals(CommentHandler.Member.OWNER) ||
	    			type.equals(CommentHandler.Member.BASKSTAGEPASS) ||
	    			type.equals(CommentHandler.Member.SYSTEM) ||
	    			type.equals(CommentHandler.Member.OFFICIAL)
	    			)){
	    		Pattern regex1 = Pattern.compile("^/[a-z0-9]+\\s+");
	    		Matcher m1 = regex1.matcher(comment);
	    		if(m1.find()){
	    			return;
	    		}
	    	}

	    	//バックステージパスはコメント部分だけ送信
	    	if(type.equals(CommentHandler.Member.BASKSTAGEPASS) ||
	    			type.equals(CommentHandler.Member.OFFICIAL)){
	    		Pattern regex1 = Pattern.compile("/press\\sshow\\s[a-z0-9]+\\s(.+)\\s@\\s(.+)");
	    		Matcher m1 = regex1.matcher(comment);
	    		if(m1.find()){
	    			comment = m1.group(1);
	    			userid = m1.group(2);
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
	    	if(this.userConfig.configHtml){
	    		Pattern regex1 = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>");
	    		Matcher m1 = regex1.matcher(comment);
	    		comment = m1.replaceAll("");
	    	}
	    	
	    	//エスケープ
	    	comment = sanitizing(comment);

	    	/* 出力形式にフォーマット(TODO:JSON形式で出力) */
	    	//output = String.format("%s‌  ‌%s‌  ‌%s‌  ‌%s‌  ‌%s", room, number, output, userid, type);
	    
	    	//JSONフォーマットに整形
	    	output = String.format("{\"room\":\"%s\",\"number\":\"%s\",\"comment\":\"%s\",\"userid\": %s,\"type\":\"%s\",\"vpos\":\"%s\"}", room, number, comment, userid, type, prognessTime);
	    	
	    	
	    	//クライアントに送信する
	    	try
	    	{
	    		_outbound.sendMessage(output);
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
	        	if(userName != null){
	        		userName = "{\"user\":{\"userId\":\"" + userid + "\", \"name\":\"" + userName + "\"}}";
	        	}else{
	        		userName = "{\"user\":{\"userId\":\"" + userid + "\", \"name\":\"" + "" + "\"}}";
	        	}
	        }

	        /* コメントの種類を取得。isOwner のコメント参照 */
	        type = attrMap.get(CommentHandler.MEMBER_TYPE);

	        /* コメントのコマンドを取得 */
	        command = sanitizing(attrMap.get(CommentHandler.COMMAND));
	        
	        /* 公式運営コメントが誰なのかを取得 */
	        String adminName = attrMap.get(CommentHandler.NAME);
	        
	        /* ユーザー ID を名前に差し替え */
	        if (userName != null) {
	            userid = userName;
	        }

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
