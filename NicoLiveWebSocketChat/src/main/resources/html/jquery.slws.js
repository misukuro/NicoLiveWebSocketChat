
function pluginLoaded(sender, args) {
    var slCtl = sender.getHost();

    window.WebSocketDraft = function (url) {
        this.slws = slCtl.Content.services.createObject("websocket");
        this.slws.Url = url;
        this.readyState = this.slws.ReadyState;
        var thisWs = this;
        this.slws.OnOpen = function (sender, args) {
            thisWs.readyState = thisWs.slws.ReadyState;
            if (thisWs.onopen) thisWs.onopen();
        };
        this.slws.OnData = function (sender, args) {
            if (thisWs.onmessage && args.TextData && args.IsFinal && !args.IsFragment)
                thisWs.onmessage({ data: String(args.TextData) });
        };
        this.slws.OnClose = function (sender, args) {
            thisWs.readyState = thisWs.slws.ReadyState;
            if (thisWs.onclose) thisWs.onclose();
        };
        this.slws.Open();
    };

    window.WebSocketDraft.prototype.send = function (message) {
        this.slws.SendMessage(message);
    };

    window.WebSocketDraft.prototype.close = function() {
        this.slws.Close();
    };

    $.slws._loaded = true;
    for (c in $.slws._callbacks) {
        $.slws._callbacks[c]();
    }
}

jQuery(function ($) {

    if (!$.slws) $.slws = {};
    else if (typeof ($.slws) != "object") {
        throw new Error("Cannot create jQuery.slws namespace: it already exists and is not an object.");
    }

    $.slws._callbacks = [];

    $.slws.ready = function (callback) {
        if (callback) {
            if ($.slws._loaded) {
                callback();
            }
            else {
                $.slws._callbacks.push(callback);
            }
        }
    }
});