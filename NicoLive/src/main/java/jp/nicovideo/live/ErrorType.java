package jp.nicovideo.live;

//ニコニコ生放送getplayerstatusのエラーコード
public enum ErrorType {
	Nothing,
	NotLogin,
	NotFound,
	Unknown,
	Maintenance,
	ServerError,
	Deleted,
	Closed,
	ParseError,
	Limitation,
	Full,
	Undefined,
	PermissionDenied,
	CommunityOnly,
	AccessLocked,
	Commingsoon
}
