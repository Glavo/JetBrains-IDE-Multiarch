<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dox="http://www.canonical.com/dbus/dox.dtd">
	<xsl:template match="*|@*">
		<xsl:copy>
			<xsl:apply-templates select="*|@*" />
		</xsl:copy>
	</xsl:template>
	<xsl:template match="@dox:*|dox:*"/>
	<xsl:template match="*">
		<xsl:element name="{local-name()}">
			<xsl:apply-templates select="@* | node()"/>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>

