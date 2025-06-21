package com.example.minor_secure_programming.security

import android.content.res.Resources
import android.content.res.XmlResourceParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Tests for verifying network security configuration
 * These tests ensure that the app enforces secure network policies
 * including certificate pinning and cleartext traffic restrictions
 */
class NetworkSecurityTest {

    private lateinit var networkSecurityConfigXml: File
    private lateinit var xmlPullParser: XmlPullParser

    @Before
    fun setup() {
        // Get the path to the network security configuration file
        val projectDir = File("").absoluteFile
        networkSecurityConfigXml = File(projectDir, 
            "src/main/res/xml/network_security_config.xml")
        
        // Create an XML parser
        val factory = XmlPullParserFactory.newInstance()
        xmlPullParser = factory.newPullParser()
        xmlPullParser.setInput(FileInputStream(networkSecurityConfigXml), "UTF-8")
    }
    
    /**
     * Verify that the network security config file exists
     */
    @Test
    fun networkSecurityConfig_fileExists() {
        assertTrue("Network security config file should exist", networkSecurityConfigXml.exists())
    }
    
    /**
     * Test that the base config disallows cleartext traffic
     */
    @Test
    fun baseConfig_disallowsCleartextTraffic() {
        var found = false
        var cleartextPermitted = true
        
        // Parse XML to check base-config
        while (xmlPullParser.eventType != XmlPullParser.END_DOCUMENT) {
            if (xmlPullParser.eventType == XmlPullParser.START_TAG && 
                xmlPullParser.name == "base-config") {
                found = true
                val cleartextAttr = xmlPullParser.getAttributeValue(null, "cleartextTrafficPermitted")
                if (cleartextAttr != null) {
                    cleartextPermitted = cleartextAttr.toBoolean()
                }
                break
            }
            xmlPullParser.next()
        }
        
        assertTrue("Network security config should have base-config element", found)
        assertFalse("Base config should disallow cleartext traffic", cleartextPermitted)
    }
    
    /**
     * Test that certificate pinning is configured for production domain
     */
    @Test
    fun productionDomain_hasCertificatePinning() {
        // Reset parser
        xmlPullParser.setInput(FileInputStream(networkSecurityConfigXml), "UTF-8")
        
        var foundDomain = false
        var foundPinSet = false
        
        while (xmlPullParser.eventType != XmlPullParser.END_DOCUMENT) {
            if (xmlPullParser.eventType == XmlPullParser.START_TAG) {
                when (xmlPullParser.name) {
                    "domain" -> {
                        val domain = xmlPullParser.nextText()
                        if (domain == "minor-secure-programming-backend.onrender.com") {
                            foundDomain = true
                        }
                    }
                    "pin-set" -> {
                        foundPinSet = true
                    }
                }
            }
            xmlPullParser.next()
        }
        
        assertTrue("Config should include production domain", foundDomain)
        assertTrue("Production domain should have certificate pinning", foundPinSet)
    }
    
    /**
     * Test that cleartext is only allowed for development servers
     */
    @Test
    fun cleartextTraffic_onlyAllowedForDevServers() {
        // Reset parser
        xmlPullParser.setInput(FileInputStream(networkSecurityConfigXml), "UTF-8")
        
        val cleartextDomains = mutableListOf<String>()
        var insideCleartextConfig = false
        
        while (xmlPullParser.eventType != XmlPullParser.END_DOCUMENT) {
            if (xmlPullParser.eventType == XmlPullParser.START_TAG) {
                if (xmlPullParser.name == "domain-config") {
                    val cleartextAttr = xmlPullParser.getAttributeValue(null, "cleartextTrafficPermitted")
                    insideCleartextConfig = cleartextAttr == "true"
                } else if (xmlPullParser.name == "domain" && insideCleartextConfig) {
                    cleartextDomains.add(xmlPullParser.nextText())
                }
            } else if (xmlPullParser.eventType == XmlPullParser.END_TAG && 
                       xmlPullParser.name == "domain-config") {
                insideCleartextConfig = false
            }
            xmlPullParser.next()
        }
        
        // Only localhost/development IPs should allow cleartext
        assertTrue("Should allow cleartext for development server", 
                  cleartextDomains.contains("10.0.2.2"))
        assertEquals("Should only allow cleartext for development domains", 
                    1, cleartextDomains.size)
    }
}
