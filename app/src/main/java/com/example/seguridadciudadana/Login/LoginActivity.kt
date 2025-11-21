package com.example.seguridadciudadana.Login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.seguridadciudadana.Admin.AdminDashboardActivity
import com.example.seguridadciudadana.Pin.CreatePinActivity
import com.example.seguridadciudadana.Pin.UnlockPinActivity
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.Registro.RegisterActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // ==========================
        // SI YA ESTÁ LOGUEADO → REVISAR ROL DIRECTAMENTE
        // ==========================
        val currentUser = auth.currentUser
        if (currentUser != null) {
            verifyRoleFirst(currentUser.uid)
            return
        }

        // Configuración Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // UI
        val btnGoogle = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val txtRegistrarse = findViewById<TextView>(R.id.txtRegistrarse)
        val txtOlvidastePassword = findViewById<TextView>(R.id.txtOlvidastePassword)

        btnGoogle.setOnClickListener { signInWithGoogle() }

        btnIngresar.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else loginWithEmail(email, password)
        }

        txtRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        txtOlvidastePassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidad no implementada aún.", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== GOOGLE SIGN-IN ==========
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                if (authResult.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { verifyRoleFirst(it.uid) }
                } else {
                    Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Google Sign-In error: ${e.message}")
            Toast.makeText(this, "Error al conectar con Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        launcher.launch(googleSignInClient.signInIntent)
    }

    // ========== LOGIN EMAIL ==========
    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = it.user
                if (user != null) verifyRoleFirst(user.uid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
    }

    // ===========================================
    //     🔥 PASO MÁS IMPORTANTE
    //     PRIMERO VERIFICAMOS EL ROL
    // ===========================================
    private fun verifyRoleFirst(uid: String) {
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Usuario no registrado
                    FirebaseAuth.getInstance().signOut()
                    googleSignInClient.signOut()
                    Toast.makeText(this, "Tu cuenta no está registrada.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val rol = doc.getString("rol") ?: "user"

                if (rol == "admin") {
                    // ADMIN NO PASA POR PIN JAMÁS
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                } else {
                    // USER → recién verificamos si tiene PIN
                    checkPin(uid)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar rol.", Toast.LENGTH_SHORT).show()
            }
    }

    // ===========================================
    // SOLO PARA USUARIOS NORMALES
    // ===========================================
    private fun checkPin(uid: String) {
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.contains("pin")) {
                    startActivity(Intent(this, UnlockPinActivity::class.java))
                } else {
                    startActivity(Intent(this, CreatePinActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, CreatePinActivity::class.java))
                finish()
            }
    }
}
