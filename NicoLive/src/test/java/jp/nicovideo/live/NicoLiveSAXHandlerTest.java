package jp.nicovideo.live;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;


import org.junit.Test;
import org.junit.runner.JUnitCore;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class NicoLiveSAXHandlerTest {
    private static final String xml = "<root><elem>content</elem></root>";
    private InputStream in = new ByteArrayInputStream(xml.getBytes());

    @Test
    public void testNicoLiveSAXHandler() throws ParserConfigurationException, SAXException, IOException {
        SAXParser sax = SAXParserFactory.newInstance().newSAXParser();

        sax.parse(in, new NicoLiveSAXHandler() {
            @Override
            public void end(String uri, String localName, String qName) {
                if (qName.equals("elem")) {
                    assertThat(innerText(), is("content"));
                }
            }
        });
    }

    public static void main(String[] args) {
        JUnitCore.main(NicoLiveSAXHandlerTest.class.getName());
    }
}
