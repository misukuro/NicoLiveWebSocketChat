package webSocketChat;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketChat {

	final static Logger logger = Logger.getLogger("SampleLogging");
	
	public static void main(String[] args) throws Exception {
		new WebSocketChat();
	}

	public WebSocketChat() throws Exception {
		//サーバーの準備
		Server server = new Server(9090);
		ResourceHandler rh = new ResourceHandler();

		//jarファイルとeclipseとで実行時にhtmlのパスが違うので
		ClassLoader classLoader = this.getClass().getClassLoader();
		String htmlPass = 
			(classLoader.getResource("resources/html") == null) ? 
					classLoader.getResource("html").toExternalForm() : 
						classLoader.getResource("resources/html").toExternalForm();
					rh.setResourceBase(htmlPass);

		MyWebSocketServlet wss = new MyWebSocketServlet();
		ServletHolder sh = new ServletHolder(wss);
		ServletContextHandler sch = new ServletContextHandler();
		sch.addServlet(sh, "/ws/*");

		HandlerList hl = new HandlerList();
		hl.setHandlers(new Handler[] {rh, sch});
		server.setHandler(hl);

		server.start();
		server.join();
		
		//データベースに接続し、初期設定(テーブル等の作成)を行う
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
		String sql;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			//データファイルにアクセス(Windows環境なら\→/に直す)
			String dataFile = "jdbc:sqlite:" + new File(".").getAbsoluteFile().getParent() + "/chat.sqlite";
    		Pattern regex1 = Pattern.compile("\\\\");
    		Matcher m1 = regex1.matcher(dataFile);
    		dataFile = m1.replaceAll("/");
			connection= DriverManager.getConnection(dataFile);

			//テーブル確認
			statement = connection.prepareStatement("select name from sqlite_master where type = 'table';");
			result = statement.executeQuery();
			boolean hasUsersTable = false;
			boolean hasLiveTable = false;
			for(int i=1; result.next(); i++){
				if(result.getString(1).equals("users")){
					hasUsersTable = true;
				}
				if(result.getString(1).equals("live")){
					hasLiveTable = true;
				}
			}

			//持っていないならusersテーブルを作成する
			if(!hasUsersTable){
				sql = "create table users (userid varchar(32) not null," +
				"name varchar(64)," +
				"comunityid varchar(12)," +
				"primary key(userid))";
				statement = connection.prepareStatement(sql);
				int res = statement.executeUpdate();
				System.out.println("usersテーブル作成：" + res);
			}
			if(!hasLiveTable){
				sql = "create table live (liveid varchar(14) not null," +
				"roomid int(2) not null," +
				"seatblock int(2) not null," +
				"thread int(12) not null," +
				"port int(4) not null," +
				"address varchar(26) not null," + 
				"seatno int(6)," +
				"seat varchar(12)," +
				"primary key(liveid,roomid,seatblock))";
				statement = connection.prepareStatement(sql);
				int res = statement.executeUpdate();
				System.out.println("liveテーブル作成：" + res);
			}

			//DBを閉じる
			connection.close();

		}finally{ //念のため
			if(connection != null){
				connection.close();
				connection = null;
			}
			if(statement != null){
				statement.close();
				statement = null;
			}
			if(result != null){
				result.close();
				result = null;
			}
		}
		
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
		final InputStream inStream = WebSocketChat.class
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

	}

}
