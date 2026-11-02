package com.wxiwei.office;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.wxiwei.office.constant.MainConstant;
import com.wxiwei.office.fc.doc.DOCReader;
import com.wxiwei.office.fc.doc.DOCXReader;
import com.wxiwei.office.fc.doc.TXTReader;
import com.wxiwei.office.fc.ppt.PPTReader;
import com.wxiwei.office.fc.ppt.PPTXReader;
import com.wxiwei.office.fc.xls.XLSReader;
import com.wxiwei.office.fc.xls.XLSXReader;

import org.junit.Test;

/**
 * Smoke test verifying the core office engine classes load, initialize,
 * and expose document format constants correctly.
 */
public class OfficeEngineSmokeTest {

    @Test
    public void officeFormats_areCorrectlyDefined() {
        assertEquals("doc", MainConstant.FILE_TYPE_DOC);
        assertEquals("docx", MainConstant.FILE_TYPE_DOCX);
        assertEquals("xls", MainConstant.FILE_TYPE_XLS);
        assertEquals("xlsx", MainConstant.FILE_TYPE_XLSX);
        assertEquals("ppt", MainConstant.FILE_TYPE_PPT);
        assertEquals("pptx", MainConstant.FILE_TYPE_PPTX);
        assertEquals("txt", MainConstant.FILE_TYPE_TXT);
    }

    @Test
    public void officeReaderClasses_canBeLoaded() {
        assertNotNull(DOCReader.class);
        assertNotNull(DOCXReader.class);
        assertNotNull(XLSReader.class);
        assertNotNull(XLSXReader.class);
        assertNotNull(PPTReader.class);
        assertNotNull(PPTXReader.class);
        assertNotNull(TXTReader.class);
    }
}
