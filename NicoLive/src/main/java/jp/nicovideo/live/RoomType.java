package jp.nicovideo.live;

//部屋の名前を表します
public enum RoomType {
	/* ユーザ生放送 */
	/** 
	 * アリーナ
	 */
	Arena(30),
	
	/** 
	 * 立ち見A
	 */
	StandA(31),
	
	/** 
	 * 立ち見B(
	 */
	StandB(32),
	
	/** 
	 * 立ち見C
	 */
	StandC(33),
	
	/*公式生放送(最前列なし) */
	/** 
	 * 1F中央
	 */
	Center1F(21),
	
	/** 
	 * 1F左
	 */
	Left1F(22),
	/** 
	 * 1F右
	 */
	Right1F(23),
	/** 
	 * 2F中央
	 */
	Center2F(24),
	/** 
	 * 2F左
	 */
	Left2F(25),
	/** 
	 * 2F右
	 */
	Right2F(26),
	
	/* ニコファーレ */
	/**
	 * アリーナAブロック
	 */
	ArenaA(60),
	/**
	 * アリーナBブロック
	 */
	ArenaB(61),
	/**
	 * アリーナCブロック
	 */
	ArenaC(62),
	/**
	 * アリーナDブロック
	 */
	ArenaD(63),
	/**
	 * アリーナEブロック
	 */
	ArenaE(64),
	/**
	 * スタンドAブロック
	 */
	StandABlock(65),
	/**
	 * スタンドBブロック
	 */
	StandBBlock(66),
	/**
	 * スタンドCブロック
	 */
	StandCBlock(67),
	/**
	 * スタンドDブロック
	 */
	StandDBlock(68),
	/**
	 * スタンドEブロック
	 */
	StandEBlock(69),
	/**
	 * スタンドGブロック
	 */
	StandGBlock(70),

	
	/* 公式生放送 */
	/** 
	 * アリーナ最前列(roomid:1)
	 */
	ForwardArena(1),
	
	/** 
	 * アリーナ(前)(roomid:2)
	 */
	FrontArena(2),
	
	/** 
	 * アリーナ裏(roomid:3)
	 */
	BackArena(3),
	
	/**
	 * 1F中央最前列
	 */
	Forward1FCenter(4),
	
	/** 
	 * 1F中央前方
	 */
	Front1FCenter(5),
	
	/** 
	 * 1F中央後方
	 */
	Back1FCenter(6),
	
	/** 
	 * 1F右前方
	 */
	Front1FRight(7),
	
	/** 
	 * 1F右後方
	 */
	Back1FRight(8),
	
	/** 
	 * 1F左前方
	 */
	Front1FLeft(9),
	
	/** 
	 * 1F左後方
	 */
	Back1FLeft(10),
	
	/** 
	 * 2F中央最前列
	 */
	Forward2FCenter(11),
	
	/** 
	 * 2F中央前方
	 */
	Front2FCenter(12),
	
	/** 
	 * 2F右Aブロック
	 */
	A2FRight(13),
	
	/** 
	 * 2F右Bブロック
	 */
	B2FRight(14),
	
	/** 
	 * 2F右Cブロック
	 */
	C2FRight(15),
	
	/** 
	 * 2F右Dブロック
	 */
	D2FRight(16),
	
	/** 
	 * 2F左Aブロック
	 */
	A2FLeft(17),
	
	/** 
	 * 2F左Bブロック
	 */
	B2FLeft(18),
	
	/** 
	 * 2F左Cブロック
	 */
	C2FLeft(19),
	
	/** 
	 * 2F左Dブロック
	 */
	D2FLeft(20),
	
	/** 
	 * 立ち見席
	 */
	Stand(100),
	
	/**
	 * デッキ(クルーズ)
	 */
	Deck(40),
	
	/** 
	 * 不明
	 */
	Unknown(200);
	
	RoomType(final int anIntValue){
		intValue = anIntValue;
	}
	
	/**
	 * 現在のRoomTypeに対応する値を返します
	 * @return
	 */
	public int getIntValue(){
		return intValue;
	}
	
	/**
	 * 引数の値に対応するRoomTypeを設定する
	 * @param anIntValue
	 * @return
	 */
	public static RoomType valueOf(final int anIntValue){
		for(RoomType room : values()){
			if(room.getIntValue() == anIntValue){
				return room;
			}
		}
		return null;
	}
	
	RoomType(){
		
	}
	
	private int intValue;
}
