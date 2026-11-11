#include <jni.h>
#include <errno.h>
#include <stddef.h>
#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <signal.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <sys/syscall.h>
#include <sys/resource.h>
#include <unistd.h>

#if defined(__aarch64__)
#define WORKER_ARCH AUDIT_ARCH_AARCH64
#elif defined(__arm__)
#define WORKER_ARCH AUDIT_ARCH_ARM
#elif defined(__x86_64__)
#define WORKER_ARCH AUDIT_ARCH_X86_64
#else
#error Unsupported Word worker architecture
#endif

/* Tighten the dedicated worker, never the reader process. New threads inherit this filter. */
JNIEXPORT jboolean JNICALL
Java_com_abhisheksati_mydoc_rendering_engine_word_WordEngineGuard_blockNetwork(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    // Ignore SIGXFSZ so writes over 256MB fail with EFBIG instead of killing process
    signal(SIGXFSZ, SIG_IGN);
    const struct rlimit file_limit = { .rlim_cur = 256L * 1024 * 1024, .rlim_max = 256L * 1024 * 1024 };
    if (setrlimit(RLIMIT_FSIZE, &file_limit) != 0) return JNI_FALSE;
    struct sock_filter rules[] = {
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, WORKER_ARCH, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_KILL_PROCESS),
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
#if defined(__x86_64__)
        BPF_JUMP(BPF_JMP | BPF_JGE | BPF_K, 0x40000000U, 0, 1),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM),
#endif
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_execve, 0, 1),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_execveat, 0, 1),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM),
        /* Block internet sockets (AF_INET, AF_INET6) while preserving AF_UNIX for liblog/system IPC */
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_socket, 0, 5),
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, args[0])),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AF_INET, 1, 0),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AF_INET6, 0, 1),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | EPERM),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW)
    };
    struct sock_fprog program = { .len = sizeof(rules) / sizeof(rules[0]), .filter = rules };
    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) return JNI_FALSE;
    return syscall(__NR_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_TSYNC, &program) == 0
        ? JNI_TRUE : JNI_FALSE;
}
