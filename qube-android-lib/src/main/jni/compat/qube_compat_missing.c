/*
 * qube_compat_missing.c
 * stubs for POSIX functions missing or not linked from Android NDK.
 */

#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <termios.h>
#include <unistd.h>
#include <sys/ioctl.h>


 // openpty not provided by Android Bionic (it lives in -lutil on Linux)
 // Implemented via POSIX /dev/ptmx.
int openpty(int *amaster, int *aslave, char *name,
            struct termios *termp, struct winsize *winp)
{
    int mfd, sfd;
    char *slave_name;
    mfd = open("/dev/ptmx", O_RDWR | O_NOCTTY | O_CLOEXEC);
    if (mfd < 0) return -1;
    if (grantpt(mfd) != 0 || unlockpt(mfd) != 0) { close(mfd); return -1; }
    slave_name = ptsname(mfd);
    if (!slave_name) { close(mfd); return -1; }
    sfd = open(slave_name, O_RDWR | O_NOCTTY | O_CLOEXEC);
    if (sfd < 0) { close(mfd); return -1; }
    if (termp) tcgetattr(sfd, termp);
    if (winp)  ioctl(sfd, TIOCSWINSZ, winp);
    if (name)  strcpy(name, slave_name);
    *amaster = mfd;
    *aslave  = sfd;
    return 0;
}

 // shm_open / shm_unlink: in Android API >= 26 only.
 // For API 21 targets emulate with a regular file under /data/local.
#if __ANDROID_API__ < 26

int shm_open(const char *name, int oflag, mode_t mode)
{
    char path[256];
    const char *n = name;
    while (*n == '/') n++;
    snprintf(path, sizeof(path), "/data/local/tmp/shm_%s", n);
    return open(path, oflag | O_CLOEXEC, mode);
}

int shm_unlink(const char *name)
{
    char path[256];
    const char *n = name;
    while (*n == '/') n++;
    snprintf(path, sizeof(path), "/data/local/tmp/shm_%s", n);
    return unlink(path);
}

#endif /* __ANDROID_API__ < 26 */

/*
 * g_libintl_* stubs — glib references these even with NLS disabled.
 * On Android we don't need translations so all functions are no-ops.
 */
#include <stddef.h>

const char *g_libintl_gettext(const char *msgid)
    { return msgid ? msgid : ""; }

const char *g_libintl_dgettext(const char *domain, const char *msgid)
    { (void)domain; return msgid ? msgid : ""; }

const char *g_libintl_dcgettext(const char *domain, const char *msgid, int category)
    { (void)domain; (void)category; return msgid ? msgid : ""; }

const char *g_libintl_dngettext(const char *domain,
                                 const char *msgid, const char *msgid_plural,
                                 unsigned long n)
    { (void)domain; return (n == 1) ? msgid : msgid_plural; }

const char *g_libintl_ngettext(const char *msgid, const char *msgid_plural,
                                unsigned long n)
    { return (n == 1) ? msgid : msgid_plural; }

const char *g_libintl_textdomain(const char *domain)
    { return domain; }

const char *g_libintl_bindtextdomain(const char *domain, const char *dir)
    { (void)domain; return dir; }

const char *g_libintl_bind_textdomain_codeset(const char *domain,
                                               const char *codeset)
    { (void)domain; return codeset; }
