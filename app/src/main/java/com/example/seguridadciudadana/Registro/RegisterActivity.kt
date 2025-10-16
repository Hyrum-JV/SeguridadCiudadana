package com.example.seguridadciudadana.Registro

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.seguridadciudadana.Login.LoginActivity
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.Pin.CreatePinActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val GOOGLE_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Referencias UI
        val editNombre = findViewById<EditText>(R.id.editNombre)
        val editApellido = findViewById<EditText>(R.id.editApellido)
        val editCelular = findViewById<EditText>(R.id.editCelular)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnGoogleSignUp = findViewById<SignInButton>(R.id.btnGoogleSignUp)
        val txtIniciarSesion = findViewById<TextView>(R.id.txtIniciarSesion)

        // Registro manual
        btnRegistrar.setOnClickListener {
            val nombre = editNombre.text.toString().trim()
            val apellido = editApellido.text.toString().trim()
            val celular = editCelular.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid ?: return@addOnCompleteListener
                        val nombreCompleto = "$nombre $apellido"

                        val userMap = hashMapOf(
                            "nombre" to nombreCompleto,
                            "telefono" to celular,
                            "correo" to email,
                            "contraseña" to password
                        )

                        db.collection("usuarios").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                user.sendEmailVerification()
                                Toast.makeText(
                                    this,
                                    "Registro exitoso. Verifique su correo antes de iniciar sesión.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Registro con Google (solo registro, no login)
        btnGoogleSignUp.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
        }

        txtIniciarSesion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val user = auth.currentUser ?: return@addOnCompleteListener
                            val userId = user.uid

                            db.collection("usuarios").document(userId).get()
                                .addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        val userMap = hashMapOf(
                                            "nombre" to (user.displayName ?: ""),
                                            "telefono" to "",
                                            "correo" to (user.email ?: ""),
                                            "contraseña" to "" // No hay contraseña con Google
                                        )
                                        db.collection("usuarios").document(userId).set(userMap)
                                    }
                                }

                            Toast.makeText(this, "Registro con Google exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, CreatePinActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Error al registrar con Google", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: ApiException) {
                Toast.makeText(this, "Error al conectar con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
