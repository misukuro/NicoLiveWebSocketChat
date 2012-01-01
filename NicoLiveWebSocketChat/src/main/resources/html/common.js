
var ws; //WebSocket
var count=0; //受信したコメント数
var status=0; //サーバとの接続状態(1=接続,0=切断)
var configHtml = 1; //htmlタグを除去するか
var configHidden = 1; //hiddenコマンドのコメントを表示するか
var configHb = 1; //運営コマンドを出すかどうか
var configBsp = 1; //運営コメ・BSPはアリーナだけ表示する
var configSubName = 1; //長いユーザネームを省略する
var isOfficial = 0; //公式生放送かどうか
var configSubComment = 1; //60文字以上のコメントを省略する
var successConnect = 0; //websocket接続が確立できたかどうか

//WebSocketが接続された時に，「send」ボタンが有効になる
function onOpenWebSocket(){
  successConnect = 1;
  $("#send")[0].addEventListener("click",sendMessage,false);
  status=1;
  printMessage("サーバに接続しました。");
  configInitial();
}

//WebSocketが切断された時に，「send」ボタンを無効にする
function onCloseWebSocket(){
  //$("#send")[0].removeEventListener("click",sendMessage,false);
  status=0;
  if(successConnect){
	  printMessage("サーバとの接続が切断されました。");
  }else{
	  printMessage("サーバとの接続が切断されました。プロキシを使っている場合(学校、会社含む)は接続できません。お問い合わせは<a href=\"https://twitter.com/#!/katoken12\" target=\"_blank\">@katoken12</a>まで");
  }
}

//接続先よりメッセージを受信した時に，空文字でなければ画面に表示する
function onMessageWebSocket(event){
  var msg=event.data;
  if(msg==""){return;}
  dispMessage(msg);
}

//ウィンドウを閉じたり画面遷移した時にWebSokcetを切断する
function onUnload(){
  ws.close();
}

