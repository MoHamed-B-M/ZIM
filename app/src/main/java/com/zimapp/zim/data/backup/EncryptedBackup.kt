/*
 * Copyright (C) 2024 Vexzure
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.zimapp.zim.data.backup

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Adapted from EasyNotes ImportExportRepository: same AES/CBC/PKCS5Padding +
// PBKDF2WithHmacSHA256 envelope (salt ‖ iv ‖ cipher), but operating on ZIM's
// JSON export bytes instead of the raw DB file (no live-DB surgery).
object EncryptedBackup {
    private const val ITERATIONS = 65536

    private fun key(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), IvParameterSpec(iv))
        return salt + iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray, password: String): ByteArray {
        require(blob.size > 32) { "Not an encrypted backup" }
        val salt = blob.copyOfRange(0, 16)
        val iv = blob.copyOfRange(16, 32)
        val cipherText = blob.copyOfRange(32, blob.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), IvParameterSpec(iv))
        return cipher.doFinal(cipherText) // throws BadPaddingException on wrong password
    }
}
