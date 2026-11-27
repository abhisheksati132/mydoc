#include <fcntl.h>
#include <unistd.h>
#include <cstdio>
#include <cstdlib>

#include "archive_bridge.h"

int main(int argc, char *argv[]) {
    const char *pidPath = std::getenv("MYDOC_ARCHIVE_PID_FILE");
    if (pidPath != nullptr) {
        const int fd = open(pidPath, O_WRONLY | O_CREAT | O_EXCL | O_NOFOLLOW, 0600);
        if (fd < 0) return 2;
        char pid[32];
        const int size = std::snprintf(pid, sizeof(pid), "%d", getpid());
        const bool written = write(fd, pid, static_cast<size_t>(size)) == size;
        close(fd);
        if (!written) return 2;
    }
    return mydoc_engine_main(argc, argv);
}
