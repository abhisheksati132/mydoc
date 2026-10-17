package org.libreoffice.kit;

import java.nio.ByteBuffer;

public class Document {
    public static final int PART_MODE_SLIDE = 0;
    public static final int PART_MODE_NOTES = 1;

    public static final int DOCTYPE_TEXT = 0;
    public static final int DOCTYPE_SPREADSHEET = 1;
    public static final int DOCTYPE_PRESENTATION = 2;
    public static final int DOCTYPE_DRAWING = 3;
    public static final int DOCTYPE_OTHER = 4;

    private final ByteBuffer handle;

    public Document(ByteBuffer handle) {
        this.handle = handle;
    }

    public native void destroy();
    public native int getDocumentType();
    public native int getParts();
    public native int getPart();
    public native void setPart(int partIndex);
    public native String getPartName(int partIndex);
    public native void setPartMode(int partMode);
    public native long getDocumentWidth();
    public native long getDocumentHeight();
    private native void paintTileNative(ByteBuffer buffer, int canvasWidth, int canvasHeight, int tilePositionX, int tilePositionY, int tileWidth, int tileHeight);

    public void paintTile(ByteBuffer buffer, int canvasWidth, int canvasHeight, int tilePositionX, int tilePositionY, int tileWidth, int tileHeight) {
        paintTileNative(buffer, canvasWidth, canvasHeight, tilePositionX, tilePositionY, tileWidth, tileHeight);
    }
}
