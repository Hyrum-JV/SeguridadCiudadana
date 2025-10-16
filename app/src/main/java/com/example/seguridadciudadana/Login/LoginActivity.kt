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

        // Si ya hay un usuario autenticado, pasamos directamente
        val currentUser = auth.currentUser
        if (currentUser != null) {
            goToNextStep(currentUser.uid)
            return
        }

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ----------- VINCULAR OBJETOS DEL LAYOUT -----------
        val btnGoogle = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val txtRegistrarse = findViewById<TextView>(R.id.txtRegistrarse)
        val txtOlvidastePassword = findViewById<TextView>(R.id.txtOlvidastePassword)

        // ----------- LOGIN CON GOOGLE -----------
        btnGoogle.setOnClickListener { signInWithGoogle() }

        // ----------- LOGIN CON CORREO Y CONTRASEÑA -----------
        btnIngresar.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                loginWithEmail(email, password)
            }
        }

        // ----------- IR A REGISTRO -----------
        txtRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // ----------- OLVIDAR CONTRASEÑA (por ahora sin función) -----------
        txtOlvidastePassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidad no implementada aún.", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------- LOGIN CON GOOGLE -----------------------

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        val user = auth.currentUser
                        user?.let { checkIfUserExists(it.uid) }
                    } else {
                        Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Google Sign-In error: ${e.message}")
                Toast.makeText(this, "Error al conectar con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    // ---------------------- LOGIN CON CORREO Y CONTRASEÑA -----------------------

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    checkIfUserExists(user.uid)
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------------- VERIFICACIÓN EN FIRESTORE -----------------------

    private fun checkIfUserExists(uid: String) {
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    goToNextStep(uid)
                } else {
                    FirebaseAuth.getInstance().signOut()
                    googleSignInClient.signOut()
                    Toast.makeText(this, "Tu cuenta no está registrada. Regístrate primero.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar usuario.", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------------- FLUJO SIGUIENTE SEGÚN PIN -----------------------

    private fun goToNextStep(userId: String) {
        firestore.collection("usuarios").document(userId).get()
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
