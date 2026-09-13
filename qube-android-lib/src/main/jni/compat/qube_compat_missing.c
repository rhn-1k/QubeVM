/*
 * qube_compat_missing.c
 * stubs for POSIX functions missing or not linked from Android NDK.
 */


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
