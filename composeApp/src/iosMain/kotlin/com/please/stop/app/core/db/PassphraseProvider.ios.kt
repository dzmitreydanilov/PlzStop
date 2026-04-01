package com.please.stop.app.core.db

import com.please.stop.app.utils.toByteArray
import com.please.stop.app.utils.toNsData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
actual class PassphraseProvider {

    actual fun getOrCreate(): String {
        val existing = readFromKeychain()
        if (existing != null) return existing

        val passphrase = generateRandomPassphrase()
        saveToKeychain(passphrase)
        return passphrase
    }

    private fun readFromKeychain(): String? = memScoped {
        val query = keychainQuery(returning = true)
        try {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return null

            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            data.toByteArray().decodeToString()
        } finally {
            CFRelease(query)
        }
    }

    private fun saveToKeychain(passphrase: String) {
        val data = passphrase.encodeToByteArray().toNsData()

        val addQuery = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to ACCOUNT_NAME,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData to data,
        )

        try {
            val status = SecItemAdd(addQuery, null)
            if (status != errSecSuccess) {
                val updateQuery = keychainQuery(returning = false)
                val updateAttrs = cfDictionaryOf(kSecValueData to data)
                try {
                    SecItemUpdate(updateQuery, updateAttrs)
                } finally {
                    CFRelease(updateQuery)
                    CFRelease(updateAttrs)
                }
            }
        } finally {
            CFRelease(addQuery)
        }
    }

    private fun generateRandomPassphrase(): String {
        val bytes = ByteArray(PASSPHRASE_LENGTH)
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, bytes.size.toULong(), pinned.addressOf(0))
        }
        return Base64.encode(bytes)
    }

    private fun keychainQuery(returning: Boolean): CFDictionaryRef {
        val map = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to ACCOUNT_NAME,
        )
        if (returning) {
            map[kSecReturnData] = true
            map[kSecMatchLimit] = kSecMatchLimitOne
        }
        return cfDictionaryOf(map)
    }

    private fun cfDictionaryOf(vararg pairs: Pair<Any?, Any?>): CFDictionaryRef {
        return cfDictionaryOf(mapOf(*pairs))
    }

    private fun cfDictionaryOf(map: Map<Any?, Any?>): CFDictionaryRef {
        val nsDict = map as NSDictionary
        return CFBridgingRetain(nsDict)?.reinterpret()
            ?: error("Failed to bridge NSDictionary to CFDictionaryRef")
    }

    private companion object {
        const val SERVICE_NAME = "com.please.stop.app.db"
        const val ACCOUNT_NAME = "db_passphrase"
        const val PASSPHRASE_LENGTH = 32
    }
}
