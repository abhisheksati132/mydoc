# Offline Office rendering fixtures

`fidelity.pptx` and `fidelity.xlsx` were generated for MyDoc with python-pptx and openpyxl. They contain original synthetic test content, a red square image, charts, tables and two slides/worksheets. No external links or macros are included.

The device tests exercise the actual LibreOffice worker and MuPDF page reader, checking text, page count, image/color pixels, password handling and cache reuse. These fixtures test representative content, not exhaustive Office compatibility.

`legacy.xls` was generated with xlwt. The protected PPTX/XLSX copies use msoffcrypto-tool encryption and the public fixture password `mydoc-test`.
