#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>

#ifndef RENAME_NOREPLACE
#define RENAME_NOREPLACE 1
#endif

static char *path_bytes(JNIEnv *env, jbyteArray bytes) {
    const jsize size = (*env)->GetArrayLength(env, bytes);
    if (size <= 0 || size > PATH_MAX) { errno = EINVAL; return NULL; }
    char *value = calloc((size_t)size + 1, 1);
    if (value == NULL) { errno = ENOMEM; return NULL; }
    (*env)->GetByteArrayRegion(env, bytes, 0, size, (jbyte *)value);
    if ((*env)->ExceptionCheck(env) || memchr(value, 0, (size_t)size) != NULL) {
        free(value); errno = EINVAL; return NULL;
    }
    return value;
}

static int directory_fd(const char *path) {
    const int fd = open(path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC);
    if (fd < 0) return -1;
    char descriptor[64];
    char actual[PATH_MAX + 1];
    snprintf(descriptor, sizeof(descriptor), "/proc/self/fd/%d", fd);
    const ssize_t count = readlink(descriptor, actual, PATH_MAX);
    if (count < 0) { const int error = errno; close(fd); errno = error; return -1; }
    actual[count] = 0;
    if (strcmp(actual, path) != 0) { close(fd); errno = EAGAIN; return -1; }
    return fd;
}

JNIEXPORT jint JNICALL Java_com_abhisheksati_mydoc_data_archive_ArchiveNativeFiles_nativeCreate(
        JNIEnv *env, jobject instance, jbyteArray path, jboolean directory) {
    (void)instance;
    char *value = path_bytes(env, path);
    if (value == NULL) return -errno;
    int result = -EINVAL;
    char *name = strrchr(value, '/');
    if (name == NULL || name[1] == 0) goto done;
    *name++ = 0;
    if (strcmp(name, ".") == 0 || strcmp(name, "..") == 0) goto done;
    const int parent = directory_fd(value[0] == 0 ? "/" : value);
    if (parent < 0) { result = -errno; goto done; }
    if (directory) {
        if (mkdirat(parent, name, 0700) != 0) result = -errno;
        else result = 0;
    } else {
        const int fd = openat(parent, name, O_WRONLY | O_CREAT | O_EXCL | O_NOFOLLOW | O_CLOEXEC, 0600);
        result = fd < 0 ? -errno : fd;
    }
    close(parent);
done:
    free(value);
    return result;
}

JNIEXPORT jint JNICALL Java_com_abhisheksati_mydoc_data_archive_ArchiveNativeFiles_nativeDelete(
        JNIEnv *env, jobject instance, jbyteArray path, jboolean directory) {
    (void)instance;
    char *value = path_bytes(env, path);
    if (value == NULL) return errno;
    int result = EINVAL;
    char *name = strrchr(value, '/');
    if (name == NULL || name[1] == 0) goto done;
    *name++ = 0;
    if (strcmp(name, ".") == 0 || strcmp(name, "..") == 0) goto done;
    const int parent = directory_fd(value[0] == 0 ? "/" : value);
    if (parent < 0) { result = errno; goto done; }
    result = unlinkat(parent, name, directory ? AT_REMOVEDIR : 0) == 0 ? 0 : errno;
    close(parent);
done:
    free(value);
    return result;
}

JNIEXPORT jint JNICALL Java_com_abhisheksati_mydoc_data_archive_ArchiveNativeFiles_nativeMove(
        JNIEnv *env, jobject instance, jbyteArray source, jbyteArray destination) {
    (void)instance;
    char *from = path_bytes(env, source);
    if (from == NULL) return errno;
    char *to = path_bytes(env, destination);
    if (to == NULL) { const int error = errno; free(from); return error; }
    int error = 0;
    int from_fd = -1;
    int to_fd = -1;
    char *from_name = strrchr(from, '/');
    char *to_name = strrchr(to, '/');
    if (from_name == NULL || to_name == NULL || from_name[1] == 0 || to_name[1] == 0) { error = EINVAL; goto done; }
    *from_name++ = 0;
    *to_name++ = 0;
    from_fd = directory_fd(from[0] == 0 ? "/" : from);
    if (from_fd < 0) { error = errno; goto done; }
    to_fd = directory_fd(to[0] == 0 ? "/" : to);
    if (to_fd < 0) { error = errno; goto done; }
    struct stat info;
    if (fstatat(from_fd, from_name, &info, AT_SYMLINK_NOFOLLOW) != 0) { error = errno; goto done; }
    if (!S_ISREG(info.st_mode) && !S_ISDIR(info.st_mode)) { error = ELOOP; goto done; }
#ifdef SYS_renameat2
    if (syscall(SYS_renameat2, from_fd, from_name, to_fd, to_name, RENAME_NOREPLACE) != 0) error = errno;
#else
    error = ENOSYS;
#endif
done:
    if (from_fd >= 0) close(from_fd);
    if (to_fd >= 0) close(to_fd);
    free(from);
    free(to);
    return error;
}
