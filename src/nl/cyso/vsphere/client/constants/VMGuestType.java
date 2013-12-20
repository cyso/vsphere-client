package nl.cyso.vsphere.client.constants;

public enum VMGuestType {
	// @formatter:off
	windows8Server64Guest("Microsoft Windows Server 2012 (64-bit)"),
	windows7Server64Guest("Microsoft Windows Server 2008 R2 (64-bit)"),
	winLonghorn64Guest("Microsoft Windows Server 2008 (64-bit)"),
	winLonghornGuest("Microsoft Windows Server 2008 (32-bit)"),
	winNetEnterprise64Guest("Microsoft Windows Server 2003 (64-bit)"),
	winNetEnterpriseGuest("Microsoft Windows Server 2003 (32-bit)"),
	winNetDatacenter64Guest("Microsoft Windows Server 2003 Datacenter (64-bit)"),
	winNetDatacenterGuest("Microsoft Windows Server 2003 Datacenter (32-bit)"),
	winNetStandard64Guest("Microsoft Windows Server 2003 Standard (64-bit)"),
	winNetStandardGuest("Microsoft Windows Server 2003 Standard (32-bit)"),
	winNetWebGuest("Microsoft Windows Server 2003 Web Edition (32-bit)"),
	windows8_64Guest("Microsoft Windows 8 (64-bit)"),
	windows8Guest("Microsoft Windows 8 (32-bit)"),
	windows7_64Guest("Microsoft Windows 7 (64-bit)"),
	windows7Guest("Microsoft Windows 7 (32-bit)"),
	winXPPro64Guest("Microsoft Windows XP Professional (64-bit)"),
	winXPProGuest("Microsoft Windows XP Professional (32-bit)"),
	rhel6_64Guest("Red Hat Enterprise Linux 6 (64-bit)"),
	rhel6Guest("Red Hat Enterprise Linux 6 (32-bit)"),
	sles11_64Guest("SUSE Linux Enterprise 11 (64-bit)"),
	sles11Guest("SUSE Linux Enterprise 11 (32-bit)"),
	centos64Guest("CentOS 4/5/6 (64-bit)"),
	centosGuest("CentOS 4/5/6 (32-bit)"),
	debian6_64Guest("Debian GNU/Linux 6 (64-bit)"),
	debian6Guest("Debian GNU/Linux 6 (32-bit)"),
	oracleLinux64Guest("Oracle Linux 4/5/6 (64-bit)"),
	oracleLinuxGuest("Oracle Linux 4/5/6 (32-bit)"),
	ubuntu64Guest("Ubuntu Linux (64-bit)"),
	ubuntuGuest("Ubuntu Linux (32-bit)"),
	otherGuest64("Other / Unknown Guest (64-bit)"),
	otherGuest("Other / Unknown Guest (32-bit)");
	// @formatter:on

	@SuppressWarnings("unused")
	private final String val;

	private VMGuestType(String val) {
		this.val = val;
	}
}
