package webSocketChat;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

//@SuppressWarnings("serial")
public class MyWebSocketServlet extends WebSocketServlet {

	private static final long serialVersionUID = 1L;

@Override
  public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) 
  {
	return new MyWebSocket(request);
  }


	
	@Override
	public boolean checkOrigin(HttpServletRequest request, String origin) {
		// TODO Auto-generated method stub
		return super.checkOrigin(request, origin);
	}



	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.service(request, response);
	}
  

  

}
