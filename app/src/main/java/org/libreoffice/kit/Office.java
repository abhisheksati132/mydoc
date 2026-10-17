package org.libreoffice.kit;

import java.nio.ByteBuffer;

public class Office {
    private final ByteBuffer handle;

    public Office(ByteBuffer handle) {
        this.handle = handle;
    }

    public native void destroy();
    public native String getError();
    private native ByteBuffer documentLoadNative(String url);

    public Document documentLoad(String url) {
        ByteBuffer documentHandle = documentLoadNative(url);
        if (documentHandle == null) {
            return null;
        }
        return new Document(documentHandle);
    }
}
