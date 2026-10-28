package com.novanest.mpesatracker.security

import android.util.Base64
import com.novanest.mpesatracker.R
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.content.Context

object SecurityHelper {
    fun verifyCode(context: Context, code: String, deviceId: String): Long? {
        try {
            val parts = code.split(":")
            if (parts.size != 2) return null

            val expiryMillis = parts[0].toLong()
            val signatureBase64 = parts[1]
            val message = deviceId + "|" + expiryMillis

            val publicKeyPem = context.getString(R.string.public_key)
            val keySpec = X509EncodedKeySpec(Base64.decode(publicKeyPem, Base64.DEFAULT))
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(message.toByteArray())

            val padding = (4 - signatureBase64.length % 4) % 4
            val paddedBase64 = signatureBase64 + "=".repeat(padding)
            
            val isVerified = signature.verify(Base64.decode(paddedBase64, Base64.DEFAULT))

            if (isVerified) return expiryMillis
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
