package com.sjm.core.util.misc;

public final class Platform {
    public static enum OSType {
        UNKNOWN, MAC, LINUX, WINDOWS, SOLARIS, FREEBSD, OPENBSD, WINDOWSCE, AIX, ANDROID, GNU, KFREEBSD, NETBSD
    }

    public static final OSType OS_TYPE;
    public static final String RESOURCE_PREFIX;
    public static final String ARCH;

    static {
        OS_TYPE = getOSType();
        RESOURCE_PREFIX = getNativeLibraryResourcePrefix();
        ARCH = System.getProperty("os.arch").toLowerCase().trim();
    }

    public static final boolean isMac() {
        return OS_TYPE == OSType.MAC;
    }

    public static final boolean isAndroid() {
        return OS_TYPE == OSType.ANDROID;
    }

    public static final boolean isLinux() {
        return OS_TYPE == OSType.LINUX;
    }

    public static final boolean isWindows() {
        return OS_TYPE == OSType.WINDOWS || OS_TYPE == OSType.WINDOWSCE;
    }

    public static final boolean isX11() {
        return !isWindows() && !isMac();
    }

    public static final boolean is64Bit() {
        String model =
                System.getProperty("sun.arch.data.model", System.getProperty("com.ibm.vm.bitmode"));
        if (model != null)
            return "64".equals(model);
        if ("x86_64".equals(ARCH) || "ia64".equals(ARCH) || "ppc64".equals(ARCH)
                || "sparcv9".equals(ARCH) || "amd64".equals(ARCH))
            return true;
        else
            return System.getProperty("os.arch").indexOf("64") != -1;
    }

    public static final boolean isIntel() {
        return ARCH.equals("i386") || ARCH.startsWith("i686") || ARCH.equals("x86")
                || ARCH.equals("x86_64") || ARCH.equals("amd64");
    }

    public static final boolean isPPC() {
        return ARCH.equals("ppc") || ARCH.equals("ppc64") || ARCH.equals("powerpc")
                || ARCH.equals("powerpc64");
    }

    public static final boolean isARM() {
        return ARCH.startsWith("arm");
    }

    public static final boolean isSPARC() {
        return ARCH.startsWith("sparc");
    }

    private static OSType getOSType() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            if ("dalvik".equals(System.getProperty("java.vm.name").toLowerCase())) {
                return OSType.ANDROID;
            } else {
                return OSType.LINUX;
            }
        } else if (osName.startsWith("AIX"))
            return OSType.AIX;
        else if (osName.startsWith("Mac") || osName.startsWith("Darwin"))
            return OSType.MAC;
        else if (osName.startsWith("Windows CE"))
            return OSType.WINDOWSCE;
        else if (osName.startsWith("Windows"))
            return OSType.WINDOWS;
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS"))
            return OSType.SOLARIS;
        else if (osName.startsWith("FreeBSD"))
            return OSType.FREEBSD;
        else if (osName.startsWith("OpenBSD"))
            return OSType.OPENBSD;
        else if (osName.equalsIgnoreCase("gnu"))
            return OSType.GNU;
        else if (osName.equalsIgnoreCase("gnu/kfreebsd"))
            return OSType.KFREEBSD;
        else if (osName.equalsIgnoreCase("netbsd"))
            return OSType.NETBSD;
        else
            return OSType.UNKNOWN;
    }

    private static String getNativeLibraryResourcePrefix() {
        return getNativeLibraryResourcePrefix(getOSType(), System.getProperty("os.arch"),
                System.getProperty("os.name"));
    }

    private static String getNativeLibraryResourcePrefix(OSType osType, String arch, String name) {
        arch = arch.toLowerCase().trim();
        if ("powerpc".equals(arch))
            arch = "ppc";
        else if ("powerpc64".equals(arch))
            arch = "ppc64";
        else if ("i386".equals(arch))
            arch = "x86";
        else if ("x86_64".equals(arch) || "amd64".equals(arch))
            arch = "x86-64";
        String osPrefix;
        switch (osType) {
            case ANDROID:
                if (arch.startsWith("arm"))
                    arch = "arm";
                osPrefix = "android-" + arch;
                break;
            case WINDOWS:
                osPrefix = "win32-" + arch;
                break;
            case WINDOWSCE:
                osPrefix = "w32ce-" + arch;
                break;
            case MAC:
                osPrefix = "darwin";
                break;
            case LINUX:
                osPrefix = "linux-" + arch;
                break;
            case SOLARIS:
                osPrefix = "sunos-" + arch;
                break;
            case FREEBSD:
                osPrefix = "freebsd-" + arch;
                break;
            case OPENBSD:
                osPrefix = "openbsd-" + arch;
                break;
            case NETBSD:
                osPrefix = "netbsd-" + arch;
                break;
            case KFREEBSD:
                osPrefix = "kfreebsd-" + arch;
                break;
            case AIX:
            case GNU:
            default:
                osPrefix = name.toLowerCase();
                int space = osPrefix.indexOf(" ");
                if (space != -1)
                    osPrefix = osPrefix.substring(0, space);
                osPrefix = osPrefix + "-" + arch;
                break;
        }
        return osPrefix;
    }
}
