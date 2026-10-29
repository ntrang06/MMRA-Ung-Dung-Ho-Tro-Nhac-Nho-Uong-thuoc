package com.example.project_lttbd.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseProvider {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val messaging: FirebaseMessaging by lazy {
        FirebaseMessaging.getInstance()
    }

    private const val USERS = "users"
    private const val MEDICINES = "medicines"
    private const val DEVICES = "devices"

    fun userDocument(uid: String): DocumentReference {
        require(uid.isNotBlank() && '/' !in uid) {
            "UID không được để trống hoặc chứa dấu /"
        }

        return firestore
            .collection(USERS)
            .document(uid)
    }

    fun medicinesCollection(uid: String): CollectionReference {
        return userDocument(uid).collection(MEDICINES)
    }

    fun devicesCollection(uid: String): CollectionReference {
        return userDocument(uid).collection(DEVICES)
    }
}