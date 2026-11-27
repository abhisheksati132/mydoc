# Independent RAR references

Decoded from libarchive test uuencoded fixtures; no archiver binary is bundled.

Upstream: https://github.com/libarchive/libarchive/tree/b4a3fb1a8bfc6812e465d9a2213ca07d8769ba4a/libarchive/test

- `rar4.rar`: `test_read_format_rar.rar.uu`; contains links and must be rejected by MyDoc's extraction policy.
  SHA256 `d421b86f6290aefad61b2a36737253b2b30fe27c156bd95abfc230f24fe0307e`.
- `rar5.rar`: `test_read_format_rar5_stored.rar.uu`; ordinary stored text reference.
  SHA256 `35d75e315d164d2e329afc28f7d844f013271b4fcffd4ddd78efcdd114a383a7`.

See `COPYING` for upstream copyright and redistribution terms.

Eight-part RAR5 reference, same pinned upstream revision. These contain test
executables used only as decompression data; tests never execute them.

- `test_read_format_rar5_multiarchive.part01.rar` SHA256 `2f3894dc4b8a1bba90830ef1f89f457b0295b6f9030c70febd57283721bc2ce3`.
- `test_read_format_rar5_multiarchive.part02.rar` SHA256 `0a5b7a8923beb5747d67f708f17bdc4ebfc8cf2c3010523332e1a1633d7a4c72`.
- `test_read_format_rar5_multiarchive.part03.rar` SHA256 `31699cbed68297245347911910743c3cb0c6075d28336a7dd4864d4019b8208a`.
- `test_read_format_rar5_multiarchive.part04.rar` SHA256 `c2d0d72b7c4dd229a944986eee95f802e487e4570e41f7ca7e77b9e82ca4119c`.
- `test_read_format_rar5_multiarchive.part05.rar` SHA256 `cc69c161ec00e2deeec263fb24f704d1f0d90ccc6b2aeb3baed2539878b5240d`.
- `test_read_format_rar5_multiarchive.part06.rar` SHA256 `86b7efeacaf2443564220c683eec0880b999dbfe27ae1bc2645f0fffe4646d8c`.
- `test_read_format_rar5_multiarchive.part07.rar` SHA256 `0b2c9bc7b5679069f9cbe68a9e08351868a3309bf24a5534d8207cf14d41b8b7`.
- `test_read_format_rar5_multiarchive.part08.rar` SHA256 `b86fa56607d4ee98e0374f84040a0e11ab18bc726a8b772151a1feee9c7045a3`.

`dot-prefix.tar` is a synthetic Python `tarfile` POSIX fixture with a `./` root
and `./folder/report.txt` containing `GNU-style path`; mtime is zero.
SHA256 `e48a03cbfd639cecbad5247abdbdb0fc5e7b907ab57b5be9a756f1f2fc776cbf`.
