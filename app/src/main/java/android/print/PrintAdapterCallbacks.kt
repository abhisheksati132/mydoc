package android.print

/**
 * Openable subclasses of the print framework's two result callbacks.
 *
 * PrintDocumentAdapter.LayoutResultCallback and WriteResultCallback are public abstract classes
 * with package-private constructors. Declaring these two here in android.print puts them in the same
 * package, enabling DocxWebRenderer to drive a PrintDocumentAdapter directly into a PDF file
 * without user interaction or print dialogs.
 */
abstract class OpenLayoutResultCallback : PrintDocumentAdapter.LayoutResultCallback()

/** @see OpenLayoutResultCallback */
abstract class OpenWriteResultCallback : PrintDocumentAdapter.WriteResultCallback()
