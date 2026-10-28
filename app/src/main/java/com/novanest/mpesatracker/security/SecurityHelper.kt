package com.novanest.mpesatracker.security
import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object SecurityHelper {
    fun verifyCode(publicKeyBase64: String, code: String, deviceId: String): Long? {
        try {
            val parts = code.split(":")
            if (parts.size != 2) return null

            val expiryMillis = parts[0].toLong()
            val signatureBase64 = parts[1]
            
            // The message that was signed on the server: DeviceID|Expiry
            val message = deviceId + "|" + expiryMillis

            // Decode the public key
            val keySpec = X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.DEFAULT))
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            // Verify the signature
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(message.toByteArray())

            // Add back the base64 padding that we stripped when generating the code
            val padding = (4 - signatureBase64.length % 4) % 4
            val paddedBase64 = signatureBase64 + "=".repeat(padding)
            
            val isVerified = signature.verify(Base64.decode(paddedBase64, Base64.DEFAULT))
            
            if (isVerified) {
                return expiryMillis
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
