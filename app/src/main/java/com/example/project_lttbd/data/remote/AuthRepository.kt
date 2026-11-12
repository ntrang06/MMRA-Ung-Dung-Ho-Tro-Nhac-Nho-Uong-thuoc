package com.example.project_lttbd.data.remote

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object AuthRepository {

    private val auth: FirebaseAuth
        get() = FirebaseProvider.auth

    val currentUser
        get() = auth.currentUser

    val currentUid: String?
        get() = auth.currentUser?.uid

    // Kiểm tra tài khoản đã có phương thức email/mật khẩu chưa.
    val hasPasswordProvider: Boolean
        get() = currentUser?.providerData?.any {
            it.providerId == EmailAuthProvider.PROVIDER_ID
        } == true

    suspend fun signUp(
        email: String,
        pass: String
    ): AuthResult {
        return auth.createUserWithEmailAndPassword(
            email.trim(),
            pass
        ).await()
    }

    suspend fun signIn(
        email: String,
        pass: String
    ): AuthResult {
        return auth.signInWithEmailAndPassword(
            email.trim(),
            pass
        ).await()
    }

    suspend fun signInWithGoogle(
        idToken: String
    ): AuthResult {
        require(idToken.isNotBlank()) {
            "Chưa nhận được thông tin xác thực Google."
        }

        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        return auth.signInWithCredential(credential).await()
    }

    // Thêm mật khẩu cho tài khoản Google hoặc đổi mật khẩu đã có.
    suspend fun setAccountPassword(
        currentPassword: String,
        newPassword: String
    ) {
        require(
            newPassword.length >= 8 &&
                    newPassword.any { it.isLetter() } &&
                    newPassword.any { it.isDigit() }
        ) {
            "Mật khẩu mới cần ít nhất 8 ký tự, gồm chữ và số."
        }

        val originalUser = auth.currentUser
            ?: error("Vui lòng đăng nhập trước khi đặt mật khẩu.")

        val uid = originalUser.uid

        // Tải lại thông tin tài khoản từ Firebase.
        originalUser.reload().await()

        val user = auth.currentUser
            ?: error("Phiên đăng nhập đã kết thúc.")

        check(user.uid == uid) {
            "Tài khoản đã thay đổi. Vui lòng mở lại màn hình."
        }

        val email = user.email?.takeIf {
            it.isNotBlank()
        } ?: error("Tài khoản này chưa có email.")

        val passwordLinked = user.providerData.any {
            it.providerId == EmailAuthProvider.PROVIDER_ID
        }

        if (passwordLinked) {
            // Đã có mật khẩu: xác thực lại trước khi đổi.
            require(currentPassword.isNotBlank()) {
                "Vui lòng nhập mật khẩu hiện tại."
            }

            val credential = EmailAuthProvider.getCredential(
                email,
                currentPassword
            )

            user.reauthenticate(credential).await()

            check(auth.currentUser?.uid == uid) {
                "Tài khoản đã thay đổi. Vui lòng thử lại."
            }

            user.updatePassword(newPassword).await()
        } else {
            // Thêm phương thức mật khẩu vào cùng tài khoản.
            val credential = EmailAuthProvider.getCredential(
                email,
                newPassword
            )

            user.linkWithCredential(credential).await()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Firebase gửi email chứa liên kết đặt lại mật khẩu.
    suspend fun sendPasswordResetEmail(email: String) {
        val address = email.trim()

        require(address.isNotBlank()) {
            "Vui lòng nhập email đã đăng ký."
        }

        auth.setLanguageCode("vi")
        auth.sendPasswordResetEmail(address).await()
    }
}