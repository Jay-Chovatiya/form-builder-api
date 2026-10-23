package com.dynamicform.api.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class FileSecurityConstants {

    private FileSecurityConstants() {}

    public static final Set<String> DANGEROUS_EXTENSIONS;

    public static final Set<String> DANGEROUS_MIME_TYPES;

    static {
        Set<String> exts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        exts.addAll(Arrays.asList(
                ".exe", ".dll", ".bat", ".cmd", ".sh", ".bash", ".ps1", ".vbs", ".js",
                ".php", ".php3", ".php4", ".php5", ".phtml", ".asp", ".aspx", ".ashx",
                ".jsp", ".jspx", ".cgi", ".pl", ".py", ".rb", ".jar", ".war", ".ear",
                ".msi", ".scr", ".pif", ".com", ".hta", ".cpl", ".reg", ".cer",
                ".app", ".bin", ".gadget", ".wsf", ".vbe", ".jse", ".action"
        ));
        DANGEROUS_EXTENSIONS = Collections.unmodifiableSet(exts);

        Set<String> mimes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        mimes.addAll(Arrays.asList(
                "application/x-msdownload",
                "application/x-msdos-program",
                "application/x-executable",
                "application/x-sh",
                "application/x-csh",
                "application/x-php",
                "application/x-httpd-php",
                "application/x-bat",
                "application/x-powershell",
                "text/javascript",
                "application/javascript",
                "application/x-javascript",
                "text/html",
                "application/xhtml+xml",
                "text/x-python",
                "application/x-python-code"
        ));
        DANGEROUS_MIME_TYPES = Collections.unmodifiableSet(mimes);
    }
}
