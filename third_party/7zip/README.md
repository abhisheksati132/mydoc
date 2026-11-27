# Official 7-Zip engine source

- Upstream: https://github.com/ip7z/7zip
- Version: 26.06, tag commit `d3296e39e1f1f92c7b41c0c3848e65640428d42d`
- Source: https://github.com/ip7z/7zip/releases/download/26.06/7z2606-src.tar.xz
- SHA-256: `a4e374c8f864f51df48c2cfd0e1413ec3b2ea030deae852d6a655402ebb61ac4`

The unmodified source distribution is included so builds do not download mutable
native code. `scripts/build-archive-engine.py` checks the digest before extraction
and builds the official standalone console bundle with the pinned Android NDK.
Upstream's Clang make configuration and warnings are retained. The build applies
documented Bionic type adjustments: widen `CFileInfoBase.nlink` to `UInt64` and
device/inode fields to `UInt64`, and its mode field to `UInt32` to retain `stat`
metadata on 32-bit Bionic as well. When setting permissions,
explicitly convert the POSIX mode bits to Bionic's narrower `mode_t` on 32-bit
Android. The MSLZ output-length calculation explicitly converts the pointer
difference before subtracting its unsigned window size on 32-bit builds.
Compiler diagnostics remain enabled.

The engine is packaged as a read-only installed native executable named
`libmydoc7z.so`; it is not loaded through JNI. Its only MyDoc entry shim writes
the child PID to an app-owned file for job control. Passwords travel through stdin,
not command arguments or persistent task metadata.

See LICENSE.txt and the source distribution's DOC directory for LGPL/BSD terms
and the unRAR restriction. RAR decompression is supported; RAR creation is not.