function unSanitizing(src){
	src = src.replace(/&amp;/g, "&");
	src = src.replace(/&lt;/g, "<");
	src = src.replace(/&gt;/g, ">");
	src = src.replace(/&quot;/g, "\"");
	src = src.replace(/&#39;/g, "'");
	src = src.replace(/<br>/g, "\r\n");
	return src;
}

//画面にメッセージを表示する
//上に表示されるメッセージが最新となる
function dispMessage(msg){
	msg = $.parseJSON(msg);
	//var data = msg.split("‌  ‌");
	if(msg.info != null){
		if(msg.info.error != null){
			printMessage(msg.info.error);
			return;
		}
		printMessage(msg.info.liveid + "に接続しました");
		$('title').text(msg.info.title + " -ニコ生コメントビューワ(仮)");
		/*
		 * 公式放送のときはNoカラムを削除したかった
		if(msg.info.isOfficial == "true"){
			isOfficial = 1;
			if($("#tableHeader")[0].childNodes.length == 5){
				$("#tableHeader")[0].removeChild(2);
			}
			alert($("#tableHeader")[0].childNodes.length);
		}else{
			isOfficial = 0;
			if($("#tableHeader")[0].childNodes.length == 4){
				var row = document.createElement("th");
				var cellText = document.createTextNode("No");
				row.appendChild(cellText);
				$("#tableHeader")[0].insertBefore(row,1);
			}
		}
		*/
		return;
	}
	//エスケープ
	msg.userid.user.name = unSanitizing(msg.userid.user.name);
	msg.comment = unSanitizing(msg.comment);
	if(msg.number == "null"){
		msg.number = "";
	}
	
	//テーブルにコメント情報を表示する
	var row = document.createElement("tr");
	//部屋名
	var cell = document.createElement("td");
	var cellText = document.createTextNode(msg.room);
	cell.appendChild(cellText);
	row.appendChild(cell);
	//部屋ごとに背景色を色分けする
	if(msg.room == "アリーナ" || msg.room == "ArenaB"){
		row.setAttribute("id", "waterblue");
	}else if(msg.room == "立ち見A" || msg.room == "ｱﾘｰﾅ最前" || msg.room == "ArenaA"){
		row.setAttribute("id", "green");
	}else if(msg.room == "立ち見B" || msg.room == "ArenaC"){
		row.setAttribute("id", "yellow");
	}else if(msg.room == "立ち見C" || msg.room == "裏ｱﾘｰﾅ"){
		row.setAttribute("id", "pink")
	}
		
	//コメ番
	cell = document.createElement("td");
	cellText = document.createTextNode(msg.number);
	cell.appendChild(cellText);
	row.appendChild(cell);
	//コメント
	
	cell = document.createElement("td"); //メッセージ欄には半角英数字の改行を許可する
	cell.setAttribute("id", "message");
	//長いコメントは省略する
	if(msg.comment.length > 60 && configSubComment == 1){
		msg.comment = msg.comment.substring(0,60) + "...";
	}
	if(msg.type == 2 || msg.type == 3){ //運営コメント
		var span = document.createElement("span");
		span.setAttribute("id","orange");
		spanText = document.createTextNode(msg.comment);
		span.appendChild(spanText);
		cell.appendChild(span);		
	}else if(msg.type == 6 || msg.type == 7){ //バックステージパス
		var span = document.createElement("span");
		span.setAttribute("id","blue");
		spanText = document.createTextNode(msg.comment);
		span.appendChild(spanText);
		cell.appendChild(span);	
	}else{
		cellText = document.createTextNode(msg.comment);
		cell.appendChild(cellText);
	}
	var cellText =  cell.innerHTML;
	cell.innerHTML = getLinkString(cellText);
	row.appendChild(cell);
	//vpos
	cell = document.createElement("td");
	cellText = document.createTextNode(msg.vpos);
	cell.appendChild(cellText);
	row.appendChild(cell);
	//ユーザID
	cell = document.createElement("td");
	//長いユーザ名は省略する
	if(msg.userid.user.name){
		var link = document.createElement("a");
		link.setAttribute("target", "_blank");
		link.setAttribute("href", "http://www.nicovideo.jp/user/" + msg.userid.user.userId);
		link.setAttribute("name", msg.userid.user.name);
		cellText = document.createTextNode(msg.userid.user.name);
		link.appendChild(cellText);
		cell.appendChild(link);
	}else{
		if(msg.userid.user.userId.length > 10 && configSubName == 1){
			msg.userid.user.userId = msg.userid.user.userId.substring(0,9) + "...";
		}
		cellText = document.createTextNode(msg.userid.user.userId);
		cell.appendChild(cellText);
	}
	row.appendChild(cell);
		
	if($("#messagein")[0].hasChildNodes()){
		$("#messagein")[0].insertBefore(row,$("#messagein")[0].childNodes.item(0));
	}else{
		$("#messagein")[0].appendChild(row);
	}
	
	//コメント数
	count++;
	//1000コメント以上受信したら
	while(count >= 1000){
		if($("#messagein")[0].hasChildNodes()){
			$("#messagein")[0].removeChild($("#messagein")[0].lastChild);
			count--;
		}
	}

}

//メッセージ入力欄が空白でなければメッセージを送信する
function sendMessage(){
  //接続していなければ再接続
  if(status == 0){initial();}
  //接続先番組IDを取得
  var liveId = getLiveId();
  if(liveId == null){return;}
  ws.send(liveId);
  $("#liveId")[0].value = liveId;
  printMessage(liveId + "への接続を開始します");
  //子ノードを全て削除
  while($("#messagein")[0].hasChildNodes()){
	  $("#messagein")[0].removeChild($("#messagein")[0].lastChild);
  }
  count = 0;
}

//NG設定を鯖に送る
function sendConfig(msg){
	ws.send("config:"+msg);
}

//入力された文字列から接続先番組を取得する
function getLiveId(){
	var url = $("#liveId")[0].value;
	if(url==""){printMessage("番組IDを入力してください。");return null;}
	regObj = new RegExp(/((lv|co|ch)\d+)/);
	var liveId = url.match(regObj);
	if(liveId[0]){
		return liveId[0];
	}
	else
	{
		printMessage("正しいliveIdを入力してください。");
		return null;
	}
}

//指定された文字列にURLが含まれていればリンクにして返します
function getLinkString(str){
	var message = str.replace(/(https?:\/\/[-_.!~*'()a-zA-Z0-9;\/?:\@&=+\$,%#]+)/, '<a href="$1" target="_blank">$1</a>');
	message = message.replace(/(sm[0-9]+)/, '<a href="http://www.nicovideo.jp/watch/$1" target="_blank">$1</a>');
	message = message.replace(/(nm[0-9]+)/, '<a href="http://www.nicovideo.jp/watch/$1" target="_blank">$1</a>');
	message = message.replace(/(co[0-9]+)/, '<a href="http://com.nicovideo.jp/community/$1" target="_blank">$1</a>');
	message = message.replace(/(lv[0-9]+)/, '<a href="http://live.nicovideo.jp/watch/$1" target="_blank">$1</a>');
	message = message.replace(/(im[0-9]+)/, '<a href="http://seiga.nicovideo.jp/seiga/$1" target="_blank">$1</a>');

	return message;
}

//お知らせを表示する
function printMessage(message){
	$("#footer")[0].innerHTML = message;
}

//コンテキストメニュー
$(function() {
	$('#setting').contextMenu('settingMenu',
			{
		bindings: {
			'html': function(t) {
				configHtml = 1 - configHtml;
				$.cookie('html', configHtml ,{ expires: 365 });
				sendConfig("html=" + configHtml);
				if(configHtml){
					$('#html')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">HTMLタグを表示しない";
				}else{
					$('#html')[0].innerHTML = "HTMLタグを表示しない";
				}
			},
			'hidden': function(t) {
				configHidden = 1 - configHidden;
				$.cookie('hidden', configHidden ,{ expires: 365 });
				sendConfig("hidden=" + configHidden);
				if(configHidden){
					$('#hidden')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">hidden,NotTalkコマンドの運営コメントを表示しない";
				}else{
					$('#hidden')[0].innerHTML = "hidden,NotTalkコマンドの運営コメントを表示しない";
				}
			},
			'hb': function(t) {
				configHb = 1 - configHb;
				$.cookie('hb', configHb ,{ expires: 365 });
				sendConfig("hb=" + configHb);
				if(configHb){
					$('#hb')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">運営コマンドを表示しない";
				}else{
					$('#hb')[0].innerHTML = "運営コマンドを表示しない";
				}
			},
			'bsp': function(t) {
				configBsp = 1 - configBsp;
				$.cookie('bsp', configBsp ,{ expires: 365 });
				sendConfig("bsp=" + configBsp);
				if(configBsp){
					$('#bsp')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">アリーナ以外の運営コメント・BSPを表示しない";
				}else{
					$('#bsp')[0].innerHTML = "アリーナ以外の運営コメント・BSPを表示しない";
				}
			},
			'subName': function(t) {
				configSubName = 1 - configSubName;
				$.cookie('subName', configSubName ,{ expires: 365 });
				//sendConfig("bsp=" + configBsp);
				if(configSubName){
					$('#subName')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">長いUserIDを省略する";
				}else{
					$('#subName')[0].innerHTML = "長いUserIDを省略する";
				}
			},
			'subComment': function(t) {
				configSubComment = 1 - configSubComment;
				$.cookie('subComment', configSubComment ,{ expires: 365 });
				//sendConfig("bsp=" + configBsp);
				if(configSubComment){
					$('#subComment')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">60文字以上のコメントを省略する";
				}else{
					$('#subComment')[0].innerHTML = "60文字以上のコメントを省略する";
				}
			}
		}
			});
});

//放送URLクリックで全選択状態にする
$(document).ready(function(){
	$('#liveId').focus(function(){
		$(this).select();
	});
	//サーバに接続する
	initial();
});

//初期化処理
function initial(){
	try{
	  //HTTPSで接続されている場合，WebSocketもセキュアにする
	  var protocol=(location.protocol=="https:")?"wss":"ws";

	  //port番号も込みで取得
	  var host=location.host;
	  
	  //本番環境では、ポート番号を取得できないので付加する
	  var regObj = new RegExp(/:9090/);
	  var hasPort = host.match(regObj);
	  if(!hasPort){
		  host = host + ":9090"
	  }
	  
	  //接続先URLの組み立て
	  var url=protocol+"://"+host+"/ws/";
	 
	  
  //対応ブラウザかチェックする
  var ua = navigator.userAgent;
  if(!ua.match(/Chrome\/([\.\d]+)/) && !ua.match(/Safari/)){ // && !ua.match(/Firefox\/4/
	  printMessage("このコメビュはGoogleChrome、Safari、Firefox6以上で動作します。(IEは10以上で対応予定)");
	  //return;
	  url = "ws://" + host+  "/ws/";
	  ws=new MozWebSocket(url);
  }else{
	  //WebSocketのインスタンス化
	  ws=new WebSocket(url);
  }

  //WebSocketのイベントの登録
  //ws.onopen = onOpenWebSocket();
  //ws.onclose = onCloseWebSocket();
  //ws.onmessage = onMessageWebSocket();
  ws.addEventListener("open",onOpenWebSocket,false);
  ws.addEventListener("close",onCloseWebSocket,false);
  ws.addEventListener("message",onMessageWebSocket,false);
  //ウィンドウを閉じたり画面遷移した時にWebSokcetを切断する
  window.addEventListener("unload",onUnload,false);
  
  }catch(e){}
}

//オンロード時の処理
function load(){
	//クッキーの読み込みを行う
	if($.cookie('html') != null){
		configHtml = $.cookie('html'); //htmlタグを除去するか
	}
	if(configHtml == 1){
		$('#html')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">HTMLタグを表示しない";
	}
	
	//hidden,NotTalk
	if($.cookie('hidden') != null){
		configHidden = $.cookie('hidden'); //hiddenコマンドのコメントを表示するか\
	}
	if(configHidden == 1){
		$('#hidden')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">hidden,NotTalkコマンドの運営コメントを表示しない";
	}
	//追い出し通知
	if($.cookie('hb') != null){
		configHb = $.cookie('hb'); //追い出し通知を出すかどうか
	}
	if(configHb == 1){
		$('#hb')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">運営コマンドを表示しない";
	}
	//アリーナ以外の全体コメ表示
	if($.cookie('bsp') != null){
		configBsp = $.cookie('bsp'); //運営コメ・BSPはアリーナだけ表示する
	}
	if(configBsp == 1){
		$('#bsp')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">アリーナ以外の運営コメント・BSPを表示しない";
	}
	//長い名前を省略
	if($.cookie('subName') != null){
		configSubName = $.cookie('subName'); //運営コメ・BSPはアリーナだけ表示する
	}
	if(configSubName == 1){
		$('#subName')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">長いUserIDを省略する";
	}	
	//６０文字以上のコメントを省略
	if($.cookie('subComment') != null){
		configSubComment = $.cookie('subComment'); //運営コメ・BSPはアリーナだけ表示する
	}
	if(configSubComment == 1){
		$('#subComment')[0].innerHTML = "<img src=\"checkbox/checkbox-5.gif\">60文字以上のコメントを省略する";
	}	

	//コメントクリック時のイベントリスナー
	$('tr').live('click', function(event){
		var str = event.currentTarget.children[0].childNodes[0].data + "【" +
					event.currentTarget.children[1].childNodes[0].data + "】　" +
					event.currentTarget.children[2].innerHTML + "　by " + 
					event.currentTarget.children[4].innerHTML;
		printMessage(str);
	});
	
}

//設定を送信する
function configInitial(){
	sendConfig("html=" + configHtml);
	sendConfig("hidden=" + configHidden);
	sendConfig("bsp=" + configBsp);
	sendConfig("hb=" + configHb);
}

//access anarysis
var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-18182691-4']);
_gaq.push(['_trackPageview']);


(function() {
	var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
	ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();

//イベントリスナー
function observe(target, type, listener) {
    if (target.addEventListener) target.addEventListener(type, listener, false);
    else if (target.attachEvent) target.attachEvent('on' + type, function() { listener.call(target, window.event); });
    else target['on' + type] = function(e) { listener.call(target, e || window.event); };
}

//オンロード時に接続する
observe(window, 'load', load);
//window.addEventListener("load",load,false);
