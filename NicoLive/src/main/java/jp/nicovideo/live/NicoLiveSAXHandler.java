package jp.nicovideo.live;


import java.io.IOException;
import java.sql.SQLException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


class NicoLiveSAXHandler extends DefaultHandler {
    private static final int BUFSIZE = 256;

    private StringBuilder sbuf = new StringBuilder(BUFSIZE);

    public String innerText() {
        return sbuf.toString();
    }

    public void start(String uri,
                      String localName,
                      String qName,
                      Attributes attributes) throws SAXException {
        // please override.
    }

    @Override
    public final void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes) throws SAXException {
        start(uri, localName, qName, attributes);
    }

    public void end(String uri,
                    String localName,
                    String qName) throws SAXException, IOException, ClassNotFoundException, SQLException {
        // please override.
    }

    @Override
    public final void endElement(String uri,
                                 String localName,
                                 String qName) throws SAXException {
        try {
			end(uri, localName, qName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        sbuf = sbuf.delete(0, sbuf.length());
    }

    @Override
    public final void characters(char[] ch, int start, int length) {
        sbuf.append(ch, start, length);
    }
} // End of NicoLiveSAXHandler class.
